"""
Analytics load for CommunityBoard.

Runs read-only queries against the live application tables and loads each
result into a table under the ``analytics`` schema for the dashboard to read:

  - analytics.summary            : total posts, comments, and users
  - analytics.category_counts    : post count per category
  - analytics.daily_post_counts  : post count per day over the last N days (gap-filled)
  - analytics.top_users          : top N users by post count

Each load is a full refresh inside one transaction: the schema and tables are
created if missing, every target table is truncated, and the freshly computed
rows are inserted with a shared ``generated_at`` UTC timestamp. The data volume
is tiny, so a truncate-and-reload is simpler and safer than incremental upserts.

Usage::

    python analytics.py                       # 30 days, top 5
    python analytics.py --days 7 --top 10

DB credentials come from .env (via the config package).
"""

from __future__ import annotations

import argparse
from datetime import datetime, timedelta, timezone

from sqlalchemy import text
from sqlalchemy.engine import Connection, Engine
from sqlalchemy.exc import SQLAlchemyError

from config import SEED
from utils import dispose_engine, get_engine, get_logger

logger = get_logger("analytics")

DEFAULT_DAYS = SEED.get("trend_window_days", 30)
DEFAULT_TOP = 5

ANALYTICS_SCHEMA = "analytics"


# --------------------------------------------------------------------------- #
# Queries (read-only, live source tables). Each returns a list of plain dicts.
# --------------------------------------------------------------------------- #
def posts_per_category(conn: Connection) -> list[dict]:
    """Post count per category. LEFT JOIN keeps zero-post categories at 0."""
    rows = conn.execute(
        text(
            """
            SELECT c.name AS category, COUNT(p.id) AS post_count
            FROM categories c
            LEFT JOIN posts p ON p.category_id = c.id
            GROUP BY c.name
            ORDER BY c.name
            """
        )
    ).all()
    return [{"category": r.category, "post_count": int(r.post_count)} for r in rows]


def posts_per_day(conn: Connection, days: int = DEFAULT_DAYS) -> list[dict]:
    """Post count per day for the last `days` days, gap-filled with zeros.

    `days` is passed as a real bind parameter (never string-interpolated).
    Missing days are filled in Python so the series is always continuous.
    """
    rows = conn.execute(
        text(
            """
            SELECT (p.created_at AT TIME ZONE 'UTC')::date AS day, COUNT(*) AS post_count
            FROM posts p
            WHERE p.created_at >= (NOW() AT TIME ZONE 'UTC') - make_interval(days => :days)
            GROUP BY day
            ORDER BY day
            """
        ),
        {"days": days},
    ).all()

    counts = {r.day: int(r.post_count) for r in rows}
    today = datetime.now(timezone.utc).date()
    start = today - timedelta(days=days - 1)
    return [
        {"day": (d := start + timedelta(days=i)), "post_count": counts.get(d, 0)}
        for i in range(days)
    ]


def top_contributors(conn: Connection, limit: int = DEFAULT_TOP) -> list[dict]:
    """Top `limit` users by post count. Tie-break on name for determinism."""
    rows = conn.execute(
        text(
            """
            SELECT u.id, u.name, u.email, COUNT(p.id) AS post_count
            FROM users u
            JOIN posts p ON p.author_id = u.id
            GROUP BY u.id, u.name, u.email
            ORDER BY post_count DESC, u.name ASC
            LIMIT :limit
            """
        ),
        {"limit": limit},
    ).all()
    return [
        {
            "user_id": int(r.id),
            "name": r.name,
            "email": r.email,
            "post_count": int(r.post_count),
        }
        for r in rows
    ]


def totals(conn: Connection) -> list[dict]:
    """Overall totals: posts, comments, users. Single-row dataset."""
    row = conn.execute(
        text(
            """
            SELECT
                (SELECT COUNT(*) FROM posts)    AS total_posts,
                (SELECT COUNT(*) FROM comments) AS total_comments,
                (SELECT COUNT(*) FROM users)    AS total_users
            """
        )
    ).one()
    return [
        {
            "total_posts": int(row.total_posts),
            "total_comments": int(row.total_comments),
            "total_users": int(row.total_users),
        }
    ]


# --------------------------------------------------------------------------- #
# Analytics schema (target tables). Created if missing; matches the agreed DDL.
# --------------------------------------------------------------------------- #
SCHEMA_SQL = f"""
CREATE SCHEMA IF NOT EXISTS {ANALYTICS_SCHEMA};

CREATE TABLE IF NOT EXISTS {ANALYTICS_SCHEMA}.category_counts (
    id           BIGSERIAL PRIMARY KEY,
    category     VARCHAR(255) NOT NULL,
    post_count   INTEGER      NOT NULL,
    generated_at TIMESTAMP    NOT NULL
);

CREATE TABLE IF NOT EXISTS {ANALYTICS_SCHEMA}.daily_post_counts (
    id           BIGSERIAL PRIMARY KEY,
    day          DATE      NOT NULL,
    post_count   INTEGER   NOT NULL,
    generated_at TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_daily_post_counts_day
    ON {ANALYTICS_SCHEMA}.daily_post_counts (day);

CREATE TABLE IF NOT EXISTS {ANALYTICS_SCHEMA}.top_users (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    name         VARCHAR(255) NOT NULL,
    email        VARCHAR(255) NOT NULL,
    post_count   INTEGER      NOT NULL,
    generated_at TIMESTAMP    NOT NULL
);

CREATE TABLE IF NOT EXISTS {ANALYTICS_SCHEMA}.summary (
    id             BIGSERIAL PRIMARY KEY,
    total_posts    INTEGER   NOT NULL,
    total_comments INTEGER   NOT NULL,
    total_users    INTEGER   NOT NULL,
    generated_at   TIMESTAMP NOT NULL
);
"""

# Target table -> ordered column list (excludes id/generated_at, added by loader).
# Keys must match the dicts returned by the query above.
LOAD_COLUMNS = {
    "category_counts": ["category", "post_count"],
    "daily_post_counts": ["day", "post_count"],
    "top_users": ["user_id", "name", "email", "post_count"],
    "summary": ["total_posts", "total_comments", "total_users"],
}


def _load_table(conn: Connection, table: str, records: list[dict], generated_at: datetime) -> None:
    """Truncate `analytics.<table>` and insert `records`, stamping generated_at."""
    qualified = f"{ANALYTICS_SCHEMA}.{table}"
    conn.execute(text(f"TRUNCATE TABLE {qualified} RESTART IDENTITY"))
    if not records:
        logger.info("Loaded analytics table (empty)", extra={"table": qualified})
        return

    cols = LOAD_COLUMNS[table]
    all_cols = [*cols, "generated_at"]
    placeholders = ", ".join(f":{c}" for c in all_cols)
    stmt = text(f"INSERT INTO {qualified} ({', '.join(all_cols)}) VALUES ({placeholders})")
    conn.execute(stmt, [{**r, "generated_at": generated_at} for r in records])
    logger.info("Loaded analytics table", extra={"rows": len(records), "table": qualified})


# --------------------------------------------------------------------------- #
# Orchestration
# --------------------------------------------------------------------------- #
def run_load(engine: Engine, days: int, limit: int) -> None:
    """Compute all datasets and load them into the analytics schema (one txn)."""
    generated_at = datetime.now(timezone.utc)
    with engine.begin() as conn:  # all-or-nothing: schema, truncates, inserts
        conn.execute(text(SCHEMA_SQL))
        _load_table(conn, "summary", totals(conn), generated_at)
        _load_table(conn, "category_counts", posts_per_category(conn), generated_at)
        _load_table(conn, "daily_post_counts", posts_per_day(conn, days), generated_at)
        _load_table(conn, "top_users", top_contributors(conn, limit), generated_at)
    logger.info("Analytics load complete", extra={"generated_at": generated_at.isoformat()})


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Load CommunityBoard analytics into the DB.")
    parser.add_argument("--days", type=int, default=DEFAULT_DAYS, help="Trend window in days.")
    parser.add_argument("--top", type=int, default=DEFAULT_TOP, help="Number of top contributors.")
    return parser.parse_args(argv)


def main() -> None:
    args = parse_args()
    engine = get_engine()
    try:
        run_load(engine, args.days, args.top)
    except SQLAlchemyError:
        logger.error("Analytics load failed (database error)", exc_info=True)
        raise SystemExit(1)
    finally:
        dispose_engine()


if __name__ == "__main__":
    main()

"""
Analytics dataset export for CommunityBoard.

Runs three read-only queries against the live application tables and writes
each result as a JSON flat file for the dashboard frontend to consume:

  - posts_per_category.json  : post count per category (NEWS/EVENT/DISCUSSION/ALERT)
  - posts_per_day.json       : post count per day over the last N days (gap-filled)
  - top_contributors.json    : top N users by post count

Each file has the shape::

    {"generated_at": "<iso8601 utc>", "count": <n>, "data": [ ... ]}

Files are written atomically (temp file + os.replace) so the frontend never
reads a half-written file.

Usage::

    python analytics.py                       # 30 days, top 5, default output dir
    python analytics.py --days 7 --top 10
    python analytics.py --output-dir /data/analytics

DB credentials come from .env (via the config package). Output dir defaults to
ANALYTICS_OUTPUT_DIR (config), overridable with --output-dir.
"""

from __future__ import annotations

import argparse
import json
import os
from datetime import datetime, timedelta, timezone
from pathlib import Path

from sqlalchemy import text
from sqlalchemy.engine import Connection, Engine
from sqlalchemy.exc import SQLAlchemyError

from config import ANALYTICS_OUTPUT_DIR, SEED
from utils import dispose_engine, get_engine, get_logger

logger = get_logger("analytics")

DEFAULT_DAYS = SEED.get("trend_window_days", 30)
DEFAULT_TOP = 5


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
        {"day": (d := start + timedelta(days=i)).isoformat(), "post_count": counts.get(d, 0)}
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
            "id": int(r.id),
            "name": r.name,
            "email": r.email,
            "post_count": int(r.post_count),
        }
        for r in rows
    ]


# --------------------------------------------------------------------------- #
# JSON export
# --------------------------------------------------------------------------- #
def _envelope(records: list[dict]) -> dict:
    return {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "count": len(records),
        "data": records,
    }


def export_json(records: list[dict], filename: str, output_dir: Path) -> Path:
    """Write `records` (wrapped in an envelope) to output_dir/filename atomically."""
    output_dir.mkdir(parents=True, exist_ok=True)
    final = output_dir / filename
    tmp = final.with_suffix(final.suffix + ".tmp")

    with tmp.open("w", encoding="utf-8") as fh:
        json.dump(_envelope(records), fh, ensure_ascii=False, indent=2, default=str)
        fh.flush()
        os.fsync(fh.fileno())
    os.replace(tmp, final)  # atomic on POSIX

    logger.info("Wrote analytics file", extra={"rows": len(records), "path": str(final)})
    return final


# --------------------------------------------------------------------------- #
# Orchestration
# --------------------------------------------------------------------------- #
def run_export(engine: Engine, days: int, limit: int, output_dir: Path) -> None:
    """Run all three queries on one connection and export their JSON files."""
    with engine.connect() as conn:
        export_json(posts_per_category(conn), "posts_per_category.json", output_dir)
        export_json(posts_per_day(conn, days), "posts_per_day.json", output_dir)
        export_json(top_contributors(conn, limit), "top_contributors.json", output_dir)
    logger.info("Analytics export complete", extra={"output_dir": str(output_dir)})


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Export CommunityBoard analytics as JSON.")
    parser.add_argument("--days", type=int, default=DEFAULT_DAYS, help="Trend window in days.")
    parser.add_argument("--top", type=int, default=DEFAULT_TOP, help="Number of top contributors.")
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=ANALYTICS_OUTPUT_DIR,
        help="Directory to write JSON files into.",
    )
    return parser.parse_args(argv)


def main() -> None:
    args = parse_args()
    engine = get_engine()
    try:
        run_export(engine, args.days, args.top, args.output_dir)
    except SQLAlchemyError:
        logger.error("Analytics export failed (database error)", exc_info=True)
        raise SystemExit(1)
    except OSError:
        logger.error("Analytics export failed (file write error)", exc_info=True)
        raise SystemExit(1)
    finally:
        dispose_engine()


if __name__ == "__main__":
    main()

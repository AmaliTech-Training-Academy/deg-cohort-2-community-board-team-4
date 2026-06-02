"""
Database bootstrap for CommunityBoard (data-engineering).

Creates the target database (if it does not exist) and its tables so
scripts/seed.py has something to write into. Schema mirrors the backend's
canonical Flyway migration (backend/.../db/migration/V1__init_schema.sql).

This is a convenience for local data-engineering work. In environments where
the backend runs Flyway, the backend owns the schema — do not run this there.

Usage:
    python scripts/init_db.py            # create DB + tables if missing
    python scripts/init_db.py --drop     # drop the 4 tables first, then recreate

DB credentials come from .env (via the config package).
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

from sqlalchemy import create_engine, text
from sqlalchemy.engine import Engine

# Allow `python scripts/init_db.py` to import sibling packages from the root.
sys.path.append(str(Path(__file__).resolve().parent.parent))
from config import APP_ENV, DATABASE_URL, DB_CONFIG, IS_PRODUCTION  # noqa: E402
from utils import get_logger  # noqa: E402

logger = get_logger("init_db")

# Tables created/dropped in dependency order (children last to create,
# first to drop) to satisfy foreign-key constraints.
TABLE_ORDER = ["users", "categories", "posts", "comments"]

# Schema mirrors backend V1__init_schema.sql exactly (posts.slug, no
# comments.updated_at). Keep in sync if the backend migration changes.
SCHEMA_SQL = """
CREATE TABLE IF NOT EXISTS users (
    id         BIGSERIAL PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    name       VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(50)  NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS posts (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    slug        VARCHAR(255) NOT NULL UNIQUE,
    content     TEXT NOT NULL,
    category_id BIGINT REFERENCES categories (id),
    author_id   BIGINT NOT NULL REFERENCES users (id),
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP
);

CREATE TABLE IF NOT EXISTS comments (
    id         BIGSERIAL PRIMARY KEY,
    content    TEXT NOT NULL,
    post_id    BIGINT NOT NULL REFERENCES posts (id),
    author_id  BIGINT NOT NULL REFERENCES users (id),
    created_at TIMESTAMP
);
"""


def _admin_url() -> str:
    """URL to the maintenance `postgres` database (target may not exist yet)."""
    return (
        f"postgresql://{DB_CONFIG['user']}:{DB_CONFIG['password']}"
        f"@{DB_CONFIG['host']}:{DB_CONFIG['port']}/postgres"
    )


def create_database() -> None:
    """Create the target database if it does not already exist.

    CREATE DATABASE cannot run inside a transaction block, so connect with
    AUTOCOMMIT against the maintenance `postgres` database.
    """
    db_name = DB_CONFIG["database"]
    engine = create_engine(_admin_url(), isolation_level="AUTOCOMMIT")
    try:
        with engine.connect() as conn:
            exists = conn.execute(
                text("SELECT 1 FROM pg_database WHERE datname = :name"),
                {"name": db_name},
            ).scalar()
            if exists:
                logger.info("Database %r already exists", db_name)
                return
            # Identifier cannot be parameterized; db_name comes from trusted .env.
            conn.execute(text(f'CREATE DATABASE "{db_name}"'))
            logger.info("Created database %r", db_name)
    finally:
        engine.dispose()


def drop_tables(conn) -> None:
    """Drop the 4 tables (CASCADE handles FKs), children first."""
    logger.info("Dropping tables: %s", ", ".join(reversed(TABLE_ORDER)))
    for table in reversed(TABLE_ORDER):
        conn.execute(text(f"DROP TABLE IF EXISTS {table} CASCADE"))


def create_tables(engine: Engine, drop: bool) -> None:
    with engine.begin() as conn:  # single transaction: all-or-nothing
        if drop:
            drop_tables(conn)
        conn.execute(text(SCHEMA_SQL))
    logger.info("Tables ready: %s", ", ".join(TABLE_ORDER))


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Create the CommunityBoard database and tables.")
    parser.add_argument(
        "--drop",
        action="store_true",
        help="Drop the 4 tables before recreating them.",
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="Override the production safety guard for --drop (DANGEROUS).",
    )
    return parser.parse_args(argv)


def guard_drop(drop: bool, force: bool) -> None:
    """Refuse to drop tables in production unless explicitly forced.

    Plain init (CREATE TABLE IF NOT EXISTS) is non-destructive and allowed
    everywhere; only --drop destroys data.
    """
    if drop and IS_PRODUCTION and not force:
        logger.error(
            "Refusing to --drop tables: APP_ENV=%r. This destroys all data. "
            "Pass --force only if you are certain.",
            APP_ENV,
        )
        raise SystemExit(1)
    if drop and IS_PRODUCTION and force:
        logger.warning("APP_ENV=production but --force given; dropping tables.")


def main() -> None:
    args = parse_args()
    guard_drop(args.drop, args.force)
    create_database()
    engine = create_engine(DATABASE_URL)
    try:
        create_tables(engine, args.drop)
    finally:
        engine.dispose()
    logger.info("Init complete.")


if __name__ == "__main__":
    main()

"""
Sample Data Generator for CommunityBoard.

Seeds the application database with realistic test data so all roles can
exercise the app:
  - users (mix of USER / ADMIN)
  - categories (NEWS, EVENT, DISCUSSION, ALERT)
  - 50+ posts distributed evenly across categories
  - 200+ comments spread across posts
  - created_at varied across the last 30 days for trend analysis

Usage:
    python scripts/seed.py                  # default volumes
    python scripts/seed.py --posts 80 --comments 300
    python scripts/seed.py --reset          # wipe seeded tables first

DB credentials come from .env; volumes/categories/seed from
config/settings.toml (both via the config package). CLI flags override.
"""
from __future__ import annotations

import argparse
import random
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path

from faker import Faker
from sqlalchemy import text
from sqlalchemy.engine import Engine

# Allow `python scripts/seed.py` to import sibling modules from the package root.
sys.path.append(str(Path(__file__).resolve().parent.parent))
from config import APP_ENV, IS_PRODUCTION, SEED  # noqa: E402
from utils import dispose_engine, get_engine, get_logger  # noqa: E402

logger = get_logger("seed")

# Defaults sourced from the [seed] table in config/settings.toml.
DEFAULT_USERS = SEED.get("users", 15)
DEFAULT_POSTS = SEED.get("posts", 52)  # >= 50, divisible by categories
DEFAULT_COMMENTS = SEED.get("comments", 220)  # >= 200
TREND_WINDOW_DAYS = SEED.get("trend_window_days", 30)
CATEGORIES = [(c["name"], c["description"]) for c in SEED.get("categories", [])]

# Deterministic output so reruns are reproducible.
_RANDOM_SEED = SEED.get("random_seed", 42)
faker = Faker()
Faker.seed(_RANDOM_SEED)
random.seed(_RANDOM_SEED)

# bcrypt hash of "password123" — placeholder so seeded users can log in.
SAMPLE_PASSWORD_HASH = "$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewKjp/SyqxR8Eq."


def random_datetime(within_days: int = TREND_WINDOW_DAYS) -> datetime:
    """A timezone-aware datetime uniformly within the last `within_days`."""
    now = datetime.now(timezone.utc)
    delta = timedelta(
        days=random.randint(0, within_days - 1),
        hours=random.randint(0, 23),
        minutes=random.randint(0, 59),
    )
    return now - delta


def reset_tables(conn) -> None:
    """Truncate seeded tables and restart identity sequences."""
    # Categories are NOT truncated — they are provisioned outside the seeder
    # and posts reference their existing ids.
    logger.info("Resetting tables (comments, posts, users)")
    conn.execute(text("TRUNCATE comments, posts, users " "RESTART IDENTITY CASCADE"))


def seed_categories(conn) -> list[int]:
    """Look up existing category ids — do NOT insert.

    Categories are provisioned by the application (migrations/prod data), so
    seeding only reads their ids to attach posts. Errors out if a configured
    category is missing rather than silently inventing one.
    """
    ids: list[int] = []
    for name, _description in CATEGORIES:
        row = conn.execute(
            text("SELECT id FROM categories WHERE LOWER(name) = LOWER(:name)"),
            {"name": name},
        ).one_or_none()
        if row is None:
            raise SystemExit(
                f"Category {name!r} not found in DB. Categories must exist " "before seeding posts."
            )
        ids.append(row.id)
    logger.info("Found %d existing categories", len(ids))
    return ids


def seed_users(conn, count: int) -> list[int]:
    ids: list[int] = []
    for i in range(count):
        created = random_datetime()
        # First user is an ADMIN so reviewers always have an admin account.
        role = "ADMIN" if i == 0 else random.choices(["USER", "ADMIN"], weights=[9, 1])[0]
        row = conn.execute(
            text(
                "INSERT INTO users (email, name, password, role, created_at, updated_at) "
                "VALUES (:email, :name, :password, :role, :created_at, :updated_at) "
                "RETURNING id"
            ),
            {
                "email": faker.unique.email(),
                "name": faker.name(),
                "password": SAMPLE_PASSWORD_HASH,
                "role": role,
                "created_at": created,
                "updated_at": created,
            },
        ).one()
        ids.append(row.id)
    logger.info("Seeded %d users", len(ids))
    return ids


def seed_posts(conn, count: int, category_ids: list[int], user_ids: list[int]) -> list[int]:
    ids: list[int] = []
    for i in range(count):
        # Round-robin keeps categories evenly distributed.
        category_id = category_ids[i % len(category_ids)]
        created = random_datetime()
        title = faker.sentence(nb_words=6).rstrip(".")
        slug = f"{faker.slug()}-{i}"  # suffix guarantees uniqueness
        row = conn.execute(
            text(
                "INSERT INTO posts "
                "(title, slug, content, category_id, author_id, created_at, updated_at) "
                "VALUES (:title, :slug, :content, :category_id, :author_id, "
                ":created_at, :updated_at) "
                "RETURNING id"
            ),
            {
                "title": title,
                "slug": slug,
                "content": faker.paragraph(nb_sentences=5),
                "category_id": category_id,
                "author_id": random.choice(user_ids),
                "created_at": created,
                "updated_at": created,
            },
        ).one()
        ids.append(row.id)
    logger.info("Seeded %d posts (even across %d categories)", len(ids), len(category_ids))
    return ids


def seed_comments(conn, count: int, post_ids: list[int], user_ids: list[int]) -> None:
    for _ in range(count):
        created = random_datetime()
        conn.execute(
            text(
                "INSERT INTO comments "
                "(content, post_id, author_id, created_at) "
                "VALUES (:content, :post_id, :author_id, :created_at)"
            ),
            {
                "content": faker.paragraph(nb_sentences=2),
                "post_id": random.choice(post_ids),
                "author_id": random.choice(user_ids),
                "created_at": created,
            },
        )
    logger.info("Seeded %d comments", count)


def run(engine: Engine, users: int, posts: int, comments: int, reset: bool) -> None:
    with engine.begin() as conn:  # single transaction: all-or-nothing
        if reset:
            reset_tables(conn)
        category_ids = seed_categories(conn)
        user_ids = seed_users(conn, users)
        post_ids = seed_posts(conn, posts, category_ids, user_ids)
        seed_comments(conn, comments, post_ids, user_ids)
    logger.info("Seed complete.")


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Seed CommunityBoard with sample data.")
    parser.add_argument("--users", type=int, default=DEFAULT_USERS)
    parser.add_argument("--posts", type=int, default=DEFAULT_POSTS)
    parser.add_argument("--comments", type=int, default=DEFAULT_COMMENTS)
    parser.add_argument(
        "--reset", action="store_true", help="Truncate seeded tables before inserting."
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="Override the production safety guard (DANGEROUS).",
    )
    return parser.parse_args(argv)


def guard_environment(force: bool) -> None:
    """Refuse to seed fake data into production unless explicitly forced.

    seed.py writes Faker-generated rows with a shared, publicly-known
    password hash — never safe for a real production database.
    """
    if IS_PRODUCTION and not force:
        logger.error(
            "Refusing to seed: APP_ENV=%r. Seeding writes fake data with a "
            "shared password hash. Pass --force only if you are certain.",
            APP_ENV,
        )
        raise SystemExit(1)
    if IS_PRODUCTION and force:
        logger.warning("APP_ENV=production but --force given; seeding anyway.")


def main() -> None:
    args = parse_args()
    guard_environment(args.force)
    engine = get_engine()
    try:
        run(engine, args.users, args.posts, args.comments, args.reset)
    finally:
        dispose_engine()


if __name__ == "__main__":
    main()

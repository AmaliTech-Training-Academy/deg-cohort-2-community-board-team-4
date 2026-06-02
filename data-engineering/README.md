# CommunityBoard — Data Engineering

ETL pipeline and data tooling for CommunityBoard analytics.

## Setup

```bash
cd data-engineering
python -m venv .venv && source .venv/bin/activate
pip install -e .            # installs deps from pyproject.toml

cp .env.example .env        # fill in your DB credentials
```

## Configuration

- **`.env`** — secrets only (DB credentials). Never committed.
- **`config/`** — config package. `config/__init__.py` is the loader (exposes
  `DATABASE_URL`, `LOGGING`, `SEED`); `config/settings.toml` holds non-secret
  `[logging]` and `[seed]` settings + categories. Env vars `LOG_LEVEL` /
  `LOG_FORMAT` override the `[logging]` table per-deploy.

## Sample Data Generator (`scripts/seed.py`)

Seeds the application database with realistic test data so every role can
test against meaningful volumes:

- Mix of `USER` / `ADMIN` accounts (first user is always `ADMIN`)
- 4 categories: `NEWS`, `EVENT`, `DISCUSSION`, `ALERT`
- **50+ posts** distributed **evenly** across the categories (round-robin)
- **200+ comments** spread across posts
- `created_at` varied across the **last 30 days** for trend analysis

Default volumes, categories, and the random seed come from the `[seed]` table
in `config/settings.toml`. Output is deterministic, so reruns produce the same data.

### Usage

```bash
# Default volumes: 15 users, 52 posts, 220 comments
python scripts/seed.py

# Custom volumes
python scripts/seed.py --users 25 --posts 80 --comments 300

# Wipe seeded tables first (TRUNCATE ... RESTART IDENTITY CASCADE)
python scripts/seed.py --reset
```

Reads `DATABASE_URL` from the [config](config/) package (loaded from `.env`). All
inserts run in a single transaction — it's all-or-nothing.

> Seeded users share the bcrypt hash of `password123` for local login.

## Utilities (`utils/`)

Shared helpers for the package.

- `utils/logging.py` — production-grade logging via `get_logger(name)`.
  Reads the `[logging]` table from `config/settings.toml`; env vars override:
  - `level` / `LOG_LEVEL` (default `INFO`)
  - `format` / `LOG_FORMAT` (`text` for local, `json` for log aggregators)

  ```python
  from utils import get_logger
  logger = get_logger(__name__)
  logger.info("ready")
  ```

## ETL Pipeline (`etl_pipeline.py`)

```bash
python etl_pipeline.py
```

Extracts posts/comments from the app DB, transforms into analytics tables.

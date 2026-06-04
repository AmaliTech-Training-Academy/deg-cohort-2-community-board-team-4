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

Runs the analytics load (below) against the app DB. Exits non-zero on failure.

## Analytics Load (`analytics.py`)

Runs read-only queries against the live application tables and loads each
result into a table under the `analytics` schema for the dashboard to read:

| Table | Dataset |
|------|---------|
| `analytics.summary` | Total posts, comments, and users |
| `analytics.category_counts` | Post count per category (NEWS/EVENT/DISCUSSION/ALERT) |
| `analytics.daily_post_counts` | Post count per day over the last N days (gap-filled with zeros) |
| `analytics.top_users` | Top N users by post count |

The load is a **full refresh in one transaction**: the schema and tables are
created if missing, every target table is `TRUNCATE ... RESTART IDENTITY`'d,
and the freshly computed rows are inserted with a shared `generated_at` UTC
timestamp. At this data volume a truncate-and-reload is simpler and safer than
incremental upserts, and it's idempotent — rerunning yields the same rows.

### Usage

```bash
python analytics.py                     # 30 days, top 5
python analytics.py --days 7 --top 10
```

## Scheduling (daily, in-container)

The pipeline runs as a long-lived service that does a full idempotent refresh
once on startup, then every 24h. At this data volume that's the right tool — no
cron daemon, no orchestrator, no extra moving parts. The whole schedule is the
loop in [`scripts/run_etl.sh`](scripts/run_etl.sh), which is the image's `CMD`.

It starts automatically with the stack:

```bash
docker compose up -d           # data-engineering runs the ETL, then loops daily
docker compose logs -f data-engineering
```

The service `depends_on` postgres being healthy, and `restart: unless-stopped`
supervises it. DB credentials come from the compose environment (`DB_HOST`,
`DB_USER`, …), not a baked-in `.env`.

- **Interval** is overridable via the `ETL_INTERVAL` env var (seconds, default
  `86400`). Drop it low — e.g. `ETL_INTERVAL=60` — to watch repeated runs locally.
- A failed run is logged and retried next cycle; it never kills the scheduler.
- For a one-shot run instead of the loop: `docker compose run --rm
  data-engineering python etl_pipeline.py`.

> Run it on the host without Docker for dev with `python etl_pipeline.py` (or
> `bash scripts/run_etl.sh` for the loop) — see [Setup](#setup).

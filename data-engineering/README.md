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

Extracts posts/comments from the app DB, transforms into analytics tables,
then runs the analytics export (below). Exits non-zero on failure.

## Analytics Export (`analytics.py`)

Runs three read-only queries against the live tables and writes one JSON file
per dataset for the dashboard frontend:

| File | Dataset |
|------|---------|
| `totals.json` | Total posts, comments, and users |
| `posts_per_category.json` | Post count per category (NEWS/EVENT/DISCUSSION/ALERT) |
| `posts_per_day.json` | Post count per day over the last N days (gap-filled with zeros) |
| `top_contributors.json` | Top N users by post count |

Each file has the shape:

```json
{ "generated_at": "<iso8601 utc>", "count": 4, "data": [ ... ] }
```

Files are written **atomically** (temp file + `os.replace`), so the frontend
never reads a half-written file.

### Usage

```bash
python analytics.py                     # 30 days, top 5, default output dir
python analytics.py --days 7 --top 10
python analytics.py --output-dir /data/analytics
```

Output dir defaults to `output/`, overridable via `--output-dir` or the
`ANALYTICS_OUTPUT_DIR` env var (e.g. a mounted volume in containers). The
`output/` directory is git-ignored.

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

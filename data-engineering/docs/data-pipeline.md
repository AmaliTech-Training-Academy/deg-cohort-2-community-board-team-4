# ETL Pipeline

How CommunityBoard turns raw application data into the analytics datasets the
dashboard reads.

The pipeline lives in [`data-engineering/etl_pipeline.py`](../data-engineering/etl_pipeline.py)
and is the container's entrypoint (`python etl_pipeline.py`).

---

## How it runs

### With Docker Compose (recommended)

The `data-engineering` service runs the pipeline automatically:

```bash
docker compose up
```

What happens:

1. Postgres starts and becomes healthy.
2. The `data-engineering` service waits for Postgres (`depends_on: condition: service_healthy`).
3. It runs `python etl_pipeline.py` **once**, then the container exits.

It is a **batch job, not a long-running service** — it runs to completion and
stops. Re-run it any time with `docker compose up data-engineering`.

> Note: the pipeline reads whatever is in the database. To get meaningful
> output, seed data first (see [the seed script](../data-engineering/README.md#sample-data-generator-scriptsseedpy)).

### Locally (without Docker)

```bash
cd data-engineering
python etl_pipeline.py
```

Reads `DATABASE_URL` from your `.env`. Exits non-zero on failure.

---

## Pipeline steps

The whole flow is in `run_pipeline()`. It is one transaction-safe sequence:
**Extract → Transform → Load → Export**.

### 1. Extract

Read raw rows from the live application tables into pandas DataFrames.

| Function           | Reads from                | Returns                                                  |
|--------------------|---------------------------|----------------------------------------------------------|
| `extract_posts()`  | `posts`, `users`, `categories` | One row per post, with author name/email and category. |
| `extract_comments()` | `comments`, `users`     | One row per comment, with post ID and author name.       |

### 2. Transform

Reshape the raw data into analytics-ready form.

| Function                     | Input        | Output                                              |
|------------------------------|--------------|-----------------------------------------------------|
| `transform_daily_activity()` | posts        | Post count grouped by `date` and `category`.        |

### 3. Load

Write the transformed table back into the database for analytics use.

| Function          | Writes to                  | Mode                          |
|-------------------|----------------------------|-------------------------------|
| `load_analytics()` | `analytics_daily_activity` | Replaces the table each run.  |

### 4. Export

Run the read-only analytics queries and write the dashboard JSON files. This
step calls `run_export()` from [`analytics.py`](../data-engineering/analytics.py).

Output files (see the [data dictionary](../data-engineering/docs/DATA_DICTIONARY.md)):

- `totals.json`
- `posts_per_category.json`
- `posts_per_day.json`
- `top_contributors.json`

On any error the pipeline logs the failure and exits with code `1`. The database
connection is always closed (`finally`).

---

## What is and isn't in the pipeline yet

The pipeline runs end to end, but two pieces are stubbed:

- **User engagement is not loaded.** `transform_user_engagement()` exists but is
  not called by `run_pipeline()`, and its comment-merge / engagement-score logic
  is a `TODO`. It produces nothing today.
- **Only one analytics table is written.** Load writes
  `analytics_daily_activity` only.

Important: the **Export step reads the live source tables directly**, not the
`analytics_daily_activity` table. So the dashboard JSON does not depend on the
Load step — the two paths are independent today.

---

## Data flow at a glance

```
Postgres (posts, comments, users, categories)
        │
        ▼
   Extract  ──► posts_df, comments_df
        │
        ▼
  Transform ──► daily activity (by date + category)
        │
        ▼
    Load    ──► analytics_daily_activity  (table in Postgres)

   Export   ──► reads LIVE source tables ──► output/*.json  (for the dashboard)
```

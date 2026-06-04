# syntax=docker/dockerfile:1

# ---------- Stage 1: build dependencies (uv) ----------
# Build the venv on the SAME base image as the runtime stage (python:3.11-slim)
# so the venv's interpreter symlinks stay valid when copied across.
FROM python:3.11-slim AS build
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    UV_PROJECT_ENVIRONMENT=/opt/venv \
    UV_PYTHON=python3.11 \
    UV_PYTHON_DOWNLOADS=never \
    UV_COMPILE_BYTECODE=1 \
    UV_LINK_MODE=copy
WORKDIR /app

# Bring in the uv binary (static). Pin a version for full reproducibility if you
# want, e.g. ghcr.io/astral-sh/uv:0.5 — dependency versions are pinned by
# uv.lock regardless of the uv version.
COPY --from=ghcr.io/astral-sh/uv:latest /uv /usr/local/bin/uv

# Resolve deps first (cached unless pyproject.toml / uv.lock change). Install
# ONLY the locked runtime deps into /opt/venv — not the project package itself
# (the code is copied into the runtime image) and not the dev tools (--no-dev).
COPY pyproject.toml uv.lock ./
RUN --mount=type=cache,target=/root/.cache/uv \
    uv sync --frozen --no-install-project --no-dev

# ---------- Stage 2: runtime ----------
FROM python:3.11-slim
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PATH="/opt/venv/bin:$PATH"
WORKDIR /app

# Dedicated non-root user/group. bash powers the ETL scheduler loop
# (scripts/run_etl.sh) — python:slim ships dash as /bin/sh, not bash.
RUN apt-get update && apt-get install -y --no-install-recommends bash \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app && useradd --system --gid app --no-create-home app

# Bring over the prebuilt venv, then the application code.
COPY --from=build /opt/venv /opt/venv
COPY --chown=app:app . .

USER app
# Startup: optionally seed sample data (idempotent), then hand off to the ETL
# scheduler loop (scripts/run_etl.sh) which keeps the container Up so compose's
# restart policy supervises it.
#
# Seeding runs ONLY when RUN_SEED=true (set per-environment in compose, never in
# prod). It is idempotent via a COUNT THRESHOLD on comments: Flyway's migrations
# already seed a small number of comments (~couple dozen), and seed.py adds 220
# more — so "comments < 150" means the bulk Faker seed has NOT run yet. A plain
# "> 0" check would never seed, because Flyway fills comments first.
# This stops restarts from re-seeding (after seeding, count is ~245 >= 150).
# seed.py also refuses to run when APP_ENV=production, so prod is guarded twice.
# `exec` hands PID 1 to run_etl.sh so it receives stop signals.
CMD ["bash", "-c", "if [ \"$RUN_SEED\" = \"true\" ]; then if python -c \"import sys; from sqlalchemy import text; from utils import get_engine; sys.exit(0 if get_engine().connect().execute(text('SELECT count(*) FROM comments')).scalar() < 220 else 1)\"; then echo 'seeding sample data'; python scripts/seed.py; else echo 'already seeded; skipping'; fi; fi; exec bash scripts/run_etl.sh"]

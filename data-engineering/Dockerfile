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
# Run the ETL once on startup, then on a daily loop — keeps the container Up so
# compose's restart policy supervises it. See scripts/run_etl.sh.
CMD ["bash", "scripts/run_etl.sh"]

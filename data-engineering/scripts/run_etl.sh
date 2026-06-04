#!/usr/bin/env bash
# CommunityBoard ETL scheduler — the container's entrypoint (see Dockerfile CMD).
#
# Runs the pipeline once on startup (so `docker compose up` produces fresh
# analytics immediately), then every ETL_INTERVAL seconds (default 24h). The
# data is tiny and the load is a full idempotent refresh, so a simple loop is
# the right tool — no cron daemon, no orchestrator, no extra packages.
#
# The process stays in the foreground forever, which is what keeps the container
# Up (CI smoke check) and lets compose's `restart: unless-stopped` supervise it.
set -uo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

INTERVAL="${ETL_INTERVAL:-86400}"   # seconds between runs; 86400 = 24h

while true; do
  echo "===== ETL run: $(date -Iseconds) ====="
  # Don't let one failed run kill the scheduler — log it and retry next cycle.
  python etl_pipeline.py || echo "ETL run FAILED (exit $?), retrying in ${INTERVAL}s"
  echo "===== sleeping ${INTERVAL}s until next run ====="
  sleep "$INTERVAL"
done

"""
Production-grade logging configuration.

Emits JSON logs when format=json (best for log aggregators like Loki,
CloudWatch, ELK) and human-readable logs otherwise. Settings come from the
`[logging]` table in config/settings.toml; env vars LOG_LEVEL / LOG_FORMAT override
(useful for per-deploy tweaks). Configuration is idempotent — calling
get_logger repeatedly will not attach duplicate handlers.
"""
from __future__ import annotations

import json
import logging
import os
import sys
from datetime import datetime, timezone

try:
    from config import LOGGING as _LOGGING_CONFIG
except Exception:  # config import may fail in isolated tests
    _LOGGING_CONFIG = {}

_CONFIGURED = False


class JsonFormatter(logging.Formatter):
    """Render log records as single-line JSON for machine ingestion."""

    def format(self, record: logging.LogRecord) -> str:
        payload = {
            "timestamp": datetime.fromtimestamp(
                record.created, tz=timezone.utc
            ).isoformat(),
            "level": record.levelname,
            "logger": record.name,
            "message": record.getMessage(),
        }
        if record.exc_info:
            payload["exception"] = self.formatException(record.exc_info)
        # Surface any structured fields passed via `extra=`.
        for key, value in record.__dict__.items():
            if key not in _RESERVED and not key.startswith("_"):
                payload[key] = value
        return json.dumps(payload, default=str)


# LogRecord attributes that are not custom `extra` fields.
_RESERVED = set(logging.makeLogRecord({}).__dict__) | {"message", "asctime"}


def _configure_root() -> None:
    global _CONFIGURED
    if _CONFIGURED:
        return

    # config/settings.toml provides defaults; env vars override.
    level = os.getenv("LOG_LEVEL", _LOGGING_CONFIG.get("level", "INFO")).upper()
    fmt = os.getenv("LOG_FORMAT", _LOGGING_CONFIG.get("format", "text")).lower()
    handler = logging.StreamHandler(sys.stdout)

    if fmt == "json":
        handler.setFormatter(JsonFormatter())
    else:
        handler.setFormatter(
            logging.Formatter("%(asctime)s %(levelname)s [%(name)s] %(message)s")
        )

    root = logging.getLogger()
    root.handlers.clear()
    root.addHandler(handler)
    root.setLevel(level)
    _CONFIGURED = True


def get_logger(name: str) -> logging.Logger:
    """Return a configured logger. Safe to call from any module."""
    _configure_root()
    return logging.getLogger(name)

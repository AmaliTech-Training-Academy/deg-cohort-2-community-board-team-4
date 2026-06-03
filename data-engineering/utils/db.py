"""
Database connection helpers for the data-engineering package.

Centralizes SQLAlchemy engine creation so every script (ETL, analytics,
seed, init) shares one configuration instead of calling
``create_engine(DATABASE_URL)`` ad hoc. Credentials come from the config
package (loaded from ``.env``).

Typical use::

    from utils import get_engine

    engine = get_engine()
    with engine.connect() as conn:
        ...

The engine is created lazily and cached per-URL, so repeated calls return
the same pooled engine. Use ``dispose_engine()`` to tear it down (tests,
graceful shutdown).
"""
from __future__ import annotations

from sqlalchemy import create_engine, text
from sqlalchemy.engine import Engine
from sqlalchemy.exc import SQLAlchemyError

from config import DATABASE_URL

from .logging import get_logger

logger = get_logger("db")

# Cache engines by URL so all callers share one connection pool.
_ENGINES: dict[str, Engine] = {}


def get_engine(url: str | None = None, **engine_kwargs) -> Engine:
    """Return a cached SQLAlchemy engine for ``url`` (default: ``DATABASE_URL``).

    ``pool_pre_ping=True`` validates pooled connections before use, so a
    connection dropped by the server (idle timeout, restart) is transparently
    replaced instead of raising a stale-connection error. Extra keyword args
    are forwarded to ``create_engine`` on first creation for a given URL.
    """
    url = url or DATABASE_URL
    engine = _ENGINES.get(url)
    if engine is None:
        kwargs = {"pool_pre_ping": True, **engine_kwargs}
        engine = create_engine(url, **kwargs)
        _ENGINES[url] = engine
        logger.debug("Created engine", extra={"url": _safe_url(url)})
    return engine


def dispose_engine(url: str | None = None) -> None:
    """Dispose and forget the cached engine for ``url`` (or all if ``None``)."""
    if url is None:
        for cached in _ENGINES.values():
            cached.dispose()
        _ENGINES.clear()
        return
    engine = _ENGINES.pop(url, None)
    if engine is not None:
        engine.dispose()


def check_connection(engine: Engine | None = None) -> bool:
    """Return True if a trivial ``SELECT 1`` succeeds against the database.

    Logs and returns False on failure instead of raising — callers decide
    whether a dead DB is fatal (e.g. fail-fast with ``SystemExit``).
    """
    engine = engine or get_engine()
    try:
        with engine.connect() as conn:
            conn.execute(text("SELECT 1"))
        return True
    except SQLAlchemyError:
        logger.error("Database connection check failed", exc_info=True)
        return False


def _safe_url(url: str) -> str:
    """Strip the password from a DB URL for safe logging."""
    # postgresql://user:password@host:port/db -> postgresql://user:***@host:port/db
    if "@" not in url or "://" not in url:
        return url
    scheme, rest = url.split("://", 1)
    creds, location = rest.split("@", 1)
    if ":" in creds:
        user = creds.split(":", 1)[0]
        creds = f"{user}:***"
    return f"{scheme}://{creds}@{location}"


__all__ = ["get_engine", "dispose_engine", "check_connection"]

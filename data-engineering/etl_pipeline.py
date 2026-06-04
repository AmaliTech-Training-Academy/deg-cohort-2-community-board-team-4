"""
ETL Pipeline for CommunityBoard Analytics.

Extracts from the live application tables, transforms into the four analytics
datasets, and loads them into the ``analytics`` schema (see analytics.py). The
transform+load is a single full refresh — the data volume is tiny, so a
truncate-and-reload is simpler and safer than incremental upserts.
"""

from analytics import DEFAULT_DAYS, DEFAULT_TOP, run_load
from utils import dispose_engine, get_engine, get_logger

logger = get_logger("etl")


def run_pipeline() -> None:
    """Execute the full ETL pipeline: load the analytics datasets into the DB."""
    logger.info("Starting CommunityBoard ETL pipeline")
    engine = get_engine()
    try:
        run_load(engine, DEFAULT_DAYS, DEFAULT_TOP)
    except Exception:
        logger.error("ETL pipeline failed", exc_info=True)
        raise SystemExit(1)
    finally:
        dispose_engine()

    logger.info("ETL pipeline complete")


if __name__ == "__main__":
    run_pipeline()

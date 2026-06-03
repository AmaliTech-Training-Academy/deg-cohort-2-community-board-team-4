"""
ETL Pipeline for CommunityBoard Analytics
Extracts data from application DB, transforms into analytics-ready format.
"""

import pandas as pd
from sqlalchemy import text

from analytics import DEFAULT_DAYS, DEFAULT_TOP, run_export
from config import ANALYTICS_OUTPUT_DIR
from utils import dispose_engine, get_engine, get_logger

logger = get_logger("etl")
engine = get_engine()


def extract_posts():
    """Extract posts data with author and category info."""
    query = text(
        """
        SELECT p.id, p.title, p.content, p.created_at, p.updated_at,
               u.name AS author_name, u.email AS author_email,
               c.name AS category_name
        FROM posts p
        JOIN users u ON p.author_id = u.id
        LEFT JOIN categories c ON p.category_id = c.id
    """
    )
    with engine.connect() as conn:
        return pd.read_sql(query, conn)


def extract_comments():
    """Extract comments with post and author info."""
    query = text(
        """
        SELECT c.id, c.content, c.created_at,
               c.post_id, u.name AS author_name
        FROM comments c
        JOIN users u ON c.author_id = u.id
    """
    )
    with engine.connect() as conn:
        return pd.read_sql(query, conn)


def transform_daily_activity(posts_df):
    """Aggregate posts by date and category."""
    if posts_df.empty:
        return pd.DataFrame(columns=["date", "category", "post_count"])
    posts_df["date"] = pd.to_datetime(posts_df["created_at"]).dt.date
    daily = posts_df.groupby(["date", "category_name"]).size().reset_index(name="post_count")
    daily.columns = ["date", "category", "post_count"]
    return daily


def transform_user_engagement(posts_df, comments_df):
    """Calculate engagement metrics per user."""
    post_counts = posts_df.groupby("author_email").size().reset_index(name="posts_created")
    # TODO: Merge with comment_counts from comments_df and compute engagement score
    return post_counts


def load_analytics(df, table_name):
    """Load transformed data into analytics tables."""
    df.to_sql(table_name, engine, if_exists="replace", index=False)
    logger.info("Loaded rows into table", extra={"rows": len(df), "table": table_name})


def run_pipeline():
    """Execute the full ETL pipeline, then export the JSON analytics datasets."""
    logger.info("Starting CommunityBoard ETL pipeline")
    try:
        # Extract
        posts_df = extract_posts()
        comments_df = extract_comments()
        logger.info(
            "Extracted source data",
            extra={"posts": len(posts_df), "comments": len(comments_df)},
        )

        # Transform + load to analytics tables
        daily_activity = transform_daily_activity(posts_df)
        load_analytics(daily_activity, "analytics_daily_activity")

        # Export JSON datasets for the dashboard frontend
        run_export(engine, DEFAULT_DAYS, DEFAULT_TOP, ANALYTICS_OUTPUT_DIR)
    except Exception:
        logger.error("ETL pipeline failed", exc_info=True)
        raise SystemExit(1)
    finally:
        dispose_engine()

    logger.info("ETL pipeline complete")


if __name__ == "__main__":
    run_pipeline()

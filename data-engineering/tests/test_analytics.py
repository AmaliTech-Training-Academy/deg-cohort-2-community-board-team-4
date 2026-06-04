"""
Unit tests for analytics.py.

No live database: queries are tested against a MagicMock connection, and the
DB load is tested against a MagicMock engine/connection. Covers the SQL-binding
regression (make_interval, not an interpolated INTERVAL string), gap-filling,
empty-DB safety, the truncate-then-insert load, and arg parsing.
"""

import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import MagicMock

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from analytics import (  # noqa: E402
    DEFAULT_DAYS,
    DEFAULT_TOP,
    LOAD_COLUMNS,
    _load_table,
    parse_args,
    posts_per_category,
    posts_per_day,
    run_load,
    top_contributors,
    totals,
)


def _mock_conn(rows):
    """A MagicMock connection whose execute(...).all() returns `rows`."""
    conn = MagicMock()
    conn.execute.return_value.all.return_value = rows
    return conn


def _mock_conn_one(row):
    """A MagicMock connection whose execute(...).one() returns `row`."""
    conn = MagicMock()
    conn.execute.return_value.one.return_value = row
    return conn


def _sql_of(conn):
    """The SQL text of the last execute() call as a string."""
    return str(conn.execute.call_args[0][0])


class TestPostsPerCategory:
    def test_maps_rows_to_records(self):
        conn = _mock_conn(
            [
                SimpleNamespace(category="NEWS", post_count=3),
                SimpleNamespace(category="EVENT", post_count=0),
            ]
        )
        result = posts_per_category(conn)
        assert result == [
            {"category": "NEWS", "post_count": 3},
            {"category": "EVENT", "post_count": 0},
        ]

    def test_uses_left_join(self):
        conn = _mock_conn([])
        posts_per_category(conn)
        assert "LEFT JOIN" in _sql_of(conn)

    def test_empty_db_returns_empty_list(self):
        assert posts_per_category(_mock_conn([])) == []

    def test_post_count_is_int(self):
        conn = _mock_conn([SimpleNamespace(category="NEWS", post_count=5)])
        assert isinstance(posts_per_category(conn)[0]["post_count"], int)


class TestPostsPerDay:
    def test_binds_days_as_parameter(self):
        """Regression: days must be a bind param, NOT interpolated into a string."""
        conn = _mock_conn([])
        posts_per_day(conn, days=30)
        params = conn.execute.call_args[0][1]
        assert params == {"days": 30}

    def test_uses_make_interval_not_string_interval(self):
        """The fixed query uses make_interval(days => :days), never INTERVAL ':days...'."""
        conn = _mock_conn([])
        posts_per_day(conn, days=30)
        sql = _sql_of(conn)
        assert "make_interval(days => :days)" in sql
        assert "INTERVAL ':days" not in sql

    def test_gap_fills_to_full_window(self):
        """Sparse DB rows produce a continuous `days`-long series, missing days = 0."""
        today = datetime.now(timezone.utc).date()
        rows = [SimpleNamespace(day=today, post_count=4)]  # only today has posts
        result = posts_per_day(_mock_conn(rows), days=7)
        assert len(result) == 7
        assert result[-1] == {"day": today, "post_count": 4}
        assert all(r["post_count"] == 0 for r in result[:-1])

    def test_days_are_contiguous_date_objects(self):
        result = posts_per_day(_mock_conn([]), days=5)
        days = [r["day"] for r in result]
        assert all(hasattr(d, "isoformat") and not isinstance(d, str) for d in days)
        assert days == sorted(days)
        for earlier, later in zip(days, days[1:]):
            assert later - earlier == timedelta(days=1)

    def test_empty_db_all_zero_full_window(self):
        result = posts_per_day(_mock_conn([]), days=30)
        assert len(result) == 30
        assert all(r["post_count"] == 0 for r in result)


class TestTopContributors:
    def test_binds_limit_and_maps_rows(self):
        conn = _mock_conn([SimpleNamespace(id=1, name="Ada", email="ada@x.io", post_count=9)])
        result = top_contributors(conn, limit=5)
        assert conn.execute.call_args[0][1] == {"limit": 5}
        assert result == [{"user_id": 1, "name": "Ada", "email": "ada@x.io", "post_count": 9}]

    def test_orders_desc_with_name_tiebreak(self):
        conn = _mock_conn([])
        top_contributors(conn)
        assert "ORDER BY post_count DESC, u.name ASC" in _sql_of(conn)

    def test_empty_db_returns_empty_list(self):
        assert top_contributors(_mock_conn([])) == []


class TestTotals:
    def test_maps_counts_to_record(self):
        conn = _mock_conn_one(SimpleNamespace(total_posts=52, total_comments=220, total_users=15))
        result = totals(conn)
        assert result == [{"total_posts": 52, "total_comments": 220, "total_users": 15}]

    def test_counts_are_ints(self):
        conn = _mock_conn_one(SimpleNamespace(total_posts=0, total_comments=0, total_users=0))
        rec = totals(conn)[0]
        assert all(isinstance(v, int) for v in rec.values())


class TestLoadTable:
    def test_truncates_then_inserts_with_generated_at(self):
        conn = MagicMock()
        ts = datetime(2026, 6, 4, tzinfo=timezone.utc)
        records = [{"category": "NEWS", "post_count": 3}]
        _load_table(conn, "category_counts", records, ts)

        # First execute is the TRUNCATE, second the INSERT.
        truncate_sql = str(conn.execute.call_args_list[0][0][0])
        assert "TRUNCATE TABLE analytics.category_counts RESTART IDENTITY" in truncate_sql

        insert_args = conn.execute.call_args_list[1][0]
        assert "INSERT INTO analytics.category_counts" in str(insert_args[0])
        # generated_at is stamped onto every row.
        assert insert_args[1] == [{"category": "NEWS", "post_count": 3, "generated_at": ts}]

    def test_empty_records_truncates_but_does_not_insert(self):
        conn = MagicMock()
        ts = datetime(2026, 6, 4, tzinfo=timezone.utc)
        _load_table(conn, "summary", [], ts)
        # Only the TRUNCATE ran; no INSERT.
        assert conn.execute.call_count == 1
        assert "TRUNCATE TABLE analytics.summary" in str(conn.execute.call_args[0][0])

    def test_load_columns_cover_all_target_tables(self):
        assert set(LOAD_COLUMNS) == {
            "category_counts",
            "daily_post_counts",
            "top_users",
            "summary",
        }


class TestRunLoad:
    def test_creates_schema_and_loads_all_tables(self):
        conn = MagicMock()
        conn.execute.return_value.all.return_value = []
        conn.execute.return_value.one.return_value = SimpleNamespace(
            total_posts=0, total_comments=0, total_users=0
        )
        engine = MagicMock()
        engine.begin.return_value.__enter__.return_value = conn

        run_load(engine, days=7, limit=5)

        executed = [str(c[0][0]) for c in conn.execute.call_args_list]
        # Schema DDL ran first, and every analytics table was truncated.
        assert any("CREATE SCHEMA IF NOT EXISTS analytics" in sql for sql in executed)
        for table in ("summary", "category_counts", "daily_post_counts", "top_users"):
            assert any(f"TRUNCATE TABLE analytics.{table}" in sql for sql in executed)


class TestParseArgs:
    def test_defaults(self):
        args = parse_args([])
        assert args.days == DEFAULT_DAYS
        assert args.top == DEFAULT_TOP

    def test_overrides(self):
        args = parse_args(["--days", "7", "--top", "10"])
        assert args.days == 7
        assert args.top == 10


if __name__ == "__main__":
    import pytest

    pytest.main([__file__, "-v"])

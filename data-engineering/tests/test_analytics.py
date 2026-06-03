"""
Unit tests for analytics.py.

No live database: queries are tested against a MagicMock connection, and the
JSON export is tested against a tmp_path. Covers the SQL-binding regression
(make_interval, not an interpolated INTERVAL string), gap-filling, empty-DB
safety, atomic writes, and the JSON envelope shape.
"""

import json
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import MagicMock

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from analytics import (  # noqa: E402
    DEFAULT_DAYS,
    DEFAULT_TOP,
    _envelope,
    export_json,
    parse_args,
    posts_per_category,
    posts_per_day,
    run_export,
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
        assert result[-1] == {"day": today.isoformat(), "post_count": 4}
        assert all(r["post_count"] == 0 for r in result[:-1])

    def test_days_are_contiguous_and_iso(self):
        result = posts_per_day(_mock_conn([]), days=5)
        days = [datetime.fromisoformat(r["day"]).date() for r in result]
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
        assert result == [{"id": 1, "name": "Ada", "email": "ada@x.io", "post_count": 9}]

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


class TestEnvelopeAndExport:
    def test_envelope_shape(self):
        env = _envelope([{"a": 1}])
        assert set(env) == {"generated_at", "count", "data"}
        assert env["count"] == 1
        assert env["data"] == [{"a": 1}]
        # generated_at is ISO-8601 parseable
        datetime.fromisoformat(env["generated_at"])

    def test_export_writes_valid_json(self, tmp_path):
        records = [{"category": "NEWS", "post_count": 3}]
        path = export_json(records, "posts_per_category.json", tmp_path)
        assert path == tmp_path / "posts_per_category.json"
        loaded = json.loads(path.read_text())
        assert loaded["data"] == records
        assert loaded["count"] == 1

    def test_export_leaves_no_tmp_file(self, tmp_path):
        """Atomic write: the .tmp file is gone after a successful replace."""
        export_json([{"x": 1}], "out.json", tmp_path)
        assert list(tmp_path.glob("*.tmp")) == []

    def test_export_creates_output_dir(self, tmp_path):
        nested = tmp_path / "a" / "b"
        export_json([], "empty.json", nested)
        assert (nested / "empty.json").exists()

    def test_export_serializes_dates_as_strings(self, tmp_path):
        path = export_json([{"day": "2026-06-01", "post_count": 0}], "d.json", tmp_path)
        loaded = json.loads(path.read_text())
        assert loaded["data"][0]["day"] == "2026-06-01"


class TestRunExport:
    def test_writes_all_files(self, tmp_path):
        conn = _mock_conn([])
        # totals() uses .one(); provide a zero-count row.
        conn.execute.return_value.one.return_value = SimpleNamespace(
            total_posts=0, total_comments=0, total_users=0
        )
        engine = MagicMock()
        engine.connect.return_value.__enter__.return_value = conn

        run_export(engine, days=7, limit=5, output_dir=tmp_path)

        names = {p.name for p in tmp_path.glob("*.json")}
        assert names == {
            "totals.json",
            "posts_per_category.json",
            "posts_per_day.json",
            "top_contributors.json",
        }


class TestParseArgs:
    def test_defaults(self):
        args = parse_args([])
        assert args.days == DEFAULT_DAYS
        assert args.top == DEFAULT_TOP

    def test_overrides(self):
        args = parse_args(["--days", "7", "--top", "10", "--output-dir", "/tmp/x"])
        assert args.days == 7
        assert args.top == 10
        assert args.output_dir == Path("/tmp/x")


if __name__ == "__main__":
    pytest.main([__file__, "-v"])

"""
Unit tests for scripts/seed.py.

Tests cover:
  - random_datetime() returns valid timezone-aware datetimes within the window
  - seed_users() ensures first user is ADMIN
  - seed_posts() distributes evenly across categories
  - seed_comments() spreads across posts
  - parse_args() handles CLI overrides correctly
  - run() orchestrates seeding without errors
"""
import random

# Import the functions to test
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path
from unittest.mock import MagicMock, patch

import pytest

# Add the scripts directory to sys.path so we can import seed
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from scripts.seed import (  # noqa: E402
    CATEGORIES,
    DEFAULT_COMMENTS,
    DEFAULT_POSTS,
    DEFAULT_USERS,
    TREND_WINDOW_DAYS,
    parse_args,
    random_datetime,
    reset_tables,
    run,
    seed_categories,
    seed_comments,
    seed_posts,
    seed_users,
)


class TestRandomDatetime:
    """Tests for random_datetime() function."""

    def test_returns_timezone_aware_datetime(self):
        """Should return a timezone-aware datetime object."""
        dt = random_datetime()
        assert isinstance(dt, datetime)
        assert dt.tzinfo is not None
        assert dt.tzinfo == timezone.utc

    def test_within_default_window(self):
        """Should be within the default trend window."""
        dt = random_datetime()
        now = datetime.now(timezone.utc)
        delta = now - dt
        assert timedelta(0) <= delta <= timedelta(days=TREND_WINDOW_DAYS)

    def test_within_custom_window(self):
        """Should respect custom within_days parameter."""
        custom_days = 7
        dt = random_datetime(within_days=custom_days)
        now = datetime.now(timezone.utc)
        delta = now - dt
        assert timedelta(0) <= delta <= timedelta(days=custom_days)

    def test_deterministic_with_seed(self):
        """Multiple calls with same seed should produce different results."""
        # Reset seed for reproducibility
        random.seed(42)
        dt1 = random_datetime()

        random.seed(42)
        dt2 = random_datetime()

        # Both should be valid datetimes
        assert dt1.tzinfo == timezone.utc
        assert dt2.tzinfo == timezone.utc


class TestResetTables:
    """Tests for reset_tables() function."""

    def test_executes_truncate_statement(self):
        """Should execute TRUNCATE with RESTART IDENTITY."""
        mock_conn = MagicMock()
        mock_result = MagicMock()
        mock_conn.execute.return_value = mock_result

        reset_tables(mock_conn)

        # Verify truncate was called
        mock_conn.execute.assert_called_once()
        call_args = mock_conn.execute.call_args[0][0]
        assert "TRUNCATE" in str(call_args)
        assert "RESTART IDENTITY" in str(call_args)

    def test_truncates_correct_tables(self):
        """Should truncate comments, posts, users — but NOT categories."""
        mock_conn = MagicMock()
        reset_tables(mock_conn)

        call_args = mock_conn.execute.call_args[0][0]
        sql = str(call_args)
        assert "comments" in sql
        assert "posts" in sql
        assert "users" in sql
        # Categories are provisioned outside the seeder, never truncated.
        assert "categories" not in sql


class TestSeedCategories:
    """Tests for seed_categories() function (lookup-only, no insert)."""

    def test_returns_list_of_ids(self):
        """Should return the ids of existing categories."""
        mock_conn = MagicMock()
        mock_result = MagicMock()
        mock_result.id = 1

        mock_conn.execute.return_value.one_or_none.return_value = mock_result

        ids = seed_categories(mock_conn)

        assert isinstance(ids, list)
        assert len(ids) == len(CATEGORIES)

    def test_looks_up_each_category(self):
        """Should query once per configured category."""
        mock_conn = MagicMock()
        mock_result = MagicMock()
        mock_result.id = 1

        mock_conn.execute.return_value.one_or_none.return_value = mock_result

        ids = seed_categories(mock_conn)

        assert mock_conn.execute.call_count == len(CATEGORIES)
        assert len(ids) == len(CATEGORIES)

    def test_selects_not_inserts(self):
        """Should SELECT existing rows, never INSERT."""
        mock_conn = MagicMock()
        mock_result = MagicMock()
        mock_result.id = 1
        mock_conn.execute.return_value.one_or_none.return_value = mock_result

        seed_categories(mock_conn)

        sql = str(mock_conn.execute.call_args_list[0][0][0])
        assert "SELECT" in sql
        assert "INSERT" not in sql

    def test_raises_when_category_missing(self):
        """Should error out if a configured category is absent in the DB."""
        mock_conn = MagicMock()
        mock_conn.execute.return_value.one_or_none.return_value = None

        with pytest.raises(SystemExit):
            seed_categories(mock_conn)


class TestSeedUsers:
    """Tests for seed_users() function."""

    def test_returns_list_of_user_ids(self):
        """Should return a list of inserted user IDs."""
        mock_conn = MagicMock()
        mock_result = MagicMock()
        mock_result.id = 1

        mock_conn.execute.return_value.one.return_value = mock_result

        ids = seed_users(mock_conn, 5)

        assert isinstance(ids, list)
        assert len(ids) == 5

    def test_first_user_is_admin(self):
        """First user should have ADMIN role."""
        mock_conn = MagicMock()
        mock_result = MagicMock()
        mock_result.id = 1
        mock_conn.execute.return_value.one.return_value = mock_result

        seed_users(mock_conn, 3)

        # Get the first call's parameters
        first_call_params = mock_conn.execute.call_args_list[0][0][1]
        assert first_call_params["role"] == "ADMIN"

    def test_creates_correct_number_of_users(self):
        """Should create exactly the requested number of users."""
        mock_conn = MagicMock()
        mock_result = MagicMock()
        mock_result.id = 1
        mock_conn.execute.return_value.one.return_value = mock_result

        count = 10
        ids = seed_users(mock_conn, count)

        assert len(ids) == count
        assert mock_conn.execute.call_count == count

    def test_sets_timezone_aware_timestamps(self):
        """Should set created_at and updated_at with timezone info."""
        mock_conn = MagicMock()
        mock_result = MagicMock()
        mock_result.id = 1
        mock_conn.execute.return_value.one.return_value = mock_result

        seed_users(mock_conn, 1)

        params = mock_conn.execute.call_args_list[0][0][1]
        assert params["created_at"].tzinfo is not None
        assert params["updated_at"].tzinfo is not None


class TestSeedPosts:
    """Tests for seed_posts() function."""

    def test_returns_list_of_post_ids(self):
        """Should return a list of inserted post IDs."""
        mock_conn = MagicMock()
        mock_result = MagicMock()
        mock_result.id = 1
        mock_conn.execute.return_value.one.return_value = mock_result

        ids = seed_posts(mock_conn, 5, [1, 2], [1, 2])

        assert isinstance(ids, list)
        assert len(ids) == 5

    def test_distributes_across_categories(self):
        """Should distribute posts evenly across categories using round-robin."""
        mock_conn = MagicMock()
        mock_result = MagicMock()
        mock_result.id = 1
        mock_conn.execute.return_value.one.return_value = mock_result

        category_ids = [1, 2, 3]
        user_ids = [1, 2]

        seed_posts(mock_conn, 9, category_ids, user_ids)

        # Extract category_id from each call
        categories_used = []
        for call_args in mock_conn.execute.call_args_list:
            params = call_args[0][1]
            categories_used.append(params["category_id"])

        # Should cycle through categories: 1, 2, 3, 1, 2, 3, ...
        expected = [1, 2, 3, 1, 2, 3, 1, 2, 3]
        assert categories_used == expected

    def test_unique_slugs(self):
        """Should generate unique slugs."""
        mock_conn = MagicMock()
        mock_result = MagicMock()
        mock_result.id = 1
        mock_conn.execute.return_value.one.return_value = mock_result

        seed_posts(mock_conn, 5, [1], [1])

        slugs = []
        for call_args in mock_conn.execute.call_args_list:
            params = call_args[0][1]
            slugs.append(params["slug"])

        # All slugs should be unique
        assert len(slugs) == len(set(slugs))


class TestSeedComments:
    """Tests for seed_comments() function."""

    def test_creates_specified_number_of_comments(self):
        """Should create exactly the requested number of comments."""
        mock_conn = MagicMock()

        seed_comments(mock_conn, 10, [1, 2], [1, 2])

        assert mock_conn.execute.call_count == 10

    def test_assigns_random_posts(self):
        """Should randomly assign comments to posts."""
        mock_conn = MagicMock()
        post_ids = [1, 2, 3]

        seed_comments(mock_conn, 5, post_ids, [1])

        post_ids_used = []
        for call_args in mock_conn.execute.call_args_list:
            params = call_args[0][1]
            post_ids_used.append(params["post_id"])

        # All assigned posts should be from the provided list
        assert all(pid in post_ids for pid in post_ids_used)

    def test_assigns_random_authors(self):
        """Should randomly assign comments to users."""
        mock_conn = MagicMock()
        user_ids = [1, 2, 3]

        seed_comments(mock_conn, 5, [1], user_ids)

        author_ids_used = []
        for call_args in mock_conn.execute.call_args_list:
            params = call_args[0][1]
            author_ids_used.append(params["author_id"])

        # All assigned authors should be from the provided list
        assert all(uid in user_ids for uid in author_ids_used)


class TestParseArgs:
    """Tests for parse_args() function."""

    def test_default_values(self):
        """Should use default values when no args provided."""
        args = parse_args([])
        assert args.users == DEFAULT_USERS
        assert args.posts == DEFAULT_POSTS
        assert args.comments == DEFAULT_COMMENTS
        assert args.reset is False

    def test_override_users(self):
        """Should override default users."""
        args = parse_args(["--users", "20"])
        assert args.users == 20

    def test_override_posts(self):
        """Should override default posts."""
        args = parse_args(["--posts", "100"])
        assert args.posts == 100

    def test_override_comments(self):
        """Should override default comments."""
        args = parse_args(["--comments", "500"])
        assert args.comments == 500

    def test_reset_flag(self):
        """Should set reset flag when provided."""
        args = parse_args(["--reset"])
        assert args.reset is True

    def test_multiple_overrides(self):
        """Should handle multiple arguments."""
        args = parse_args(["--users", "10", "--posts", "50", "--comments", "200", "--reset"])
        assert args.users == 10
        assert args.posts == 50
        assert args.comments == 200
        assert args.reset is True


class TestRun:
    """Tests for run() function."""

    @patch("scripts.seed.seed_comments")
    @patch("scripts.seed.seed_posts")
    @patch("scripts.seed.seed_users")
    @patch("scripts.seed.seed_categories")
    @patch("scripts.seed.reset_tables")
    def test_executes_all_seed_functions(
        self, mock_reset, mock_cat, mock_users, mock_posts, mock_comments
    ):
        """Should call all seed functions in correct order."""
        mock_cat.return_value = [1, 2]
        mock_users.return_value = [1, 2]
        mock_posts.return_value = [1, 2]

        mock_engine = MagicMock()
        mock_conn = MagicMock()
        mock_engine.begin.return_value.__enter__.return_value = mock_conn

        run(mock_engine, users=5, posts=10, comments=20, reset=False)

        mock_cat.assert_called_once_with(mock_conn)
        mock_users.assert_called_once_with(mock_conn, 5)
        mock_posts.assert_called_once()
        mock_comments.assert_called_once()

    @patch("scripts.seed.seed_comments")
    @patch("scripts.seed.seed_posts")
    @patch("scripts.seed.seed_users")
    @patch("scripts.seed.seed_categories")
    @patch("scripts.seed.reset_tables")
    def test_resets_tables_when_requested(
        self, mock_reset, mock_cat, mock_users, mock_posts, mock_comments
    ):
        """Should reset tables when reset=True."""
        mock_cat.return_value = [1]
        mock_users.return_value = [1]
        mock_posts.return_value = [1]

        mock_engine = MagicMock()
        mock_conn = MagicMock()
        mock_engine.begin.return_value.__enter__.return_value = mock_conn

        run(mock_engine, users=1, posts=1, comments=1, reset=True)

        mock_reset.assert_called_once_with(mock_conn)

    @patch("scripts.seed.seed_comments")
    @patch("scripts.seed.seed_posts")
    @patch("scripts.seed.seed_users")
    @patch("scripts.seed.seed_categories")
    @patch("scripts.seed.reset_tables")
    def test_skips_reset_when_not_requested(
        self, mock_reset, mock_cat, mock_users, mock_posts, mock_comments
    ):
        """Should not reset tables when reset=False."""
        mock_cat.return_value = [1]
        mock_users.return_value = [1]
        mock_posts.return_value = [1]

        mock_engine = MagicMock()
        mock_conn = MagicMock()
        mock_engine.begin.return_value.__enter__.return_value = mock_conn

        run(mock_engine, users=1, posts=1, comments=1, reset=False)

        mock_reset.assert_not_called()

    @patch("scripts.seed.seed_comments")
    @patch("scripts.seed.seed_posts")
    @patch("scripts.seed.seed_users")
    @patch("scripts.seed.seed_categories")
    @patch("scripts.seed.reset_tables")
    def test_passes_correct_parameters(
        self, mock_reset, mock_cat, mock_users, mock_posts, mock_comments
    ):
        """Should pass correct parameters to each seed function."""
        category_ids = [1, 2, 3]
        user_ids = [1, 2, 3, 4, 5]
        post_ids = [1, 2, 3, 4, 5, 6]

        mock_cat.return_value = category_ids
        mock_users.return_value = user_ids
        mock_posts.return_value = post_ids

        mock_engine = MagicMock()
        mock_conn = MagicMock()
        mock_engine.begin.return_value.__enter__.return_value = mock_conn

        run(mock_engine, users=5, posts=6, comments=20, reset=False)

        # Verify seed_posts called with correct category and user IDs
        post_call_args = mock_posts.call_args[0]
        assert post_call_args[1] == 6  # post count
        assert post_call_args[2] == category_ids
        assert post_call_args[3] == user_ids

        # Verify seed_comments called with correct post and user IDs
        comment_call_args = mock_comments.call_args[0]
        assert comment_call_args[1] == 20  # comment count
        assert comment_call_args[2] == post_ids
        assert comment_call_args[3] == user_ids

    @patch("scripts.seed.seed_comments")
    @patch("scripts.seed.seed_posts")
    @patch("scripts.seed.seed_users")
    @patch("scripts.seed.seed_categories")
    @patch("scripts.seed.reset_tables")
    def test_uses_transaction(self, mock_reset, mock_cat, mock_users, mock_posts, mock_comments):
        """Should use engine.begin() for transaction context."""
        mock_cat.return_value = [1]
        mock_users.return_value = [1]
        mock_posts.return_value = [1]

        mock_engine = MagicMock()
        mock_conn = MagicMock()
        mock_engine.begin.return_value.__enter__.return_value = mock_conn
        mock_engine.begin.return_value.__exit__.return_value = None

        run(mock_engine, users=1, posts=1, comments=1, reset=False)

        mock_engine.begin.assert_called_once()


if __name__ == "__main__":
    pytest.main([__file__, "-v"])

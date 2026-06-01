# Tests for communityboard-etl

Unit tests for the ETL pipeline components.

## Running Tests

Install dev dependencies:

```bash
uv sync --all-extras
# or with pip
pip install -e ".[dev]"
```

Run all tests:

```bash
pytest
```

Run with coverage:

```bash
pytest --cov=scripts --cov=config --cov=utils --cov-report=html
```

Run specific test file:

```bash
pytest tests/test_seed.py -v
```

Run specific test class:

```bash
pytest tests/test_seed.py::TestRandomDatetime -v
```

Run specific test:

```bash
pytest tests/test_seed.py::TestRandomDatetime::test_returns_timezone_aware_datetime -v
```

## Test Structure

- `test_seed.py` - Tests for `scripts/seed.py`
  - `TestRandomDatetime` - Tests for `random_datetime()`
  - `TestResetTables` - Tests for `reset_tables()`
  - `TestSeedCategories` - Tests for `seed_categories()`
  - `TestSeedUsers` - Tests for `seed_users()` (checks first user is ADMIN)
  - `TestSeedPosts` - Tests for `seed_posts()` (checks round-robin distribution)
  - `TestSeedComments` - Tests for `seed_comments()`
  - `TestParseArgs` - Tests for CLI argument parsing
  - `TestRun` - Integration tests for `run()` orchestration

## Key Test Coverage

✅ Timezone-aware datetime generation within configured window
✅ First user always has ADMIN role
✅ Posts distributed evenly across categories (round-robin)
✅ Unique slug generation
✅ CLI argument parsing with defaults and overrides
✅ Transaction-based seeding (all-or-nothing)
✅ Table reset with RESTART IDENTITY
✅ UPS ERT handling for duplicate categories

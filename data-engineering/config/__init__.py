"""
Configuration package.

Secrets (DB credentials) come from environment / .env.
Non-secret settings (logging, seed defaults, categories) come from
settings.toml in this package.
"""
import os
import tomllib
from pathlib import Path

from dotenv import load_dotenv

# Load environment variables from .env file
# Copy .env.example to .env and update values as needed
load_dotenv()

DB_CONFIG = {
    "host": os.getenv("DB_HOST"),
    "port": os.getenv("DB_PORT"),
    "database": os.getenv("DB_NAME"),
    "user": os.getenv("DB_USER"),
    "password": os.getenv("DB_PASSWORD"),
}

DATABASE_URL = f"postgresql://{DB_CONFIG['user']}:{DB_CONFIG['password']}@{DB_CONFIG['host']}:{DB_CONFIG['port']}/{DB_CONFIG['database']}"

# Non-secret settings from settings.toml (alongside this file).
_SETTINGS_PATH = Path(__file__).resolve().parent / "settings.toml"
with _SETTINGS_PATH.open("rb") as fh:
    SETTINGS = tomllib.load(fh)

LOGGING = SETTINGS.get("logging", {})
SEED = SETTINGS.get("seed", {})

__all__ = ["DATABASE_URL", "DB_CONFIG", "SETTINGS", "LOGGING", "SEED"]

"""Settings read from environment variables, with defaults that work for local development."""

import os

DATABASE_URL = os.getenv("DATABASE_URL", "sqlite:///./teaching.db")

# Change JWT_SECRET for anything that is not your own laptop.
JWT_SECRET = os.getenv("JWT_SECRET", "dev-only-secret-change-me-in-production-0123456789")
JWT_ALGORITHM = "HS256"
JWT_EXPIRE_MINUTES = int(os.getenv("JWT_EXPIRE_MINUTES", str(60 * 24 * 7)))  # one week

# Seed demo accounts and lessons on startup when the database is empty.
SEED_DEMO_DATA = os.getenv("SEED_DEMO_DATA", "true").lower() == "true"

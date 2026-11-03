"""Typed settings loaded from environment variables (and an optional .env file).

Every value can be overridden without touching code, e.g. `JWT_SECRET=... uvicorn app.main:app`.
See .env.example for the full list.
"""

from functools import lru_cache

from pydantic import Field, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    database_url: str = "sqlite:///./teaching.db"

    jwt_secret: str = Field(default="dev-only-secret-change-me-in-production-0123456789", min_length=32)
    jwt_algorithm: str = "HS256"
    jwt_expire_minutes: int = Field(default=60 * 24 * 7, ge=5)

    seed_demo_data: bool = True

    # Comma-separated list, e.g. "http://localhost:5173,https://teach.example.edu". "*" allows all.
    cors_origins: list[str] = ["*"]

    @field_validator("cors_origins", mode="before")
    @classmethod
    def split_origins(cls, value: object) -> object:
        if isinstance(value, str) and not value.strip().startswith("["):
            return [origin.strip() for origin in value.split(",") if origin.strip()]
        return value


@lru_cache
def get_settings() -> Settings:
    return Settings()


settings = get_settings()

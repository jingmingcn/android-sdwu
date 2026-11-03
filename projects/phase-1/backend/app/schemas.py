from enum import Enum

from pydantic import BaseModel, Field


class Difficulty(str, Enum):
    BEGINNER = "BEGINNER"
    INTERMEDIATE = "INTERMEDIATE"
    ADVANCED = "ADVANCED"


class Lesson(BaseModel):
    """A single lesson. Field names are snake_case on the wire (JSON)."""

    id: str = Field(examples=["kotlin-basics"])
    order: int = Field(ge=1)
    title: str
    summary: str
    content: str
    category: str
    difficulty: Difficulty
    duration_minutes: int = Field(ge=1)
    video_url: str | None = None

"""Phase 1 backend: a minimal, read-only lesson API.

There is no database and no auth yet: lessons are loaded once from
``app/data/lessons.json`` (the same seed data the Android app ships with).
Phase 2 replaces this with SQLAlchemy + SQLite, JWT auth and progress endpoints.
"""

import json
from functools import lru_cache
from pathlib import Path

from fastapi import FastAPI, HTTPException

from .schemas import Lesson

DATA_FILE = Path(__file__).parent / "data" / "lessons.json"

app = FastAPI(
    title="Android Teaching Framework API",
    version="0.1.0",
    description="Phase 1: static lesson catalogue.",
)


@lru_cache
def load_lessons() -> list[Lesson]:
    raw = json.loads(DATA_FILE.read_text(encoding="utf-8"))
    return sorted((Lesson.model_validate(item) for item in raw), key=lambda l: l.order)


@app.get("/health", tags=["meta"])
def health() -> dict[str, str]:
    return {"status": "ok", "phase": "1"}


@app.get("/lessons", response_model=list[Lesson], tags=["lessons"])
def list_lessons() -> list[Lesson]:
    return load_lessons()


@app.get("/lessons/{lesson_id}", response_model=Lesson, tags=["lessons"])
def get_lesson(lesson_id: str) -> Lesson:
    for lesson in load_lessons():
        if lesson.id == lesson_id:
            return lesson
    raise HTTPException(status_code=404, detail=f"Lesson '{lesson_id}' not found")

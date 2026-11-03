"""Fills an empty database with lessons, quizzes and two demo accounts."""

import json
from pathlib import Path

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from .models import Lesson, QuizQuestion, User
from .security import hash_password

DATA_DIR = Path(__file__).parent / "data"

# Demo accounts for local development only (also listed in the README).
DEMO_USERS = [
    {"email": "student@example.com", "password": "student123", "display_name": "Demo Student", "is_instructor": False},
    {"email": "teacher@example.com", "password": "teacher123", "display_name": "Demo Teacher", "is_instructor": True},
]


def _load(name: str) -> list[dict]:
    return json.loads((DATA_DIR / name).read_text(encoding="utf-8"))


def seed(db: Session) -> None:
    if db.scalar(select(func.count()).select_from(Lesson)) == 0:
        for item in _load("lessons.json"):
            db.add(Lesson(**item))
        db.flush()  # lessons must exist before their questions (foreign key)
        for item in _load("quizzes.json"):
            db.add(QuizQuestion(**item))

    for demo in DEMO_USERS:
        if db.scalar(select(User).where(User.email == demo["email"])) is None:
            db.add(
                User(
                    email=demo["email"],
                    display_name=demo["display_name"],
                    password_hash=hash_password(demo["password"]),
                    is_instructor=demo["is_instructor"],
                )
            )
    db.commit()

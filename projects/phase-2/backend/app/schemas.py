"""Request/response models. JSON field names are snake_case."""

from enum import Enum

from pydantic import BaseModel, ConfigDict, EmailStr, Field, model_validator


class Difficulty(str, Enum):
    BEGINNER = "BEGINNER"
    INTERMEDIATE = "INTERMEDIATE"
    ADVANCED = "ADVANCED"


class ProgressStatus(str, Enum):
    NOT_STARTED = "NOT_STARTED"
    IN_PROGRESS = "IN_PROGRESS"
    COMPLETED = "COMPLETED"


# ---------- auth ----------

class RegisterRequest(BaseModel):
    email: EmailStr
    # bcrypt only looks at the first 72 bytes, so longer passwords are rejected.
    password: str = Field(min_length=6, max_length=72)
    display_name: str = Field(min_length=1, max_length=100)


class LoginRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=1, max_length=72)


class UserOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    email: str
    display_name: str
    is_instructor: bool


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: UserOut


# ---------- lessons ----------

class LessonBase(BaseModel):
    order: int = Field(ge=1)
    title: str = Field(min_length=1, max_length=200)
    summary: str = Field(max_length=500)
    content: str
    category: str = Field(min_length=1, max_length=50)
    difficulty: Difficulty
    duration_minutes: int = Field(ge=1, le=600)
    video_url: str | None = None


class LessonCreate(LessonBase):
    id: str = Field(pattern=r"^[a-z0-9-]{2,64}$", examples=["my-new-lesson"])


class LessonUpdate(LessonBase):
    pass


class LessonOut(LessonBase):
    model_config = ConfigDict(from_attributes=True)

    id: str


# ---------- quizzes ----------

class QuizQuestionOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: str
    lesson_id: str
    order: int
    question: str
    options: list[str]
    # Sent to the client on purpose: Phase 2 scores quizzes locally so they work offline.
    correct_index: int
    explanation: str


# ---------- progress ----------

class ProgressIn(BaseModel):
    status: ProgressStatus
    quiz_best_score: int | None = Field(default=None, ge=0)
    quiz_total: int | None = Field(default=None, ge=1)
    # Epoch milliseconds (System.currentTimeMillis() on Android). The newest write wins.
    updated_at: int = Field(ge=0)

    @model_validator(mode="after")
    def score_not_above_total(self) -> "ProgressIn":
        if self.quiz_best_score is not None and self.quiz_total is not None and self.quiz_best_score > self.quiz_total:
            raise ValueError("quiz_best_score cannot be greater than quiz_total")
        return self


class ProgressOut(ProgressIn):
    model_config = ConfigDict(from_attributes=True)

    lesson_id: str


class ProgressSyncItem(ProgressIn):
    lesson_id: str

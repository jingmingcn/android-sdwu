"""Request/response models. JSON field names are snake_case.

Input models use `StrictInput`: surrounding whitespace is stripped and unknown fields are
rejected (422), so typos such as "displayName" are reported instead of silently ignored.
"""

from datetime import datetime
from enum import Enum
from typing import Literal

from pydantic import AnyHttpUrl, BaseModel, ConfigDict, EmailStr, Field, field_validator, model_validator


class Difficulty(str, Enum):
    BEGINNER = "BEGINNER"
    INTERMEDIATE = "INTERMEDIATE"
    ADVANCED = "ADVANCED"


class ProgressStatus(str, Enum):
    NOT_STARTED = "NOT_STARTED"
    IN_PROGRESS = "IN_PROGRESS"
    COMPLETED = "COMPLETED"


class StrictInput(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True, extra="forbid")


# ---------- auth ----------

class RegisterRequest(StrictInput):
    email: EmailStr
    # bcrypt only looks at the first 72 bytes, so longer passwords are rejected.
    password: str = Field(min_length=6, max_length=72)
    display_name: str = Field(min_length=1, max_length=100)

    @field_validator("password")
    @classmethod
    def letters_and_digits(cls, value: str) -> str:
        if not any(c.isalpha() for c in value) or not any(c.isdigit() for c in value):
            raise ValueError("password must contain at least one letter and one digit")
        return value


class LoginRequest(StrictInput):
    email: EmailStr
    password: str = Field(min_length=1, max_length=72)


class UserUpdate(StrictInput):
    """PATCH /auth/me: only the fields that are sent are changed. Send avatar_url=null to remove it."""

    display_name: str | None = Field(default=None, min_length=1, max_length=100)
    avatar_url: AnyHttpUrl | None = None


class UserOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    email: str
    display_name: str
    avatar_url: str | None = None
    is_instructor: bool


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: UserOut


# ---------- lessons ----------

class LessonBase(StrictInput):
    order: int = Field(ge=1)
    title: str = Field(min_length=1, max_length=200)
    summary: str = Field(max_length=500)
    content: str
    category: str = Field(min_length=1, max_length=50)
    difficulty: Difficulty
    duration_minutes: int = Field(ge=1, le=600)
    # Either a full http(s) URL or a file served by this backend under /media/.
    video_url: str | None = Field(default=None, pattern=r"^(https?://\S+|/media/[\w.\-/]+)$", max_length=500)


class LessonCreate(LessonBase):
    id: str = Field(pattern=r"^[a-z0-9-]{2,64}$", examples=["my-new-lesson"])


class LessonUpdate(LessonBase):
    pass


class LessonOut(LessonBase):
    model_config = ConfigDict(from_attributes=True, extra="ignore")

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

class ProgressIn(StrictInput):
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
    model_config = ConfigDict(from_attributes=True, extra="ignore")

    lesson_id: str


class ProgressSyncItem(ProgressIn):
    lesson_id: str


# ---------- student modules (class catalog) ----------

# Same rule as the app's framework (VALID_ID in framework/Contracts.kt).
MODULE_ID_PATTERN = r"^[a-z][a-z0-9-]{2,39}$"


class ModuleCategory(str, Enum):
    STUDY_TOOL = "STUDY_TOOL"
    LESSON_RENDERER = "LESSON_RENDERER"
    QUIZ = "QUIZ"
    UTILITY = "UTILITY"
    FUN = "FUN"


class ContributionIn(StrictInput):
    type: Literal["function", "tool", "renderer"]
    id: str = Field(pattern=MODULE_ID_PATTERN)
    title: str = Field(min_length=1, max_length=100)


class ModuleIn(StrictInput):
    id: str = Field(pattern=MODULE_ID_PATTERN)
    name: str = Field(min_length=1, max_length=100)
    description: str = Field(default="", max_length=500)
    author: str = Field(min_length=1, max_length=100)
    version: str = Field(pattern=r"^\d+\.\d+\.\d+$", examples=["1.0.0"])
    category: ModuleCategory = ModuleCategory.UTILITY
    contributions: list[ContributionIn] = Field(min_length=1, max_length=20)


class ModuleOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: str
    name: str
    description: str
    author: str
    version: str
    category: ModuleCategory
    contributions: list[ContributionIn]
    owner_name: str
    updated_at: datetime

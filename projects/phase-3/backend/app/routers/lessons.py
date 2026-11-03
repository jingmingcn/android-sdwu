from fastapi import APIRouter, Depends, HTTPException, Query, Response, status
from sqlalchemy import or_, select
from sqlalchemy.orm import Session

from ..database import get_db
from ..models import Lesson, QuizQuestion, User
from ..schemas import Difficulty, LessonCreate, LessonOut, LessonUpdate, QuizQuestionOut
from ..security import require_instructor

router = APIRouter(tags=["lessons"])


def _get_or_404(db: Session, lesson_id: str) -> Lesson:
    lesson = db.get(Lesson, lesson_id)
    if lesson is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, detail=f"Lesson '{lesson_id}' not found")
    return lesson


# ----- public reads -----

@router.get("/lessons", response_model=list[LessonOut])
def list_lessons(
    q: str | None = Query(default=None, max_length=100, description="Search title, summary and content"),
    category: str | None = Query(default=None, max_length=50),
    difficulty: Difficulty | None = None,
    db: Session = Depends(get_db),
) -> list[Lesson]:
    """All lessons, optionally filtered. (The app filters its offline copy itself; this is for other clients.)"""
    query = select(Lesson).order_by(Lesson.order)
    if q:
        pattern = f"%{q.strip()}%"
        query = query.where(or_(Lesson.title.ilike(pattern), Lesson.summary.ilike(pattern), Lesson.content.ilike(pattern)))
    if category:
        query = query.where(Lesson.category == category)
    if difficulty:
        query = query.where(Lesson.difficulty == difficulty.value)
    return list(db.scalars(query))


@router.get("/lessons/{lesson_id}", response_model=LessonOut)
def get_lesson(lesson_id: str, db: Session = Depends(get_db)) -> Lesson:
    return _get_or_404(db, lesson_id)


@router.get("/lessons/{lesson_id}/quiz", response_model=list[QuizQuestionOut])
def get_lesson_quiz(lesson_id: str, db: Session = Depends(get_db)) -> list[QuizQuestion]:
    return _get_or_404(db, lesson_id).questions


@router.get("/quizzes", response_model=list[QuizQuestionOut], tags=["quizzes"])
def list_all_questions(db: Session = Depends(get_db)) -> list[QuizQuestion]:
    """Every question of every lesson in one call, so the app can cache them for offline use."""
    return list(db.scalars(select(QuizQuestion).order_by(QuizQuestion.lesson_id, QuizQuestion.order)))


# ----- instructor writes -----

@router.post("/lessons", response_model=LessonOut, status_code=status.HTTP_201_CREATED)
def create_lesson(body: LessonCreate, db: Session = Depends(get_db), _: User = Depends(require_instructor)) -> Lesson:
    if db.get(Lesson, body.id) is not None:
        raise HTTPException(status.HTTP_409_CONFLICT, detail=f"Lesson '{body.id}' already exists")
    lesson = Lesson(**body.model_dump(mode="json"))
    db.add(lesson)
    db.commit()
    return lesson


@router.put("/lessons/{lesson_id}", response_model=LessonOut)
def update_lesson(
    lesson_id: str, body: LessonUpdate, db: Session = Depends(get_db), _: User = Depends(require_instructor)
) -> Lesson:
    lesson = _get_or_404(db, lesson_id)
    for field, value in body.model_dump(mode="json").items():
        setattr(lesson, field, value)
    db.commit()
    return lesson


@router.delete("/lessons/{lesson_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_lesson(lesson_id: str, db: Session = Depends(get_db), _: User = Depends(require_instructor)) -> Response:
    db.delete(_get_or_404(db, lesson_id))
    db.commit()
    return Response(status_code=status.HTTP_204_NO_CONTENT)

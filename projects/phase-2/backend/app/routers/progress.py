from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from ..database import get_db
from ..models import Lesson, Progress, User
from ..schemas import ProgressIn, ProgressOut, ProgressSyncItem
from ..security import get_current_user

router = APIRouter(prefix="/progress", tags=["progress"])


def _merge(db: Session, user: User, lesson_id: str, incoming: ProgressIn) -> None:
    """Upsert one row. The newer updated_at wins; the best quiz score never goes down."""
    if db.get(Lesson, lesson_id) is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, detail=f"Lesson '{lesson_id}' not found")

    row = db.scalar(select(Progress).where(Progress.user_id == user.id, Progress.lesson_id == lesson_id))
    if row is None:
        row = Progress(user_id=user.id, lesson_id=lesson_id, status=incoming.status.value, updated_at=incoming.updated_at)
        db.add(row)
    elif incoming.updated_at >= row.updated_at:
        row.status = incoming.status.value
        row.updated_at = incoming.updated_at

    if incoming.quiz_best_score is not None and (
        row.quiz_best_score is None or incoming.quiz_best_score > row.quiz_best_score
    ):
        row.quiz_best_score = incoming.quiz_best_score
        row.quiz_total = incoming.quiz_total


def _all_for(db: Session, user: User) -> list[Progress]:
    return list(db.scalars(select(Progress).where(Progress.user_id == user.id)))


@router.get("", response_model=list[ProgressOut])
def list_progress(user: User = Depends(get_current_user), db: Session = Depends(get_db)) -> list[Progress]:
    return _all_for(db, user)


@router.put("/{lesson_id}", response_model=ProgressOut)
def put_progress(
    lesson_id: str, body: ProgressIn, user: User = Depends(get_current_user), db: Session = Depends(get_db)
) -> Progress:
    _merge(db, user, lesson_id, body)
    db.commit()
    return db.scalar(select(Progress).where(Progress.user_id == user.id, Progress.lesson_id == lesson_id))


@router.post("/sync", response_model=list[ProgressOut])
def sync_progress(
    items: list[ProgressSyncItem], user: User = Depends(get_current_user), db: Session = Depends(get_db)
) -> list[Progress]:
    """Push local changes and get the merged server state back in one round trip (used by the app)."""
    for item in items:
        _merge(db, user, item.lesson_id, item)
    db.commit()
    return _all_for(db, user)

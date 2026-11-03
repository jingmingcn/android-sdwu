from fastapi import APIRouter, Depends, HTTPException, Query, Response, status
from sqlalchemy import or_, select
from sqlalchemy.orm import Session

from ..database import get_db
from ..models import StudentModule, User
from ..schemas import ModuleIn, ModuleOut
from ..security import get_current_user

router = APIRouter(prefix="/modules", tags=["modules"])


def _to_out(module: StudentModule) -> ModuleOut:
    return ModuleOut(
        id=module.id,
        name=module.name,
        description=module.description,
        author=module.author,
        version=module.version,
        category=module.category,
        contributions=module.contributions,
        source_url=module.source_url,
        framework_version=module.framework_version,
        owner_name=module.owner.display_name,
        updated_at=module.updated_at,
    )


def _can_edit(user: User, module: StudentModule) -> bool:
    return module.owner_id == user.id or user.is_instructor


@router.get("", response_model=list[ModuleOut])
def list_modules(
    q: str | None = Query(default=None, max_length=100, description="Search name, description and author"),
    db: Session = Depends(get_db),
) -> list[ModuleOut]:
    """The class catalog: every module students have published, newest first."""
    query = select(StudentModule).order_by(StudentModule.updated_at.desc())
    if q:
        pattern = f"%{q.strip()}%"
        query = query.where(
            or_(StudentModule.name.ilike(pattern), StudentModule.description.ilike(pattern), StudentModule.author.ilike(pattern))
        )
    return [_to_out(m) for m in db.scalars(query)]


@router.get("/{module_id}", response_model=ModuleOut)
def get_module(module_id: str, db: Session = Depends(get_db)) -> ModuleOut:
    module = db.get(StudentModule, module_id)
    if module is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, detail=f"Module '{module_id}' not found")
    return _to_out(module)


@router.put("/{module_id}", response_model=ModuleOut)
def publish_module(
    module_id: str,
    body: ModuleIn,
    response: Response,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> ModuleOut:
    """Create or update (upsert). Only the student who published it first (or an instructor) may update it."""
    if body.id != module_id:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_CONTENT, detail="Body id must match the URL")
    data = body.model_dump(mode="json")
    module = db.get(StudentModule, module_id)
    if module is None:
        module = StudentModule(**data, owner_id=user.id)
        db.add(module)
        response.status_code = status.HTTP_201_CREATED
    elif not _can_edit(user, module):
        raise HTTPException(status.HTTP_403_FORBIDDEN, detail="This module id belongs to another student")
    else:
        for field, value in data.items():
            setattr(module, field, value)
    db.commit()
    db.refresh(module)
    return _to_out(module)


@router.delete("/{module_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_module(module_id: str, user: User = Depends(get_current_user), db: Session = Depends(get_db)) -> Response:
    module = db.get(StudentModule, module_id)
    if module is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, detail=f"Module '{module_id}' not found")
    if not _can_edit(user, module):
        raise HTTPException(status.HTTP_403_FORBIDDEN, detail="Only the owner or an instructor can delete it")
    db.delete(module)
    db.commit()
    return Response(status_code=status.HTTP_204_NO_CONTENT)

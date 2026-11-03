"""Phase 2 backend: SQLite via SQLAlchemy, JWT auth, lessons CRUD, quizzes and progress sync."""

from contextlib import asynccontextmanager

from fastapi import FastAPI

from . import config
from .database import Base, SessionLocal, engine
from .routers import auth, lessons, progress
from .seed import seed


@asynccontextmanager
async def lifespan(_: FastAPI):
    # Phase 2 keeps it simple: create missing tables on startup (no Alembic migrations yet).
    Base.metadata.create_all(engine)
    if config.SEED_DEMO_DATA:
        with SessionLocal() as db:
            seed(db)
    yield


app = FastAPI(
    title="Android Teaching Framework API",
    version="0.2.0",
    description="Phase 2: auth, lessons, quizzes and progress.",
    lifespan=lifespan,
)

app.include_router(auth.router)
app.include_router(lessons.router)
app.include_router(progress.router)


@app.get("/health", tags=["meta"])
def health() -> dict[str, str]:
    return {"status": "ok", "phase": "2"}

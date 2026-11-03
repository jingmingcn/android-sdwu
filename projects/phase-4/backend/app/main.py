"""Phase 4 backend: Phase 3 + a class catalog of student modules (/modules)."""

import logging
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from fastapi.staticfiles import StaticFiles
from sqlalchemy import text
from sqlalchemy.exc import IntegrityError

from .config import settings
from .database import Base, SessionLocal, engine
from .routers import auth, lessons, modules, progress
from .seed import seed

logger = logging.getLogger("teaching")


@asynccontextmanager
async def lifespan(_: FastAPI):
    # Still no Alembic: tables are created on startup. Delete teaching.db after changing models.
    Base.metadata.create_all(engine)
    if settings.seed_demo_data:
        with SessionLocal() as db:
            seed(db)
    if settings.jwt_secret.startswith("dev-only"):
        logger.warning("Using the development JWT_SECRET. Set JWT_SECRET for any shared deployment.")
    yield


app = FastAPI(
    title="Android Teaching Framework API",
    version="0.4.0",
    description="Phase 4: auth, lessons, quizzes, progress, profile, student module catalog.",
    lifespan=lifespan,
)

# CORS only matters for browser clients (e.g. a web dashboard); the Android app ignores it.
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    # Browsers refuse credentials with a wildcard origin, so only allow them for explicit lists.
    allow_credentials="*" not in settings.cors_origins,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.exception_handler(IntegrityError)
async def integrity_error_handler(_: Request, exc: IntegrityError) -> JSONResponse:
    """A unique/foreign-key violation that slipped past the explicit checks becomes a clean 409."""
    logger.info("Integrity error: %s", exc.orig)
    return JSONResponse(status_code=status.HTTP_409_CONFLICT, content={"detail": "Conflicts with existing data"})


# Lesson videos hosted by this server, so the class works on a local network without internet.
# Lessons refer to them with a relative path ("/media/compose-intro.mp4"); the app adds the server address.
app.mount("/media", StaticFiles(directory=Path(__file__).parent / "static" / "media"), name="media")

app.include_router(auth.router)
app.include_router(lessons.router)
app.include_router(progress.router)
app.include_router(modules.router)


@app.get("/health", tags=["meta"])
def health() -> dict[str, str]:
    """Liveness + database check, for monitoring and a quick "is the server up?" test."""
    with SessionLocal() as db:
        db.execute(text("SELECT 1"))
    return {"status": "ok", "phase": "4", "database": "ok"}

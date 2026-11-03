import os
import tempfile

import pytest

# Point the app at a throwaway SQLite file before app.config is imported.
_db_dir = tempfile.mkdtemp()
os.environ["DATABASE_URL"] = f"sqlite:///{_db_dir}/test.db"

from fastapi.testclient import TestClient  # noqa: E402

from app.database import Base, engine  # noqa: E402
from app.main import app  # noqa: E402


@pytest.fixture()
def client():
    Base.metadata.drop_all(engine)
    with TestClient(app) as c:  # "with" runs the lifespan: create tables + seed
        yield c


def login(client: TestClient, email: str, password: str) -> dict[str, str]:
    response = client.post("/auth/login", json={"email": email, "password": password})
    assert response.status_code == 200, response.text
    return {"Authorization": f"Bearer {response.json()['access_token']}"}


@pytest.fixture()
def student(client):
    return login(client, "student@example.com", "student123")


@pytest.fixture()
def teacher(client):
    return login(client, "teacher@example.com", "teacher123")

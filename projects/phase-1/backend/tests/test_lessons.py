from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "ok"


def test_list_lessons_is_seeded_and_ordered():
    response = client.get("/lessons")
    assert response.status_code == 200
    lessons = response.json()
    assert len(lessons) == 8
    assert [l["order"] for l in lessons] == sorted(l["order"] for l in lessons)
    assert lessons[0]["id"] == "kotlin-basics"


def test_get_lesson_by_id():
    assert client.get("/lessons/room-database").json()["title"] == "Local Data with Room"
    assert client.get("/lessons/does-not-exist").status_code == 404

NEW_LESSON = {
    "id": "coroutines-intro",
    "order": 9,
    "title": "Coroutines",
    "summary": "suspend functions and scopes",
    "content": "Text",
    "category": "Kotlin",
    "difficulty": "INTERMEDIATE",
    "duration_minutes": 30,
    "video_url": None,
}


def test_seeded_lessons_and_quizzes(client):
    lessons = client.get("/lessons").json()
    assert len(lessons) == 8
    assert lessons[0]["id"] == "kotlin-basics"

    quiz = client.get("/lessons/kotlin-basics/quiz").json()
    assert len(quiz) == 3
    assert quiz[0]["options"][quiz[0]["correct_index"]] == "val"
    assert len(client.get("/quizzes").json()) == 24


def test_unknown_lesson_is_404(client):
    assert client.get("/lessons/nope").status_code == 404


def test_only_instructors_can_write(client, student, teacher):
    assert client.post("/lessons", json=NEW_LESSON).status_code == 401
    assert client.post("/lessons", json=NEW_LESSON, headers=student).status_code == 403

    assert client.post("/lessons", json=NEW_LESSON, headers=teacher).status_code == 201
    assert client.post("/lessons", json=NEW_LESSON, headers=teacher).status_code == 409

    update = {k: v for k, v in NEW_LESSON.items() if k != "id"} | {"title": "Coroutines 101"}
    assert client.put("/lessons/coroutines-intro", json=update, headers=teacher).json()["title"] == "Coroutines 101"

    assert client.delete("/lessons/coroutines-intro", headers=teacher).status_code == 204
    assert client.get("/lessons/coroutines-intro").status_code == 404


def test_deleting_a_lesson_deletes_its_quiz_and_progress(client, teacher):
    client.put("/progress/kotlin-basics", json={"status": "COMPLETED", "updated_at": 1}, headers=teacher)
    assert client.delete("/lessons/kotlin-basics", headers=teacher).status_code == 204
    assert all(q["lesson_id"] != "kotlin-basics" for q in client.get("/quizzes").json())
    assert client.get("/progress", headers=teacher).json() == []


def test_search_and_filters(client):
    assert [l["id"] for l in client.get("/lessons", params={"q": "room"}).json()] == ["room-database"]
    assert len(client.get("/lessons", params={"category": "Compose"}).json()) == 3
    advanced = client.get("/lessons", params={"difficulty": "ADVANCED"}).json()
    assert [l["id"] for l in advanced] == ["datastore-preferences"]
    assert client.get("/lessons", params={"difficulty": "EXPERT"}).status_code == 422


def test_invalid_video_url_rejected(client, teacher):
    bad = NEW_LESSON | {"video_url": "ftp://broken"}
    assert client.post("/lessons", json=bad, headers=teacher).status_code == 422


def test_health_and_cors(client):
    assert client.get("/health").json()["database"] == "ok"
    response = client.get("/lessons", headers={"Origin": "http://localhost:5173"})
    assert response.headers["access-control-allow-origin"] == "*"


def test_local_media_is_served_with_range_support(client):
    lesson = client.get("/lessons/compose-fundamentals").json()
    assert lesson["video_url"] == "/media/compose-intro.mp4"
    response = client.get(lesson["video_url"], headers={"Range": "bytes=0-99"})
    assert response.status_code == 206  # partial content: players can seek
    assert response.headers["content-type"] == "video/mp4"


def test_video_url_must_be_http_or_local_media(client, teacher):
    for good in ["https://example.com/v.mp4", "/media/clip.mp4"]:
        lesson = NEW_LESSON | {"id": f"v-{len(good)}", "video_url": good}
        assert client.post("/lessons", json=lesson, headers=teacher).status_code == 201
    bad = NEW_LESSON | {"id": "v-bad", "video_url": "/etc/passwd"}
    assert client.post("/lessons", json=bad, headers=teacher).status_code == 422

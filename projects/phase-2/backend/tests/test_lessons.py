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

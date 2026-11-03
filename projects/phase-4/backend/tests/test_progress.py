def test_progress_requires_auth(client):
    assert client.get("/progress").status_code == 401


def test_put_and_list_progress(client, student):
    body = {"status": "IN_PROGRESS", "updated_at": 1000}
    assert client.put("/progress/kotlin-basics", json=body, headers=student).status_code == 200

    rows = client.get("/progress", headers=student).json()
    assert rows == [
        {"lesson_id": "kotlin-basics", "status": "IN_PROGRESS", "quiz_best_score": None, "quiz_total": None, "updated_at": 1000}
    ]


def test_newest_write_wins_and_best_score_never_drops(client, student):
    client.put("/progress/kotlin-basics", json={"status": "COMPLETED", "quiz_best_score": 3, "quiz_total": 3, "updated_at": 2000}, headers=student)

    # An older write from another device: status ignored, lower score ignored.
    stale = [{"lesson_id": "kotlin-basics", "status": "IN_PROGRESS", "quiz_best_score": 1, "quiz_total": 3, "updated_at": 1500}]
    merged = client.post("/progress/sync", json=stale, headers=student).json()
    assert merged[0]["status"] == "COMPLETED"
    assert merged[0]["quiz_best_score"] == 3
    assert merged[0]["updated_at"] == 2000


def test_progress_is_per_user(client, student, teacher):
    client.put("/progress/kotlin-basics", json={"status": "COMPLETED", "updated_at": 1}, headers=student)
    assert client.get("/progress", headers=teacher).json() == []


def test_progress_validation(client, student):
    bad_score = {"status": "COMPLETED", "quiz_best_score": 5, "quiz_total": 3, "updated_at": 1}
    assert client.put("/progress/kotlin-basics", json=bad_score, headers=student).status_code == 422
    unknown = {"status": "COMPLETED", "updated_at": 1}
    assert client.put("/progress/nope", json=unknown, headers=student).status_code == 404

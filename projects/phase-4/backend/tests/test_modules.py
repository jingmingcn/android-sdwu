MODULE = {
    "id": "code-snippets",
    "name": "Code Snippet Viewer",
    "description": "Browse Kotlin snippets",
    "author": "Ada",
    "version": "1.0.0",
    "category": "STUDY_TOOL",
    "contributions": [{"type": "function", "id": "snippet-list", "title": "Snippets"}],
}


def test_catalog_is_public_and_starts_empty(client):
    assert client.get("/modules").json() == []


def test_publish_requires_login(client):
    assert client.put("/modules/code-snippets", json=MODULE).status_code == 401


def test_publish_then_update(client, student):
    created = client.put("/modules/code-snippets", json=MODULE, headers=student)
    assert created.status_code == 201
    assert created.json()["owner_name"] == "Demo Student"

    updated = client.put("/modules/code-snippets", json=MODULE | {"version": "1.1.0"}, headers=student)
    assert updated.status_code == 200
    assert updated.json()["version"] == "1.1.0"
    assert [m["id"] for m in client.get("/modules", params={"q": "snippet"}).json()] == ["code-snippets"]


def test_only_owner_or_instructor_can_change(client, student, teacher):
    client.put("/modules/code-snippets", json=MODULE, headers=student)
    other = client.post(
        "/auth/register", json={"email": "bob@example.com", "password": "secret12", "display_name": "Bob"}
    ).json()["access_token"]
    bob = {"Authorization": f"Bearer {other}"}
    assert client.put("/modules/code-snippets", json=MODULE, headers=bob).status_code == 403
    assert client.delete("/modules/code-snippets", headers=bob).status_code == 403
    assert client.delete("/modules/code-snippets", headers=teacher).status_code == 204
    assert client.get("/modules/code-snippets").status_code == 404


def test_module_validation(client, student):
    bad_id = MODULE | {"id": "Bad Id"}
    assert client.put("/modules/Bad%20Id", json=bad_id, headers=student).status_code == 422
    assert client.put("/modules/other-id", json=MODULE, headers=student).status_code == 422  # id mismatch
    assert client.put("/modules/code-snippets", json=MODULE | {"version": "v1"}, headers=student).status_code == 422
    assert client.put("/modules/code-snippets", json=MODULE | {"contributions": []}, headers=student).status_code == 422
    wrong_type = MODULE | {"contributions": [{"type": "widget", "id": "abc", "title": "x"}]}
    assert client.put("/modules/code-snippets", json=wrong_type, headers=student).status_code == 422

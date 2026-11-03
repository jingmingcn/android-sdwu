def test_register_login_and_me(client):
    response = client.post(
        "/auth/register", json={"email": "New@Example.com", "password": "secret1", "display_name": "New Student"}
    )
    assert response.status_code == 201
    body = response.json()
    assert body["user"]["email"] == "new@example.com"
    assert body["user"]["is_instructor"] is False

    me = client.get("/auth/me", headers={"Authorization": f"Bearer {body['access_token']}"})
    assert me.json()["display_name"] == "New Student"


def test_duplicate_email_is_rejected(client):
    payload = {"email": "student@example.com", "password": "whatever1", "display_name": "Dup"}
    assert client.post("/auth/register", json=payload).status_code == 409


def test_wrong_password(client):
    response = client.post("/auth/login", json={"email": "student@example.com", "password": "nope"})
    assert response.status_code == 401


def test_validation_errors(client):
    assert client.post("/auth/register", json={"email": "not-an-email", "password": "secret1", "display_name": "x"}).status_code == 422
    assert client.post("/auth/register", json={"email": "a@b.co", "password": "123", "display_name": "x"}).status_code == 422


def test_me_requires_token(client):
    assert client.get("/auth/me").status_code == 401
    assert client.get("/auth/me", headers={"Authorization": "Bearer garbage"}).status_code == 401


def test_password_needs_letters_and_digits(client):
    for password in ["abcdefg", "1234567"]:
        body = {"email": "p@example.com", "password": password, "display_name": "P"}
        assert client.post("/auth/register", json=body).status_code == 422


def test_unknown_fields_are_rejected(client):
    body = {"email": "x@example.com", "password": "secret1", "display_name": "X", "displayName": "typo"}
    assert client.post("/auth/register", json=body).status_code == 422


def test_whitespace_is_stripped(client):
    body = {"email": "  space@example.com ", "password": "secret1", "display_name": "  Spacey  "}
    assert client.post("/auth/register", json=body).json()["user"]["display_name"] == "Spacey"


def test_update_profile(client, student):
    response = client.patch(
        "/auth/me", json={"display_name": "Renamed", "avatar_url": "https://example.com/a.png"}, headers=student
    )
    assert response.status_code == 200
    assert response.json()["display_name"] == "Renamed"
    assert response.json()["avatar_url"] == "https://example.com/a.png"

    # Only sent fields change; null removes the avatar.
    cleared = client.patch("/auth/me", json={"avatar_url": None}, headers=student).json()
    assert cleared["display_name"] == "Renamed"
    assert cleared["avatar_url"] is None


def test_update_profile_validation(client, student):
    assert client.patch("/auth/me", json={"avatar_url": "not a url"}, headers=student).status_code == 422
    assert client.patch("/auth/me", json={"display_name": ""}, headers=student).status_code == 422
    assert client.patch("/auth/me", json={"display_name": None}, headers=student).status_code == 422
    assert client.patch("/auth/me", json={"display_name": "x"}).status_code == 401

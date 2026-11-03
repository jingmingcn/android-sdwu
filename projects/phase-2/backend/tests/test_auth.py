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
    payload = {"email": "student@example.com", "password": "whatever", "display_name": "Dup"}
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

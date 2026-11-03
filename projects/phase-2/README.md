# Phase 2: Core functionality

Phase 2 connects the Phase 1 prototype to a real local backend. The app now has
**email/password login with JWT**, **lesson progress**, **multiple-choice quizzes** and an
**offline-first** data layer. Everything still works with no server at all.

```
phase-2/
├── README.md
├── backend/                        FastAPI + SQLAlchemy 2 + SQLite
│   ├── app/
│   │   ├── main.py                 app setup; creates tables and seeds data on startup
│   │   ├── config.py               settings from environment variables
│   │   ├── database.py             engine, session, get_db dependency, SQLite foreign keys
│   │   ├── models.py               User, Lesson, QuizQuestion, Progress
│   │   ├── schemas.py              Pydantic request/response models with validation
│   │   ├── security.py             bcrypt hashing, JWT create/verify, current-user dependencies
│   │   ├── seed.py                 lessons, quizzes and two demo accounts
│   │   ├── routers/                auth.py, lessons.py (CRUD + quizzes), progress.py
│   │   └── data/                   lessons.json, quizzes.json (seed data)
│   ├── tests/                      pytest: auth, lessons, progress (14 tests)
│   └── requirements.txt
└── android/
    └── app/src/main/java/edu/sdwu/teaching/
        ├── domain/
        │   ├── model/              + LessonProgress, QuizQuestion, QuizScorer, User, SessionState
        │   └── repository/         + AuthRepository, ProgressRepository (interfaces)
        ├── data/
        │   ├── remote/             NEW: TeachingApi (Retrofit), Network.kt (OkHttp + interceptors), dto/
        │   ├── local/              + QuizQuestionEntity, ProgressEntity, ProgressDao, Converters, Migrations
        │   ├── prefs/              + SessionStore (JWT in DataStore), server URL preference
        │   └── repository/         OfflineFirstLessonRepository, OfflineFirstProgressRepository,
        │                           RemoteAuthRepository, ProgressMerge.kt
        ├── di/AppContainer.kt      wires the network layer into the repositories
        └── ui/
            ├── auth/               NEW: login / register / continue as guest
            ├── quiz/               NEW: one question at a time, local scoring, review screen
            ├── lessons/ home/ profile/   progress, refresh, sync, sign out, server address
            └── components/         LessonCard with progress, ServerUrlDialog
```

## Run it

The Android app runs from **Android Studio**. The backend runs directly on your computer (`localhost`) with Python and uvicorn; no Docker is needed.

### What you need

| Tool | Version |
|---|---|
| Android Studio | a 2026 release that supports AGP 9.5. Its bundled JDK is fine; no separate JDK is needed |
| Android SDK | Platform 37. Android Studio offers to install it during the first Gradle sync |
| Device | an emulator from Android Studio's Device Manager, or a phone, with Android 8.0 (API 26) or newer |
| Python | 3.12 or newer, for the backend |

### Start the backend on localhost

Use any terminal, for example the **Terminal** tab at the bottom of Android Studio.

```bash
cd projects/phase-2/backend
```

Create a virtual environment and install the dependencies (first time only). On macOS or Linux:

```bash
python3 -m venv .venv && source .venv/bin/activate && pip install -r requirements.txt
```

On Windows (PowerShell):

```powershell
py -m venv .venv; .venv\Scripts\Activate.ps1; pip install -r requirements.txt
```

Start the server:

```bash
uvicorn app.main:app --reload --host 127.0.0.1 --port 8000
```

Leave it running and stop it with **Ctrl+C**. The next time, you only need to activate the environment
(`source .venv/bin/activate`, or `.venv\Scripts\Activate.ps1` on Windows) and run `uvicorn` again.
`--reload` restarts the server whenever you save a Python file.

On first start the server creates `teaching.db` (SQLite) in the `backend/` folder and seeds the lessons, the quiz
questions and two **demo accounts for local development**:

| Email | Password | Role |
|---|---|---|
| `student@example.com` | `student123` | student |
| `teacher@example.com` | `teacher123` | instructor (may create, edit and delete lessons) |

Check that it works: open http://localhost:8000/docs (interactive Swagger UI) or http://localhost:8000/health.

Backend tests (14), with the environment active: `pytest`.

Settings come from environment variables; the defaults work for local development:

| Variable | Default | Meaning |
|---|---|---|
| `DATABASE_URL` | `sqlite:///./teaching.db` | any SQLAlchemy URL |
| `JWT_SECRET` | a development value | **change this** outside your laptop |
| `JWT_EXPIRE_MINUTES` | `10080` (7 days) | token lifetime |
| `SEED_DEMO_DATA` | `true` | seed lessons and demo users when empty |

To start again with fresh seed data, stop the server and delete `teaching.db`.

### Run the app in Android Studio

1. **File → Open…** and choose `projects/phase-2/android`, the folder that contains `settings.gradle.kts`
   (not `projects/` or `phase-2/`).
2. Wait for the **Gradle sync** to finish. If Android Studio asks to install SDK Platform 37 or to accept
   licences, accept. It writes `local.properties` with your SDK path itself.
3. If the sync fails with a JDK error: **Settings → Build, Execution, Deployment → Build Tools → Gradle →
   Gradle JDK** → choose the bundled JDK (17 or newer).
4. No emulator yet? **Tools → Device Manager → + (Create Virtual Device)** → a Pixel phone with a recent
   system image (API 26 or newer; API 35 or 36 recommended).
5. In the toolbar select the **app** run configuration and your device, then click **Run ▶**
   (Shift+F10, or Ctrl+R on macOS).

Unit tests (12, no device needed): in Android Studio's **Terminal** tab run

```bash
./gradlew testDebugUnitTest
```

On Windows use `gradlew.bat` instead of `./gradlew`. You can also right-click a test file or a `test` folder
in the Project view and choose **Run**.

### Connect the app to the backend

The app's default server is `http://10.0.2.2:8000/`. On the Android **emulator**, `10.0.2.2` is your
computer's `localhost`, so with the backend started as above, the app connects with no extra setup.
Plain HTTP is allowed in **debug builds only** (`src/debug/AndroidManifest.xml`).

To use another address, tap **Server: …** on the sign-in screen or go to **Profile → Server**:

| Situation | What to do |
|---|---|
| Port 8000 is already used on your computer | Start uvicorn with `--port 8010` and set the server to `http://10.0.2.2:8010/` |
| Real phone on USB (with USB debugging on) | Run `adb reverse tcp:8000 tcp:8000` once per connection, then set the server to `http://localhost:8000/`. The server stays on localhost |
| Real phone on the same Wi-Fi | Start uvicorn with `--host 0.0.0.0` (this opens the server to your network) and set the server to `http://<your-computer-ip>:8000/` |

The app works without the server too: lessons and quizzes are cached in Room, and your progress syncs the next
time the server is reachable.

## What to try

1. **Continue without account** works offline: the lessons and quizzes are bundled in the app.
2. Open a lesson: it becomes *In progress*. Take the quiz. A score of 70% or more marks the lesson *Completed*.
3. Go to **Profile → Sign in or create account**, then sign in as `student@example.com`. Your guest
   progress is uploaded to the account (`GET /progress` in Swagger shows it).
4. Change progress on the server (Swagger `PUT /progress/{lesson_id}`), then tap **Profile → Sync now**.
   The app pulls the change.
5. Stop the backend and tap **Refresh** on the Lessons tab. A snackbar says you are offline and the cached
   lessons stay on screen.
6. **Sign out**: the account's local progress is removed, and the next user starts clean.
7. Upgrading from Phase 1 keeps the data: Room migration `1 → 2` adds the new tables.

## How offline-first works here

```
  UI ──observes──▶ Room (single source of truth)  ◀──writes── Repository ◀──fetches── Retrofit API
                          ▲                                       │
                          └──── local writes (pending_sync=1) ────┘──push/pull──▶ POST /progress/sync
```

* **Reads** always come from Room Flows, so screens never wait for the network.
* **Lessons/quizzes:** `refresh()` downloads both in parallel and replaces the catalogue in one Room
  `@Transaction`. If it fails, nothing changes.
* **Progress writes** go to Room immediately with `pending_sync = 1`, then a background sync is attempted.
  If it fails, the rows stay pending and are retried on the next app start, sign-in, refresh or *Sync now*.
* **Conflicts:** the server keeps the row with the newest `updated_at` (epoch millis), and the best quiz
  score never goes down. The client side of the merge is the pure function `resolveSyncedProgress`
  (unit tested in `ProgressMergeTest`).
* **401 from the server** (expired token) signs the user out automatically.

## API summary

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/auth/register` | - | create account → `{access_token, user}` |
| POST | `/auth/login` | - | sign in → `{access_token, user}` |
| GET | `/auth/me` | user | current user |
| GET | `/lessons`, `/lessons/{id}` | - | read lessons |
| POST / PUT / DELETE | `/lessons`, `/lessons/{id}` | instructor | lessons CRUD |
| GET | `/lessons/{id}/quiz`, `/quizzes` | - | quiz questions (one lesson / all) |
| GET | `/progress` | user | my progress |
| PUT | `/progress/{lesson_id}` | user | upsert one row |
| POST | `/progress/sync` | user | push a list, receive the merged state |

## What changed since Phase 1

| Area | Phase 1 | Phase 2 |
|---|---|---|
| Backend | static JSON, 1 endpoint | SQLAlchemy + SQLite, JWT auth, CRUD, progress sync, 14 tests |
| Network | none | Retrofit 3 + OkHttp 5 + kotlinx.serialization, auth and server-URL interceptors |
| Room | `lessons` (v1) | + `quiz_questions`, `progress` (v2) with a hand-written `Migration(1, 2)` |
| DataStore | theme | + server URL, separate `session` file for JWT and user |
| Repositories | `OfflineLessonRepository` | `OfflineFirstLessonRepository`, `OfflineFirstProgressRepository`, `RemoteAuthRepository` |
| UI | Home, Lessons, Detail, Profile | + Login/Register/Guest gate, Quiz, progress chips, refresh, sync, sign out |
| Tests | 3 | 12 Android unit tests (scoring, merge, mapping, converters) + 14 backend tests |

**The extension point from Phase 1 paid off.** `LessonRepository` stayed an interface, so the lesson screens only
changed to *add* features. Nothing changed because the data now comes from the network.

New extension points to notice:

* `QuizScorer` is a plain object with a pure `score()` function. You could swap in another scoring strategy, for
  example partial credit.
* `TeachingApi` adds an endpoint with one annotated function.
* `ServerUrlInterceptor` / `AuthInterceptor` are small OkHttp interceptors. Add your own (e.g. a request ID header).
* `SessionState` is a sealed interface: `MainActivity` decides what to show with an exhaustive `if`/`when`.

## Student exercises

1. Add a "Forgot password?" placeholder screen to the auth flow.
2. Give instructors a "New lesson" button that calls `POST /lessons` (hint: `User.isInstructor` is already in the session).
3. Shuffle the quiz options while keeping scoring correct.
4. Add a `last_opened_at` column to `ProgressEntity` with a `Migration(2, 3)`, and show "Last opened 2 days ago".

## Notes and limits (addressed in Phase 3)

* No pull-to-refresh, WorkManager background sync, Hilt, or ViewModel tests yet. Those all arrive in Phase 3.
* The JWT is stored in plain DataStore. That's fine for a course project, but a production app should encrypt it.
* Quiz answers are sent to the client so scoring works offline. Don't do this for graded exams.
* No Alembic migrations on the backend yet: delete `teaching.db` if you change the models.

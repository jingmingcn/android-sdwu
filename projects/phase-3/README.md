# Phase 3: Polished, feature-complete base app

Phase 3 turns the app into a real teaching-class product:
- notes and bookmarks
- search and filters
- a 100% Compose video player
- background sync and daily study reminders with WorkManager
- profile editing with avatars loaded by Coil
- loading, error and empty states everywhere, plus pull-to-refresh
- Hilt for dependency injection everywhere
- unit tests for ViewModels and repositories

The backend gains:
- typed settings from environment variables
- CORS
- stricter validation
- profile editing and lesson search
- locally hosted lesson videos

```
phase-3/
├── README.md
├── backend/
│   ├── .env.example                every setting, documented
│   ├── app/
│   │   ├── config.py               pydantic-settings Settings (env vars / .env)
│   │   ├── main.py                 CORS, IntegrityError → 409, /media static files, /health with DB check
│   │   ├── schemas.py              StrictInput: strips whitespace, rejects unknown fields
│   │   ├── routers/auth.py         + PATCH /auth/me (name, avatar_url)
│   │   ├── routers/lessons.py      + ?q=&category=&difficulty= search
│   │   └── static/media/           compose-intro.mp4 (12 s sample clip served at /media/…)
│   └── tests/                      24 pytest tests
└── android/app/src/
    ├── main/java/edu/sdwu/teaching/
    │   ├── TeachingApp.kt          @HiltAndroidApp, WorkManager + Coil configuration
    │   ├── di/AppModules.kt        Hilt modules (replace Phase 2's AppContainer)
    │   ├── domain/
    │   │   ├── model/              + Note, LessonFilter (pure filtering), Validation, resolveMediaUrl
    │   │   └── repository/         + NoteRepository, BookmarkRepository
    │   ├── data/
    │   │   ├── local/              + StudyEntities.kt (notes, bookmarks, StudyDao), Migration(2, 3)
    │   │   ├── work/               NEW: SyncWorker + SyncScheduler, StudyReminderWorker + ReminderScheduler
    │   │   └── repository/         + LocalNoteRepository, LocalBookmarkRepository
    │   └── ui/
    │       ├── components/         + LessonVideoPlayer (Media3), UserAvatar (Coil), StateViews,
    │       │                         NoteDialog, BookmarkIcons (custom ImageVectors)
    │       ├── lessons/            search bar, filter chips, pull-to-refresh, notes, bookmark
    │       ├── notes/              NEW: Notes & bookmarks screen
    │       └── profile/            + EditProfileScreen, reminders, notification permission
    └── test/java/edu/sdwu/teaching/
        ├── testing/                Fakes.kt (every domain interface), MainDispatcherRule
        ├── ui/ViewModelTests.kt    6 ViewModels, 18 tests (one with Turbine)
        └── data/repository/        progress sync + lesson refresh with fake DAO/API/session
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
cd projects/phase-3/backend
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

Backend tests (24), with the environment active: `pytest`.

To change a setting, copy `.env.example` to `.env` in the `backend/` folder and edit it; the server reads it on
start. Outside your own computer, set `JWT_SECRET` to at least 32 random characters (the server logs a warning
while the development secret is in use). To start again with fresh seed data, stop the server and delete
`teaching.db`.

### Run the app in Android Studio

1. **File → Open…** and choose `projects/phase-3/android`, the folder that contains `settings.gradle.kts`
   (not `projects/` or `phase-3/`).
2. Wait for the **Gradle sync** to finish. If Android Studio asks to install SDK Platform 37 or to accept
   licences, accept. It writes `local.properties` with your SDK path itself.
3. If the sync fails with a JDK error: **Settings → Build, Execution, Deployment → Build Tools → Gradle →
   Gradle JDK** → choose the bundled JDK (17 or newer).
4. No emulator yet? **Tools → Device Manager → + (Create Virtual Device)** → a Pixel phone with a recent
   system image (API 26 or newer; API 35 or 36 recommended).
5. In the toolbar select the **app** run configuration and your device, then click **Run ▶**
   (Shift+F10, or Ctrl+R on macOS).

Unit tests (45, no device needed): in Android Studio's **Terminal** tab run

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

1. **Lessons tab**: type "room" in the search bar, then combine the chips (Bookmarked, status, category,
   difficulty). Pull down to refresh. With the server stopped, a snackbar explains that you're offline.
2. **Lesson detail**:
   - Tap the bookmark icon.
   - Play the video. *Jetpack Compose Fundamentals* plays a clip served by **your own backend**
     (`/media/compose-intro.mp4`), so it works on a classroom LAN without internet.
   - Add, edit and delete notes at the bottom of the lesson.
3. **Profile → Notes & bookmarks** lists everything you saved.
4. **Profile → Edit** (when signed in): change your name and paste an image URL. Coil previews it live.
5. **Profile → Daily study reminder**: Android 13+ asks for notification permission. Pick a time, or tap
   **Send a test reminder now**.
6. **Background sync**: mark a lesson complete while signed in. WorkManager uploads it even if you close the
   app. Check `GET /progress` in Swagger.
7. **Upgrading from Phase 2** keeps everything: Room migration `2 → 3` adds the `notes` and `bookmarks` tables.

## What changed since Phase 2

| Area | Phase 2 | Phase 3 |
|---|---|---|
| DI | manual `AppContainer` + `AppViewModelProvider` | **Hilt**: `@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`, `@HiltWorker`, `@Binds` modules |
| Route arguments | `SavedStateHandle.toRoute()` inside ViewModels | NavHost decodes the route and passes `lessonId` via **assisted injection** (testable on the JVM) |
| Background work | fire-and-forget coroutine | **WorkManager**: one-time + 6-hourly `SyncWorker` with backoff, daily `StudyReminderWorker` |
| Room | v2 | v3: `notes`, `bookmarks` (+ `Migration(2, 3)`) |
| Lessons screen | list + refresh button | search, 4 kinds of filter chips, pull-to-refresh, empty state |
| Video | placeholder | Media3 ExoPlayer + `PlayerSurface` (no XML `PlayerView`); backend-hosted `/media` clips |
| Profile | name, sync, sign out | Coil avatar, edit profile screen, reminders + `POST_NOTIFICATIONS` permission, notes link |
| States | ad-hoc | shared `LoadingState`, `ErrorState`, `EmptyState` |
| Tests | 12 pure tests | 45: ViewModels (fakes + coroutines-test + Turbine), repositories (fake DAO/API), pure logic |
| Backend | env via `os.getenv` | pydantic-settings, CORS, strict input models, password rule, `PATCH /auth/me`, lesson search, static media, Compose |

## Design notes worth teaching

* **Interfaces everywhere.** `SessionDataSource`, `SyncScheduler` and `ReminderScheduler` were promoted to
  interfaces in this phase so that repositories and ViewModels can be tested with in-memory fakes
  (`testing/Fakes.kt`). Hilt binds the real implementations in `di/AppModules.kt`.
* **The one-time sync has no network constraint, on purpose.** Android only reports "connected" for networks with
  validated *internet* access. A class server on a Wi-Fi with no internet, or an emulator behind a proxy, would
  never qualify, and the work would wait forever. The worker just tries, and WorkManager retries with backoff. We
  found this while testing on the emulator. The comment in `SyncWork.kt` explains it.
* **Two OkHttp clients from one pool.** Coil uses the plain base client. The API client adds the server-URL and auth
  interceptors on top, so image URLs are never rewritten to point at the backend.
* **`"avatar_url": null` really means "remove".** `NetworkJson` drops nulls, so the PATCH body is built with
  `buildJsonObject` (`updateProfileBody`). Partial updates need explicit nulls.
* **Personal data on sign-out.** An account's progress, notes and bookmarks are deleted when it signs out. A
  guest's data is kept so it can be uploaded after the guest creates an account.

## Extension points (preview of Phase 4)

| Extension point | How to extend it |
|---|---|
| `LessonFilter.apply` | add a field (e.g. "has notes") and a chip; it's pure Kotlin, so add a unit test first |
| `StateViews.kt` | reuse `EmptyState` / `ErrorState` in any new screen |
| `SyncScheduler`, `ReminderScheduler` | add another worker (e.g. "download videos for offline") behind an interface |
| `RepositoryModule` | `@Binds` a new interface to its implementation; every ViewModel can then inject it |
| `LessonVideoPlayer(videoUrl)` | swap or decorate the player (subtitles, speed control) without touching screens |

## Student exercises

1. Add a "Has notes" filter chip. Hint: `NoteRepository.observeAllNotes()` and `LessonFilter`.
2. Sync notes to the backend: add a `notes` table and `/notes` endpoints, then reuse the pending-sync pattern
   from `OfflineFirstProgressRepository`.
3. Add playback-speed buttons to `LessonVideoPlayer` (`player.setPlaybackSpeed(1.5f)`).
4. Write a ViewModel test for `NotesViewModel` using the existing fakes.

## Notes and limits

* The JWT is still stored in plain DataStore. Encrypt it for production.
* There are no backend migrations (Alembic) yet. Delete `teaching.db` after changing models (development only).
* Two lessons use public sample videos (Google's Media3 test media and test-videos.co.uk). If those hosts
  disappear, the player shows a clear error with a retry button, and the backend-hosted clip keeps working.

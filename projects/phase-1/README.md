# Phase 1: Sketch / MVP

A runnable local prototype of the **Android Teaching Framework**. It shows the core UI flow
(Home, Lessons and Profile), stores lessons in Room, and stores the theme in DataStore.
The Android app runs fully offline. The backend is a separate, minimal FastAPI server that
returns the same lessons over HTTP. In Phase 2 the app starts calling it.

```
phase-1/
├── README.md                     ← you are here
├── backend/                      FastAPI, read-only lesson API
│   ├── app/
│   │   ├── main.py               GET /health, GET /lessons, GET /lessons/{id}
│   │   ├── schemas.py            Pydantic Lesson model
│   │   └── data/lessons.json     seed data (source of truth for both sides)
│   ├── tests/test_lessons.py
│   └── requirements.txt
└── android/                      single-module Compose app
    ├── settings.gradle.kts, build.gradle.kts, gradle.properties
    ├── gradle/libs.versions.toml version catalog (all dependency versions)
    └── app/
        ├── build.gradle.kts
        ├── schemas/              exported Room schema (version 1)
        └── src/main/java/edu/sdwu/teaching/
            ├── TeachingApp.kt            Application: builds AppContainer, seeds Room
            ├── MainActivity.kt           reads the theme, hosts Compose
            ├── di/AppContainer.kt        manual DI (replaced by Hilt in Phase 3)
            ├── domain/
            │   ├── model/                Lesson, Difficulty, ThemeMode, UserPreferences
            │   └── repository/           LessonRepository, PreferencesRepository (interfaces)
            ├── data/
            │   ├── local/                Room: AppDatabase, LessonDao, LessonEntity, SeedData
            │   ├── prefs/                DataStore implementation of PreferencesRepository
            │   └── repository/           OfflineLessonRepository (Room only)
            └── ui/
                ├── AppViewModelProvider.kt   one ViewModel factory for every screen
                ├── navigation/           type-safe routes + Scaffold with bottom bar
                ├── home/  lessons/  profile/ Screen (stateless Content + stateful wrapper) + ViewModel
                ├── components/           LessonCard, LessonMeta, VideoPlaceholder
                └── theme/                Material 3 colors, typography, dark + dynamic color
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
cd projects/phase-1/backend
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

Check that it works: open http://localhost:8000/docs (interactive Swagger UI) or http://localhost:8000/health.

Backend tests (3), with the environment active: `pytest`.

### Run the app in Android Studio

1. **File → Open…** and choose `projects/phase-1/android`, the folder that contains `settings.gradle.kts`
   (not `projects/` or `phase-1/`).
2. Wait for the **Gradle sync** to finish. If Android Studio asks to install SDK Platform 37 or to accept
   licences, accept. It writes `local.properties` with your SDK path itself.
3. If the sync fails with a JDK error: **Settings → Build, Execution, Deployment → Build Tools → Gradle →
   Gradle JDK** → choose the bundled JDK (17 or newer).
4. No emulator yet? **Tools → Device Manager → + (Create Virtual Device)** → a Pixel phone with a recent
   system image (API 26 or newer; API 35 or 36 recommended).
5. In the toolbar select the **app** run configuration and your device, then click **Run ▶**
   (Shift+F10, or Ctrl+R on macOS).

Unit tests (3, no device needed): in Android Studio's **Terminal** tab run

```bash
./gradlew testDebugUnitTest
```

On Windows use `gradlew.bat` instead of `./gradlew`. You can also right-click a test file or a `test` folder
in the Project view and choose **Run**.

> Phase 1's app doesn't call the server yet: it runs fully offline. From Phase 2 the emulator reaches the
> server at `http://10.0.2.2:8000/`, which is your computer's `localhost`.

## What to try

1. **Home** shows statistics computed from Room and the first three lessons.
2. Tap a lesson to open **Lesson detail**: a video placeholder, the metadata chips and the lesson text.
   Back returns to the previous tab.
3. **Lessons** lists all 8 seeded lessons. Switch tabs and come back: the scroll position is kept.
4. **Profile → Theme → Dark.** Kill the app and reopen it. The choice was saved in DataStore.
   On Android 12+ you can also turn on **Dynamic color**.
5. Turn on TalkBack: each lesson card is read as one sentence, and headings are marked as headings.

## Seed data

`backend/app/data/lessons.json` has 8 Android lessons (Kotlin, Compose, layouts, state, navigation,
ViewModel, Room and DataStore). `android/.../data/local/SeedData.kt` contains the same lessons with
the same ids. On first launch they are inserted into Room by `LessonRepository.seedIfEmpty()`, so the
app is never empty. Both sides use the same ids so that Phase 2 can refresh rows from the server
without creating duplicates.

## Architecture in one picture

```
Composable Screen ──collects──▶ ViewModel (StateFlow<UiState>) ──▶ Repository interface (domain)
                                                                       │
                                                  OfflineLessonRepository / DataStorePreferencesRepository (data)
                                                                       │
                                                                Room DAO / DataStore
```

* **Screens** are split into a stateful `XxxScreen` (gets the ViewModel) and a stateless
  `XxxContent` (just parameters). Previews and future UI tests use `XxxContent`.
* **UI state** is a sealed interface (`Loading`, `Success`, `NotFound`), so `when` must handle every case.
* **ViewModels depend on interfaces**, not on Room or DataStore. Phase 2 adds a network-backed
  repository without changing a single screen.

## Extension points already visible in Phase 1

The full module system comes in Phase 4. These seams are already in place:

| Seam | How you extend it |
|---|---|
| `TopLevelDestination` enum + `TeachingAppRoot` NavHost | Add a route and an enum entry to get a new tab |
| `LessonRepository` interface | Swap or decorate the data source |
| `AppContainer` + `AppViewModelProvider` | Register a new dependency or ViewModel in one place |
| `VideoPlaceholder(hasVideo)` | Phase 3 replaces it with a real player using the same call site |

## Student exercises

1. Add a `favorite` column to `LessonEntity` (bump the DB version, add a migration) and a star on `LessonCard`.
2. Add a fourth bottom tab, "About", that shows the course syllabus.
3. Add a "text size" preference to DataStore and apply it in `TeachingTheme`.
4. Add an `Error` state to `LessonListUiState` and show a retry button.

## Tech choices (Phase 1)

* AGP 9.5 with **built-in Kotlin** (no `kotlin-android` plugin), Kotlin 2.4, Gradle 9.8, KSP 2.3
* Compose BOM 2026.09.00, Material 3, Navigation Compose 2.10 (type-safe `@Serializable` routes)
* Room 2.8 (KSP, exported schema), DataStore Preferences 1.3, Lifecycle 2.11
* compileSdk 37, targetSdk 36, minSdk 26
* **Not used yet, by design:** Retrofit/OkHttp and Coil (Phase 2), WorkManager and Hilt (Phase 3)

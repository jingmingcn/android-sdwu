# Phase 5: Student custom function development

The final phase makes it **easy for students to build their own features**:
- **Scaffolding:** one Gradle command creates a working module from a template.
- **Discovery:** modules are found automatically, with no central list to edit.
- **Two example student modules:** a Code Snippet Viewer and a Simple Calculator.
- **Enable/disable switches** for every module.
- **Crash protection:** a module that crashes the app is switched off automatically.
- **Guides:** a developer guide inside the app, and the full [**STUDENT_GUIDE.md**](STUDENT_GUIDE.md).

```
phase-5/
├── STUDENT_GUIDE.md                 ★ "How students add a custom function" (step by step)
├── backend/                         + source_url and framework_version on catalog entries (30 tests)
└── android/
    ├── build.gradle.kts             ★ ./gradlew newModule task
    ├── settings.gradle.kts          ★ includes every folder in modules/ automatically
    ├── templates/module/            ★ the template newModule copies (screen, test, discovery file, README)
    ├── framework/
    │   ├── ModuleDiscovery.kt       ★ ServiceLoader discovery, BuiltInModule marker
    │   └── consumer-rules.pro       keeps providers when the app is minified
    ├── modules/
    │   ├── study-timer/             built-in (Phase 4)
    │   ├── lesson-formatter/        built-in (Phase 4)
    │   ├── code-snippets/           ★ student example 1: screen + Home widget + storage + syntax highlighting
    │   └── calculator/              ★ student example 2: screen + parser (Expression.kt) + history
    └── app/src/main/java/edu/sdwu/teaching/
        ├── TeachingApp.kt           discover → register → apply crash guard
        ├── modules/InstalledModules.kt          optional manual registration (empty by default)
        ├── data/modules/ModuleCrashGuard.kt     ★ disables a module that crashed the app
        └── ui/modules/              ★ switches, DeveloperGuideScreen, class catalog with code links
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
cd projects/phase-5/backend
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

Backend tests (30), with the environment active: `pytest`.

To change a setting, copy `.env.example` to `.env` in the `backend/` folder and edit it; the server reads it on
start. Outside your own computer, set `JWT_SECRET` to at least 32 random characters (the server logs a warning
while the development secret is in use). To start again with fresh seed data, stop the server and delete
`teaching.db`.

### Run the app in Android Studio

1. **File → Open…** and choose `projects/phase-5/android`, the folder that contains `settings.gradle.kts`
   (not `projects/` or `phase-5/`).
2. Wait for the **Gradle sync** to finish. If Android Studio asks to install SDK Platform 37 or to accept
   licences, accept. It writes `local.properties` with your SDK path itself.
3. If the sync fails with a JDK error: **Settings → Build, Execution, Deployment → Build Tools → Gradle →
   Gradle JDK** → choose the bundled JDK (17 or newer).
4. No emulator yet? **Tools → Device Manager → + (Create Virtual Device)** → a Pixel phone with a recent
   system image (API 26 or newer; API 35 or 36 recommended).
5. In the toolbar select the **app** run configuration and your device, then click **Run ▶**
   (Shift+F10, or Ctrl+R on macOS).

Unit tests (77 (app, framework and every module), no device needed): in Android Studio's **Terminal** tab run

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

## Add a custom function in three steps

1. Create the module. In Android Studio's **Terminal** tab (it opens in the `android/` folder), run:

   ```bash
   ./gradlew newModule -Pid=flash-cards -Pname="Flash Cards" -Pauthor="Ada Lovelace"
   ```

2. Click **File → Sync Project with Gradle Files** (or the elephant icon in the toolbar). The new
   `modules/flash-cards` folder appears in the Project view. Run its tests:

   ```bash
   ./gradlew :modules:flash-cards:testDebugUnitTest
   ```

3. Click **Run ▶** with the **app** configuration selected.

Then open **Modules → Flash Cards**. That's it. [STUDENT_GUIDE.md](STUDENT_GUIDE.md) explains what to do next.
Inside the app, **Modules → Build your own module** has the same steps.

## How dynamic registration works

```
modules/<id>/build.gradle.kts exists
        │  settings.gradle.kts: include(":modules:<id>")        (folder scan)
        │  app/build.gradle.kts: implementation(project(...))   (same scan)
        ▼
modules/<id>/src/main/resources/META-INF/services/edu.sdwu.teaching.framework.ModuleProvider
        │  one line: edu.sdwu.teaching.modules.<pkg>.<Class>Module
        │  merged by Gradle into the APK
        ▼
TeachingApp.onCreate ─▶ ModuleDiscovery.discover()   (java.util.ServiceLoader)
        │                 broken entries → ModuleProblem (others still load)
        ▼
ModuleRegistry.register(provider, BUILT_IN if it implements BuiltInModule else STUDENT)
        │                 validation → ModuleProblem, or accepted + state saved in Room
        ▼
ModuleCrashGuard.applyPendingCrash()   → switches off a module that crashed the app last run
```

There are two ways to register, both supported:
- **Discovery** (the normal way): described above.
- **A simple registry call:** `moduleRegistry.register(MyModule())`, or a Hilt `@Provides @IntoSet` in
  `InstalledModules.kt`. Use this for providers that need constructor arguments.

## The two student examples

| | Code Snippet Viewer (`code-snippets`) | Simple Calculator (`calculator`) |
|---|---|---|
| Created with | `newModule -Pid=code-snippets` | `newModule -Pid=calculator` |
| Contributions | `CustomFunction` (screen) + `ToolEntry` ("Snippet of the day" on Home) | `CustomFunction` (keypad screen) |
| Storage | favourites as `"id1,id2"` | last 5 calculations |
| Host features | `openFunction` from the widget, `showMessage` after copying | `storage` only: a self-contained tool |
| Pure logic + tests | `tokenizeKotlin`, `filterSnippets`, `snippetOfTheDay` (5 tests) | recursive-descent `evaluate` with BigDecimal (3 tests) |
| Shows | syntax highlighting with `AnnotatedString`, clipboard, filter chips | keeping logic out of the UI, error handling as values |

## What changed since Phase 4

| Area | Phase 4 | Phase 5 |
|---|---|---|
| Plugging in a module | edit `settings.gradle.kts`, `app/build.gradle.kts`, `InstalledModules.kt` | **nothing**: folder scan + ServiceLoader |
| Creating a module | copy an example by hand | `./gradlew newModule` from `templates/module` |
| Examples | 2 built-in | 2 built-in + 2 student modules |
| Enable/disable | registry API only | switches on the Modules list and on module detail |
| Broken modules | listed under Problems | + crashes switch the module off on the next start |
| Documentation | README | STUDENT_GUIDE.md + in-app developer guide + per-module README checklist |
| Framework API | v1 | + `ModuleDiscovery`, `BuiltInModule`, `ModuleRegistry.reportProblem`, `ModuleInfo.sourceUrl` |
| Backend catalog | metadata | + `source_url`, `framework_version` |
| Tests | 60 Android / 29 backend | **77 Android / 30 backend** |

## The whole project at a glance

| Phase | Theme | Key result |
|---|---|---|
| 1 | Sketch / MVP | Compose UI, Navigation, Room, DataStore, static FastAPI |
| 2 | Core | JWT auth, Retrofit, offline-first progress sync, quizzes, Room migrations |
| 3 | Product | Hilt, WorkManager, Media3 video, notes, search, Coil, 45 tests |
| 4 | Framework | `:framework` contracts, Module Registry, Modules tab, class catalog |
| 5 | Students build | scaffolding, auto-discovery, student examples, switches, crash guard, guides |

## Known limits

* Modules are compiled into the app (Gradle modules), not downloaded at runtime. That's deliberate: it's
  safe, debuggable, and what students learn in class. Loading code at runtime would need a dynamic feature
  delivery setup.
* The crash guard switches a module off only *after* a crash, on the next start. A module that crashes
  inside a background coroutine of its own is caught the same way. A crash in app code is never blamed on a
  module.
* The JWT is stored in plain DataStore (fine for class, encrypt for production). The backend has no
  Alembic migrations (delete `teaching.db` after model changes during development).

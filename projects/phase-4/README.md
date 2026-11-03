# Phase 4: Framework core (extensibility foundation)

Phase 4 turns the app into a **framework**. Features can now live in separate Gradle modules that
implement a few small contracts. A central **Module Registry** validates them, lists them and
persists their state. The app shows them in a new **Modules** tab, on Home (tool widgets) and inside
lessons (renderers). Two built-in example modules show the pattern. The backend gets a **class catalog**
where students share their modules' metadata.

```
phase-4/
├── backend/                         Phase 3 backend + /modules (class catalog), 29 tests
└── android/
    ├── settings.gradle.kts          include(":framework"), include(":modules:…")
    ├── framework/                   ★ THE FRAMEWORK API: modules depend on this only
    │   └── src/main/java/edu/sdwu/teaching/framework/
    │       ├── Contracts.kt         ModuleProvider, ModuleInfo, CustomFunction, ToolEntry,
    │       │                        LessonRenderer, LessonContent, FunctionHost, ModuleStorage
    │       ├── ModuleRegistry.kt    ModuleRegistry, DefaultModuleRegistry (validation), pickRenderer
    │       ├── ComposeSupport.kt    storage.rememberValue(key), storage.rememberSetter()
    │       └── PreviewSupport.kt    PreviewFunctionHost, InMemoryModuleStorage, SampleLessons
    ├── modules/
    │   ├── study-timer/             example 1: CustomFunction (screen) + ToolEntry (Home widget) + storage
    │   └── lesson-formatter/        example 2: LessonRenderer (key points + exercise cards)
    └── app/src/main/java/edu/sdwu/teaching/
        ├── modules/InstalledModules.kt   ★ where modules are plugged in (Hilt @IntoSet)
        ├── data/modules/                 Room-backed ModuleStateStore + ModuleStorage, ModuleHostFactory
        ├── data/local/ModuleEntities.kt  tables `modules` and `module_data` (Room v4, Migration 3→4)
        ├── data/repository/RemoteModuleCatalogRepository.kt   /modules client
        └── ui/modules/                   Modules tab, module detail, FunctionScreen, LocalModuleHosts
```

## The extension contracts

| Contract | What it is | Where it appears |
|---|---|---|
| `ModuleProvider` | entry point of a module: `info` + lists of contributions | Modules tab |
| `ModuleInfo` | id, name, description, author, version, icon, category, min framework version | Modules tab, class catalog |
| `CustomFunction` | a full screen: `@Composable Content(host)` | Modules → module → Screens |
| `ToolEntry` | a compact widget: `@Composable Widget(host, modifier)` | Home → Tools |
| `LessonRenderer` | `canRender(lesson)` + `@Composable Render(lesson, modifier)`; highest `priority` wins | Lesson detail body |
| `FunctionHost` | what a module may use: lessons, student name, private storage, openLesson, openFunction, showMessage | passed to every Content/Widget |
| `ModuleStorage` | per-module key-value storage in Room (`module_data`) | `host.storage` |

The smallest possible module:

```kotlin
class HelloModule : ModuleProvider {
    override val info = ModuleInfo(
        id = "hello", name = "Hello", description = "Says hi",
        author = "Ada", version = "1.0.0", icon = Icons.Filled.Face,
    )
    override val functions = listOf(object : CustomFunction {
        override val id = "hello-screen"
        override val title = "Hello"
        override val description = "A greeting"
        override val icon = Icons.Filled.Face
        @Composable override fun Content(host: FunctionHost) {
            val name by host.studentName.collectAsState(initial = null)
            Text("Hello, ${name ?: "guest"}!")
        }
    })
}
```

Plug it in (three lines in total):

1. `settings.gradle.kts`: `include(":modules:hello")`
2. `app/build.gradle.kts`: `implementation(project(":modules:hello"))`
3. `app/.../modules/InstalledModules.kt`:
   `@Provides @IntoSet fun hello() = InstalledModule(HelloModule(), ModuleSource.STUDENT)`

## How the registry works

```
InstalledModules (Hilt @IntoSet) ──▶ Set<InstalledModule> ──▶ TeachingApp.onCreate
                                                               │ registry.register(provider, source)
                                                               ▼
                          DefaultModuleRegistry (framework, pure Kotlin)
                          • validates: id format, duplicate module ids, duplicate/taken
                            contribution ids, empty modules, minFrameworkVersion
                          • invalid → ModuleProblem (shown in the Modules tab, app keeps running)
                          • valid → modules StateFlow + ModuleStateStore (Room `modules` table)
                                                               │
              enabledFunctions / enabledTools / enabledRenderers (Flows)
                     │                  │                    │
              FunctionScreen       Home "Tools"        Lesson detail (pickRenderer)
```

* **In memory:** the providers (code) and their contributions.
* **In Room:** the enabled flag, first-seen and last-seen dates, name and version. The registry API already
  supports `setEnabled(id, …)`; the enable/disable UI arrives in Phase 5.
* **Problems, not crashes:** a broken module is listed under *Problems* with a readable reason, and every other
  module still loads.

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
cd projects/phase-4/backend
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

Backend tests (29), with the environment active: `pytest`.

To change a setting, copy `.env.example` to `.env` in the `backend/` folder and edit it; the server reads it on
start. Outside your own computer, set `JWT_SECRET` to at least 32 random characters (the server logs a warning
while the development secret is in use). To start again with fresh seed data, stop the server and delete
`teaching.db`.

### Run the app in Android Studio

1. **File → Open…** and choose `projects/phase-4/android`, the folder that contains `settings.gradle.kts`
   (not `projects/` or `phase-4/`).
2. Wait for the **Gradle sync** to finish. If Android Studio asks to install SDK Platform 37 or to accept
   licences, accept. It writes `local.properties` with your SDK path itself.
3. If the sync fails with a JDK error: **Settings → Build, Execution, Deployment → Build Tools → Gradle →
   Gradle JDK** → choose the bundled JDK (17 or newer).
4. No emulator yet? **Tools → Device Manager → + (Create Virtual Device)** → a Pixel phone with a recent
   system image (API 26 or newer; API 35 or 36 recommended).
5. In the toolbar select the **app** run configuration and your device, then click **Run ▶**
   (Shift+F10, or Ctrl+R on macOS).

Unit tests (60 (app, framework and every module), no device needed): in Android Studio's **Terminal** tab run

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

1. **Home → Tools**: the *Study Timer* widget (a `ToolEntry`). Tap **Focus** to open its full screen through
   `host.openFunction(...)`.
2. **Study Timer**: choose *Demo 1* and start it. When it finishes, a snackbar appears (`host.showMessage`) and the
   counter increases. The counter lives in the module's private `host.storage`, and the Home widget shows the
   same number.
3. Open any lesson: numbered points get badges and the exercise is a highlighted card, with the caption
   *Formatted by the "Structured Lessons" module*. That's the `LessonRenderer` contract.
4. **Modules tab**: the installed modules with category, version, author, source and contribution summary.
   Open one to see its screens, tools, renderers and technical ids.
5. While signed in, tap **Share with class** in a module. Pull the *Class catalog* (refresh icon). Your module is
   listed with your name. Swagger: `GET /modules`.
6. Break a module on purpose (e.g. give a function the id `"Bad Id"`) and run again. It shows up under
   *Problems* with the reason, and the rest of the app still works.

## Backend: class catalog

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/modules?q=` | - | list published modules (newest first), optional search |
| GET | `/modules/{id}` | - | one module |
| PUT | `/modules/{id}` | user | publish or update (upsert); only the first publisher or an instructor may update |
| DELETE | `/modules/{id}` | owner / instructor | remove from the catalog |

Validation mirrors the framework: ids `^[a-z][a-z0-9-]{2,39}$`, semantic version `x.y.z`, 1-20
contributions of type `function`, `tool` or `renderer`. Only metadata is sent, never code.

## What changed since Phase 3

| Area | Phase 3 | Phase 4 |
|---|---|---|
| Gradle | single `:app` module | `:app` + `:framework` + `:modules:study-timer` + `:modules:lesson-formatter` |
| Extension points | interfaces inside the app | public framework API: `ModuleProvider`, `CustomFunction`, `ToolEntry`, `LessonRenderer`, `FunctionHost`, `ModuleStorage` |
| Registry | none | `DefaultModuleRegistry`: validation, problems, enabled state persisted in Room |
| Discovery | none | Hilt multibinding `Set<InstalledModule>` (`InstalledModules.kt`) |
| UI | 3 tabs | 4 tabs (+ Modules), module detail, function host screen, Home tools, lesson renderers |
| Room | v3 | v4: `modules`, `module_data` (+ `Migration(3, 4)`) |
| Backend | no module support | `/modules` class catalog (+5 tests) |
| Tests | 45 | 60 (framework 5, modules 4, app 51) |

## Two Compose bugs we hit while building the example module

Both are now prevented or documented by the framework. They're good classroom material.

1. **Endless recomposition.** `host.storage.observe(key).collectAsState(null)` creates a *new* Flow on every
   recomposition. `collectAsState` restarts for every new Flow, the restart emits, the emit recomposes, and so on.
   The app froze with an ANR. Fix: remember the Flow. The framework provides `host.storage.rememberValue(key)`,
   which does exactly that.
2. **A cancelled effect.** Inside `LaunchedEffect(running)`, setting `running = false` *before* saving the counter
   cancelled the effect, because changing a key cancels its effect, so the save never ran. Fix: change the key
   last (see the comment in `StudyTimerUi.kt`).

## Limits (Phase 5 addresses them)

* Plugging in a module still means editing `InstalledModules.kt`. Phase 5 adds automatic discovery.
* No enable/disable switch in the UI yet (the registry already supports it).
* There's no template or scaffolding for new modules yet. Phase 5 adds a template module, a script and a guide.
* A module that throws inside a composable can still crash the app. Compose can't catch exceptions during
  composition. Test modules with `PreviewFunctionHost` before plugging them in.

# Android Teaching Framework

A learning platform for an Android development course that is also an extensible framework:
students build their own custom functions (screens, tools, lesson renderers, quiz types)
and plug them into the app.

**Start here:** the latest, complete version is [`phase-5/`](phase-5/README.md). Students building their own
feature should read [phase-5/STUDENT_GUIDE.md](phase-5/STUDENT_GUIDE.md).

The project is built in five progressive phases. Every phase is a complete, separately runnable
snapshot (Android app + backend) in its own folder:

| Folder | Phase | Status |
|---|---|---|
| [`phase-1/`](phase-1/README.md) | Sketch / MVP: Compose, bottom navigation, Room seed data, DataStore theme, static FastAPI | ✅ done |
| [`phase-2/`](phase-2/README.md) | Core: Retrofit, JWT auth, offline-first lesson + progress cache, quizzes | ✅ done |
| [`phase-3/`](phase-3/README.md) | Polished app: notes, bookmarks, search, video player, WorkManager, Hilt, tests | ✅ done |
| [`phase-4/`](phase-4/README.md) | Framework core: `CustomFunction` / `ModuleProvider` contracts, module registry, example modules | ✅ done |
| [`phase-5/`](phase-5/README.md) | Student module development: template, two student modules, enable/disable UI, guide ([STUDENT_GUIDE.md](phase-5/STUDENT_GUIDE.md)) | ✅ done |

Stack: Kotlin · Jetpack Compose · Material 3 · Navigation · ViewModel · Room · DataStore ·
Retrofit/OkHttp + Kotlin Serialization · Coil · WorkManager · Hilt. Backend: Python FastAPI + SQLAlchemy + SQLite.

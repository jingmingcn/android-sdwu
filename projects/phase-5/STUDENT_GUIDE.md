# How students add a custom function

This guide walks you from an empty folder to your own feature running inside the Android Teaching
Framework app, shared with your class. It takes about 15 minutes before you start on your own idea.

> Open `projects/phase-5/android` in Android Studio. Type the commands below in its **Terminal** tab, which
> opens in that folder. On Windows, use `gradlew.bat` instead of `./gradlew`. To share your module (step 7),
> start the backend on localhost first; see [README.md → Run it](README.md#run-it).

---

## 1. Understand the idea (2 minutes)

The app is a host. Your feature is a **module**: a small Gradle project in `modules/<your-id>/` that
depends only on `:framework`. A module gives the app one or more **contributions**:

| You implement | You get | Example in this repo |
|---|---|---|
| `CustomFunction` | a full screen, opened from the **Modules** tab or from your widget | Calculator, Code Snippets, Study Timer |
| `ToolEntry` | a card in the **Tools** section of Home | "Snippet of the day", Study Timer widget |
| `LessonRenderer` | a custom layout for the body of matching lessons | Structured Lessons |

Everything your code may use from the app is on **`FunctionHost`**:

| Member | What it does |
|---|---|
| `host.lessons: Flow<List<LessonContent>>` | all lessons (works offline) |
| `host.studentName: Flow<String?>` | the signed-in student's name, `null` for guests |
| `host.storage` | your module's private key-value storage, kept across restarts |
| `host.openLesson(id)` / `host.openFunction(id)` | navigate |
| `host.showMessage(text)` | show a snackbar |

You never touch Room, Retrofit, Hilt or the app's navigation. That keeps your module small, and the
app can change without breaking your work.

## 2. Create your module (1 minute)

```bash
./gradlew newModule -Pid=flash-cards -Pname="Flash Cards" -Pauthor="Ada Lovelace"
```

Rules for `-Pid`: 3-33 characters, lowercase letters, digits and dashes, starting with a letter.
The command creates:

```
modules/flash-cards/
├── build.gradle.kts                      depends on :framework only
├── README.md                             your checklist
└── src/
    ├── main/java/edu/sdwu/teaching/modules/flashcards/
    │   └── FlashCardsModule.kt           ModuleProvider + a working screen + @Preview
    ├── main/resources/META-INF/services/
    │   └── edu.sdwu.teaching.framework.ModuleProvider   one line: your provider's class name
    └── test/java/edu/sdwu/teaching/modules/flashcards/
        └── FlashCardsModuleTest.kt       "passes validation" + "is discoverable"
```

You don't need to edit anything else:
- `settings.gradle.kts` includes every folder in `modules/`.
- `app/build.gradle.kts` depends on all of them.
- The app finds your provider at startup through the `META-INF/services` file (Java's `ServiceLoader`).

## 3. Run it (2 minutes)

In Android Studio, click **File → Sync Project with Gradle Files**, select the **app** run configuration and
your emulator, then click **Run ▶**.

Open the **Modules** tab. *Flash Cards* is listed as a **Student** module. Tap it, then tap its screen.
You'll see a greeting, a counter that survives restarts, and a "Show a message" button.

## 4. Build your feature

Open `FlashCardsModule.kt`. The app draws the top bar (title and back button); you draw everything in
`Content(host)`:

```kotlin
internal object FlashCardsFunction : CustomFunction {
    override val id = "flash-cards-screen"
    override val title = "Flash Cards"
    override val description = "Practise Android terms"
    override val icon = Icons.Filled.Star

    @Composable
    override fun Content(host: FunctionHost) = FlashCardsScreen(host)
}
```

**Iterate with the preview.** The generated `@Preview` uses `PreviewFunctionHost`, a fake host with
sample lessons and in-memory storage, so you can design the screen without running the app.

**Keep logic out of composables.** Put calculations in plain Kotlin functions and test them. See
`modules/calculator/.../Expression.kt` (a parser) and `modules/code-snippets/.../Snippets.kt` (search and
syntax tokens). Both are fully unit tested without an emulator.

### Saving data

```kotlin
val known by host.storage.rememberValue("known")   // String? that updates live
val save = host.storage.rememberSetter()           // save(key, value); null deletes
Button(onClick = { save("known", "${(known?.toIntOrNull() ?: 0) + 1}") }) { Text("I knew it") }
```

Store small things as strings. A list can be `"a,b,c"`, as the Code Snippets favourites are.

### Reading lessons

```kotlin
val lessons by host.lessons.collectAsState(initial = emptyList())
lessons.forEach { lesson ->
    TextButton(onClick = { host.openLesson(lesson.id) }) { Text(lesson.title) }
}
```

## 5. Optional: a Home widget and a lesson renderer

```kotlin
class FlashCardsModule : ModuleProvider {
    override val info = ModuleInfo(/* … */)
    override val functions = listOf(FlashCardsFunction)
    override val tools = listOf(FlashCardsWidget)          // Home → Tools
    override val lessonRenderers = listOf(MyRenderer)      // lesson body
}

internal object FlashCardsWidget : ToolEntry {
    override val id = "flash-cards-widget"
    override val title = "Card of the day"

    @Composable
    override fun Widget(host: FunctionHost, modifier: Modifier) {
        Card(modifier) {
            TextButton(onClick = { host.openFunction("flash-cards-screen") }) { Text("Practise now") }
        }
    }
}
```

A `LessonRenderer` answers `canRender(lesson)` quickly (no I/O) and draws `Render(lesson, modifier)`.
When several renderers accept a lesson, the highest `priority` wins. The built-in *Structured Lessons*
renderer has priority 10.

## 6. Test

```bash
./gradlew :modules:flash-cards:testDebugUnitTest
```

The two generated tests check that the framework accepts your module and that the app can discover it.
Add your own tests next to them.

## 7. Share with your class

Sign in, open **Modules → Flash Cards** and tap **Share with class**. The catalog stores your module's
name, description, version, author and contributions, plus `sourceUrl` if you set it in `ModuleInfo`
(e.g. your Git repository). It never stores code. Classmates see it under **Modules → Class catalog**.

## 8. Switch it off and on

Every module has a switch in the Modules tab. Off means its screens, widgets and renderers disappear
immediately, and the choice survives restarts. Use the switch to compare the app with and without your
module, for example to see a lesson with and without your renderer.

---

## Rules the framework checks

Broken modules are listed under **Modules → Problems** with the reason. The rest of the app keeps working.

| Rule | Example of a problem message |
|---|---|
| Module and contribution ids: `^[a-z][a-z0-9-]{2,39}$` | `Contribution id "Flash Cards" must be 3-40 lowercase letters…` |
| Ids are unique across the whole app | `Contribution id "timer" is already used by another module` |
| At least one contribution | `Module contributes nothing (add a function, tool or renderer)` |
| `minFrameworkVersion` ≤ the app's framework version | `Needs framework v2, this app has v1` |
| The provider has a public no-argument constructor | `Could not load a module: …` |

**Crash protection.** If your composable throws, the app crashes; Compose can't catch it. The framework
notices the crash happened in your package. On the next start your module is **switched off** and listed
under Problems with the exception message. Fix it, then switch it back on.

## Two Compose pitfalls (we hit both while building the examples)

1. **Don't collect a new Flow on every recomposition.**
   `host.storage.observe("k").collectAsState(null)` creates a new Flow each time the composable runs, so
   collection restarts forever and the app freezes. Use `host.storage.rememberValue("k")`, which remembers
   the Flow for you.
2. **Change `LaunchedEffect` keys last.** Changing a key cancels the running effect at its next suspension
   point. Do your `storage.put(...)` first, then set the key, as `StudyTimerUi.kt` does.

## Troubleshooting

| Symptom | Fix |
|---|---|
| Module not in the Modules tab | Did Gradle sync? Is the class name in `META-INF/services/...ModuleProvider` exactly right (package + class)? |
| "Could not load a module" | The provider needs a public no-argument constructor (`class X : ModuleProvider`, not `class X(val a: Int)`). |
| Module listed under Problems | Read the message: usually an id format or a duplicate id. |
| Module switched itself off | It crashed. The exception is under Problems. Reproduce it in the @Preview or a unit test. |
| "Unresolved reference: Room/Retrofit/Hilt" | Modules may only use `:framework`. Ask through `FunctionHost`, or propose a new host method. |
| Need a library (e.g. a chart library) | Add `implementation(...)` to your module's `build.gradle.kts`. |

## Checklist before you hand in

- [ ] `ModuleInfo.description` explains the module in one sentence
- [ ] Works in light and dark mode and at large font sizes
- [ ] Every icon button has a `contentDescription`
- [ ] Logic is in plain functions with unit tests; `testDebugUnitTest` passes
- [ ] Switching the module off and on in the Modules tab works
- [ ] Shared with the class

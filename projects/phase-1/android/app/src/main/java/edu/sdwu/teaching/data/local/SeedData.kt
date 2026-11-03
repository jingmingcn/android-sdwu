package edu.sdwu.teaching.data.local

import edu.sdwu.teaching.domain.model.Difficulty
import edu.sdwu.teaching.domain.model.Lesson

/**
 * Hard-coded lessons inserted into Room on first launch so the app is never empty.
 *
 * Generated from backend/app/data/lessons.json - keep both in sync (same ids),
 * because from Phase 2 the app refreshes these rows from the backend.
 */
object SeedData {
    val lessons: List<Lesson> = listOf(
        Lesson(
            id = "kotlin-basics",
            order = 1,
            title = "Kotlin Essentials for Android",
            summary = "Variables, null safety, data classes and lambdas: the Kotlin you will use every day.",
            content = "Kotlin is the official language for Android development.\n\n1. val vs var: prefer val (read-only) and only use var when a value really changes.\n\n2. Null safety: a String can never be null, a String? can. Use ?. (safe call), ?: (Elvis) and avoid !! in production code.\n\n3. Data classes: data class Lesson(val id: String, val title: String) gives you equals, hashCode, toString and copy for free.\n\n4. Lambdas and higher-order functions: lessons.filter { it.minutes < 30 }.map { it.title }.\n\nExercise: model a Student with a name, an optional nickname and a list of finished lesson ids, then print a greeting that falls back to the name when there is no nickname.",
            category = "Kotlin",
            difficulty = Difficulty.BEGINNER,
            durationMinutes = 25,
            videoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        ),
        Lesson(
            id = "compose-fundamentals",
            order = 2,
            title = "Jetpack Compose Fundamentals",
            summary = "Composable functions, previews and how Compose turns state into UI.",
            content = "Jetpack Compose is Android's declarative UI toolkit: you describe what the UI should look like for a given state, and Compose updates it when the state changes.\n\n1. A composable is a Kotlin function annotated with @Composable.\n\n2. Composables emit UI; they do not return views.\n\n3. Use @Preview to see a composable in Android Studio without running the app.\n\n4. Recomposition: when state read by a composable changes, Compose re-runs that function.\n\nExercise: write a Greeting(name: String) composable and preview it in light and dark mode.",
            category = "Compose",
            difficulty = Difficulty.BEGINNER,
            durationMinutes = 30,
            videoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
        ),
        Lesson(
            id = "compose-layouts",
            order = 3,
            title = "Layouts and Modifiers",
            summary = "Column, Row, Box, LazyColumn and the Modifier chain.",
            content = "Compose layouts are built from three primitives: Column (vertical), Row (horizontal) and Box (stacked).\n\n1. Modifiers decorate a composable: size, padding, background, click handling and semantics.\n\n2. Order matters: Modifier.padding(16.dp).background(Color.Red) is different from Modifier.background(Color.Red).padding(16.dp).\n\n3. LazyColumn only composes the items that are visible, so use it for long lists.\n\n4. Material 3 components (Card, ListItem, Scaffold) give you consistent spacing and theming.\n\nExercise: build a lesson card with a title, a summary and a duration chip aligned to the end.",
            category = "Compose",
            difficulty = Difficulty.BEGINNER,
            durationMinutes = 35,
            videoUrl = null,
        ),
        Lesson(
            id = "compose-state",
            order = 4,
            title = "State and State Hoisting",
            summary = "remember, mutableStateOf, unidirectional data flow and stateless composables.",
            content = "State is any value that can change over time. In Compose, UI = f(state).\n\n1. remember { mutableStateOf(0) } keeps a value across recompositions.\n\n2. rememberSaveable also survives configuration changes such as rotation.\n\n3. State hoisting: move state up to the caller and pass value + onValueChange down. Stateless composables are easier to reuse and test.\n\n4. Unidirectional data flow: events go up, state comes down.\n\nExercise: build a counter, then hoist its state so two counters can share one total.",
            category = "Compose",
            difficulty = Difficulty.INTERMEDIATE,
            durationMinutes = 40,
            videoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
        ),
        Lesson(
            id = "navigation-compose",
            order = 5,
            title = "Navigation Compose",
            summary = "NavHost, type-safe routes and a bottom navigation bar.",
            content = "Navigation Compose moves the user between screens inside a single Activity.\n\n1. A NavController keeps the back stack. A NavHost maps routes to composables.\n\n2. Type-safe routes: annotate an object or data class with @Serializable and use it as the route.\n\n3. Arguments: data class LessonDetailRoute(val lessonId: String), read with backStackEntry.toRoute<LessonDetailRoute>().\n\n4. Bottom bars: navigate with popUpTo(startDestination) { saveState = true }, launchSingleTop = true and restoreState = true.\n\nExercise: add a fourth tab to this app and make it keep its scroll position when switching tabs.",
            category = "Architecture",
            difficulty = Difficulty.INTERMEDIATE,
            durationMinutes = 35,
            videoUrl = null,
        ),
        Lesson(
            id = "viewmodel-lifecycle",
            order = 6,
            title = "ViewModel and Lifecycle",
            summary = "Keep UI state across rotation and expose it with StateFlow.",
            content = "A ViewModel holds UI state and business logic for a screen and survives configuration changes.\n\n1. Expose state as StateFlow<UiState>; keep the mutable version private.\n\n2. Collect it in Compose with collectAsStateWithLifecycle() so collection stops when the app is in the background.\n\n3. Use viewModelScope.launch for coroutines that should be cancelled when the screen is gone.\n\n4. Model screen state with a sealed interface: Loading, Success, Error.\n\nExercise: add an Error state to the lesson list and show a retry button.",
            category = "Architecture",
            difficulty = Difficulty.INTERMEDIATE,
            durationMinutes = 40,
            videoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
        ),
        Lesson(
            id = "room-database",
            order = 7,
            title = "Local Data with Room",
            summary = "Entities, DAOs and reactive queries with Flow.",
            content = "Room is an abstraction over SQLite with compile-time checked SQL.\n\n1. @Entity describes a table, @PrimaryKey its key.\n\n2. @Dao interfaces declare queries. A query returning Flow<List<T>> re-emits whenever the table changes.\n\n3. @Database lists the entities and the version. Bump the version and add a migration when the schema changes.\n\n4. Never touch the database on the main thread: DAO functions are suspend or return Flow.\n\nExercise: add a 'favorite' column to LessonEntity and a query that returns only favorite lessons.",
            category = "Data",
            difficulty = Difficulty.INTERMEDIATE,
            durationMinutes = 45,
            videoUrl = null,
        ),
        Lesson(
            id = "datastore-preferences",
            order = 8,
            title = "Preferences with DataStore",
            summary = "Store small key-value settings such as the theme, asynchronously and safely.",
            content = "DataStore replaces SharedPreferences with a coroutine and Flow based API.\n\n1. Preferences DataStore stores key-value pairs; Proto DataStore stores typed objects.\n\n2. Read: dataStore.data.map { it[THEME_KEY] } returns a Flow.\n\n3. Write: dataStore.edit { it[THEME_KEY] = \"DARK\" } is a suspend function and is transactional.\n\n4. Create exactly one DataStore instance per file (use the preferencesDataStore delegate).\n\nExercise: this app stores the theme in DataStore. Add a 'text size' preference and apply it in the theme.",
            category = "Data",
            difficulty = Difficulty.ADVANCED,
            durationMinutes = 30,
            videoUrl = null,
        ),
    )
}

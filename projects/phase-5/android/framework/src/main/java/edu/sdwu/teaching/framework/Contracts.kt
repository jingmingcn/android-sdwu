package edu.sdwu.teaching.framework

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.flow.Flow

/*
 * ============================================================================================
 *  Android Teaching Framework: extension contracts
 * ============================================================================================
 *
 *  A *module* is a unit of student code that plugs into the app. It is described by one
 *  [ModuleProvider], which hands the app any number of *contributions*:
 *
 *    CustomFunction  - a full screen, opened from the Modules tab        (e.g. a calculator)
 *    ToolEntry       - a small widget card on the Home screen            (e.g. a timer)
 *    LessonRenderer  - a custom way to display a lesson's text           (e.g. highlight code)
 *
 *  Modules never see Room, Retrofit or Hilt. Everything they may use from the app comes
 *  through [FunctionHost]. That keeps modules small and testable, and lets the app change its
 *  internals without breaking anyone's module.
 *
 *  Minimal module:
 *
 *      class HelloModule : ModuleProvider {
 *          override val info = ModuleInfo(id = "hello", name = "Hello", description = "Says hi",
 *                                         author = "Ada", version = "1.0.0", icon = Icons.Filled.Face)
 *          override val functions = listOf(object : CustomFunction {
 *              override val id = "hello-screen"
 *              override val title = "Hello"
 *              override val description = "A greeting"
 *              override val icon = Icons.Filled.Face
 *              @Composable override fun Content(host: FunctionHost) { Text("Hello!") }
 *          })
 *      }
 * ============================================================================================
 */

/** Bumped when contracts change in a way that old modules cannot handle. */
const val FRAMEWORK_VERSION: Int = 1

/** Ids of modules and contributions: lowercase letters, digits and dashes, 3-40 characters. */
val VALID_ID: Regex = Regex("^[a-z][a-z0-9-]{2,39}$")

enum class ModuleCategory(val label: String) {
    STUDY_TOOL("Study tool"),
    LESSON_RENDERER("Lesson renderer"),
    QUIZ("Quiz"),
    UTILITY("Utility"),
    FUN("Fun"),
}

/** Who wrote the module and what it is. Shown in the Modules tab and sent to the class catalog. */
data class ModuleInfo(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    val version: String,
    val icon: ImageVector,
    val category: ModuleCategory = ModuleCategory.UTILITY,
    /** The oldest framework version this module works with. */
    val minFrameworkVersion: Int = FRAMEWORK_VERSION,
    /** Optional link to the module's code (e.g. your Git repository), shown in the class catalog. */
    val sourceUrl: String? = null,
)

/**
 * Entry point of a module. Implement this once per module and register it (see the
 * app's `modules/InstalledModules.kt`). Override only the lists you need.
 */
interface ModuleProvider {
    val info: ModuleInfo
    val functions: List<CustomFunction> get() = emptyList()
    val tools: List<ToolEntry> get() = emptyList()
    val lessonRenderers: List<LessonRenderer> get() = emptyList()
}

/** A full-screen feature. The app supplies the top bar (title + back), you supply the body. */
interface CustomFunction {
    val id: String
    val title: String
    val description: String
    val icon: ImageVector

    @Composable
    fun Content(host: FunctionHost)
}

/** A compact card shown in the "Tools" section of the Home screen. Keep it short (<= ~120 dp high). */
interface ToolEntry {
    val id: String
    val title: String

    @Composable
    fun Widget(host: FunctionHost, modifier: Modifier)
}

/**
 * Displays a lesson's body text in a custom way. For each lesson, the app asks every enabled
 * renderer [canRender] and uses the one with the highest [priority]; the built-in plain-text
 * renderer is used when nobody volunteers.
 */
interface LessonRenderer {
    val id: String
    val name: String
    val priority: Int get() = 0

    /** Must be fast and side-effect free: it is called for every lesson that is opened. */
    fun canRender(lesson: LessonContent): Boolean

    @Composable
    fun Render(lesson: LessonContent, modifier: Modifier)
}

/** Read-only view of a lesson, as seen by modules. */
data class LessonContent(
    val id: String,
    val title: String,
    val summary: String,
    val category: String,
    val difficulty: String,
    /** Plain text; paragraphs are separated by a blank line. */
    val body: String,
)

/**
 * Everything a module may ask the app for. The app passes one host per module, so [storage]
 * is private to that module.
 */
interface FunctionHost {
    /** The module this host belongs to. */
    val moduleId: String

    /** All lessons (from the offline cache), in course order. */
    val lessons: Flow<List<LessonContent>>

    /** The signed-in student's name, or null for guests. */
    val studentName: Flow<String?>

    /** Small key-value storage that survives restarts. Keys are private to the module. */
    val storage: ModuleStorage

    /** Navigates to a lesson's detail screen. */
    fun openLesson(lessonId: String)

    /** Opens a [CustomFunction] screen by id (e.g. from your Home widget to your full screen). */
    fun openFunction(functionId: String)

    /** Shows a short snackbar message. */
    fun showMessage(text: String)
}

interface ModuleStorage {
    fun observe(key: String): Flow<String?>
    suspend fun get(key: String): String?
    suspend fun put(key: String, value: String?)
}

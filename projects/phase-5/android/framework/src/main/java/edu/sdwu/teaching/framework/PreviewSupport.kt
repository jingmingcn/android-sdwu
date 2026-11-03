package edu.sdwu.teaching.framework

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * A [FunctionHost] that works without the app, for @Preview functions and unit tests of
 * modules. Storage lives in memory; navigation and messages are recorded in lists.
 *
 *     @Preview @Composable
 *     private fun MyScreenPreview() = MyFunction.Content(PreviewFunctionHost())
 */
class PreviewFunctionHost(
    override val moduleId: String = "preview",
    lessons: List<LessonContent> = SampleLessons,
    studentName: String? = "Ada Student",
) : FunctionHost {
    override val lessons: Flow<List<LessonContent>> = flowOf(lessons)
    override val studentName: Flow<String?> = flowOf(studentName)
    override val storage: ModuleStorage = InMemoryModuleStorage()

    val openedLessons = mutableListOf<String>()
    val openedFunctions = mutableListOf<String>()
    val messages = mutableListOf<String>()

    override fun openLesson(lessonId: String) {
        openedLessons += lessonId
    }

    override fun openFunction(functionId: String) {
        openedFunctions += functionId
    }

    override fun showMessage(text: String) {
        messages += text
    }
}

class InMemoryModuleStorage : ModuleStorage {
    private val values = MutableStateFlow<Map<String, String>>(emptyMap())
    override fun observe(key: String): Flow<String?> = values.map { it[key] }
    override suspend fun get(key: String): String? = values.value[key]
    override suspend fun put(key: String, value: String?) =
        values.update { if (value == null) it - key else it + (key to value) }
}

val SampleLessons: List<LessonContent> = listOf(
    LessonContent(
        id = "kotlin-basics",
        title = "Kotlin Essentials for Android",
        summary = "Variables, null safety, data classes and lambdas.",
        category = "Kotlin",
        difficulty = "BEGINNER",
        body = "Kotlin is the official language for Android development.\n\n" +
            "1. val vs var: prefer val.\n\n2. Null safety: use ?. and ?:\n\n" +
            "Exercise: model a Student with a name and an optional nickname.",
    ),
    LessonContent(
        id = "compose-state",
        title = "State and State Hoisting",
        summary = "remember, mutableStateOf and unidirectional data flow.",
        category = "Compose",
        difficulty = "INTERMEDIATE",
        body = "State is any value that can change over time.\n\n1. remember { mutableStateOf(0) }\n\n" +
            "Exercise: build a counter and hoist its state.",
    ),
)

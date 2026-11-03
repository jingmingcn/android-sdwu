package edu.sdwu.teaching.modules.lessonformatter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import edu.sdwu.teaching.framework.LessonContent
import edu.sdwu.teaching.framework.LessonRenderer
import edu.sdwu.teaching.framework.ModuleCategory
import edu.sdwu.teaching.framework.ModuleInfo
import edu.sdwu.teaching.framework.ModuleProvider
import edu.sdwu.teaching.framework.SampleLessons

/**
 * Built-in example module #2: a [LessonRenderer].
 *
 * Lessons are plain text. This renderer recognises two conventions used by the course
 * material and gives them a richer layout:
 *   "1. something"    -> a numbered "key point" with a badge
 *   "Exercise: ..."   -> a highlighted exercise card
 * Disable the module in the Modules tab and lessons fall back to plain paragraphs.
 */
class LessonFormatterModule : ModuleProvider {
    override val info = ModuleInfo(
        id = "lesson-formatter",
        name = "Structured Lessons",
        description = "Shows numbered key points and highlights exercises in lessons.",
        author = "Course staff",
        version = "1.0.0",
        icon = Icons.Filled.Build,
        category = ModuleCategory.LESSON_RENDERER,
    )

    override val lessonRenderers: List<LessonRenderer> = listOf(StructuredLessonRenderer)
}

/** The building blocks a lesson body is split into. */
sealed interface LessonBlock {
    data class Paragraph(val text: String) : LessonBlock
    data class KeyPoint(val number: Int, val text: String) : LessonBlock
    data class Exercise(val text: String) : LessonBlock
}

private val KEY_POINT = Regex("^(\\d+)\\.\\s+(.*)$", RegexOption.DOT_MATCHES_ALL)
private const val EXERCISE_PREFIX = "Exercise:"

/** Splits a body into blocks. Pure function: unit tested in LessonBlocksTest. */
fun parseLessonBlocks(body: String): List<LessonBlock> =
    body.split(Regex("\\n\\s*\\n"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { paragraph ->
            val point = KEY_POINT.find(paragraph)
            when {
                point != null -> LessonBlock.KeyPoint(point.groupValues[1].toInt(), point.groupValues[2].trim())
                paragraph.startsWith(EXERCISE_PREFIX) -> LessonBlock.Exercise(paragraph.removePrefix(EXERCISE_PREFIX).trim())
                else -> LessonBlock.Paragraph(paragraph)
            }
        }

object StructuredLessonRenderer : LessonRenderer {
    override val id = "structured-lesson-renderer"
    override val name = "Structured Lessons"
    override val priority = 10

    /** Only worth it when the lesson actually uses one of the conventions. */
    override fun canRender(lesson: LessonContent): Boolean =
        parseLessonBlocks(lesson.body).any { it !is LessonBlock.Paragraph }

    @Composable
    override fun Render(lesson: LessonContent, modifier: Modifier) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            parseLessonBlocks(lesson.body).forEach { block ->
                when (block) {
                    is LessonBlock.Paragraph -> Text(block.text, style = MaterialTheme.typography.bodyLarge)
                    is LessonBlock.KeyPoint -> KeyPointRow(block)
                    is LessonBlock.Exercise -> ExerciseCard(block)
                }
            }
        }
    }
}

@Composable
private fun KeyPointRow(point: LessonBlock.KeyPoint) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    point.number.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
        Text(point.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ExerciseCard(exercise: LessonBlock.Exercise) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Create, contentDescription = null)
                Text("Exercise", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            }
            Text(exercise.text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StructuredLessonPreview() {
    MaterialTheme { StructuredLessonRenderer.Render(SampleLessons.first(), Modifier.padding(16.dp)) }
}

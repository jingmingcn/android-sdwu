package edu.sdwu.teaching.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import edu.sdwu.teaching.data.local.SeedData
import edu.sdwu.teaching.domain.model.Difficulty
import edu.sdwu.teaching.domain.model.Lesson
import edu.sdwu.teaching.domain.model.LessonProgress
import edu.sdwu.teaching.domain.model.ProgressStatus
import edu.sdwu.teaching.ui.theme.TeachingTheme

@Composable
fun LessonCard(
    lesson: Lesson,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: LessonProgress? = null,
    isBookmarked: Boolean = false,
) {
    val completed = progress?.isCompleted == true
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            // TalkBack reads one sentence instead of every text fragment separately.
            .semantics(mergeDescendants = true) {
                contentDescription = "Lesson ${lesson.order}: ${lesson.title}. " +
                    "${lesson.difficulty.label}, ${lesson.durationMinutes} minutes. " +
                    progressLabel(progress) + if (isBookmarked) " Bookmarked." else ""
            },
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = if (completed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (completed) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary)
                    } else {
                        Text(
                            text = lesson.order.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = lesson.category.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = lesson.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (isBookmarked) {
                        Icon(Icons.Filled.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = lesson.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                LessonMeta(lesson, progress)
            }
        }
    }
}

/** "Beginner · 25 min" chips shown on cards and on the detail screen. */
@Composable
fun LessonMeta(lesson: Lesson, progress: LessonProgress? = null, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MetaChip(lesson.difficulty.label, difficultyColor(lesson.difficulty))
        MetaChip("${lesson.durationMinutes} min", MaterialTheme.colorScheme.surfaceVariant)
        when (progress?.status) {
            ProgressStatus.IN_PROGRESS -> MetaChip("In progress", MaterialTheme.colorScheme.surfaceVariant)
            ProgressStatus.COMPLETED -> MetaChip("Completed", MaterialTheme.colorScheme.secondaryContainer)
            ProgressStatus.NOT_STARTED, null -> Unit
        }
    }
}

private fun progressLabel(progress: LessonProgress?): String = when (progress?.status) {
    ProgressStatus.IN_PROGRESS -> "In progress."
    ProgressStatus.COMPLETED -> "Completed."
    ProgressStatus.NOT_STARTED, null -> "Not started."
}

@Composable
private fun MetaChip(text: String, color: androidx.compose.ui.graphics.Color) {
    Surface(shape = MaterialTheme.shapes.small, color = color) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun difficultyColor(difficulty: Difficulty) = when (difficulty) {
    Difficulty.BEGINNER -> MaterialTheme.colorScheme.secondaryContainer
    Difficulty.INTERMEDIATE -> MaterialTheme.colorScheme.primaryContainer
    Difficulty.ADVANCED -> MaterialTheme.colorScheme.tertiaryContainer
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LessonCardPreview() {
    TeachingTheme(darkTheme = false) {
        LessonCard(
            lesson = SeedData.lessons.first(),
            onClick = {},
            progress = LessonProgress("kotlin-basics", ProgressStatus.COMPLETED, 3, 3, 0),
        )
    }
}

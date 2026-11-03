package edu.sdwu.teaching.ui.lessons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.sdwu.teaching.data.local.SeedData
import edu.sdwu.teaching.ui.AppViewModelProvider
import edu.sdwu.teaching.ui.components.LessonMeta
import edu.sdwu.teaching.ui.components.VideoPlaceholder
import edu.sdwu.teaching.ui.theme.TeachingTheme

@Composable
fun LessonDetailScreen(
    onBack: () -> Unit,
    onTakeQuiz: (lessonId: String) -> Unit,
    viewModel: LessonDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LessonDetailContent(
        uiState = uiState,
        onBack = onBack,
        onTakeQuiz = onTakeQuiz,
        onSetCompleted = viewModel::setCompleted,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonDetailContent(
    uiState: LessonDetailUiState,
    onBack: () -> Unit,
    onTakeQuiz: (lessonId: String) -> Unit,
    onSetCompleted: (Boolean) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text((uiState as? LessonDetailUiState.Success)?.lesson?.category ?: "Lesson") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        when (uiState) {
            LessonDetailUiState.Loading -> Box(modifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            LessonDetailUiState.NotFound -> Box(modifier, contentAlignment = Alignment.Center) {
                Text("Lesson not found", style = MaterialTheme.typography.titleMedium)
            }
            is LessonDetailUiState.Success -> LessonBody(uiState, onTakeQuiz, onSetCompleted, modifier)
        }
    }
}

@Composable
private fun LessonBody(
    state: LessonDetailUiState.Success,
    onTakeQuiz: (lessonId: String) -> Unit,
    onSetCompleted: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lesson = state.lesson
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        VideoPlaceholder(hasVideo = lesson.videoUrl != null)
        Text(
            text = lesson.title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        LessonMeta(lesson, state.progress)
        Text(
            text = lesson.summary,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider()
        // Content is plain text with blank lines between paragraphs (Markdown arrives later).
        lesson.content.split("\n\n").forEach { paragraph ->
            Text(text = paragraph, style = MaterialTheme.typography.bodyLarge)
        }
        HorizontalDivider()
        ProgressActions(state, onTakeQuiz, onSetCompleted)
    }
}

/** "Mark as complete" and "Take the quiz", shown below the lesson text. */
@Composable
private fun ProgressActions(
    state: LessonDetailUiState.Success,
    onTakeQuiz: (lessonId: String) -> Unit,
    onSetCompleted: (Boolean) -> Unit,
) {
    val completed = state.progress?.isCompleted == true
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.quizQuestionCount > 0) {
            Button(onClick = { onTakeQuiz(state.lesson.id) }, modifier = Modifier.fillMaxWidth()) {
                Text("Take the quiz (${state.quizQuestionCount} questions)")
            }
            val best = state.progress?.quizBestScore
            val total = state.progress?.quizTotal
            Text(
                text = if (best != null && total != null) "Best score: $best / $total" else "Not attempted yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilledTonalButton(onClick = { onSetCompleted(!completed) }, modifier = Modifier.fillMaxWidth()) {
            if (completed) Icon(Icons.Filled.Check, contentDescription = null)
            Text(if (completed) " Completed - tap to undo" else "Mark as complete")
        }
    }
}

@Preview
@Composable
private fun LessonDetailPreview() {
    TeachingTheme(darkTheme = true) {
        LessonDetailContent(
            LessonDetailUiState.Success(SeedData.lessons[3], progress = null, quizQuestionCount = 3),
            onBack = {},
            onTakeQuiz = {},
            onSetCompleted = {},
        )
    }
}

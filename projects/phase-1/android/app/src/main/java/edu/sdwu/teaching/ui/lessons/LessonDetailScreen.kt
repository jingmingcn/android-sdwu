package edu.sdwu.teaching.ui.lessons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import edu.sdwu.teaching.domain.model.Lesson
import edu.sdwu.teaching.ui.AppViewModelProvider
import edu.sdwu.teaching.ui.components.LessonMeta
import edu.sdwu.teaching.ui.components.VideoPlaceholder
import edu.sdwu.teaching.ui.theme.TeachingTheme

@Composable
fun LessonDetailScreen(
    onBack: () -> Unit,
    viewModel: LessonDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LessonDetailContent(uiState = uiState, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonDetailContent(
    uiState: LessonDetailUiState,
    onBack: () -> Unit,
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
            is LessonDetailUiState.Success -> LessonBody(uiState.lesson, modifier)
        }
    }
}

@Composable
private fun LessonBody(lesson: Lesson, modifier: Modifier = Modifier) {
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
        LessonMeta(lesson)
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
    }
}

@Preview
@Composable
private fun LessonDetailPreview() {
    TeachingTheme(darkTheme = true) {
        LessonDetailContent(LessonDetailUiState.Success(SeedData.lessons[3]), onBack = {})
    }
}

package edu.sdwu.teaching.ui.lessons

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.sdwu.teaching.data.local.SeedData
import edu.sdwu.teaching.domain.model.Note
import edu.sdwu.teaching.ui.components.Bookmark
import edu.sdwu.teaching.ui.components.BookmarkBorder
import edu.sdwu.teaching.ui.components.EmptyState
import edu.sdwu.teaching.ui.components.LessonMeta
import edu.sdwu.teaching.ui.components.LessonVideoPlayer
import edu.sdwu.teaching.ui.components.LoadingState
import edu.sdwu.teaching.ui.components.NoteDialog
import edu.sdwu.teaching.ui.components.VideoPlaceholder
import edu.sdwu.teaching.ui.theme.TeachingTheme

@Composable
fun LessonDetailScreen(
    lessonId: String,
    onBack: () -> Unit,
    onTakeQuiz: (lessonId: String) -> Unit,
    viewModel: LessonDetailViewModel = hiltViewModel<LessonDetailViewModel, LessonDetailViewModel.Factory>(
        key = lessonId,
        creationCallback = { factory -> factory.create(lessonId) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LessonDetailContent(
        uiState = uiState,
        onBack = onBack,
        onTakeQuiz = onTakeQuiz,
        onSetCompleted = viewModel::setCompleted,
        onToggleBookmark = viewModel::toggleBookmark,
        onSaveNote = viewModel::saveNote,
        onDeleteNote = viewModel::deleteNote,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonDetailContent(
    uiState: LessonDetailUiState,
    onBack: () -> Unit,
    onTakeQuiz: (lessonId: String) -> Unit,
    onSetCompleted: (Boolean) -> Unit,
    onToggleBookmark: () -> Unit,
    onSaveNote: (text: String, noteId: Long?) -> Unit,
    onDeleteNote: (Long) -> Unit,
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
                actions = {
                    if (uiState is LessonDetailUiState.Success) {
                        IconButton(onClick = onToggleBookmark) {
                            Icon(
                                imageVector = if (uiState.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (uiState.isBookmarked) "Remove bookmark" else "Bookmark lesson",
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        when (uiState) {
            LessonDetailUiState.Loading -> LoadingState(modifier)
            LessonDetailUiState.NotFound -> EmptyState(
                title = "Lesson not found",
                message = "It may have been removed by your instructor.",
                icon = Icons.Outlined.Info,
                actionLabel = "Go back",
                onAction = onBack,
                modifier = modifier,
            )
            is LessonDetailUiState.Success ->
                LessonBody(uiState, onTakeQuiz, onSetCompleted, onSaveNote, onDeleteNote, modifier)
        }
    }
}

@Composable
private fun LessonBody(
    state: LessonDetailUiState.Success,
    onTakeQuiz: (lessonId: String) -> Unit,
    onSetCompleted: (Boolean) -> Unit,
    onSaveNote: (text: String, noteId: Long?) -> Unit,
    onDeleteNote: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lesson = state.lesson
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val videoUrl = state.videoUrl
        if (videoUrl != null) LessonVideoPlayer(videoUrl) else VideoPlaceholder(hasVideo = false)
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
        // Content is plain text with blank lines between paragraphs.
        lesson.content.split("\n\n").forEach { paragraph ->
            Text(text = paragraph, style = MaterialTheme.typography.bodyLarge)
        }
        HorizontalDivider()
        ProgressActions(state, onTakeQuiz, onSetCompleted)
        HorizontalDivider()
        NotesSection(state.notes, onSaveNote, onDeleteNote)
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

@Composable
private fun NotesSection(
    notes: List<Note>,
    onSaveNote: (text: String, noteId: Long?) -> Unit,
    onDeleteNote: (Long) -> Unit,
) {
    // null = closed, -1 = new note, otherwise the id of the note being edited.
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "My notes",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            TextButton(onClick = { editingId = NEW_NOTE }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Add note")
            }
        }
        if (notes.isEmpty()) {
            Text(
                "No notes yet. Write down what you want to remember.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        notes.forEach { note ->
            NoteCard(note, onEdit = { editingId = note.id }, onDelete = { onDeleteNote(note.id) })
        }
    }

    editingId?.let { id ->
        val existing = notes.firstOrNull { it.id == id }
        NoteDialog(
            initialText = existing?.text.orEmpty(),
            isNew = existing == null,
            onDismiss = { editingId = null },
            onSave = { text ->
                onSaveNote(text, existing?.id)
                editingId = null
            },
        )
    }
}

@Composable
fun NoteCard(note: Note, onEdit: () -> Unit, onDelete: () -> Unit, title: String? = null) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (title != null) Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(note.text, style = MaterialTheme.typography.bodyLarge)
                Text(
                    editedLabel(note.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit note") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete note") }
        }
    }
}

private const val NEW_NOTE = -1L

private fun editedLabel(updatedAt: Long, now: Long = System.currentTimeMillis()): String =
    if (now - updatedAt < DateUtils.MINUTE_IN_MILLIS) "Edited just now"
    else "Edited " + DateUtils.getRelativeTimeSpanString(updatedAt, now, DateUtils.MINUTE_IN_MILLIS)

@Preview
@Composable
private fun LessonDetailPreview() {
    TeachingTheme(darkTheme = true) {
        LessonDetailContent(
            LessonDetailUiState.Success(
                SeedData.lessons[2],
                progress = null,
                quizQuestionCount = 3,
                isBookmarked = true,
                notes = listOf(Note(1, "compose-layouts", "Modifier order matters!", 0, 0)),
            ),
            onBack = {}, onTakeQuiz = {}, onSetCompleted = {}, onToggleBookmark = {},
            onSaveNote = { _, _ -> }, onDeleteNote = {},
        )
    }
}

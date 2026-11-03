package edu.sdwu.teaching.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.sdwu.teaching.domain.model.Lesson
import edu.sdwu.teaching.domain.model.NoteWithLesson
import edu.sdwu.teaching.domain.repository.BookmarkRepository
import edu.sdwu.teaching.domain.repository.LessonRepository
import edu.sdwu.teaching.domain.repository.NoteRepository
import edu.sdwu.teaching.ui.components.EmptyState
import edu.sdwu.teaching.ui.components.LessonCard
import edu.sdwu.teaching.ui.components.LoadingState
import edu.sdwu.teaching.ui.lessons.NoteCard
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotesUiState(
    val isLoading: Boolean = true,
    val bookmarkedLessons: List<Lesson> = emptyList(),
    val notes: List<NoteWithLesson> = emptyList(),
)

@HiltViewModel
class NotesViewModel @Inject constructor(
    lessonRepository: LessonRepository,
    bookmarkRepository: BookmarkRepository,
    private val noteRepository: NoteRepository,
) : ViewModel() {

    val uiState: StateFlow<NotesUiState> = combine(
        lessonRepository.observeLessons(),
        bookmarkRepository.observeBookmarkedIds(),
        noteRepository.observeAllNotes(),
    ) { lessons, bookmarks, notes ->
        NotesUiState(isLoading = false, bookmarkedLessons = lessons.filter { it.id in bookmarks }, notes = notes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotesUiState())

    fun deleteNote(id: Long) {
        viewModelScope.launch { noteRepository.deleteNote(id) }
    }
}

/** Everything the student saved: bookmarked lessons first, then all notes (newest first). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    onBack: () -> Unit,
    onLessonClick: (String) -> Unit,
    viewModel: NotesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notes & bookmarks") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))
            state.notes.isEmpty() && state.bookmarkedLessons.isEmpty() -> EmptyState(
                title = "Nothing saved yet",
                message = "Tap the bookmark icon on a lesson, or add a note at the bottom of a lesson.",
                icon = Icons.Filled.Edit,
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (state.bookmarkedLessons.isNotEmpty()) {
                    item { SectionTitle("Bookmarked lessons") }
                    items(state.bookmarkedLessons, key = { "b-" + it.id }) { lesson ->
                        LessonCard(lesson = lesson, isBookmarked = true, onClick = { onLessonClick(lesson.id) })
                    }
                }
                if (state.notes.isNotEmpty()) {
                    item { SectionTitle("All notes") }
                    items(state.notes, key = { "n-" + it.note.id }) { item ->
                        NoteCard(
                            note = item.note,
                            title = item.lessonTitle,
                            // Editing happens in the lesson, next to the content the note is about.
                            onEdit = { onLessonClick(item.note.lessonId) },
                            onDelete = { viewModel.deleteNote(item.note.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp).semantics { heading() },
    )
}

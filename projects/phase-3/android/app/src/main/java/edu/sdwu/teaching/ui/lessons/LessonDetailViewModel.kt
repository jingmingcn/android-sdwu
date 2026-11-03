package edu.sdwu.teaching.ui.lessons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.sdwu.teaching.domain.model.Lesson
import edu.sdwu.teaching.domain.model.LessonProgress
import edu.sdwu.teaching.domain.model.Note
import edu.sdwu.teaching.domain.model.resolveMediaUrl
import edu.sdwu.teaching.domain.repository.BookmarkRepository
import edu.sdwu.teaching.domain.repository.LessonRepository
import edu.sdwu.teaching.domain.repository.NoteRepository
import edu.sdwu.teaching.domain.repository.PreferencesRepository
import edu.sdwu.teaching.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface LessonDetailUiState {
    data object Loading : LessonDetailUiState
    data class Success(
        val lesson: Lesson,
        val progress: LessonProgress?,
        val quizQuestionCount: Int,
        val isBookmarked: Boolean = false,
        val notes: List<Note> = emptyList(),
        /** Ready-to-play URL (relative /media paths already resolved), or null for no video. */
        val videoUrl: String? = null,
    ) : LessonDetailUiState
    data object NotFound : LessonDetailUiState
}

/**
 * The lesson id comes from the type-safe route and is passed in with Hilt *assisted injection*:
 * Hilt supplies the repositories, the screen supplies the id. Plain constructor parameters keep
 * the ViewModel easy to unit test (no SavedStateHandle/Bundle needed).
 */
@HiltViewModel(assistedFactory = LessonDetailViewModel.Factory::class)
class LessonDetailViewModel @AssistedInject constructor(
    @Assisted private val lessonId: String,
    lessonRepository: LessonRepository,
    private val progressRepository: ProgressRepository,
    private val noteRepository: NoteRepository,
    private val bookmarkRepository: BookmarkRepository,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {

    /** The lesson plus its playable video URL (combine() accepts at most 5 typed flows). */
    private val lessonWithVideo = combine(
        lessonRepository.observeLesson(lessonId),
        preferencesRepository.preferences.map { it.serverUrl }.distinctUntilChanged(),
    ) { lesson, serverUrl -> lesson?.let { it to resolveMediaUrl(it.videoUrl, serverUrl) } }

    val uiState: StateFlow<LessonDetailUiState> = combine(
        lessonWithVideo,
        progressRepository.observe(lessonId),
        lessonRepository.observeQuiz(lessonId).map { it.size },
        bookmarkRepository.observeBookmarkedIds().map { lessonId in it },
        noteRepository.observeNotes(lessonId),
    ) { lessonAndVideo, progress, quizCount, bookmarked, notes ->
        if (lessonAndVideo == null) {
            LessonDetailUiState.NotFound
        } else {
            val (lesson, videoUrl) = lessonAndVideo
            LessonDetailUiState.Success(lesson, progress, quizCount, bookmarked, notes, videoUrl)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LessonDetailUiState.Loading)

    init {
        // Opening a lesson counts as starting it.
        viewModelScope.launch { progressRepository.markOpened(lessonId) }
    }

    fun setCompleted(completed: Boolean) {
        viewModelScope.launch { progressRepository.setCompleted(lessonId, completed) }
    }

    fun toggleBookmark() {
        viewModelScope.launch { bookmarkRepository.toggle(lessonId) }
    }

    /** Creates a note, or edits an existing one when [noteId] is given. */
    fun saveNote(text: String, noteId: Long? = null) {
        viewModelScope.launch { noteRepository.saveNote(lessonId, text, noteId) }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch { noteRepository.deleteNote(noteId) }
    }

    @AssistedFactory
    interface Factory {
        fun create(lessonId: String): LessonDetailViewModel
    }
}

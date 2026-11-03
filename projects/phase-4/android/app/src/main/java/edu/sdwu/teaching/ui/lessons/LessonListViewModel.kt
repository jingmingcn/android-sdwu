package edu.sdwu.teaching.ui.lessons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.sdwu.teaching.data.remote.toUserMessage
import edu.sdwu.teaching.domain.model.Difficulty
import edu.sdwu.teaching.domain.model.Lesson
import edu.sdwu.teaching.domain.model.LessonFilter
import edu.sdwu.teaching.domain.model.LessonProgress
import edu.sdwu.teaching.domain.model.ProgressStatus
import edu.sdwu.teaching.domain.repository.BookmarkRepository
import edu.sdwu.teaching.domain.repository.LessonRepository
import edu.sdwu.teaching.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LessonListUiState {
    data object Loading : LessonListUiState
    data class Success(
        /** Lessons after search + filters. */
        val lessons: List<Lesson>,
        val totalCount: Int,
        val categories: List<String>,
        val filter: LessonFilter,
        val progress: Map<String, LessonProgress>,
        val bookmarks: Set<String>,
        val isRefreshing: Boolean,
        /** One-shot message (snackbar). The screen calls onMessageShown() after showing it. */
        val message: String?,
    ) : LessonListUiState
}

/** Data that comes from repositories, combined first (combine() takes at most 5 flows). */
private data class LessonData(
    val lessons: List<Lesson>,
    val progress: Map<String, LessonProgress>,
    val bookmarks: Set<String>,
)

@HiltViewModel
class LessonListViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
    private val progressRepository: ProgressRepository,
    bookmarkRepository: BookmarkRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(LessonFilter())
    private val isRefreshing = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    private val data = combine(
        lessonRepository.observeLessons(),
        progressRepository.observeAll(),
        bookmarkRepository.observeBookmarkedIds(),
        ::LessonData,
    )

    val uiState: StateFlow<LessonListUiState> = combine(data, filter, isRefreshing, message) { d, f, refreshing, msg ->
        LessonListUiState.Success(
            lessons = f.apply(d.lessons, d.progress, d.bookmarks),
            totalCount = d.lessons.size,
            categories = d.lessons.map { it.category }.distinct(),
            filter = f,
            progress = d.progress,
            bookmarks = d.bookmarks,
            isRefreshing = refreshing,
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LessonListUiState.Loading)

    fun onQueryChange(query: String) = filter.update { it.copy(query = query) }

    /** Tapping the selected chip again clears it. */
    fun onCategorySelected(category: String) =
        filter.update { it.copy(category = if (it.category == category) null else category) }

    fun onDifficultySelected(difficulty: Difficulty) =
        filter.update { it.copy(difficulty = if (it.difficulty == difficulty) null else difficulty) }

    fun onStatusSelected(status: ProgressStatus) =
        filter.update { it.copy(status = if (it.status == status) null else status) }

    fun onBookmarkedOnlyToggle() = filter.update { it.copy(bookmarkedOnly = !it.bookmarkedOnly) }

    fun clearFilters() = filter.update { LessonFilter() }

    /** Pull-to-refresh. Runs in the foreground so the spinner reflects the real network call. */
    fun refresh() {
        if (isRefreshing.value) return
        viewModelScope.launch {
            isRefreshing.value = true
            lessonRepository.refresh()
                .onSuccess { progressRepository.sync() }
                .onFailure { e -> message.value = "Offline - showing saved lessons. ${e.toUserMessage()}" }
            isRefreshing.value = false
        }
    }

    fun onMessageShown() = message.update { null }
}

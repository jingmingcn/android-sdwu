package edu.sdwu.teaching.ui.lessons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.sdwu.teaching.data.remote.toUserMessage
import edu.sdwu.teaching.domain.model.Lesson
import edu.sdwu.teaching.domain.model.LessonProgress
import edu.sdwu.teaching.domain.repository.LessonRepository
import edu.sdwu.teaching.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface LessonListUiState {
    data object Loading : LessonListUiState
    data class Success(
        val lessons: List<Lesson>,
        val progress: Map<String, LessonProgress>,
        val isRefreshing: Boolean,
        /** One-shot message (snackbar). The screen calls onMessageShown() after showing it. */
        val message: String?,
    ) : LessonListUiState
}

class LessonListViewModel(
    private val lessonRepository: LessonRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel() {

    private val isRefreshing = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<LessonListUiState> = combine(
        lessonRepository.observeLessons(),
        progressRepository.observeAll(),
        isRefreshing,
        message,
    ) { lessons, progress, refreshing, msg ->
        LessonListUiState.Success(lessons, progress, refreshing, msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LessonListUiState.Loading)

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

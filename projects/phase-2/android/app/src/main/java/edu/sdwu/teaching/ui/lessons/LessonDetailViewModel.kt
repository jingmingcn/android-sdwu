package edu.sdwu.teaching.ui.lessons

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import edu.sdwu.teaching.domain.model.Lesson
import edu.sdwu.teaching.domain.model.LessonProgress
import edu.sdwu.teaching.domain.repository.LessonRepository
import edu.sdwu.teaching.domain.repository.ProgressRepository
import edu.sdwu.teaching.ui.navigation.LessonDetailRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface LessonDetailUiState {
    data object Loading : LessonDetailUiState
    data class Success(
        val lesson: Lesson,
        val progress: LessonProgress?,
        val quizQuestionCount: Int,
    ) : LessonDetailUiState
    data object NotFound : LessonDetailUiState
}

class LessonDetailViewModel(
    savedStateHandle: SavedStateHandle,
    lessonRepository: LessonRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel() {

    /** Navigation arguments arrive through SavedStateHandle; toRoute() decodes them type-safely. */
    private val lessonId: String = savedStateHandle.toRoute<LessonDetailRoute>().lessonId

    val uiState: StateFlow<LessonDetailUiState> = combine(
        lessonRepository.observeLesson(lessonId),
        progressRepository.observe(lessonId),
        lessonRepository.observeQuiz(lessonId),
    ) { lesson, progress, quiz ->
        if (lesson == null) LessonDetailUiState.NotFound else LessonDetailUiState.Success(lesson, progress, quiz.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LessonDetailUiState.Loading)

    init {
        // Opening a lesson counts as starting it.
        viewModelScope.launch { progressRepository.markOpened(lessonId) }
    }

    fun setCompleted(completed: Boolean) {
        viewModelScope.launch { progressRepository.setCompleted(lessonId, completed) }
    }
}

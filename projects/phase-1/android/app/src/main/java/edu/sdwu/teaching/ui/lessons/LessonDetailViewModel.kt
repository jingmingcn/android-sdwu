package edu.sdwu.teaching.ui.lessons

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import edu.sdwu.teaching.domain.model.Lesson
import edu.sdwu.teaching.domain.repository.LessonRepository
import edu.sdwu.teaching.ui.navigation.LessonDetailRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface LessonDetailUiState {
    data object Loading : LessonDetailUiState
    data class Success(val lesson: Lesson) : LessonDetailUiState
    data object NotFound : LessonDetailUiState
}

class LessonDetailViewModel(
    savedStateHandle: SavedStateHandle,
    repository: LessonRepository,
) : ViewModel() {

    /** Navigation arguments arrive through SavedStateHandle; toRoute() decodes them type-safely. */
    private val lessonId: String = savedStateHandle.toRoute<LessonDetailRoute>().lessonId

    val uiState: StateFlow<LessonDetailUiState> = repository.observeLesson(lessonId)
        .map { lesson -> lesson?.let { LessonDetailUiState.Success(it) } ?: LessonDetailUiState.NotFound }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LessonDetailUiState.Loading)
}

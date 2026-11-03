package edu.sdwu.teaching.ui.lessons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.sdwu.teaching.domain.model.Lesson
import edu.sdwu.teaching.domain.repository.LessonRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface LessonListUiState {
    data object Loading : LessonListUiState
    data class Success(val lessons: List<Lesson>) : LessonListUiState
}

class LessonListViewModel(repository: LessonRepository) : ViewModel() {

    val uiState: StateFlow<LessonListUiState> = repository.observeLessons()
        .map<List<Lesson>, LessonListUiState> { LessonListUiState.Success(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LessonListUiState.Loading)
}

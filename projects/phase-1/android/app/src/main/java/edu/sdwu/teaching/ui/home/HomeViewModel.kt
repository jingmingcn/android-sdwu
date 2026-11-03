package edu.sdwu.teaching.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.sdwu.teaching.domain.model.Lesson
import edu.sdwu.teaching.domain.repository.LessonRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val isLoading: Boolean = true,
    val lessonCount: Int = 0,
    val totalMinutes: Int = 0,
    val categoryCount: Int = 0,
    val featured: List<Lesson> = emptyList(),
)

class HomeViewModel(repository: LessonRepository) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = repository.observeLessons()
        .map { lessons ->
            HomeUiState(
                isLoading = false,
                lessonCount = lessons.size,
                totalMinutes = lessons.sumOf { it.durationMinutes },
                categoryCount = lessons.map { it.category }.distinct().size,
                featured = lessons.take(FEATURED_COUNT),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private companion object {
        const val FEATURED_COUNT = 3
    }
}

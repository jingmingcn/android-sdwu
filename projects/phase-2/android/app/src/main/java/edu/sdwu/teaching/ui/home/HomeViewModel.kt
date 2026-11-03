package edu.sdwu.teaching.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.sdwu.teaching.domain.model.Lesson
import edu.sdwu.teaching.domain.model.LessonProgress
import edu.sdwu.teaching.domain.model.SessionState
import edu.sdwu.teaching.domain.repository.AuthRepository
import edu.sdwu.teaching.domain.repository.LessonRepository
import edu.sdwu.teaching.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val isLoading: Boolean = true,
    val greetingName: String? = null,
    val lessonCount: Int = 0,
    val completedCount: Int = 0,
    val totalMinutes: Int = 0,
    /** The first lesson that is not completed yet, or null when everything is done. */
    val continueLesson: Lesson? = null,
    val upNext: List<Lesson> = emptyList(),
    val progress: Map<String, LessonProgress> = emptyMap(),
)

class HomeViewModel(
    lessonRepository: LessonRepository,
    progressRepository: ProgressRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        lessonRepository.observeLessons(),
        progressRepository.observeAll(),
        authRepository.session,
    ) { lessons, progress, session ->
        val notDone = lessons.filterNot { progress[it.id]?.isCompleted == true }
        HomeUiState(
            isLoading = false,
            greetingName = (session as? SessionState.SignedIn)?.user?.displayName,
            lessonCount = lessons.size,
            completedCount = lessons.size - notDone.size,
            totalMinutes = lessons.sumOf { it.durationMinutes },
            continueLesson = notDone.firstOrNull(),
            upNext = notDone.drop(1).take(UP_NEXT_COUNT),
            progress = progress,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private companion object {
        const val UP_NEXT_COUNT = 2
    }
}

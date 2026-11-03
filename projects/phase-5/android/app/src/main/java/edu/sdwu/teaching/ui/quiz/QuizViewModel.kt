package edu.sdwu.teaching.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.sdwu.teaching.domain.model.QuizQuestion
import edu.sdwu.teaching.domain.model.QuizResult
import edu.sdwu.teaching.domain.model.QuizScorer
import edu.sdwu.teaching.domain.repository.LessonRepository
import edu.sdwu.teaching.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface QuizUiState {
    data object Loading : QuizUiState
    data object Empty : QuizUiState

    data class Answering(
        val questions: List<QuizQuestion>,
        val index: Int,
        /** questionId -> chosen option index */
        val answers: Map<String, Int>,
    ) : QuizUiState {
        val current: QuizQuestion get() = questions[index]
        val isLast: Boolean get() = index == questions.lastIndex
    }

    data class Finished(
        val questions: List<QuizQuestion>,
        val answers: Map<String, Int>,
        val result: QuizResult,
    ) : QuizUiState
}

/** User actions, kept in one private state so the UI state is always derived from it. */
private data class QuizProgress(val index: Int = 0, val answers: Map<String, Int> = emptyMap(), val finished: Boolean = false)

@HiltViewModel(assistedFactory = QuizViewModel.Factory::class)
class QuizViewModel @AssistedInject constructor(
    @Assisted private val lessonId: String,
    private val lessonRepository: LessonRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel() {

    private val progress = MutableStateFlow(QuizProgress())

    val uiState: StateFlow<QuizUiState> = combine(lessonRepository.observeQuiz(lessonId), progress) { questions, p ->
        when {
            questions.isEmpty() -> QuizUiState.Empty
            p.finished -> QuizUiState.Finished(questions, p.answers, QuizScorer.score(questions, p.answers))
            else -> QuizUiState.Answering(questions, p.index.coerceAtMost(questions.lastIndex), p.answers)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuizUiState.Loading)

    fun selectAnswer(questionId: String, optionIndex: Int) =
        progress.update { it.copy(answers = it.answers + (questionId to optionIndex)) }

    fun previous() = progress.update { it.copy(index = (it.index - 1).coerceAtLeast(0)) }

    fun next() = progress.update { it.copy(index = it.index + 1) }

    fun submit() {
        viewModelScope.launch {
            val questions = lessonRepository.observeQuiz(lessonId).first()
            val result = QuizScorer.score(questions, progress.value.answers)
            progressRepository.recordQuizResult(lessonId, result.score, result.total)
            progress.update { it.copy(finished = true) }
        }
    }

    fun retry() = progress.update { QuizProgress() }

    @AssistedFactory
    interface Factory {
        fun create(lessonId: String): QuizViewModel
    }
}

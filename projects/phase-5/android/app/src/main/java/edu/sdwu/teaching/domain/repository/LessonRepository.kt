package edu.sdwu.teaching.domain.repository

import edu.sdwu.teaching.domain.model.Lesson
import edu.sdwu.teaching.domain.model.QuizQuestion
import kotlinx.coroutines.flow.Flow

/**
 * Contract for lessons and their quizzes.
 *
 * Offline-first: every `observe` function reads Room only, so the UI works without a
 * network. [refresh] downloads the latest catalogue and writes it into Room, and the
 * open Flows then emit the new data automatically.
 */
interface LessonRepository {
    fun observeLessons(): Flow<List<Lesson>>
    fun observeLesson(id: String): Flow<Lesson?>
    fun observeQuiz(lessonId: String): Flow<List<QuizQuestion>>

    /** Inserts the bundled seed lessons and quizzes the first time the app starts. */
    suspend fun seedIfEmpty()

    /** Downloads lessons + quizzes from the backend. Failure leaves the cached data untouched. */
    suspend fun refresh(): Result<Unit>
}

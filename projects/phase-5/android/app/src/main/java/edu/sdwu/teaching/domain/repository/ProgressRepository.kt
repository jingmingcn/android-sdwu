package edu.sdwu.teaching.domain.repository

import edu.sdwu.teaching.domain.model.LessonProgress
import kotlinx.coroutines.flow.Flow

/**
 * Lesson progress. Writes go to Room first and are marked "pending"; [sync] pushes pending
 * rows to the backend and pulls the merged result when a user is signed in.
 */
interface ProgressRepository {
    /** lessonId -> progress. Lessons never opened are missing from the map. */
    fun observeAll(): Flow<Map<String, LessonProgress>>
    fun observe(lessonId: String): Flow<LessonProgress?>

    /** NOT_STARTED -> IN_PROGRESS. Does nothing if the lesson is already started or completed. */
    suspend fun markOpened(lessonId: String)
    suspend fun setCompleted(lessonId: String, completed: Boolean)

    /** Keeps the best score. A passing score also marks the lesson completed. */
    suspend fun recordQuizResult(lessonId: String, score: Int, total: Int)

    /** Push + pull with the backend. Returns success without doing anything when not signed in. */
    suspend fun sync(): Result<Unit>

    /** Removes all local progress (used on sign-out so the next user starts clean). */
    suspend fun clearLocal()
}

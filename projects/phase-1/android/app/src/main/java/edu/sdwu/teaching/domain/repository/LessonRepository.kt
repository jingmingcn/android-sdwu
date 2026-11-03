package edu.sdwu.teaching.domain.repository

import edu.sdwu.teaching.domain.model.Lesson
import kotlinx.coroutines.flow.Flow

/**
 * Contract for lesson data. ViewModels depend on this interface only, so the
 * implementation can change (Room only in Phase 1, Room + Retrofit in Phase 2)
 * without touching any UI code.
 */
interface LessonRepository {
    fun observeLessons(): Flow<List<Lesson>>
    fun observeLesson(id: String): Flow<Lesson?>

    /** Inserts the bundled seed lessons the first time the app starts. */
    suspend fun seedIfEmpty()
}

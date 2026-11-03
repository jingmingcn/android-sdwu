package edu.sdwu.teaching.data.repository

import edu.sdwu.teaching.data.local.LessonDao
import edu.sdwu.teaching.data.local.SeedData
import edu.sdwu.teaching.data.local.toDomain
import edu.sdwu.teaching.data.local.toEntity
import edu.sdwu.teaching.data.remote.TeachingApi
import edu.sdwu.teaching.data.remote.dto.toEntity
import edu.sdwu.teaching.domain.model.Lesson
import edu.sdwu.teaching.domain.model.QuizQuestion
import edu.sdwu.teaching.domain.repository.LessonRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room is the single source of truth for the UI; the network only ever writes into Room.
 */
@Singleton
class OfflineFirstLessonRepository @Inject constructor(
    private val dao: LessonDao,
    private val api: TeachingApi,
) : LessonRepository {

    override fun observeLessons(): Flow<List<Lesson>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeLesson(id: String): Flow<Lesson?> =
        dao.observeById(id).map { it?.toDomain() }

    override fun observeQuiz(lessonId: String): Flow<List<QuizQuestion>> =
        dao.observeQuiz(lessonId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun seedIfEmpty() {
        if (dao.count() == 0) dao.upsertAll(SeedData.lessons.map { it.toEntity() })
        // Checked separately: a database upgraded from Phase 1 has lessons but no quizzes yet.
        if (dao.quizQuestionCount() == 0) dao.upsertQuestions(SeedData.quizQuestions.map { it.toEntity() })
    }

    override suspend fun refresh(): Result<Unit> = try {
        coroutineScope {
            // The two downloads are independent, so run them in parallel.
            val lessons = async { api.lessons() }
            val questions = async { api.quizzes() }
            dao.replaceCatalog(lessons.await().map { it.toEntity() }, questions.await().map { it.toEntity() })
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e // Never swallow cancellation.
    } catch (e: Exception) {
        Result.failure(e)
    }
}

package edu.sdwu.teaching.data.repository

import edu.sdwu.teaching.data.local.LessonDao
import edu.sdwu.teaching.data.local.SeedData
import edu.sdwu.teaching.data.local.toDomain
import edu.sdwu.teaching.data.local.toEntity
import edu.sdwu.teaching.domain.model.Lesson
import edu.sdwu.teaching.domain.repository.LessonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Phase 1 implementation: Room is the only source of truth. */
class OfflineLessonRepository(
    private val dao: LessonDao,
) : LessonRepository {

    override fun observeLessons(): Flow<List<Lesson>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeLesson(id: String): Flow<Lesson?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun seedIfEmpty() {
        if (dao.count() == 0) {
            dao.upsertAll(SeedData.lessons.map { it.toEntity() })
        }
    }
}

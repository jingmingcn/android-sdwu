package edu.sdwu.teaching.data.repository

import dagger.Lazy
import edu.sdwu.teaching.data.local.ProgressDao
import edu.sdwu.teaching.data.local.ProgressEntity
import edu.sdwu.teaching.data.local.toDomain
import edu.sdwu.teaching.data.prefs.SessionDataSource
import edu.sdwu.teaching.data.remote.TeachingApi
import edu.sdwu.teaching.data.remote.dto.toDto
import edu.sdwu.teaching.data.work.SyncScheduler
import edu.sdwu.teaching.domain.model.LessonProgress
import edu.sdwu.teaching.domain.model.ProgressStatus
import edu.sdwu.teaching.domain.model.QuizScorer
import edu.sdwu.teaching.domain.repository.AuthRepository
import edu.sdwu.teaching.domain.repository.ProgressRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstProgressRepository @Inject constructor(
    private val dao: ProgressDao,
    private val api: TeachingApi,
    private val session: SessionDataSource,
    /** Phase 3: WorkManager runs the sync, with retries, even if the app is closed. */
    private val syncScheduler: SyncScheduler,
    /** Lazy breaks the cycle AuthRepository -> (clears progress) / ProgressRepository -> (401 signs out). */
    private val authRepository: Lazy<AuthRepository>,
    private val clock: Clock,
) : ProgressRepository {

    /** Only one sync at a time, otherwise two round trips could overwrite each other. */
    private val syncMutex = Mutex()

    override fun observeAll(): Flow<Map<String, LessonProgress>> =
        dao.observeAll().map { rows -> rows.associate { it.lessonId to it.toDomain() } }

    override fun observe(lessonId: String): Flow<LessonProgress?> =
        dao.observe(lessonId).map { it?.toDomain() }

    override suspend fun markOpened(lessonId: String) {
        if (dao.get(lessonId) == null) write(lessonId) { it.copy(status = ProgressStatus.IN_PROGRESS.name) }
    }

    override suspend fun setCompleted(lessonId: String, completed: Boolean) = write(lessonId) {
        it.copy(status = if (completed) ProgressStatus.COMPLETED.name else ProgressStatus.IN_PROGRESS.name)
    }

    override suspend fun recordQuizResult(lessonId: String, score: Int, total: Int) = write(lessonId) { row ->
        val isBest = row.quizBestScore == null || score > row.quizBestScore
        row.copy(
            quizBestScore = if (isBest) score else row.quizBestScore,
            quizTotal = if (isBest) total else row.quizTotal,
            status = if (QuizScorer.isPassing(score, total)) ProgressStatus.COMPLETED.name else row.status,
        )
    }

    /** Every local write: update the row, mark it pending, then ask WorkManager to sync. */
    private suspend fun write(lessonId: String, change: (ProgressEntity) -> ProgressEntity) {
        val current = dao.get(lessonId) ?: ProgressEntity(
            lessonId = lessonId,
            status = ProgressStatus.IN_PROGRESS.name,
            quizBestScore = null,
            quizTotal = null,
            updatedAt = 0,
            pendingSync = true,
        )
        dao.upsert(change(current).copy(updatedAt = clock.now(), pendingSync = true))
        if (session.token() != null) syncScheduler.requestSync()
    }

    override suspend fun sync(): Result<Unit> = syncMutex.withLock {
        if (session.token() == null) return Result.success(Unit) // Guest: progress stays local.
        try {
            val pending = dao.pending()
            val pushedAt = pending.associate { it.lessonId to it.updatedAt }
            val merged = api.syncProgress(pending.map { it.toDto() })
            val toSave = merged.mapNotNull { remote ->
                resolveSyncedProgress(dao.get(remote.lessonId), pushedAt[remote.lessonId], remote)
            }
            dao.upsertAll(toSave)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            if (e.code() == 401) authRepository.get().onTokenExpired()
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearLocal() = dao.deleteAll()
}

/** Injectable time source, so tests can control "now". */
fun interface Clock {
    fun now(): Long
}

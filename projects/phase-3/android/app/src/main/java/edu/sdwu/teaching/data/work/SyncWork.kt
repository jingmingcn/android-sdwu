package edu.sdwu.teaching.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import edu.sdwu.teaching.domain.repository.LessonRepository
import edu.sdwu.teaching.domain.repository.ProgressRepository
import retrofit2.HttpException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Asks for a background sync. An interface so repositories can be tested without WorkManager. */
interface SyncScheduler {
    /** Sync soon, as soon as there is a network connection. Repeated calls are merged. */
    fun requestSync()

    /** Keep data fresh every few hours, even if the app is not opened. */
    fun schedulePeriodicSync()
}

@Singleton
class WorkManagerSyncScheduler @Inject constructor(
    private val workManager: WorkManager,
) : SyncScheduler {

    private val needsNetwork = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    override fun requestSync() {
        // No network constraint here on purpose: Android only reports "connected" for networks
        // with validated *internet* access. A class server on a Wi-Fi without internet (or an
        // emulator behind a proxy) never qualifies, so the work would wait forever. Instead the
        // worker simply tries, and WorkManager retries with backoff when the server is unreachable.
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        // REPLACE: the newest request wins. Sync is idempotent (and serialized by a Mutex), so
        // restarting it is safe, and a stale request can never block newer ones.
        workManager.enqueueUniqueWork(ONE_TIME_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    override fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(needsNetwork)
            .build()
        workManager.enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private companion object {
        const val ONE_TIME_NAME = "sync-now"
        const val PERIODIC_NAME = "sync-periodic"
    }
}

/**
 * Refreshes the lesson catalogue and pushes/pulls progress. WorkManager retries it with
 * exponential backoff when the server is unreachable, even after the app is closed.
 * @HiltWorker + @AssistedInject let Hilt pass repositories into a Worker.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val lessonRepository: LessonRepository,
    private val progressRepository: ProgressRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val lessons = lessonRepository.refresh()
        val progress = progressRepository.sync()
        val failure = lessons.exceptionOrNull() ?: progress.exceptionOrNull() ?: return Result.success()
        // 4xx errors will not fix themselves by retrying (e.g. 401 already signed the user out).
        val clientError = failure is HttpException && failure.code() in 400..499
        return if (clientError || runAttemptCount >= MAX_ATTEMPTS) Result.failure() else Result.retry()
    }

    private companion object {
        const val MAX_ATTEMPTS = 5
    }
}

package edu.sdwu.teaching.data.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import edu.sdwu.teaching.MainActivity
import edu.sdwu.teaching.R
import edu.sdwu.teaching.domain.repository.LessonRepository
import edu.sdwu.teaching.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Schedules the daily local "time to study" notification. */
interface ReminderScheduler {
    fun schedule(enabled: Boolean, hour: Int)

    /** Fires the reminder right away, so students can test notifications without waiting a day. */
    fun sendTestReminder()
}

@Singleton
class WorkManagerReminderScheduler @Inject constructor(
    private val workManager: WorkManager,
) : ReminderScheduler {
    override fun schedule(enabled: Boolean, hour: Int) {
        if (!enabled) {
            workManager.cancelUniqueWork(DAILY_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<StudyReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayUntilHour(hour, LocalDateTime.now()).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        // UPDATE keeps the schedule but applies a changed hour.
        workManager.enqueueUniquePeriodicWork(DAILY_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    override fun sendTestReminder() {
        workManager.enqueueUniqueWork(TEST_NAME, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<StudyReminderWorker>().build())
    }

    private companion object {
        const val DAILY_NAME = "study-reminder"
        const val TEST_NAME = "study-reminder-test"
    }
}

/** Time from [now] until the next [hour]:00. Pure function, unit tested. */
fun delayUntilHour(hour: Int, now: LocalDateTime): Duration {
    var next = now.toLocalDate().atTime(LocalTime.of(hour, 0))
    if (!next.isAfter(now)) next = next.plusDays(1)
    return Duration.between(now, next)
}

@HiltWorker
class StudyReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val lessonRepository: LessonRepository,
    private val progressRepository: ProgressRepository,
    private val notifier: StudyNotifier,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val progress = progressRepository.observeAll().first()
        val next = lessonRepository.observeLessons().first().firstOrNull { progress[it.id]?.isCompleted != true }
        notifier.showReminder(
            if (next == null) "You finished every lesson. Review your notes!" else "Next up: ${next.title}",
        )
        return Result.success()
    }
}

@Singleton
class StudyNotifier @Inject constructor(@ApplicationContext private val context: Context) {

    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Study reminders", NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = "Daily reminder to continue your lessons"
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun showReminder(text: String) {
        if (!canNotify()) return // Android 13+: the user has not granted notifications.
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time to study")
            .setContentText(text)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        @Suppress("MissingPermission") // Checked by canNotify() above.
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val CHANNEL_ID = "study_reminders"
        const val NOTIFICATION_ID = 1001
    }
}

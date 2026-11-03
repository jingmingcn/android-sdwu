package edu.sdwu.teaching.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import edu.sdwu.teaching.domain.model.LessonProgress
import edu.sdwu.teaching.domain.model.ProgressStatus

/**
 * Local progress for the current user. [pendingSync] is true when the row changed on the
 * device and has not been accepted by the backend yet: the heart of offline-first writes.
 */
@Entity(tableName = "progress")
data class ProgressEntity(
    @PrimaryKey @ColumnInfo(name = "lesson_id") val lessonId: String,
    val status: String,
    @ColumnInfo(name = "quiz_best_score") val quizBestScore: Int?,
    @ColumnInfo(name = "quiz_total") val quizTotal: Int?,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "pending_sync") val pendingSync: Boolean,
)

fun ProgressEntity.toDomain() = LessonProgress(
    lessonId = lessonId,
    status = ProgressStatus.entries.firstOrNull { it.name == status } ?: ProgressStatus.NOT_STARTED,
    quizBestScore = quizBestScore,
    quizTotal = quizTotal,
    updatedAt = updatedAt,
)

package edu.sdwu.teaching.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import edu.sdwu.teaching.domain.model.Difficulty
import edu.sdwu.teaching.domain.model.Lesson

@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "order_index") val order: Int,
    val title: String,
    val summary: String,
    val content: String,
    val category: String,
    /** Stored as the enum name, e.g. "BEGINNER". */
    val difficulty: String,
    @ColumnInfo(name = "duration_minutes") val durationMinutes: Int,
    @ColumnInfo(name = "video_url") val videoUrl: String?,
)

fun LessonEntity.toDomain() = Lesson(
    id = id,
    order = order,
    title = title,
    summary = summary,
    content = content,
    category = category,
    difficulty = Difficulty.entries.firstOrNull { it.name == difficulty } ?: Difficulty.BEGINNER,
    durationMinutes = durationMinutes,
    videoUrl = videoUrl,
)

fun Lesson.toEntity() = LessonEntity(
    id = id,
    order = order,
    title = title,
    summary = summary,
    content = content,
    category = category,
    difficulty = difficulty.name,
    durationMinutes = durationMinutes,
    videoUrl = videoUrl,
)

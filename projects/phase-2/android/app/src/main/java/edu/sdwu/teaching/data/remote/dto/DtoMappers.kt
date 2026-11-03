package edu.sdwu.teaching.data.remote.dto

import edu.sdwu.teaching.data.local.LessonEntity
import edu.sdwu.teaching.data.local.ProgressEntity
import edu.sdwu.teaching.data.local.QuizQuestionEntity
import edu.sdwu.teaching.domain.model.User

fun LessonDto.toEntity() = LessonEntity(
    id = id,
    order = order,
    title = title,
    summary = summary,
    content = content,
    category = category,
    difficulty = difficulty,
    durationMinutes = durationMinutes,
    videoUrl = videoUrl,
)

fun QuizQuestionDto.toEntity() = QuizQuestionEntity(id, lessonId, order, question, options, correctIndex, explanation)

fun UserDto.toDomain() = User(id = id, email = email, displayName = displayName, isInstructor = isInstructor)

fun ProgressEntity.toDto() = ProgressDto(lessonId, status, quizBestScore, quizTotal, updatedAt)

fun ProgressDto.toEntity(pendingSync: Boolean = false) =
    ProgressEntity(lessonId, status, quizBestScore, quizTotal, updatedAt, pendingSync)

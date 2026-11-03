package edu.sdwu.teaching.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import edu.sdwu.teaching.domain.model.QuizQuestion

@Entity(tableName = "quiz_questions", indices = [Index("lesson_id")])
data class QuizQuestionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "lesson_id") val lessonId: String,
    @ColumnInfo(name = "order_index") val order: Int,
    val question: String,
    /** Stored as a JSON array by [Converters]. */
    val options: List<String>,
    @ColumnInfo(name = "correct_index") val correctIndex: Int,
    val explanation: String,
)

fun QuizQuestionEntity.toDomain() = QuizQuestion(id, lessonId, order, question, options, correctIndex, explanation)

fun QuizQuestion.toEntity() = QuizQuestionEntity(id, lessonId, order, question, options, correctIndex, explanation)

package edu.sdwu.teaching.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Lessons and their quiz questions (the "catalogue" that comes from the backend). */
@Dao
abstract class LessonDao {
    @Query("SELECT * FROM lessons ORDER BY order_index")
    abstract fun observeAll(): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE id = :id")
    abstract fun observeById(id: String): Flow<LessonEntity?>

    @Query("SELECT COUNT(*) FROM lessons")
    abstract suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertAll(lessons: List<LessonEntity>)

    @Query("DELETE FROM lessons WHERE id NOT IN (:keepIds)")
    abstract suspend fun deleteAllExcept(keepIds: List<String>)

    @Query("SELECT * FROM quiz_questions WHERE lesson_id = :lessonId ORDER BY order_index")
    abstract fun observeQuiz(lessonId: String): Flow<List<QuizQuestionEntity>>

    @Query("SELECT COUNT(*) FROM quiz_questions")
    abstract suspend fun quizQuestionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertQuestions(questions: List<QuizQuestionEntity>)

    @Query("DELETE FROM quiz_questions")
    abstract suspend fun deleteAllQuestions()

    /**
     * Replaces the whole catalogue atomically: observers never see a half-updated state,
     * and lessons deleted on the server disappear locally too.
     */
    @Transaction
    open suspend fun replaceCatalog(lessons: List<LessonEntity>, questions: List<QuizQuestionEntity>) {
        upsertAll(lessons)
        deleteAllExcept(lessons.map { it.id })
        deleteAllQuestions()
        upsertQuestions(questions)
    }
}

package edu.sdwu.teaching.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/** Built by Hilt in di/DatabaseModule.kt. */
@Database(
    entities = [
        LessonEntity::class,
        QuizQuestionEntity::class,
        ProgressEntity::class,
        NoteEntity::class,
        BookmarkEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lessonDao(): LessonDao
    abstract fun progressDao(): ProgressDao
    abstract fun studyDao(): StudyDao

    companion object {
        const val NAME = "teaching.db"
        val MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
    }
}

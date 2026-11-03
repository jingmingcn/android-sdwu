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
        ModuleStateEntity::class,
        ModuleDataEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lessonDao(): LessonDao
    abstract fun progressDao(): ProgressDao
    abstract fun studyDao(): StudyDao
    abstract fun moduleDao(): ModuleDao

    companion object {
        const val NAME = "teaching.db"
        val MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
    }
}

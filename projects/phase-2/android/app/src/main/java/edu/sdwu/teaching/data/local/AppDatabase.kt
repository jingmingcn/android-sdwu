package edu.sdwu.teaching.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [LessonEntity::class, QuizQuestionEntity::class, ProgressEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lessonDao(): LessonDao
    abstract fun progressDao(): ProgressDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "teaching.db")
                // Phase 1 users keep their data: see Migrations.kt.
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}

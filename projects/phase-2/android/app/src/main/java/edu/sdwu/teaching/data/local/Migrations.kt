package edu.sdwu.teaching.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Version 1 (Phase 1) had only `lessons`. Version 2 adds quiz questions and progress.
 *
 * The SQL must match exactly what Room expects. Copy it from the `createSql` entries in
 * app/schemas/edu.sdwu.teaching.data.local.AppDatabase/2.json, which Room exports at build time.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `quiz_questions` (`id` TEXT NOT NULL, `lesson_id` TEXT NOT NULL, " +
                "`order_index` INTEGER NOT NULL, `question` TEXT NOT NULL, `options` TEXT NOT NULL, " +
                "`correct_index` INTEGER NOT NULL, `explanation` TEXT NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_quiz_questions_lesson_id` ON `quiz_questions` (`lesson_id`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `progress` (`lesson_id` TEXT NOT NULL, `status` TEXT NOT NULL, " +
                "`quiz_best_score` INTEGER, `quiz_total` INTEGER, `updated_at` INTEGER NOT NULL, " +
                "`pending_sync` INTEGER NOT NULL, PRIMARY KEY(`lesson_id`))",
        )
    }
}

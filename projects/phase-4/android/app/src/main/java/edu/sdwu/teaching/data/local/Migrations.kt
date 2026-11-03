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

/** Version 3 (Phase 3) adds personal notes and bookmarks. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`lesson_id` TEXT NOT NULL, `text` TEXT NOT NULL, `created_at` INTEGER NOT NULL, " +
                "`updated_at` INTEGER NOT NULL)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_lesson_id` ON `notes` (`lesson_id`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `bookmarks` (`lesson_id` TEXT NOT NULL, `created_at` INTEGER NOT NULL, " +
                "PRIMARY KEY(`lesson_id`))",
        )
    }
}

/** Version 4 (Phase 4) adds the module registry state and per-module storage. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `modules` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `version` TEXT NOT NULL, " +
                "`source` TEXT NOT NULL, `enabled` INTEGER NOT NULL, `first_seen_at` INTEGER NOT NULL, " +
                "`last_seen_at` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `module_data` (`module_id` TEXT NOT NULL, `key` TEXT NOT NULL, " +
                "`value` TEXT NOT NULL, PRIMARY KEY(`module_id`, `key`))",
        )
    }
}

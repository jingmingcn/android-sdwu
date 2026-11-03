package edu.sdwu.teaching.di

import android.content.Context
import edu.sdwu.teaching.data.local.AppDatabase
import edu.sdwu.teaching.data.prefs.DataStorePreferencesRepository
import edu.sdwu.teaching.data.repository.OfflineLessonRepository
import edu.sdwu.teaching.domain.repository.LessonRepository
import edu.sdwu.teaching.domain.repository.PreferencesRepository

/**
 * Manual dependency injection for Phase 1: one place that builds every
 * long-lived object. Phase 3 replaces this class with Hilt modules that
 * provide exactly the same interfaces.
 */
class AppContainer(context: Context) {
    private val database: AppDatabase by lazy { AppDatabase.build(context) }

    val lessonRepository: LessonRepository by lazy { OfflineLessonRepository(database.lessonDao()) }
    val preferencesRepository: PreferencesRepository by lazy { DataStorePreferencesRepository(context) }
}

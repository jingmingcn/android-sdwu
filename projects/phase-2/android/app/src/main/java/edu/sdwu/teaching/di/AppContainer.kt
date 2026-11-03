package edu.sdwu.teaching.di

import android.content.Context
import edu.sdwu.teaching.data.local.AppDatabase
import edu.sdwu.teaching.data.prefs.DataStorePreferencesRepository
import edu.sdwu.teaching.data.prefs.SessionStore
import edu.sdwu.teaching.data.remote.TeachingApi
import edu.sdwu.teaching.data.remote.createTeachingApi
import edu.sdwu.teaching.data.repository.OfflineFirstLessonRepository
import edu.sdwu.teaching.data.repository.OfflineFirstProgressRepository
import edu.sdwu.teaching.data.repository.RemoteAuthRepository
import edu.sdwu.teaching.domain.repository.AuthRepository
import edu.sdwu.teaching.domain.repository.LessonRepository
import edu.sdwu.teaching.domain.repository.PreferencesRepository
import edu.sdwu.teaching.domain.repository.ProgressRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first

/**
 * Manual dependency injection: one place that builds every long-lived object.
 * Phase 3 replaces this class with Hilt modules that provide exactly the same interfaces.
 */
class AppContainer(context: Context, appScope: CoroutineScope) {
    private val database: AppDatabase by lazy { AppDatabase.build(context) }
    private val sessionStore by lazy { SessionStore(context) }

    val preferencesRepository: PreferencesRepository by lazy { DataStorePreferencesRepository(context) }

    private val api: TeachingApi by lazy {
        createTeachingApi(
            serverUrl = { preferencesRepository.preferences.first().serverUrl },
            token = { sessionStore.token() },
        )
    }

    val lessonRepository: LessonRepository by lazy { OfflineFirstLessonRepository(database.lessonDao(), api) }

    val progressRepository: ProgressRepository by lazy {
        OfflineFirstProgressRepository(
            dao = database.progressDao(),
            api = api,
            sessionStore = sessionStore,
            appScope = appScope,
            onUnauthorized = { authRepository.onTokenExpired() },
        )
    }

    val authRepository: AuthRepository by lazy {
        RemoteAuthRepository(api, sessionStore, clearUserData = { progressRepository.clearLocal() })
    }
}

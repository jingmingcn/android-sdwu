package edu.sdwu.teaching.domain.repository

import edu.sdwu.teaching.domain.model.ThemeMode
import edu.sdwu.teaching.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)

    suspend fun setReminder(enabled: Boolean, hour: Int)

    /** @return false if [url] is not a valid http(s) URL (nothing is saved then). */
    suspend fun setServerUrl(url: String): Boolean
}

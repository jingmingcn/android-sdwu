package edu.sdwu.teaching.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import edu.sdwu.teaching.BuildConfig
import edu.sdwu.teaching.domain.model.ThemeMode
import edu.sdwu.teaching.domain.model.UserPreferences
import edu.sdwu.teaching.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// Exactly one DataStore instance per file, created by this property delegate.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class DataStorePreferencesRepository @Inject constructor(
    @ApplicationContext context: Context,
) : PreferencesRepository {

    private val dataStore = context.applicationContext.dataStore

    override val preferences: Flow<UserPreferences> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            UserPreferences(
                themeMode = prefs[Keys.THEME_MODE]
                    ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                    ?: ThemeMode.SYSTEM,
                dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
                serverUrl = prefs[Keys.SERVER_URL] ?: BuildConfig.DEFAULT_SERVER_URL,
                reminderEnabled = prefs[Keys.REMINDER_ENABLED] ?: false,
                reminderHour = prefs[Keys.REMINDER_HOUR] ?: 19,
            )
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    override suspend fun setReminder(enabled: Boolean, hour: Int) {
        require(hour in 0..23) { "hour must be 0-23" }
        dataStore.edit {
            it[Keys.REMINDER_ENABLED] = enabled
            it[Keys.REMINDER_HOUR] = hour
        }
    }

    override suspend fun setServerUrl(url: String): Boolean {
        val trimmed = url.trim().let { if (it.endsWith("/")) it else "$it/" }
        val parsed = trimmed.toHttpUrlOrNull() ?: return false
        dataStore.edit { it[Keys.SERVER_URL] = parsed.toString() }
        return true
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val SERVER_URL = stringPreferencesKey("server_url")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
    }
}

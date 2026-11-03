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
import edu.sdwu.teaching.domain.model.SessionState
import edu.sdwu.teaching.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

// A separate file from user_prefs: signing out clears this file only, never the theme.
private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

/**
 * Persists the JWT and the signed-in user.
 *
 * Teaching note: DataStore is not encrypted. That is acceptable for a course project; a
 * production app would encrypt the token (e.g. with a key from the Android Keystore).
 */
class SessionStore(context: Context) {

    private val dataStore = context.applicationContext.sessionDataStore

    val session: Flow<SessionState> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            val token = prefs[Keys.TOKEN]
            val userId = prefs[Keys.USER_ID]
            when {
                token != null && userId != null -> SessionState.SignedIn(
                    user = User(
                        id = userId,
                        email = prefs[Keys.EMAIL].orEmpty(),
                        displayName = prefs[Keys.DISPLAY_NAME].orEmpty(),
                        isInstructor = prefs[Keys.IS_INSTRUCTOR] ?: false,
                    ),
                    token = token,
                )
                prefs[Keys.GUEST] == true -> SessionState.Guest
                else -> SessionState.SignedOut
            }
        }

    suspend fun token(): String? = (session.first() as? SessionState.SignedIn)?.token

    suspend fun saveSignedIn(user: User, token: String) {
        dataStore.edit {
            it.clear()
            it[Keys.TOKEN] = token
            it[Keys.USER_ID] = user.id
            it[Keys.EMAIL] = user.email
            it[Keys.DISPLAY_NAME] = user.displayName
            it[Keys.IS_INSTRUCTOR] = user.isInstructor
        }
    }

    suspend fun saveGuest() {
        dataStore.edit {
            it.clear()
            it[Keys.GUEST] = true
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private object Keys {
        val TOKEN = stringPreferencesKey("token")
        val USER_ID = intPreferencesKey("user_id")
        val EMAIL = stringPreferencesKey("email")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val IS_INSTRUCTOR = booleanPreferencesKey("is_instructor")
        val GUEST = booleanPreferencesKey("guest")
    }
}

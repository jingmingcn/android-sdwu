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
import edu.sdwu.teaching.domain.model.SessionState
import edu.sdwu.teaching.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// A separate file from user_prefs: signing out clears this file only, never the theme.
private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

/** Where the session lives. An interface so repository tests can use an in-memory fake. */
interface SessionDataSource {
    val session: Flow<SessionState>
    suspend fun token(): String?
    suspend fun saveSignedIn(user: User, token: String)
    suspend fun updateUser(user: User)
    suspend fun saveGuest()
    suspend fun clear()
}

/**
 * Persists the JWT and the signed-in user.
 *
 * Teaching note: DataStore is not encrypted. That is acceptable for a course project; a
 * production app would encrypt the token (e.g. with a key from the Android Keystore).
 */
@Singleton
class SessionStore @Inject constructor(@ApplicationContext context: Context) : SessionDataSource {

    private val dataStore = context.applicationContext.sessionDataStore

    override val session: Flow<SessionState> = dataStore.data
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
                        avatarUrl = prefs[Keys.AVATAR_URL],
                    ),
                    token = token,
                )
                prefs[Keys.GUEST] == true -> SessionState.Guest
                else -> SessionState.SignedOut
            }
        }

    override suspend fun token(): String? = (session.first() as? SessionState.SignedIn)?.token

    override suspend fun saveSignedIn(user: User, token: String) {
        dataStore.edit {
            it.clear()
            it[Keys.TOKEN] = token
            it.writeUser(user)
        }
    }

    override suspend fun updateUser(user: User) {
        dataStore.edit { it.writeUser(user) }
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.writeUser(user: User) {
        this[Keys.USER_ID] = user.id
        this[Keys.EMAIL] = user.email
        this[Keys.DISPLAY_NAME] = user.displayName
        this[Keys.IS_INSTRUCTOR] = user.isInstructor
        if (user.avatarUrl != null) this[Keys.AVATAR_URL] = user.avatarUrl else remove(Keys.AVATAR_URL)
    }

    override suspend fun saveGuest() {
        dataStore.edit {
            it.clear()
            it[Keys.GUEST] = true
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private object Keys {
        val TOKEN = stringPreferencesKey("token")
        val USER_ID = intPreferencesKey("user_id")
        val EMAIL = stringPreferencesKey("email")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val IS_INSTRUCTOR = booleanPreferencesKey("is_instructor")
        val AVATAR_URL = stringPreferencesKey("avatar_url")
        val GUEST = booleanPreferencesKey("guest")
    }
}

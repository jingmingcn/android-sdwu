package edu.sdwu.teaching.domain.repository

import edu.sdwu.teaching.domain.model.SessionState
import edu.sdwu.teaching.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val session: Flow<SessionState>

    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(email: String, password: String, displayName: String): Result<User>
    suspend fun continueAsGuest()

    /** Back to the login screen. Local data of a signed-in account is cleared; guest data is kept. */
    suspend fun signOut()

    /** PATCH /auth/me. Pass avatarUrl = null to remove the picture. */
    suspend fun updateProfile(displayName: String, avatarUrl: String?): Result<User>

    /** Called when the backend rejects the token (HTTP 401). */
    suspend fun onTokenExpired()
}

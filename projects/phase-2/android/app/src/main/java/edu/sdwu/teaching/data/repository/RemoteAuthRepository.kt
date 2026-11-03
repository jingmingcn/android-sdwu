package edu.sdwu.teaching.data.repository

import edu.sdwu.teaching.data.prefs.SessionStore
import edu.sdwu.teaching.data.remote.TeachingApi
import edu.sdwu.teaching.data.remote.dto.LoginRequestDto
import edu.sdwu.teaching.data.remote.dto.RegisterRequestDto
import edu.sdwu.teaching.data.remote.dto.TokenResponseDto
import edu.sdwu.teaching.data.remote.dto.toDomain
import edu.sdwu.teaching.domain.model.SessionState
import edu.sdwu.teaching.domain.model.User
import edu.sdwu.teaching.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class RemoteAuthRepository(
    private val api: TeachingApi,
    private val sessionStore: SessionStore,
    /** Clears per-user data (progress) when an account signs out. */
    private val clearUserData: suspend () -> Unit,
) : AuthRepository {

    override val session: Flow<SessionState> = sessionStore.session

    override suspend fun login(email: String, password: String): Result<User> =
        authenticate { api.login(LoginRequestDto(email.trim(), password)) }

    override suspend fun register(email: String, password: String, displayName: String): Result<User> =
        authenticate { api.register(RegisterRequestDto(email.trim(), password, displayName.trim())) }

    private suspend fun authenticate(call: suspend () -> TokenResponseDto): Result<User> = try {
        val response = call()
        val user = response.user.toDomain()
        sessionStore.saveSignedIn(user, response.accessToken)
        Result.success(user)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun continueAsGuest() = sessionStore.saveGuest()

    override suspend fun signOut() {
        // Guest progress is kept so it can be uploaded once the guest creates an account.
        if (sessionStore.session.first() is SessionState.SignedIn) clearUserData()
        sessionStore.clear()
    }

    override suspend fun onTokenExpired() = signOut()
}

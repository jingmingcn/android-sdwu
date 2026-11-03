package edu.sdwu.teaching.data.repository

import edu.sdwu.teaching.data.local.ProgressDao
import edu.sdwu.teaching.data.local.StudyDao
import edu.sdwu.teaching.data.prefs.SessionDataSource
import edu.sdwu.teaching.data.remote.TeachingApi
import edu.sdwu.teaching.data.remote.dto.LoginRequestDto
import edu.sdwu.teaching.data.remote.dto.RegisterRequestDto
import edu.sdwu.teaching.data.remote.dto.TokenResponseDto
import edu.sdwu.teaching.data.remote.dto.toDomain
import edu.sdwu.teaching.data.remote.updateProfileBody
import edu.sdwu.teaching.domain.model.SessionState
import edu.sdwu.teaching.domain.model.User
import edu.sdwu.teaching.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteAuthRepository @Inject constructor(
    private val api: TeachingApi,
    private val sessionStore: SessionDataSource,
    private val progressDao: ProgressDao,
    private val studyDao: StudyDao,
) : AuthRepository {

    override val session: Flow<SessionState> = sessionStore.session

    override suspend fun login(email: String, password: String): Result<User> =
        authenticate { api.login(LoginRequestDto(email.trim(), password)) }

    override suspend fun register(email: String, password: String, displayName: String): Result<User> =
        authenticate { api.register(RegisterRequestDto(email.trim(), password, displayName.trim())) }

    private suspend fun authenticate(call: suspend () -> TokenResponseDto): Result<User> = runCatchingApi {
        val response = call()
        val user = response.user.toDomain()
        sessionStore.saveSignedIn(user, response.accessToken)
        user
    }

    override suspend fun updateProfile(displayName: String, avatarUrl: String?): Result<User> = runCatchingApi {
        val user = api.updateMe(updateProfileBody(displayName.trim(), avatarUrl?.trim()?.ifEmpty { null })).toDomain()
        sessionStore.updateUser(user)
        user
    }

    override suspend fun continueAsGuest() = sessionStore.saveGuest()

    override suspend fun signOut() {
        // An account's personal data is removed so the next person on this phone starts clean.
        // Guest data is kept so it can be uploaded once the guest creates an account.
        if (sessionStore.session.first() is SessionState.SignedIn) {
            progressDao.deleteAll()
            studyDao.deleteAllNotes()
            studyDao.deleteAllBookmarks()
        }
        sessionStore.clear()
    }

    override suspend fun onTokenExpired() = signOut()
}

/** Like runCatching, but never swallows coroutine cancellation. */
internal inline fun <T> runCatchingApi(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}

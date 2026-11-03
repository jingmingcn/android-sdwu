package edu.sdwu.teaching.data.remote

import edu.sdwu.teaching.data.remote.dto.LessonDto
import edu.sdwu.teaching.data.remote.dto.LoginRequestDto
import edu.sdwu.teaching.data.remote.dto.ProgressDto
import edu.sdwu.teaching.data.remote.dto.QuizQuestionDto
import edu.sdwu.teaching.data.remote.dto.RegisterRequestDto
import edu.sdwu.teaching.data.remote.dto.TokenResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit description of the backend. Paths are relative: the server address is
 * added by [ServerUrlInterceptor], and the bearer token by [AuthInterceptor].
 */
interface TeachingApi {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): TokenResponseDto

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequestDto): TokenResponseDto

    @GET("lessons")
    suspend fun lessons(): List<LessonDto>

    @GET("quizzes")
    suspend fun quizzes(): List<QuizQuestionDto>

    /** Pushes local changes; the response is the merged server state for this user. */
    @POST("progress/sync")
    suspend fun syncProgress(@Body items: List<ProgressDto>): List<ProgressDto>
}

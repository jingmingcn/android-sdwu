package edu.sdwu.teaching.data.remote

import edu.sdwu.teaching.data.remote.dto.CatalogModuleDto
import edu.sdwu.teaching.data.remote.dto.LessonDto
import edu.sdwu.teaching.data.remote.dto.PublishModuleDto
import edu.sdwu.teaching.data.remote.dto.LoginRequestDto
import edu.sdwu.teaching.data.remote.dto.ProgressDto
import edu.sdwu.teaching.data.remote.dto.QuizQuestionDto
import edu.sdwu.teaching.data.remote.dto.RegisterRequestDto
import edu.sdwu.teaching.data.remote.dto.TokenResponseDto
import edu.sdwu.teaching.data.remote.dto.UserDto
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Retrofit description of the backend. Paths are relative: the server address is
 * added by [ServerUrlInterceptor], and the bearer token by [AuthInterceptor].
 */
interface TeachingApi {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): TokenResponseDto

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequestDto): TokenResponseDto

    /** Body built with [updateProfileBody] so that `"avatar_url": null` is really sent. */
    @PATCH("auth/me")
    suspend fun updateMe(@Body body: JsonObject): UserDto

    @GET("lessons")
    suspend fun lessons(): List<LessonDto>

    @GET("quizzes")
    suspend fun quizzes(): List<QuizQuestionDto>

    @GET("modules")
    suspend fun moduleCatalog(): List<CatalogModuleDto>

    /** Create or update a module's metadata in the class catalog. */
    @PUT("modules/{id}")
    suspend fun publishModule(@Path("id") id: String, @Body body: PublishModuleDto): CatalogModuleDto

    /** Pushes local changes; the response is the merged server state for this user. */
    @POST("progress/sync")
    suspend fun syncProgress(@Body items: List<ProgressDto>): List<ProgressDto>
}

/**
 * NetworkJson drops null fields (explicitNulls = false), but PATCH needs an explicit null
 * to mean "remove my avatar". Building the JSON by hand makes that intent visible.
 */
fun updateProfileBody(displayName: String, avatarUrl: String?): JsonObject = buildJsonObject {
    put("display_name", displayName)
    put("avatar_url", avatarUrl)
}

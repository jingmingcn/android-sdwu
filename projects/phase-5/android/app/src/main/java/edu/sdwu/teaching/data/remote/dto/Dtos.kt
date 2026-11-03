package edu.sdwu.teaching.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Data Transfer Objects: the exact JSON shape of the FastAPI backend (snake_case).
// They are mapped to domain models in DtoMappers.kt and never reach the UI.

@Serializable
data class LessonDto(
    val id: String,
    val order: Int,
    val title: String,
    val summary: String,
    val content: String,
    val category: String,
    val difficulty: String,
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("video_url") val videoUrl: String? = null,
)

@Serializable
data class QuizQuestionDto(
    val id: String,
    @SerialName("lesson_id") val lessonId: String,
    val order: Int,
    val question: String,
    val options: List<String>,
    @SerialName("correct_index") val correctIndex: Int,
    val explanation: String = "",
)

@Serializable
data class LoginRequestDto(val email: String, val password: String)

@Serializable
data class RegisterRequestDto(
    val email: String,
    val password: String,
    @SerialName("display_name") val displayName: String,
)

@Serializable
data class UserDto(
    val id: Int,
    val email: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("is_instructor") val isInstructor: Boolean,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
data class TokenResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    val user: UserDto,
)

@Serializable
data class ProgressDto(
    @SerialName("lesson_id") val lessonId: String,
    val status: String,
    @SerialName("quiz_best_score") val quizBestScore: Int? = null,
    @SerialName("quiz_total") val quizTotal: Int? = null,
    @SerialName("updated_at") val updatedAt: Long,
)

/** FastAPI error body: {"detail": "..."} (validation errors use a list instead of a string). */
@Serializable
data class ErrorDto(val detail: String? = null)

// ----- Student module catalog (/modules) -----

@Serializable
data class ContributionDto(val type: String, val id: String, val title: String)

/** PUT /modules/{id} body: metadata only, the module's code stays in the app. */
@Serializable
data class PublishModuleDto(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    val version: String,
    val category: String,
    val contributions: List<ContributionDto>,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("framework_version") val frameworkVersion: Int = 1,
)

@Serializable
data class CatalogModuleDto(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    val version: String,
    val category: String,
    val contributions: List<ContributionDto>,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("owner_name") val ownerName: String,
    @SerialName("updated_at") val updatedAt: String,
)

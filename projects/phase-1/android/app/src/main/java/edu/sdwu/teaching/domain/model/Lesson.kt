package edu.sdwu.teaching.domain.model

/**
 * Domain model for a lesson. UI and ViewModels only ever see this type,
 * never the Room entity or (from Phase 2) the network DTO.
 */
data class Lesson(
    val id: String,
    val order: Int,
    val title: String,
    val summary: String,
    val content: String,
    val category: String,
    val difficulty: Difficulty,
    val durationMinutes: Int,
    val videoUrl: String?,
)

enum class Difficulty(val label: String) {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced"),
}

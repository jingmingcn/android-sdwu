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
    /** A full http(s) URL, or a path like "/media/clip.mp4" served by the course backend. */
    val videoUrl: String?,
)

/**
 * Turns a lesson's video path into a playable URL. Relative paths are resolved against the
 * backend address from Profile -> Server, so videos hosted by the class server work on a LAN.
 */
fun resolveMediaUrl(videoUrl: String?, serverUrl: String): String? = when {
    videoUrl.isNullOrBlank() -> null
    videoUrl.startsWith("http://") || videoUrl.startsWith("https://") -> videoUrl
    else -> serverUrl.trimEnd('/') + "/" + videoUrl.trimStart('/')
}

enum class Difficulty(val label: String) {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced"),
}

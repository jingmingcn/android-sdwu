package edu.sdwu.teaching.domain.model

/** A student's private note on a lesson. Stored only on the device in Phase 3. */
data class Note(
    val id: Long,
    val lessonId: String,
    val text: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/** A note together with the title of its lesson, for the "My notes" screen. */
data class NoteWithLesson(val note: Note, val lessonTitle: String)

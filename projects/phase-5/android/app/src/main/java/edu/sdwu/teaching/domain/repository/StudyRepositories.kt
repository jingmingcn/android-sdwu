package edu.sdwu.teaching.domain.repository

import edu.sdwu.teaching.domain.model.Note
import edu.sdwu.teaching.domain.model.NoteWithLesson
import kotlinx.coroutines.flow.Flow

/** Personal notes per lesson (local only). */
interface NoteRepository {
    fun observeNotes(lessonId: String): Flow<List<Note>>
    fun observeAllNotes(): Flow<List<NoteWithLesson>>

    /** Creates a note, or updates it when [id] is not null. Blank text is ignored. */
    suspend fun saveNote(lessonId: String, text: String, id: Long? = null)
    suspend fun deleteNote(id: Long)
}

/** Bookmarked lessons (local only). */
interface BookmarkRepository {
    fun observeBookmarkedIds(): Flow<Set<String>>
    suspend fun toggle(lessonId: String)
}

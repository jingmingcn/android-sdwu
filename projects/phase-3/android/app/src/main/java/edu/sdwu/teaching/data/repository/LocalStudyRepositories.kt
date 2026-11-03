package edu.sdwu.teaching.data.repository

import edu.sdwu.teaching.data.local.BookmarkEntity
import edu.sdwu.teaching.data.local.NoteEntity
import edu.sdwu.teaching.data.local.StudyDao
import edu.sdwu.teaching.data.local.toDomain
import edu.sdwu.teaching.domain.model.Note
import edu.sdwu.teaching.domain.model.NoteWithLesson
import edu.sdwu.teaching.domain.repository.BookmarkRepository
import edu.sdwu.teaching.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalNoteRepository @Inject constructor(
    private val dao: StudyDao,
    private val clock: Clock,
) : NoteRepository {

    override fun observeNotes(lessonId: String): Flow<List<Note>> =
        dao.observeNotes(lessonId).map { rows -> rows.map { it.toDomain() } }

    override fun observeAllNotes(): Flow<List<NoteWithLesson>> =
        dao.observeAllNotes().map { rows ->
            rows.map { NoteWithLesson(it.note.toDomain(), it.lessonTitle ?: "Removed lesson") }
        }

    override suspend fun saveNote(lessonId: String, text: String, id: Long?) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val now = clock.now()
        val existing = id?.let { dao.getNote(it) }
        dao.upsertNote(
            existing?.copy(text = trimmed, updatedAt = now)
                ?: NoteEntity(lessonId = lessonId, text = trimmed, createdAt = now, updatedAt = now),
        )
    }

    override suspend fun deleteNote(id: Long) = dao.deleteNote(id)
}

@Singleton
class LocalBookmarkRepository @Inject constructor(
    private val dao: StudyDao,
    private val clock: Clock,
) : BookmarkRepository {

    override fun observeBookmarkedIds(): Flow<Set<String>> = dao.observeBookmarkIds().map { it.toSet() }

    override suspend fun toggle(lessonId: String) {
        if (dao.isBookmarked(lessonId)) dao.deleteBookmark(lessonId) else dao.insertBookmark(BookmarkEntity(lessonId, clock.now()))
    }
}

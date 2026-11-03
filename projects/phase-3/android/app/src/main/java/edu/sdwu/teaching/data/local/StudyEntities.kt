package edu.sdwu.teaching.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import edu.sdwu.teaching.domain.model.Note
import kotlinx.coroutines.flow.Flow

// Notes and bookmarks belong to the student, not to the catalogue, so they live in their
// own tables without foreign keys: a lesson refresh never deletes a student's notes.

@Entity(tableName = "notes", indices = [Index("lesson_id")])
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "lesson_id") val lessonId: String,
    val text: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

fun NoteEntity.toDomain() = Note(id, lessonId, text, createdAt, updatedAt)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey @ColumnInfo(name = "lesson_id") val lessonId: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

/** Result row of the notes + lessons JOIN. */
data class NoteWithLessonRow(
    @Embedded val note: NoteEntity,
    @ColumnInfo(name = "lesson_title") val lessonTitle: String?,
)

@Dao
interface StudyDao {
    @Query("SELECT * FROM notes WHERE lesson_id = :lessonId ORDER BY updated_at DESC")
    fun observeNotes(lessonId: String): Flow<List<NoteEntity>>

    // LEFT JOIN: a note whose lesson was removed from the catalogue is still shown.
    @Query(
        """
        SELECT notes.*, lessons.title AS lesson_title FROM notes
        LEFT JOIN lessons ON lessons.id = notes.lesson_id
        ORDER BY notes.updated_at DESC
        """,
    )
    fun observeAllNotes(): Flow<List<NoteWithLessonRow>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNote(id: Long): NoteEntity?

    @Upsert
    suspend fun upsertNote(note: NoteEntity): Long

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    @Query("SELECT lesson_id FROM bookmarks")
    fun observeBookmarkIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE lesson_id = :lessonId)")
    suspend fun isBookmarked(lessonId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE lesson_id = :lessonId")
    suspend fun deleteBookmark(lessonId: String)

    /** Used on sign-out of an account: personal data must not leak to the next user. */
    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAllBookmarks()
}

package edu.sdwu.teaching.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress")
    fun observeAll(): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress WHERE lesson_id = :lessonId")
    fun observe(lessonId: String): Flow<ProgressEntity?>

    @Query("SELECT * FROM progress WHERE lesson_id = :lessonId")
    suspend fun get(lessonId: String): ProgressEntity?

    @Query("SELECT * FROM progress WHERE pending_sync = 1")
    suspend fun pending(): List<ProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: ProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<ProgressEntity>)

    @Query("DELETE FROM progress")
    suspend fun deleteAll()
}

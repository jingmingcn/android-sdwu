package edu.sdwu.teaching.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** What the module registry remembers about each module (see framework/ModuleRegistry.kt). */
@Entity(tableName = "modules", primaryKeys = ["id"])
data class ModuleStateEntity(
    val id: String,
    val name: String,
    val version: String,
    /** "BUILT_IN" or "STUDENT". */
    val source: String,
    val enabled: Boolean,
    @ColumnInfo(name = "first_seen_at") val firstSeenAt: Long,
    @ColumnInfo(name = "last_seen_at") val lastSeenAt: Long,
)

/** Private key-value storage of each module (FunctionHost.storage). */
@Entity(tableName = "module_data", primaryKeys = ["module_id", "key"])
data class ModuleDataEntity(
    @ColumnInfo(name = "module_id") val moduleId: String,
    val key: String,
    val value: String,
)

@Dao
interface ModuleDao {
    @Query("SELECT * FROM modules")
    suspend fun loadStates(): List<ModuleStateEntity>

    @Upsert
    suspend fun saveState(state: ModuleStateEntity)

    @Query("SELECT value FROM module_data WHERE module_id = :moduleId AND `key` = :key")
    fun observeValue(moduleId: String, key: String): Flow<String?>

    @Query("SELECT value FROM module_data WHERE module_id = :moduleId AND `key` = :key")
    suspend fun getValue(moduleId: String, key: String): String?

    @Upsert
    suspend fun putValue(row: ModuleDataEntity)

    @Query("DELETE FROM module_data WHERE module_id = :moduleId AND `key` = :key")
    suspend fun deleteValue(moduleId: String, key: String)
}

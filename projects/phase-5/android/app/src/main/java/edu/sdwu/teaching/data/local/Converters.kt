package edu.sdwu.teaching.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

/** Room can only store primitive columns; lists are saved as JSON text. */
class Converters {
    @TypeConverter
    fun stringListToJson(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun jsonToStringList(value: String): List<String> = Json.decodeFromString(value)
}

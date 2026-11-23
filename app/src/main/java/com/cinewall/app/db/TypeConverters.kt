package com.cinewall.app.db

import androidx.room.TypeConverter
import com.cinewall.app.model.MediaType
import java.util.Date

class DateConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
}

class MediaTypeConverter {
    @TypeConverter
    fun fromMediaType(value: MediaType?): String? = value?.name

    @TypeConverter
    fun toMediaType(value: String?): MediaType =
        value?.let { runCatching { MediaType.valueOf(it) }.getOrNull() } ?: MediaType.MOVIE
}

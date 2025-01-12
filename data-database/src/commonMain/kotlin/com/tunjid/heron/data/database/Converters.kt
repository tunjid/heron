package com.tunjid.heron.data.database

import androidx.room.TypeConverter
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import kotlinx.datetime.Instant

internal class DateConverters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? =
        value?.let(Instant.Companion::fromEpochMilliseconds)

    @TypeConverter
    fun dateToTimestamp(instant: Instant?): Long? =
        instant?.toEpochMilliseconds()
}

internal class UriConverters {

    @TypeConverter
    fun fromString(value: String?): Uri? =
        value?.let(::Uri)

    @TypeConverter
    fun toUriString(uri: Uri?): String? =
        uri?.uri

}

internal class IdConverters {
    @TypeConverter
    fun fromString(value: String?): Id? =
        value?.let(::Id)

    @TypeConverter
    fun toUriString(id: Id?): String? =
        id?.id
}

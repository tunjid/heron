package com.tunjid.heron.data.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.daos.EmbedDao
import com.tunjid.heron.data.database.daos.TimelineDao
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.entities.TimelineFetchKeyEntity
import com.tunjid.heron.data.database.entities.PostAuthorsEntity
import com.tunjid.heron.data.database.entities.PostEntity
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.TimelineItemEntity
import com.tunjid.heron.data.database.entities.postembeds.ExternalEmbedEntity
import com.tunjid.heron.data.database.entities.postembeds.ImageEntity
import com.tunjid.heron.data.database.entities.postembeds.PostExternalEmbedEntity
import com.tunjid.heron.data.database.entities.postembeds.PostImageEntity
import com.tunjid.heron.data.database.entities.postembeds.PostPostEntity
import com.tunjid.heron.data.database.entities.postembeds.PostVideoEntity
import com.tunjid.heron.data.database.entities.postembeds.VideoEntity
import com.tunjid.heron.data.database.entities.profile.ProfileBlocksEntity
import com.tunjid.heron.data.database.entities.profile.ProfileFollowsEntity
import com.tunjid.heron.data.database.entities.profile.ProfilePostStatisticsEntity
import kotlinx.datetime.Instant

@Database(
    version = 1,
    entities = [
        ExternalEmbedEntity::class,
        ImageEntity::class,
        VideoEntity::class,
        PostExternalEmbedEntity::class,
        PostImageEntity::class,
        PostVideoEntity::class,
        PostPostEntity::class,
        PostEntity::class,
        PostAuthorsEntity::class,
        ProfilePostStatisticsEntity::class,
        ProfileFollowsEntity::class,
        ProfileBlocksEntity::class,
        ProfileEntity::class,
        TimelineItemEntity::class,
        TimelineFetchKeyEntity::class,
    ],
)
@TypeConverters(
    DateConverters::class,
    UriConverters::class,
    IdConverters::class,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun postDao(): PostDao
    abstract fun embedDao(): EmbedDao
    abstract fun feedDao(): TimelineDao
}

// The Room compiler generates the `actual` implementations.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

fun interface TransactionWriter {
    suspend fun inTransaction(block: suspend () -> Unit)
}

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
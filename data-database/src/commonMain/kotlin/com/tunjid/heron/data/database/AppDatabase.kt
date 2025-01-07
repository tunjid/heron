package com.tunjid.heron.data.database

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.tunjid.heron.data.core.models.Constants
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.daos.EmbedDao
import com.tunjid.heron.data.database.daos.NotificationsDao
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.daos.TimelineDao
import com.tunjid.heron.data.database.entities.FeedGeneratorEntity
import com.tunjid.heron.data.database.entities.ListEntity
import com.tunjid.heron.data.database.entities.NotificationEntity
import com.tunjid.heron.data.database.entities.PostAuthorsEntity
import com.tunjid.heron.data.database.entities.PostEntity
import com.tunjid.heron.data.database.entities.PostThreadEntity
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.TimelineFetchKeyEntity
import com.tunjid.heron.data.database.entities.TimelineItemEntity
import com.tunjid.heron.data.database.entities.postembeds.ExternalEmbedEntity
import com.tunjid.heron.data.database.entities.postembeds.ImageEntity
import com.tunjid.heron.data.database.entities.postembeds.PostExternalEmbedEntity
import com.tunjid.heron.data.database.entities.postembeds.PostImageEntity
import com.tunjid.heron.data.database.entities.postembeds.PostPostEntity
import com.tunjid.heron.data.database.entities.postembeds.PostVideoEntity
import com.tunjid.heron.data.database.entities.postembeds.VideoEntity
import com.tunjid.heron.data.database.entities.profile.PostViewerStatisticsEntity
import com.tunjid.heron.data.database.entities.profile.ProfileProfileRelationshipsEntity
import kotlinx.datetime.Instant

@Database(
    version = 6,
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
        PostThreadEntity::class,
        PostViewerStatisticsEntity::class,
        ProfileProfileRelationshipsEntity::class,
        ProfileEntity::class,
        ListEntity::class,
        FeedGeneratorEntity::class,
        NotificationEntity::class,
        TimelineItemEntity::class,
        TimelineFetchKeyEntity::class,
    ],
    autoMigrations = [
        // firstMigration
        AutoMigration(from = 1, to = 2),
        // listsAndFeedGeneratorsMigration
        AutoMigration(from = 2, to = 3),
        // notificationMigration
        AutoMigration(from = 3, to = 4),
        // serializedPostRecordMigration
        AutoMigration(from = 4, to = 5),
    ],
    exportSchema = true,
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
    abstract fun notificationsDao(): NotificationsDao
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

internal class NotificationReasonConverters {
    @TypeConverter
    fun fromOrdinal(ordinal: Int?): Notification.Reason =
        ordinal?.let(Notification.Reason.entries::getOrNull)
            ?: Notification.Reason.Unknown

    @TypeConverter
    fun toOrdinal(reason: Notification.Reason): Int =
        reason.ordinal
}

object NonNullPostUriAndAuthorMigration : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        // Add Unknown user to the db
        connection.execSQL(
            """
           INSERT INTO profiles (did, handle, displayName, description, avatar, banner, followersCount, followsCount, postsCount, joinedViaStarterPack, indexedAt, createdAt)
           VALUES ('${Constants.UNKNOWN}', '${Constants.UNKNOWN}', '', '', NULL, NULL, 0, 0, 0, NULL, 0, 0);
            """.trimIndent()
        )

        connection.execSQL(
            """
                CREATE TABLE IF NOT EXISTS posts_new (
                cid TEXT NOT NULL,
                uri TEXT NOT NULL,
                authorId TEXT NOT NULL, 
                replyCount INTEGER, 
                repostCount INTEGER, 
                likeCount INTEGER, 
                quoteCount INTEGER, 
                indexedAt INTEGER NOT NULL, 
                text TEXT, 
                base64EncodedRecord TEXT, 
                createdAt INTEGER, 
                PRIMARY KEY(cid)
              )
            """.trimIndent()
        )

        connection.execSQL(
            """
                INSERT INTO posts_new (
                cid,
                uri,
                authorId,
                replyCount,
                repostCount,
                likeCount,
                quoteCount,
                indexedAt,
                text,
                base64EncodedRecord, 
                createdAt
                )
                SELECT
                cid,
                COALESCE(uri, '${Constants.UNKNOWN}'),
                COALESCE(authorId, '${Constants.UNKNOWN}'),
                replyCount,
                repostCount,
                likeCount,
                quoteCount,
                indexedAt,
                text,
                base64EncodedRecord, 
                createdAt
                FROM posts
            """.trimIndent()
        )

        // Remove the old table
        connection.execSQL("DROP TABLE posts")

        // Change the table name to the correct one
        connection.execSQL("ALTER TABLE posts_new RENAME TO posts")
    }
}
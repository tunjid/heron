package com.tunjid.heron.data.database

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
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
import com.tunjid.heron.data.database.entities.PostLikeEntity
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    version = 7,
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
        PostLikeEntity::class,
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
        AutoMigration(
            from = 6,
            to = 7,
            spec = PostViewerStatisticsAutoMigration::class
        )
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

fun RoomDatabase.Builder<AppDatabase>.configureAndBuild() =
    fallbackToDestructiveMigrationOnDowngrade(
        dropAllTables = true,
    )
        .addMigrations(
            NonNullPostUriAndAuthorMigration,
        )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()

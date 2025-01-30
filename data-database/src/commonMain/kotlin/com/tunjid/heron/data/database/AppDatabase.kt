/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

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
import com.tunjid.heron.data.database.entities.PostRepostEntity
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
import com.tunjid.heron.data.database.entities.profile.ProfileViewerStateEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    version = 9,
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
        ProfileViewerStateEntity::class,
        ProfileEntity::class,
        PostLikeEntity::class,
        PostRepostEntity::class,
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
        ),
        // postLikes and postReposts
        AutoMigration(from = 7, to = 8),
        // profile profile relationships, follows, mutes, blocks, etc
        AutoMigration(
            from = 8,
            to = 9,
            spec = ProfileViewersAutoMigration::class,
        ),
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

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

package com.tunjid.heron.data.di

import androidx.room.RoomDatabase
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.tunjid.heron.data.database.AppDatabase
import com.tunjid.heron.data.database.TransactionWriter
import com.tunjid.heron.data.database.configureAndBuild
import com.tunjid.heron.data.database.daos.EmbedDao
import com.tunjid.heron.data.database.daos.FeedGeneratorDao
import com.tunjid.heron.data.database.daos.ListDao
import com.tunjid.heron.data.database.daos.NotificationsDao
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.daos.StarterPackDao
import com.tunjid.heron.data.database.daos.TimelineDao
import com.tunjid.heron.data.network.KtorNetworkService
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.AuthTokenRepository
import com.tunjid.heron.data.repository.DataStoreSavedStateRepository
import com.tunjid.heron.data.repository.NotificationsRepository
import com.tunjid.heron.data.repository.OfflineNotificationsRepository
import com.tunjid.heron.data.repository.OfflinePostRepository
import com.tunjid.heron.data.repository.OfflineProfileRepository
import com.tunjid.heron.data.repository.OfflineSearchRepository
import com.tunjid.heron.data.repository.OfflineTimelineRepository
import com.tunjid.heron.data.repository.PostRepository
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.SavedStateRepository
import com.tunjid.heron.data.repository.SearchRepository
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.utilities.writequeue.SnapshotWriteQueue
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import okio.FileSystem
import okio.Path
import sh.christian.ozone.BlueskyJson

class DataModule(
    val appScope: CoroutineScope,
    val savedStatePath: Path,
    val savedStateFileSystem: FileSystem,
    val databaseBuilder: RoomDatabase.Builder<AppDatabase>,
)

@BindingContainer
class DataComponent(
    private val module: DataModule
) {

    @Named("AppScope")
    @Provides
    fun provideAppScope(): CoroutineScope = module.appScope

    @Provides
    fun provideSavedStatePath(): Path = module.savedStatePath

    @Provides
    fun provideSavedStateFileSystem(): FileSystem = module.savedStateFileSystem

    @Provides
    fun provideRoomDatabase(): AppDatabase = module.databaseBuilder.configureAndBuild()

    @Provides
    fun providePostDao(
        database: AppDatabase,
    ): PostDao = database.postDao()

    @Provides
    fun provideProfileDao(
        database: AppDatabase,
    ): ProfileDao = database.profileDao()

    @Provides
    fun provideListDao(
        database: AppDatabase,
    ): ListDao = database.listDao()

    @Provides
    fun provideEmbedDao(
        database: AppDatabase,
    ): EmbedDao = database.embedDao()

    @Provides
    fun provideTimelineDao(
        database: AppDatabase,
    ): TimelineDao = database.timelineDao()

    @Provides
    fun provideFeedGeneratorDao(
        database: AppDatabase,
    ): FeedGeneratorDao = database.feedGeneratorDao()

    @Provides
    fun provideNotificationsDao(
        database: AppDatabase,
    ): NotificationsDao = database.notificationsDao()

    @Provides
    fun provideStarterPackDao(
        database: AppDatabase,
    ): StarterPackDao = database.starterPackDao()

    @Provides
    fun provideTransactionWriter(
        database: AppDatabase,
    ): TransactionWriter = TransactionWriter { block ->
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                block()
            }
        }
    }

    @Provides
    fun provideAppJson(): Json = BlueskyJson

    @Provides
    fun provideAppProtoBuff(): ProtoBuf = ProtoBuf {
    }

    @Provides
    fun provideKtorNetworkService(
        ktorNetworkService: KtorNetworkService
    ): NetworkService = ktorNetworkService

    @Provides
    fun provideSnapshotWriteQueue(
        snapshotWriteQueue: SnapshotWriteQueue
    ): WriteQueue = snapshotWriteQueue

    @Provides
    private fun provideDataStoreSavedStateRepository(
        dataStoreSavedStateRepository: DataStoreSavedStateRepository
    ): SavedStateRepository = dataStoreSavedStateRepository

    @Provides
    fun provideAuthTokenRepository(
        authTokenRepository: AuthTokenRepository
    ): AuthRepository = authTokenRepository

    @Provides
    fun provideOfflineTimelineRepository(
        offlineTimelineRepository: OfflineTimelineRepository
    ): TimelineRepository = offlineTimelineRepository

    @Provides
    fun provideOfflineProfileRepository(
        offlineProfileRepository: OfflineProfileRepository
    ): ProfileRepository = offlineProfileRepository

    @Provides
    fun provideOfflineNotificationsRepository(
        offlineNotificationsRepository: OfflineNotificationsRepository
    ): NotificationsRepository = offlineNotificationsRepository

    @Provides
    fun provideOfflineSearchRepository(
        offlineSearchRepository: OfflineSearchRepository
    ): SearchRepository = offlineSearchRepository

    @Provides
    fun provideOfflinePostRepository(
        offlinePostRepository: OfflinePostRepository
    ): PostRepository = offlinePostRepository
}
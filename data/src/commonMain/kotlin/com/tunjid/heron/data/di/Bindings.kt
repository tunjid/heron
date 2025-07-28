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
import com.tunjid.heron.data.database.daos.MessageDao
import com.tunjid.heron.data.database.daos.NotificationsDao
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.daos.StarterPackDao
import com.tunjid.heron.data.database.daos.TimelineDao
import com.tunjid.heron.data.network.ConnectivityNetworkMonitor
import com.tunjid.heron.data.network.KtorNetworkService
import com.tunjid.heron.data.network.NetworkMonitor
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.AuthTokenRepository
import com.tunjid.heron.data.repository.DataStoreSavedStateRepository
import com.tunjid.heron.data.repository.MessageRepository
import com.tunjid.heron.data.repository.NotificationsRepository
import com.tunjid.heron.data.repository.OfflineMessageRepository
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
import dev.jordond.connectivity.Connectivity
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

class DataBindingArgs(
    val appScope: CoroutineScope,
    val connectivity: Connectivity,
    val savedStatePath: Path,
    val savedStateFileSystem: FileSystem,
    val databaseBuilder: RoomDatabase.Builder<AppDatabase>,
)

@BindingContainer
class DataBindings(
    private val args: DataBindingArgs
) {

    @Named("AppScope")
    @SingleIn(AppScope::class)
    @Provides
    fun provideAppScope(): CoroutineScope = args.appScope

    @SingleIn(AppScope::class)
    @Provides
    fun provideConnectivity(): Connectivity = args.connectivity

    @SingleIn(AppScope::class)
    @Provides
    fun provideSavedStatePath(): Path = args.savedStatePath

    @SingleIn(AppScope::class)
    @Provides
    fun provideSavedStateFileSystem(): FileSystem = args.savedStateFileSystem

    @SingleIn(AppScope::class)
    @Provides
    fun provideRoomDatabase(): AppDatabase = args.databaseBuilder.configureAndBuild()

    @SingleIn(AppScope::class)
    @Provides
    internal fun provideNetworkMonitor(
        connectivityNetworkMonitor: ConnectivityNetworkMonitor,
    ): NetworkMonitor = connectivityNetworkMonitor

    @SingleIn(AppScope::class)
    @Provides
    fun providePostDao(
        database: AppDatabase,
    ): PostDao = database.postDao()

    @SingleIn(AppScope::class)
    @Provides
    fun provideProfileDao(
        database: AppDatabase,
    ): ProfileDao = database.profileDao()

    @SingleIn(AppScope::class)
    @Provides
    fun provideListDao(
        database: AppDatabase,
    ): ListDao = database.listDao()

    @SingleIn(AppScope::class)
    @Provides
    fun provideEmbedDao(
        database: AppDatabase,
    ): EmbedDao = database.embedDao()

    @SingleIn(AppScope::class)
    @Provides
    fun provideTimelineDao(
        database: AppDatabase,
    ): TimelineDao = database.timelineDao()

    @SingleIn(AppScope::class)
    @Provides
    fun provideFeedGeneratorDao(
        database: AppDatabase,
    ): FeedGeneratorDao = database.feedGeneratorDao()

    @SingleIn(AppScope::class)
    @Provides
    fun provideNotificationsDao(
        database: AppDatabase,
    ): NotificationsDao = database.notificationsDao()

    @SingleIn(AppScope::class)
    @Provides
    fun provideStarterPackDao(
        database: AppDatabase,
    ): StarterPackDao = database.starterPackDao()

    @SingleIn(AppScope::class)
    @Provides
    fun provideMessageDao(
        database: AppDatabase,
    ): MessageDao = database.messagesDao()

    @SingleIn(AppScope::class)
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

    @SingleIn(AppScope::class)
    @Provides
    fun provideAppJson(): Json = BlueskyJson

    @SingleIn(AppScope::class)
    @Provides
    fun provideAppProtoBuff(): ProtoBuf = ProtoBuf {
    }

    @SingleIn(AppScope::class)
    @Provides
    fun provideKtorNetworkService(
        ktorNetworkService: KtorNetworkService
    ): NetworkService = ktorNetworkService

    @SingleIn(AppScope::class)
    @Provides
    internal fun provideSnapshotWriteQueue(
        snapshotWriteQueue: SnapshotWriteQueue
    ): WriteQueue = snapshotWriteQueue

    @SingleIn(AppScope::class)
    @Provides
    private fun provideDataStoreSavedStateRepository(
        dataStoreSavedStateRepository: DataStoreSavedStateRepository
    ): SavedStateRepository = dataStoreSavedStateRepository

    @SingleIn(AppScope::class)
    @Provides
    internal fun provideAuthTokenRepository(
        authTokenRepository: AuthTokenRepository
    ): AuthRepository = authTokenRepository

    @SingleIn(AppScope::class)
    @Provides
    internal fun provideOfflineTimelineRepository(
        offlineTimelineRepository: OfflineTimelineRepository
    ): TimelineRepository = offlineTimelineRepository

    @SingleIn(AppScope::class)
    @Provides
    internal fun provideOfflineProfileRepository(
        offlineProfileRepository: OfflineProfileRepository
    ): ProfileRepository = offlineProfileRepository

    @SingleIn(AppScope::class)
    @Provides
    internal fun provideOfflineNotificationsRepository(
        offlineNotificationsRepository: OfflineNotificationsRepository
    ): NotificationsRepository = offlineNotificationsRepository

    @SingleIn(AppScope::class)
    @Provides
    internal fun provideOfflineSearchRepository(
        offlineSearchRepository: OfflineSearchRepository
    ): SearchRepository = offlineSearchRepository

    @SingleIn(AppScope::class)
    @Provides
    internal fun provideOfflinePostRepository(
        offlinePostRepository: OfflinePostRepository
    ): PostRepository = offlinePostRepository

    @SingleIn(AppScope::class)
    @Provides
    internal fun provideOfflineMessageRepository(
        offlineMessageRepository: OfflineMessageRepository
    ): MessageRepository = offlineMessageRepository
}
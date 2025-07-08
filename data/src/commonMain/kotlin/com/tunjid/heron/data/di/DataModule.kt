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
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import okio.FileSystem
import okio.Path
import sh.christian.ozone.BlueskyJson

abstract class DataScope private constructor()

class DataModule(
    val appScope: CoroutineScope,
    val savedStatePath: Path,
    val savedStateFileSystem: FileSystem,
    val databaseBuilder: RoomDatabase.Builder<AppDatabase>,
)

@DependencyGraph(
    scope = DataScope::class,
    isExtendable = true,
)
interface DataComponent {

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides module: DataModule
        ): DataComponent
    }

    val module: DataModule

    @Named("AppScope")
    @SingleIn(DataScope::class)
    @Provides
    fun provideAppScope(): CoroutineScope = module.appScope

    @SingleIn(DataScope::class)
    @Provides
    fun provideSavedStatePath(): Path = module.savedStatePath

    @SingleIn(DataScope::class)
    @Provides
    fun provideSavedStateFileSystem(): FileSystem = module.savedStateFileSystem

    @SingleIn(DataScope::class)
    @Provides
    fun provideRoomDatabase(): AppDatabase = module.databaseBuilder.configureAndBuild()

    @SingleIn(DataScope::class)
    @Provides
    fun providePostDao(
        database: AppDatabase,
    ): PostDao = database.postDao()

    @SingleIn(DataScope::class)
    @Provides
    fun provideProfileDao(
        database: AppDatabase,
    ): ProfileDao = database.profileDao()

    @SingleIn(DataScope::class)
    @Provides
    fun provideListDao(
        database: AppDatabase,
    ): ListDao = database.listDao()

    @SingleIn(DataScope::class)
    @Provides
    fun provideEmbedDao(
        database: AppDatabase,
    ): EmbedDao = database.embedDao()

    @SingleIn(DataScope::class)
    @Provides
    fun provideTimelineDao(
        database: AppDatabase,
    ): TimelineDao = database.timelineDao()

    @SingleIn(DataScope::class)
    @Provides
    fun provideFeedGeneratorDao(
        database: AppDatabase,
    ): FeedGeneratorDao = database.feedGeneratorDao()

    @SingleIn(DataScope::class)
    @Provides
    fun provideNotificationsDao(
        database: AppDatabase,
    ): NotificationsDao = database.notificationsDao()

    @SingleIn(DataScope::class)
    @Provides
    fun provideStarterPackDao(
        database: AppDatabase,
    ): StarterPackDao = database.starterPackDao()

    @SingleIn(DataScope::class)
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

    @SingleIn(DataScope::class)
    @Provides
    fun provideAppJson(): Json = BlueskyJson

    @SingleIn(DataScope::class)
    @Provides
    fun provideAppProtoBuff(): ProtoBuf = ProtoBuf {
    }

    @SingleIn(DataScope::class)
    @Binds
    val KtorNetworkService.bind: NetworkService

    @SingleIn(DataScope::class)
    @Binds
    val SnapshotWriteQueue.bind: WriteQueue

    @SingleIn(DataScope::class)
    @Binds
    val DataStoreSavedStateRepository.bind: SavedStateRepository

    @SingleIn(DataScope::class)
    @Binds
    val AuthTokenRepository.bind: AuthRepository

    @SingleIn(DataScope::class)
    @Binds
    val OfflineTimelineRepository.bind: TimelineRepository

    @SingleIn(DataScope::class)
    @Binds
    val OfflineProfileRepository.bind: ProfileRepository

    @SingleIn(DataScope::class)
    @Binds
    val OfflineNotificationsRepository.bind: NotificationsRepository

    @SingleIn(DataScope::class)
    @Binds
    val OfflineSearchRepository.bind: SearchRepository

    @SingleIn(DataScope::class)
    @Binds
    val OfflinePostRepository.bind: PostRepository
}
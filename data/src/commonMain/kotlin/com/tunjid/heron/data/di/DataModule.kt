/*
 * Copyright 2024 Adetunji Dahunsi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.heron.data.di

import androidx.room.RoomDatabase
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.tunjid.heron.data.database.AppDatabase
import com.tunjid.heron.data.database.TransactionWriter
import com.tunjid.heron.data.network.KtorNetworkService
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.AuthTokenRepository
import com.tunjid.heron.data.repository.DataStoreSavedStateRepository
import com.tunjid.heron.data.repository.NotificationsRepository
import com.tunjid.heron.data.repository.OfflineNotificationsRepository
import com.tunjid.heron.data.repository.OfflineProfileRepository
import com.tunjid.heron.data.repository.OfflineTimelineRepository
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.SavedStateRepository
import com.tunjid.heron.data.repository.TimelineRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.serialization.protobuf.ProtoBuf
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope
import okio.FileSystem
import okio.Path
import sh.christian.ozone.BlueskyJson

@Scope
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class DataScope

class DataModule(
    val appScope: CoroutineScope,
    val savedStatePath: Path,
    val savedStateFileSystem: FileSystem,
    val databaseBuilder: RoomDatabase.Builder<AppDatabase>,
)

@DataScope
@Component
abstract class DataComponent(
    private val module: DataModule,
) {

    @DataScope
    @Provides
    fun provideAppScope(): CoroutineScope = module.appScope

    @DataScope
    @Provides
    fun provideSavedStatePath(): Path = module.savedStatePath

    @DataScope
    @Provides
    fun provideSavedStateFileSystem(): FileSystem = module.savedStateFileSystem

    @DataScope
    @Provides
    fun provideRoomDatabase(): AppDatabase = module.databaseBuilder
//        .addMigrations(MIGRATIONS)
        .fallbackToDestructiveMigrationOnDowngrade(true)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()

    @DataScope
    @Provides
    fun providePostDao(
        database: AppDatabase,
    ) = database.postDao()

    @DataScope
    @Provides
    fun provideProfileDao(
        database: AppDatabase,
    ) = database.profileDao()

    @DataScope
    @Provides
    fun provideEmbedDao(
        database: AppDatabase,
    ) = database.embedDao()

    @DataScope
    @Provides
    fun provideFeedDao(
        database: AppDatabase,
    ) = database.feedDao()

    @DataScope
    @Provides
    fun provideNotificationsDao(
        database: AppDatabase,
    ) = database.notificationsDao()

    @DataScope
    @Provides
    fun provideTransactionWriter(
        database: AppDatabase,
    ): TransactionWriter = TransactionWriter { block ->
        //  TODO: Rewrite this when https://issuetracker.google.com/issues/340606803 is fixed
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                block()
            }
        }
        database.invalidationTracker.refreshAsync()
    }

    @DataScope
    @Provides
    fun provideAppJson() = BlueskyJson

    @DataScope
    @Provides
    fun provideAppProtoBuff() = ProtoBuf {
    }

    val KtorNetworkService.bind: NetworkService
        @DataScope
        @Provides get() = this

    val DataStoreSavedStateRepository.bind: SavedStateRepository
        @DataScope
        @Provides get() = this

    val AuthTokenRepository.bind: AuthRepository
        @DataScope
        @Provides get() = this

    val OfflineTimelineRepository.bind: TimelineRepository
        @DataScope
        @Provides get() = this

    val OfflineProfileRepository.bind: ProfileRepository
        @DataScope
        @Provides get() = this

    val OfflineNotificationsRepository.bind: NotificationsRepository
        @DataScope
        @Provides get() = this
}
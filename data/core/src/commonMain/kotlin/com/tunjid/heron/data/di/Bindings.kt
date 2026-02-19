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
import com.tunjid.heron.data.database.daos.LabelDao
import com.tunjid.heron.data.database.daos.ListDao
import com.tunjid.heron.data.database.daos.MessageDao
import com.tunjid.heron.data.database.daos.NotificationsDao
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.daos.StarterPackDao
import com.tunjid.heron.data.database.daos.ThreadGateDao
import com.tunjid.heron.data.database.daos.TimelineDao
import com.tunjid.heron.data.files.FileManager
import com.tunjid.heron.data.files.createFileManager
import com.tunjid.heron.data.network.BlueskyJson
import com.tunjid.heron.data.network.ConnectivityNetworkMonitor
import com.tunjid.heron.data.network.FeedCreationService
import com.tunjid.heron.data.network.GrazeFeedCreationService
import com.tunjid.heron.data.network.KtorNetworkService
import com.tunjid.heron.data.network.NetworkConnectionException
import com.tunjid.heron.data.network.NetworkMonitor
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.network.PersistedSessionManager
import com.tunjid.heron.data.network.SessionManager
import com.tunjid.heron.data.network.SuspendingVideoUploadService
import com.tunjid.heron.data.network.VideoUploadService
import com.tunjid.heron.data.network.isNetworkConnectionError
import com.tunjid.heron.data.network.oauth.crypto.platformCryptographyProvider
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.AuthTokenRepository
import com.tunjid.heron.data.repository.DataStoreSavedStateDataSource
import com.tunjid.heron.data.repository.MessageRepository
import com.tunjid.heron.data.repository.NotificationsRepository
import com.tunjid.heron.data.repository.OfflineMessageRepository
import com.tunjid.heron.data.repository.OfflineNotificationsRepository
import com.tunjid.heron.data.repository.OfflinePostRepository
import com.tunjid.heron.data.repository.OfflineProfileRepository
import com.tunjid.heron.data.repository.OfflineRecordRepository
import com.tunjid.heron.data.repository.OfflineSearchRepository
import com.tunjid.heron.data.repository.OfflineTimelineRepository
import com.tunjid.heron.data.repository.OfflineUserDataRepository
import com.tunjid.heron.data.repository.PostRepository
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.RecordRepository
import com.tunjid.heron.data.repository.SavedStateDataSource
import com.tunjid.heron.data.repository.SearchRepository
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.data.utilities.TidGenerator
import com.tunjid.heron.data.utilities.preferenceupdater.NotificationPreferenceUpdater
import com.tunjid.heron.data.utilities.preferenceupdater.PreferenceUpdater
import com.tunjid.heron.data.utilities.preferenceupdater.ThingNotificationPreferenceUpdater
import com.tunjid.heron.data.utilities.preferenceupdater.ThingPreferenceUpdater
import com.tunjid.heron.data.utilities.profileLookup.OfflineProfileLookup
import com.tunjid.heron.data.utilities.profileLookup.ProfileLookup
import com.tunjid.heron.data.utilities.recordResolver.OfflineRecordResolver
import com.tunjid.heron.data.utilities.recordResolver.RecordResolver
import com.tunjid.heron.data.utilities.writequeue.PersistedWriteQueue
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import dev.jordond.connectivity.Connectivity
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.CryptographyProviderApi
import dev.whyoleg.cryptography.CryptographySystem
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpCallValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.serialization.protobuf.ProtoBuf
import okio.FileSystem
import okio.Path

@Qualifier @Retention(AnnotationRetention.RUNTIME) annotation class AppMainScope

@Qualifier @Retention(AnnotationRetention.RUNTIME) internal annotation class IODispatcher

@Qualifier @Retention(AnnotationRetention.RUNTIME) internal annotation class DefaultDispatcher

class DataBindingArgs(
    val appMainScope: CoroutineScope,
    val connectivity: Connectivity,
    val savedStatePath: Path,
    val savedStateFileSystem: FileSystem,
    val databaseBuilder: RoomDatabase.Builder<AppDatabase>,
)

@BindingContainer
class DataBindings(private val args: DataBindingArgs) {

    @AppMainScope
    @SingleIn(AppScope::class)
    @Provides
    fun provideAppScope(): CoroutineScope = args.appMainScope

    @IODispatcher
    @SingleIn(AppScope::class)
    @Provides
    internal fun provideIODispatcher(): CoroutineDispatcher = Dispatchers.IO

    @DefaultDispatcher
    @SingleIn(AppScope::class)
    @Provides
    internal fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @SingleIn(AppScope::class) @Provides fun provideConnectivity(): Connectivity = args.connectivity

    @SingleIn(AppScope::class) @Provides fun provideSavedStatePath(): Path = args.savedStatePath

    @SingleIn(AppScope::class)
    @Provides
    fun provideSavedStateFileSystem(): FileSystem = args.savedStateFileSystem

    @SingleIn(AppScope::class)
    @Provides
    fun provideRoomDatabase(): AppDatabase = args.databaseBuilder.configureAndBuild()

    @SingleIn(AppScope::class)
    @Provides
    internal fun provideTidGenerator(): TidGenerator = TidGenerator()

    @SingleIn(AppScope::class)
    @Provides
    internal fun provideFileManager(): FileManager = createFileManager()

    @SingleIn(AppScope::class)
    @Provides
    internal fun provideSessionManager(
        httpClient: HttpClient,
        savedStateDataSource: SavedStateDataSource,
    ): SessionManager =
        PersistedSessionManager(
            httpClient = httpClient,
            savedStateDataSource = savedStateDataSource,
        )

    @SingleIn(AppScope::class)
    @Provides
    internal fun providePreferenceUpdater(tidGenerator: TidGenerator): PreferenceUpdater =
        ThingPreferenceUpdater(tidGenerator = tidGenerator)

    @SingleIn(AppScope::class)
    @Provides
    internal fun provideNotificationPreferenceUpdater(
        thingNotificationPreferenceUpdater: ThingNotificationPreferenceUpdater
    ): NotificationPreferenceUpdater = thingNotificationPreferenceUpdater

    @SingleIn(AppScope::class)
    @Provides
    internal fun provideProfileLookup(offlineProfileLookup: OfflineProfileLookup): ProfileLookup =
        offlineProfileLookup

    @SingleIn(AppScope::class)
    @Provides
    internal fun provideRecordResolver(
        offlineRecordResolver: OfflineRecordResolver
    ): RecordResolver = offlineRecordResolver

    @SingleIn(AppScope::class)
    @Provides
    internal fun provideHttpClient(): HttpClient = HttpClient {
        expectSuccess = false
        install(ContentNegotiation) { json(BlueskyJson) }
        install(HttpTimeout) { requestTimeoutMillis = 4.seconds.inWholeMilliseconds }
        install(HttpCallValidator) {
            handleResponseExceptionWithRequest { throwable, request ->
                if (throwable.isNetworkConnectionError()) {
                    throw NetworkConnectionException(url = request.url, cause = throwable)
                }
            }
        }
    }

    @OptIn(CryptographyProviderApi::class)
    @SingleIn(AppScope::class)
    @Provides
    internal fun provideCryptographyProvider(): CryptographyProvider =
        platformCryptographyProvider().also { CryptographySystem.registerProvider(lazyOf(it), 0) }

    @SingleIn(AppScope::class)
    @Provides
    internal fun provideNetworkMonitor(
        connectivityNetworkMonitor: ConnectivityNetworkMonitor
    ): NetworkMonitor = connectivityNetworkMonitor

    @SingleIn(AppScope::class)
    @Provides
    internal fun provideVideoUploadService(
        videoUploadService: SuspendingVideoUploadService
    ): VideoUploadService = videoUploadService

    @SingleIn(AppScope::class)
    @Provides
    internal fun provideFeedCreationService(
        feedCreationService: GrazeFeedCreationService
    ): FeedCreationService = feedCreationService

    @SingleIn(AppScope::class)
    @Provides
    fun providePostDao(database: AppDatabase): PostDao = database.postDao()

    @SingleIn(AppScope::class)
    @Provides
    fun provideProfileDao(database: AppDatabase): ProfileDao = database.profileDao()

    @SingleIn(AppScope::class)
    @Provides
    fun provideLabelDao(database: AppDatabase): LabelDao = database.labelDao()

    @SingleIn(AppScope::class)
    @Provides
    fun provideListDao(database: AppDatabase): ListDao = database.listDao()

    @SingleIn(AppScope::class)
    @Provides
    fun provideEmbedDao(database: AppDatabase): EmbedDao = database.embedDao()

    @SingleIn(AppScope::class)
    @Provides
    fun provideTimelineDao(database: AppDatabase): TimelineDao = database.timelineDao()

    @SingleIn(AppScope::class)
    @Provides
    fun provideFeedGeneratorDao(database: AppDatabase): FeedGeneratorDao =
        database.feedGeneratorDao()

    @SingleIn(AppScope::class)
    @Provides
    fun provideNotificationsDao(database: AppDatabase): NotificationsDao =
        database.notificationsDao()

    @SingleIn(AppScope::class)
    @Provides
    fun provideStarterPackDao(database: AppDatabase): StarterPackDao = database.starterPackDao()

    @SingleIn(AppScope::class)
    @Provides
    fun provideMessageDao(database: AppDatabase): MessageDao = database.messagesDao()

    @SingleIn(AppScope::class)
    @Provides
    fun provideThreadGateDao(database: AppDatabase): ThreadGateDao = database.threadGateDao()

    @SingleIn(AppScope::class)
    @Provides
    fun provideTransactionWriter(database: AppDatabase): TransactionWriter =
        TransactionWriter { block ->
            database.useWriterConnection { transactor ->
                transactor.immediateTransaction { block() }
            }
        }

    @SingleIn(AppScope::class) @Provides fun provideAppProtoBuff(): ProtoBuf = ProtoBuf {}

    @SingleIn(AppScope::class)
    @Provides
    internal fun provideKtorNetworkService(ktorNetworkService: KtorNetworkService): NetworkService =
        ktorNetworkService

    @SingleIn(AppScope::class)
    @Provides
    internal fun provideWriteQueue(writeQueue: PersistedWriteQueue): WriteQueue = writeQueue

    @SingleIn(AppScope::class)
    @Provides
    private fun provideDataStoreSavedStateRepository(
        dataStoreSavedStateRepository: DataStoreSavedStateDataSource
    ): SavedStateDataSource = dataStoreSavedStateRepository

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

    @SingleIn(AppScope::class)
    @Provides
    internal fun provideOfflineUserDataRepository(
        offlineUserDataRepository: OfflineUserDataRepository
    ): UserDataRepository = offlineUserDataRepository

    @SingleIn(AppScope::class)
    @Provides
    internal fun provideRecordRepository(
        offlineRecordRepository: OfflineRecordRepository
    ): RecordRepository = offlineRecordRepository
}

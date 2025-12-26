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

package com.tunjid.heron.data.repository

import app.bsky.feed.GetPostsQueryParams
import app.bsky.feed.GetPostsResponse
import app.bsky.feed.PostView
import app.bsky.notification.GetUnreadCountQueryParams
import app.bsky.notification.ListNotificationsNotification
import app.bsky.notification.ListNotificationsQueryParams
import app.bsky.notification.ListNotificationsResponse
import app.bsky.notification.UpdateSeenRequest
import com.tunjid.heron.data.InternalEndpoints
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.offset
import com.tunjid.heron.data.core.models.value
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.database.daos.NotificationsDao
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.entities.PopulatedNotificationEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.network.NetworkMonitor
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.multipleEntitysaver.associatedPostUri
import com.tunjid.heron.data.utilities.nextCursorFlow
import com.tunjid.heron.data.utilities.runCatchingWithNetworkRetry
import com.tunjid.heron.data.utilities.toOutcome
import com.tunjid.heron.data.utilities.withRefresh
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.takeFrom
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import sh.christian.ozone.api.response.AtpResponse

@Serializable
data class NotificationsQuery(
    override val data: CursorQuery.Data,
) : CursorQuery {
    init {
        require(data.limit < 20) {
            "Notification query limit must be less than 20 items"
        }
    }
}

interface NotificationsRepository {
    val unreadCount: Flow<Long>

    val lastRefreshed: Flow<Instant?>

    val unreadNotifications: Flow<List<Notification>>

    fun notifications(
        query: NotificationsQuery,
        cursor: Cursor,
    ): Flow<CursorList<Notification>>

    suspend fun markRead(at: Instant)

    suspend fun registerPushNotificationToken(
        token: String,
    ): Outcome
}

internal class OfflineNotificationsRepository @Inject constructor(
    @Named("AppScope") appScope: CoroutineScope,
    private val postDao: PostDao,
    private val notificationsDao: NotificationsDao,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
    private val networkService: NetworkService,
    private val networkMonitor: NetworkMonitor,
    private val savedStateDataSource: SavedStateDataSource,
    httpClient: HttpClient,
) : NotificationsRepository {

    private val notificationsClient = httpClient.config {
        install(DefaultRequest) {
            url.takeFrom(InternalEndpoints.HeronEndpoint)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15.seconds.inWholeMilliseconds
        }
        install(Logging) {
            level = LogLevel.BODY
            logger = object : Logger {
                override fun log(message: String) {
//                    println("Logger Notifications => $message")
                }
            }
        }
    }

    override val unreadCount: Flow<Long> =
        savedStateDataSource.singleAuthorizedSessionFlow {
            flow {
                while (true) {
                    val unreadCount = networkService.runCatchingWithMonitoredNetworkRetry {
                        getUnreadCount(
                            params = GetUnreadCountQueryParams(),
                        )
                    }
                        .getOrNull()?.count ?: 0
                    emit(unreadCount)
                    delay(30.seconds)
                }
            }
        }
            .stateIn(
                scope = appScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = 0,
            )

    override val lastRefreshed: Flow<Instant?> = savedStateDataSource.savedState
        .map { it.signedInProfileNotifications()?.lastRefreshed }
        .distinctUntilChanged()

    override val unreadNotifications: Flow<List<Notification>> =
        savedStateDataSource.singleAuthorizedSessionFlow { signedInProfileId ->
            lastRefreshed
                .filterNotNull()
                .flatMapLatest { refreshed ->
                    notificationsDao.unreadNotifications(
                        ownerId = signedInProfileId.id,
                        lastRead = refreshed,
                    )
                        .distinctUntilChanged()
                        .flatMapLatest { populatedNotificationEntities ->
                            asExternalModel(
                                signedInProfileId = signedInProfileId,
                                populatedNotificationEntities = populatedNotificationEntities,
                            )
                        }
                        .withRefresh {
                            notificationsWithAssociatedPosts(
                                queryParams = ListNotificationsQueryParams(
                                    limit = UnreadNotificationsLimit,
                                    cursor = null,
                                    seenAt = refreshed,
                                ),
                            )
                        }
                }
        }

    override fun notifications(
        query: NotificationsQuery,
        cursor: Cursor,
    ): Flow<CursorList<Notification>> =
        savedStateDataSource.singleAuthorizedSessionFlow { signedInProfileId ->
            observeAndRefreshNotifications(
                query = query,
                nextCursorFlow = networkService.nextCursorFlow(
                    currentCursor = cursor,
                    currentRequestWithNextCursor = {
                        notificationsWithAssociatedPosts(
                            queryParams = ListNotificationsQueryParams(
                                limit = query.data.limit,
                                cursor = cursor.value,
                            ),
                        )
                    },
                    nextCursor = {
                        first.cursor
                    },
                    onResponse = {
                        multipleEntitySaverProvider.saveInTransaction {
                            if (query.data.page == 0) {
                                notificationsDao.deleteAllNotifications()
                            }
                            add(
                                viewingProfileId = signedInProfileId,
                                listNotificationsNotification = first.notifications,
                                associatedPosts = second,
                            )
                        }
                        if (query.data.page == 0) {
                            savedStateDataSource.updateSignedInUserNotifications {
                                copy(lastRefreshed = query.data.cursorAnchor)
                            }
                        }
                    },
                ),
            )
                .distinctUntilChanged()
        }

    override suspend fun markRead(at: Instant) {
        val lastReadAt = savedStateDataSource.savedState
            .value
            .signedInProfileNotifications()
            ?.lastRead
        if (lastReadAt != null && lastReadAt > at) return

        val isSuccess = networkService.runCatchingWithMonitoredNetworkRetry {
            updateSeen(
                // Add 1 millisecond to the request to be past the time on the backend
                request = UpdateSeenRequest(at + 1.milliseconds),
            )
        }.isSuccess
        if (isSuccess) savedStateDataSource.updateSignedInUserNotifications {
            // Try to always make this increment
            copy(
                lastRead = maxOf(
                    a = lastRead ?: Instant.DISTANT_PAST,
                    b = at,
                ),
            )
        }
    }

    override suspend fun registerPushNotificationToken(
        token: String,
    ) = savedStateDataSource.inCurrentProfileSession { signedProfileId ->
        if (signedProfileId == null) return@inCurrentProfileSession expiredSessionOutcome()
        val saveNotificationTokenRequest = SaveNotificationTokenRequest(
            did = signedProfileId.id,
            token = token,
        )
        networkMonitor.runCatchingWithNetworkRetry(
            block = {
                notificationsClient.post(SaveNotificationTokenPath) {
                    contentType(ContentType.Application.Json)
                    setBody(saveNotificationTokenRequest)
                }
            },
        ).toOutcome()
    } ?: expiredSessionOutcome()

    private fun observeAndRefreshNotifications(
        query: NotificationsQuery,
        nextCursorFlow: Flow<Cursor>,
    ): Flow<CursorList<Notification>> =
        combine(
            observeNotifications(query),
            nextCursorFlow,
            ::CursorList,
        )

    private fun observeNotifications(
        query: NotificationsQuery,
    ): Flow<List<Notification>> =
        savedStateDataSource.singleAuthorizedSessionFlow { signedInProfileId ->
            notificationsDao.notifications(
                ownerId = signedInProfileId.id,
                before = query.data.cursorAnchor,
                offset = query.data.offset,
                limit = query.data.limit,
            )
                .flatMapLatest { populatedNotificationEntities ->
                    asExternalModel(
                        signedInProfileId = signedInProfileId,
                        populatedNotificationEntities = populatedNotificationEntities,
                    )
                }
        }

    private fun asExternalModel(
        signedInProfileId: ProfileId,
        populatedNotificationEntities: List<PopulatedNotificationEntity>,
    ) = postDao.posts(
        viewingProfileId = signedInProfileId.id,
        postUris = populatedNotificationEntities
            .mapNotNull { it.entity.associatedPostUri }
            .toSet(),
    ).map { posts ->
        val urisToPosts = posts.associateBy { it.entity.uri }
        populatedNotificationEntities.map {
            it.asExternalModel(
                associatedPost = it.entity.associatedPostUri
                    ?.let(urisToPosts::get)
                    ?.asExternalModel(
                        embeddedRecord = null,
                    ),
            )
        }
    }

    private suspend fun notificationsWithAssociatedPosts(
        queryParams: ListNotificationsQueryParams,
    ): AtpResponse<Pair<ListNotificationsResponse, List<PostView>>> =
        networkService.api
            .listNotifications(queryParams)
            .map { response ->
                val chunkedPostViews = coroutineScope {
                    response.notifications
                        .mapNotNullTo(
                            destination = mutableSetOf(),
                            transform = ListNotificationsNotification::associatedPostUri,
                        )
                        .chunked(MaxPostsFetchedPerQuery)
                        .map { postUris ->
                            async {
                                networkService.api
                                    .getPosts(GetPostsQueryParams(uris = postUris))
                                    .map(GetPostsResponse::posts)
                                    .requireResponse()
                            }
                        }
                }.awaitAll()

                response to chunkedPostViews.flatten()
            }
}

private fun SavedState.signedInProfileNotifications() =
    signedInProfileData
        ?.notifications

@Serializable
private data class SaveNotificationTokenRequest(
    val did: String,
    val token: String,
)

private const val SaveNotificationTokenPath = "/saveNotificationToken"

private const val UnreadNotificationsLimit = 100L
private const val MaxPostsFetchedPerQuery = 25

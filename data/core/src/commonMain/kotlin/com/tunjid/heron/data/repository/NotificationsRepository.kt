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
import app.bsky.notification.GetUnreadCountQueryParams
import app.bsky.notification.ListNotificationsNotification
import app.bsky.notification.ListNotificationsQueryParams
import app.bsky.notification.UpdateSeenRequest
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.offset
import com.tunjid.heron.data.core.models.value
import com.tunjid.heron.data.database.daos.NotificationsDao
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.multipleEntitysaver.associatedPostUri
import com.tunjid.heron.data.utilities.nextCursorFlow
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import sh.christian.ozone.api.response.AtpResponse

@Serializable
data class NotificationsQuery(
    override val data: CursorQuery.Data,
) : CursorQuery

interface NotificationsRepository {
    val unreadCount: Flow<Long>

    val lastRefreshed: Flow<Instant?>

    fun notifications(
        query: NotificationsQuery,
        cursor: Cursor,
    ): Flow<CursorList<Notification>>

    suspend fun markRead(at: Instant)
}

internal class OfflineNotificationsRepository @Inject constructor(
    @Named("AppScope") appScope: CoroutineScope,
    private val postDao: PostDao,
    private val notificationsDao: NotificationsDao,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
    private val networkService: NetworkService,
    private val savedStateDataSource: SavedStateDataSource,
) : NotificationsRepository {

    override val unreadCount: Flow<Long> =
        savedStateDataSource.observedSignedInProfileId
            .filterNotNull()
            .flatMapLatest {
                flow {
                    while (true) {
                        val unreadCount = networkService.runCatchingWithMonitoredNetworkRetry {
                            getUnreadCount(
                                params = GetUnreadCountQueryParams(),
                            )
                        }
                            .getOrNull()?.count ?: 0
                        emit(unreadCount)
                        kotlinx.coroutines.delay(30_000)
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

    override fun notifications(
        query: NotificationsQuery,
        cursor: Cursor,
    ): Flow<CursorList<Notification>> =
        observeAndRefreshNotifications(
            query = query,
            nextCursorFlow = networkService.nextCursorFlow(
                currentCursor = cursor,
                currentRequestWithNextCursor = {
                    val notificationsAtpResponse = networkService.api
                        .listNotifications(
                            ListNotificationsQueryParams(
                                limit = query.data.limit,
                                cursor = cursor.value,
                            ),
                        )
                    when (notificationsAtpResponse) {
                        is AtpResponse.Failure -> AtpResponse.Failure(
                            statusCode = notificationsAtpResponse.statusCode,
                            response = null,
                            error = notificationsAtpResponse.error,
                            headers = notificationsAtpResponse.headers,
                        )

                        is AtpResponse.Success -> {
                            networkService.api
                                .getPosts(
                                    GetPostsQueryParams(
                                        uris = notificationsAtpResponse.response.notifications
                                            .mapNotNull(
                                                ListNotificationsNotification::associatedPostUri,
                                            )
                                            .distinct(),
                                    ),
                                )
                                .map {
                                    notificationsAtpResponse.requireResponse() to it.posts
                                }
                        }
                    }
                },
                nextCursor = {
                    first.cursor
                },
                onResponse = {
                    val authProfileId = savedStateDataSource.savedState.value.auth?.authProfileId
                    if (authProfileId != null) multipleEntitySaverProvider.saveInTransaction {
                        if (query.data.page == 0) {
                            notificationsDao.deleteAllNotifications()
                        }
                        add(
                            viewingProfileId = authProfileId,
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
        savedStateDataSource.observedSignedInProfileId
            .filterNotNull()
            .flatMapLatest { signedInProfileId ->
                notificationsDao.notifications(
                    ownerId = signedInProfileId.id,
                    before = query.data.cursorAnchor,
                    offset = query.data.offset,
                    limit = query.data.limit,
                )
                    .flatMapLatest { populatedNotificationEntities ->
                        postDao.posts(
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
                                        ?.asExternalModel(quote = null),
                                )
                            }
                        }
                    }
            }
}

private fun SavedState.signedInProfileNotifications() =
    signedInProfileData
        ?.notifications

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
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.value
import com.tunjid.heron.data.database.daos.NotificationsDao
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.utilities.CursorQuery
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.multipleEntitysaver.associatedPostUri
import com.tunjid.heron.data.utilities.nextCursorFlow
import com.tunjid.heron.data.utilities.runCatchingWithNetworkRetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import me.tatarka.inject.annotations.Inject
import sh.christian.ozone.api.response.AtpResponse

@Serializable
data class NotificationsQuery(
    override val data: CursorQuery.Data,
) : CursorQuery

interface NotificationsRepository {
    val unreadCount: Flow<Long>

    fun notifications(
        query: NotificationsQuery,
        cursor: Cursor,
    ): Flow<CursorList<Notification>>

    suspend fun markRead()
}

class OfflineNotificationsRepository @Inject constructor(
    appScope: CoroutineScope,
    private val postDao: PostDao,
    private val notificationsDao: NotificationsDao,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
    private val networkService: NetworkService,
    private val savedStateRepository: SavedStateRepository,
) : NotificationsRepository {

    override val unreadCount: Flow<Long> = savedStateRepository.savedState
        .map { it.notifications?.lastSeen }
        .distinctUntilChanged()
        .map { lastSeen ->
            runCatchingWithNetworkRetry {
                networkService.api.getUnreadCount(
                    params = GetUnreadCountQueryParams(
                        seenAt = lastSeen
                    )
                )
            }
                .getOrNull()?.count ?: 0
        }
        .stateIn(
            scope = appScope,
            started = SharingStarted.Eagerly,
            initialValue = 0,
        )

    override fun notifications(
        query: NotificationsQuery,
        cursor: Cursor,
    ): Flow<CursorList<Notification>> =
        observeAndRefreshNotifications(
            query = query,
            nextCursorFlow = nextCursorFlow(
                currentCursor = cursor,
                currentRequestWithNextCursor = {
                    networkService.api
                        .listNotifications(
                            ListNotificationsQueryParams(
                                limit = query.data.limit,
                                cursor = cursor.value
                            )
                        )
                    val notificationsAtpResponse = networkService.api
                        .listNotifications(
                            ListNotificationsQueryParams(
                                limit = query.data.limit,
                                cursor = cursor.value
                            )
                        )
                    when (notificationsAtpResponse) {
                        is AtpResponse.Failure -> AtpResponse.Failure(
                            statusCode = notificationsAtpResponse.statusCode,
                            response = null,
                            error = notificationsAtpResponse.error,
                            headers = notificationsAtpResponse.headers
                        )

                        is AtpResponse.Success -> {
                            networkService.api
                                .getPosts(
                                    GetPostsQueryParams(
                                        uris = notificationsAtpResponse.response.notifications
                                            .mapNotNull(
                                                ListNotificationsNotification::associatedPostUri
                                            )
                                            .distinct()
                                    )
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
                    val authProfileId =
                        savedStateRepository.savedState.value.auth?.authProfileId
                    if (authProfileId != null) multipleEntitySaverProvider.saveInTransaction {
                        add(
                            viewingProfileId = savedStateRepository.signedInProfileId,
                            listNotificationsNotification = first.notifications,
                            associatedPosts = second,
                        )
                    }
                },
            )
        )
            .distinctUntilChanged()

    override suspend fun markRead() {
        runCatchingWithNetworkRetry {
            networkService.api.updateSeen(
                request = UpdateSeenRequest(Clock.System.now())
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
        notificationsDao.notifications(
            before = query.data.cursorAnchor,
            offset = query.data.page * query.data.limit,
            limit = query.data.limit,
        )
            .flatMapLatest { populatedNotificationEntities ->
                postDao.posts(
                    populatedNotificationEntities
                        .mapNotNull { it.entity.associatedPostId }
                        .toSet()
                ).map { posts ->
                    val idsToPosts = posts.associateBy { it.entity.cid }
                    populatedNotificationEntities.map {
                        it.asExternalModel(
                            associatedPost = it.entity.associatedPostId
                                ?.let(idsToPosts::get)
                                ?.asExternalModel(quote = null)
                        )
                    }
                }
            }
}

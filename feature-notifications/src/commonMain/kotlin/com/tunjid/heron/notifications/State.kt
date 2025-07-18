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

package com.tunjid.heron.notifications

import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.associatedPostUri
import com.tunjid.heron.data.repository.NotificationsQuery
import com.tunjid.heron.data.utilities.CursorQuery
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.tiledItems
import com.tunjid.tiler.buildTiledList
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


@Serializable
data class State(
    val lastRefreshed: Instant? = null,
    @Transient
    val signedInProfile: Profile? = null,
    override val tilingData: TilingState.Data<NotificationsQuery, Notification> = TilingState.Data(
        currentQuery = NotificationsQuery(
            data = CursorQuery.Data(
                page = 0,
                cursorAnchor = Clock.System.now(),
            )
        ),
    ),
    @Transient
    val messages: List<String> = emptyList(),
) : TilingState<NotificationsQuery, Notification>

fun State.aggregateNotifications() = buildTiledList<NotificationsQuery, AggregatedNotification> {
    tiledItems.forEachIndexed { index, notification ->
        when {
            isNotEmpty() && last().canAggregate(notification) -> {
                val last = remove(lastIndex)
                add(
                    query = tiledItems.queryAt(index),
                    item = last.copy(
                        isRead = last.isRead && notification.isRead,
                        aggregatedProfiles = last.aggregatedProfiles + notification.author,
                    )
                )
            }

            else -> add(
                query = tiledItems.queryAt(index),
                item = AggregatedNotification(
                    isRead = notification.isRead,
                    notification = notification,
                    aggregatedProfiles = listOf(notification.author),
                )
            )
        }
    }
}

sealed class Action(val key: String) {

    data class Tile(
        val tilingAction: TilingState.Action,
    ) : Action(key = "Tile")

    data class SendPostInteraction(
        val interaction: Post.Interaction,
    ) : Action(key = "SendPostInteraction")

    data class MarkNotificationsRead(
        val at: Instant,
    ) : Action(key = "markNotificationsRead")

    sealed class Navigate : Action(key = "Navigate"), NavigationAction {

        data class DelegateTo(
            val delegate: NavigationAction.Common,
        ) : Navigate(), NavigationAction by delegate

    }
}

private fun AggregatedNotification.canAggregate(
    other: Notification,
): Boolean = when {
    notification::class != other::class -> false
    notification.associatedPostUri == null && other.associatedPostUri == null -> true
    else -> notification.associatedPostUri == other.associatedPostUri
}

val AggregatedNotification.id get() = notification.cid.id

data class AggregatedNotification(
    val isRead: Boolean,
    val notification: Notification,
    val aggregatedProfiles: List<Profile>,
)
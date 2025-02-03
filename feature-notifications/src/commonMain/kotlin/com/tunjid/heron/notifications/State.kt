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
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.buildTiledList
import com.tunjid.tiler.emptyTiledList
import com.tunjid.treenav.pop
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


@Serializable
data class State(
    val currentQuery: NotificationsQuery = NotificationsQuery(
        data = CursorQuery.Data(
            page = 0,
            cursorAnchor = Clock.System.now(),
        )
    ),
    val unreadNotificationCount: Long = 0,
    @Transient
    val signedInProfile: Profile? = null,
    @Transient
    val notifications: TiledList<NotificationsQuery, Notification> = emptyTiledList(),
    @Transient
    val messages: List<String> = emptyList(),
)

fun State.aggregateNotifications() = buildTiledList<NotificationsQuery, AggregatedNotification> {
    notifications.forEachIndexed { index, notification ->
        when {
            isNotEmpty() && last().canAggregate(notification) -> {
                val last = remove(lastIndex)
                add(
                    query = notifications.queryAt(index),
                    item = last.copy(
                        aggregatedProfiles = last.aggregatedProfiles + notification.author
                    )
                )
            }

            else -> add(
                query = notifications.queryAt(index),
                item = AggregatedNotification(
                    notification = notification,
                    aggregatedProfiles = listOf(notification.author),
                )
            )
        }
    }
}

sealed class Action(val key: String) {

    data class LoadAround(
        val query: NotificationsQuery,
    ) : Action("LoadAround")

    data class SendPostInteraction(
        val interaction: Post.Interaction,
    ) : Action(key = "SendPostInteraction")

    data object MarkNotificationsRead : Action(key = "markNotificationsRead")

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
    val notification: Notification,
    val aggregatedProfiles: List<Profile>,
)
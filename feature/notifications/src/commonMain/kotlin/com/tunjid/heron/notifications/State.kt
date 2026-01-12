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

import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.MutedWordPreference
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.associatedPostUri
import com.tunjid.heron.data.repository.NotificationsQuery
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.tiledItems
import com.tunjid.heron.ui.text.Memo
import com.tunjid.tiler.buildTiledList
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class State(
    val lastRefreshed: Instant? = null,
    @Transient
    val preferences: Preferences = Preferences.EmptyPreferences,
    @Transient
    val recentConversations: List<Conversation> = emptyList(),
    @Transient
    val signedInProfile: Profile? = null,
    @Transient
    val canAnimateRequestPermissionsButton: Boolean = false,
    override val tilingData: TilingState.Data<NotificationsQuery, Notification> = TilingState.Data(
        currentQuery = NotificationsQuery(
            data = CursorQuery.Data(
                page = 0,
                cursorAnchor = Clock.System.now(),
                limit = 18,
            ),
        ),
    ),
    @Transient
    val messages: List<Memo> = emptyList(),
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
                    ),
                )
            }

            else -> add(
                query = tiledItems.queryAt(index),
                item = AggregatedNotification(
                    isRead = notification.isRead,
                    notification = notification,
                    aggregatedProfiles = listOf(notification.author),
                ),
            )
        }
    }
}

sealed class Action(val key: String) {

    data class Tile(
        val tilingAction: TilingState.Action,
    ) : Action(key = "Tile")

    data class UpdateMutedWord(
        val mutedWordPreference: List<MutedWordPreference>,
    ) : Action(key = "UpdateMutedWord")

    data class SendPostInteraction(
        val interaction: Post.Interaction,
    ) : Action(key = "SendPostInteraction")

    data class SnackbarDismissed(
        val message: Memo,
    ) : Action(key = "SnackbarDismissed")

    data class MarkNotificationsRead(
        val at: Instant,
    ) : Action(key = "markNotificationsRead")

    sealed class Navigate :
        Action(key = "Navigate"),
        NavigationAction {

        data class To(
            val delegate: NavigationAction.Destination,
        ) : Navigate(),
            NavigationAction by delegate
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

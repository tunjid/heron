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

package com.tunjid.heron.notifications

import com.tunjid.heron.data.core.models.Notification
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
            firstRequestInstant = Clock.System.now(),
        )
    ),
    @Transient
    val notifications: TiledList<NotificationsQuery, Notification> = emptyTiledList(),
    @Transient
    val messages: List<String> = emptyList(),
)

sealed class Action(val key: String) {

    data class LoadAround(
        val query: NotificationsQuery,
    ) : Action("LoadAround")

    sealed class Navigate : Action(key = "Navigate"), NavigationAction {
        data object Pop : Navigate() {
            override val navigationMutation: NavigationMutation = {
                navState.pop()
            }
        }
    }
}

val Notification.id get() = cid.id

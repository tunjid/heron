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

package com.tunjid.heron.home

import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.repository.TimelineQuery
import com.tunjid.heron.domain.timeline.TimelineLoadAction
import com.tunjid.heron.home.di.RoutePattern
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.currentRoute
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.emptyTiledList
import com.tunjid.treenav.pop
import com.tunjid.treenav.push
import com.tunjid.treenav.strings.routeString
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


@Serializable
data class State(
    val currentQuery: TimelineQuery.Home,
    val numColumns: Int = 1,
    @Transient
    val feed: TiledList<TimelineQuery.Home, TimelineItem> = emptyTiledList(),
    @Transient
    val messages: List<String> = emptyList(),
)


sealed class Action(val key: String) {

    sealed class LoadFeed : Action("List") {
        data class LoadAround(val query: TimelineQuery.Home) : LoadFeed()
        data class GridSize(val numColumns: Int) : LoadFeed()
    }

    sealed class Navigate : Action(key = "Navigate"), NavigationAction {
        data object Pop : Navigate() {
            override val navigationMutation: NavigationMutation = {
                navState.pop()
            }
        }

        data class ToProfile(
            val profileId: Id,
            val profileAvatar: Uri?,
            val avatarSharedElementKey: String?,
        ) : Navigate() {
            override val navigationMutation: NavigationMutation = {
                navState.push(
                    routeString(
                        path = "/profile/${profileId.id}",
                        queryParams =
                        if (currentRoute.id != RoutePattern) mapOf(
                            "avatarSharedElementKey" to listOfNotNull(avatarSharedElementKey),
                            "profileAvatar" to listOfNotNull(profileAvatar?.uri),
                        )
                        else mapOf(
                            "referringRoute" to listOf(currentRoute.encodeToQueryParam()),
                            "profileAvatar" to listOfNotNull(profileAvatar?.uri),
                            "avatarSharedElementKey" to listOfNotNull(avatarSharedElementKey),
                        )
                    ).toRoute
                )
            }
        }
    }
}
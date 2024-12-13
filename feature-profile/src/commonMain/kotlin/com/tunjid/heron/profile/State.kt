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

package com.tunjid.heron.profile

import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.repository.TimelineQuery
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.emptyTiledList
import com.tunjid.treenav.pop
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


@Serializable
data class State(
    val currentQuery: TimelineQuery.Profile,
    val numColumns: Int = 1,
    val profile: Profile,
    @Transient
    val feed: TiledList<TimelineQuery.Profile, TimelineItem> = emptyTiledList(),
    @Transient
    val messages: List<String> = emptyList(),
)


sealed class Action(val key: String) {

    sealed class LoadFeed : Action("List") {
        data class LoadAround(val query: TimelineQuery.Profile) : LoadFeed()
        data class GridSize(val numColumns: Int) : LoadFeed()
    }

    sealed class Navigate : Action(key = "Navigate"), NavigationAction {
        data object Pop : Navigate() {
            override val navigationMutation: NavigationMutation = {
                navState.pop()
            }
        }
    }
}
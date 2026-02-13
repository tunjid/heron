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

package com.tunjid.heron.graze.editor

import androidx.navigationevent.NavigationEventInfo
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.data.graze.Filter
import com.tunjid.heron.data.graze.GrazeFeed
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.ui.text.Memo
import com.tunjid.treenav.strings.Route
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class State(
    val feed: GrazeFeed = GrazeFeed.Pending(
        recordKey = RecordKey("test2"),
        filter = Filter.And(
            filters = emptyList(),
        ),
    ),
    val currentPath: List<Int> = emptyList(),
    @Transient
    val suggestedProfiles: List<Profile> = emptyList(),
    @Transient
    val messages: List<Memo> = emptyList(),
)

fun State(
    route: Route,
) = State()

val State.currentFilter
    get() = currentPath.fold(feed.filter) { current, index ->
        current.filters[index] as Filter.Root
    }

data class FilterNavigationEventInfo(
    val filter: Filter.Root,
) : NavigationEventInfo()

sealed class Action(val key: String) {
    sealed class EditorNavigation : Action("EditorNavigation") {
        data class EnterFilter(val index: Int) : EditorNavigation()
        data object ExitFilter : EditorNavigation()
    }

    sealed class EditFilter : Action("EditFilter") {
        abstract val path: List<Int>

        data class FlipRootFilter(
            override val path: List<Int>,
        ) : EditFilter()

        data class AddFilter(
            override val path: List<Int>,
            val filter: Filter,
        ) : EditFilter()

        data class UpdateFilter(
            override val path: List<Int>,
            val filter: Filter,
            val index: Int,
        ) : EditFilter()

        data class RemoveFilter(
            override val path: List<Int>,
            val index: Int,
        ) : EditFilter()
    }

    data class SearchProfiles(
        val query: String,
    ) : Action("SearchProfiles")

    data class Save(
        val feed: GrazeFeed,
    ) : Action("Save")

    sealed class Navigate :
        Action(key = "Navigate"),
        NavigationAction {
        data object Pop : Navigate(), NavigationAction by NavigationAction.Pop
    }
}

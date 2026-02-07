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
import com.tunjid.heron.data.graze.Filter
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.ui.text.Memo
import com.tunjid.treenav.strings.Route
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class State(
    val filter: Filter.Root = Filter.And(emptyList()),
    val currentPath: List<Int> = emptyList(),
    @Transient
    val messages: List<Memo> = emptyList(),
)

fun State(
    route: Route,
) = State()

val State.currentFilter
    get() = currentPath.fold(filter) { current, index ->
        current.filters[index] as Filter.Root
    }

fun Filter.Root.updateAt(
    path: List<Int>,
    update: (Filter.Root) -> Filter.Root,
): Filter.Root {
    if (path.isEmpty()) return update(this)
    val index = path.first()
    // This cast should be safe if path logic is correct
    val child = filters[index] as Filter.Root
    val updatedChild = child.updateAt(path.drop(1), update)

    val newFilters = filters.toMutableList()
    newFilters[index] = updatedChild

    return when (this) {
        is Filter.And -> copy(filters = newFilters)
        is Filter.Or -> copy(filters = newFilters)
    }
}

data class FilterNavigationEventInfo(
    val filter: Filter.Root,
) : NavigationEventInfo()

sealed class Action(val key: String) {

    data class EnterFilter(val index: Int) : Action("EnterFilter")

    data object ExitFilter : Action("ExitFilter")

    data class AddFilter(val filter: Filter) : Action("AddFilter")

    sealed class Navigate :
        Action(key = "Navigate"),
        NavigationAction {
        data object Pop : Navigate(), NavigationAction by NavigationAction.Pop
    }
}

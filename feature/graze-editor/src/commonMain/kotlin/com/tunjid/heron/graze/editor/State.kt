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

sealed class Action(val key: String) {

    data class EnterFilter(val index: Int) : Action("EnterFilter")

    data object ExitFilter : Action("ExitFilter")

    sealed class Navigate :
        Action(key = "Navigate"),
        NavigationAction {
        data object Pop : Navigate(), NavigationAction by NavigationAction.Pop
    }
}

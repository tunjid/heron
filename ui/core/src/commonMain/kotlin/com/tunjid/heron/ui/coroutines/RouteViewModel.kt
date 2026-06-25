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

package com.tunjid.heron.ui.coroutines

import androidx.lifecycle.ViewModel
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.CoroutineScope

/**
 * Base class for the [ViewModel]s that back navigation destinations (feature screens). Its lifecycle
 * is owned by the [scope] passed in (typically a [viewModelCoroutineScope]) and it is created on
 * demand for its [route] from the app's dependency graph via a [RouteViewModelInitializer].
 */
abstract class RouteViewModel(
    scope: CoroutineScope,
    val route: Route,
) : ViewModel(viewModelScope = scope)

/**
 * Factory for a [RouteViewModel] given its [CoroutineScope] and [Route]. Each feature contributes one
 * of these into the app graph's `Map<KClass<out RouteViewModel>, RouteViewModelInitializer>`, keyed
 * by ViewModel type, so any screen's ViewModel can be resolved through `PaneScaffoldState`.
 */
fun interface RouteViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): RouteViewModel
}

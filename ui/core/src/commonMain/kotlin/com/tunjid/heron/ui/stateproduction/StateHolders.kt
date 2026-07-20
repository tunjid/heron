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

package com.tunjid.heron.ui.stateproduction

import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.CoroutineScope

/**
 * Marker type for the state holders that back navigation destinations (feature screens).
 */
interface RouteStateHolder

/**
 * Marker type for the state holders that back app level bottom sheets.
 */
interface SheetStateHolder

/**
 * Factory for a [RouteStateHolder] given its [CoroutineScope] and [Route].
 */
fun interface RouteStateHolderInitializer {
    fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): RouteStateHolder
}

/**
 * Factory for a [SheetStateHolder] given its [CoroutineScope].
 */
fun interface SheetStateHolderInitializer {
    fun invoke(
        scope: CoroutineScope,
    ): SheetStateHolder
}

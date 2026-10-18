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

package com.tunjid.heron.ui.scaffold.scaffold

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

/**
 * The system's vertical bar: the side column devices like the iPhone Duo mount the status bar in,
 * and lay bars out along.
 *
 * Platforms that have one report it through [AppState.onSystemVerticalBarChanged].
 */
@Immutable
internal data class SystemVerticalBar(
    /**
     * The physical edge the system places the vertical bar on.
     */
    val edge: NavRailEdge,
    /**
     * The width of the safe area inset the vertical bar occupies on [edge].
     */
    val width: Dp,
    /**
     * How far the center of the region the system lays a bar out in is from [edge]. It is not
     * necessarily the center of the safe area inset.
     */
    val centerFromEdge: Dp,
)

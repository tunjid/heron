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

package com.tunjid.heron.ui

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Stable
import com.tunjid.treenav.compose.PaneMovableElementSharedTransitionScope
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.strings.Route

@Stable
interface PaneTransitionScope : PaneMovableElementSharedTransitionScope<ThreePane, Route> {
    /**
     * [BoundsTransform] for general elements in the scope. It creates transforms suitable
     * for the scope's current configuration.
     */
    val childBoundsTransform: BoundsTransform

    /**
     * [BoundsTransform] for shared elements of scrolling content. It informs the scope
     * of where elements transition from and to.
     */
    val sharedElementBoundsTransform: BoundsTransform

    /**
     * [SharedTransitionScope.OverlayClip] for shared elements in the pane that clips them to
     * the bounds of the pane when it is transformed.
     */
    val paneOverlayClip: SharedTransitionScope.OverlayClip

    /**
     * [WindowInsets] for the navigation rail is present in the pane.
     */
    val navigationRailInsets: WindowInsets
}

val PaneTransitionScope.isPrimaryOrActive
    get() = paneState.pane == ThreePane.Primary || isActive

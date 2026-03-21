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

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.tunjid.treenav.compose.PaneMovableElementSharedTransitionScope
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.strings.Route

interface PaneTransitionScope : PaneMovableElementSharedTransitionScope<ThreePane, Route>

val PaneTransitionScope.isPrimaryOrActive
    get() = paneState.pane == ThreePane.Primary || isActive

// This should only be callable from this scope
@Suppress("UnusedReceiverParameter")
inline val PaneTransitionScope.localOverlayClip: SharedTransitionScope.OverlayClip
    @ReadOnlyComposable @Composable get() = LocalSharedElementOverlayClip.current

val DefaultSharedElementOverlayClip = object : SharedTransitionScope.OverlayClip {
    override fun getClipPath(
        sharedContentState: SharedTransitionScope.SharedContentState,
        bounds: Rect,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Path? {
        return sharedContentState.parentSharedContentState?.clipPathInOverlay
    }
}

val LocalSharedElementOverlayClip = staticCompositionLocalOf {
    DefaultSharedElementOverlayClip
}

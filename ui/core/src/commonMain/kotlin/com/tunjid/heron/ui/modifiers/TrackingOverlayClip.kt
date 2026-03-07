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

package com.tunjid.heron.ui.modifiers

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.onLayoutRectChanged
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toRect

class TrackingOverlayClip : SharedTransitionScope.OverlayClip {

    internal var parentBounds by mutableStateOf(IntRect.Zero)
    private var path: Path? = null

    override fun getClipPath(
        sharedContentState: SharedTransitionScope.SharedContentState,
        bounds: Rect,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Path? {
        if (parentBounds == IntRect.Zero) return null
        val clipPath = path ?: Path().also { path = it }

        clipPath.rewind()
        clipPath.addRect(parentBounds.toRect())
        return clipPath
    }
}

/**
 * Used to track the bounds of a Composable so it may be used for clipping later.
 * There should be a 1:1 use of this Modifier to [TrackingOverlayClip], it should not be
 * reused.
 */
fun Modifier.trackOverlayClipBounds(
    trackingOverlayClip: TrackingOverlayClip,
) = onLayoutRectChanged { bounds ->
    trackingOverlayClip.parentBounds = bounds.boundsInScreen
}

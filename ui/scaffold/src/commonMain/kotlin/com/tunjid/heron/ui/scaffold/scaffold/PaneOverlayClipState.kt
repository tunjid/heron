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

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

/**
 * Captures the coordinates of the pane so [PaneOverlayClipState] so shared elements outside
 * it can be clipped appropriately.
 */
internal fun Modifier.trackPaneOverlayClipBounds(
    paneOverlayClipState: PaneOverlayClipState,
): Modifier = this then PaneCoordinatesElement(
    paneOverlayClipState = paneOverlayClipState,
)

private data class PaneCoordinatesElement(
    val paneOverlayClipState: PaneOverlayClipState,
) : ModifierNodeElement<PaneCoordinatesNode>() {

    override fun create(): PaneCoordinatesNode =
        PaneCoordinatesNode(
            paneOverlayClipState = paneOverlayClipState,
        )

    override fun update(
        node: PaneCoordinatesNode
    ) = node.update(
        paneOverlayClipState = paneOverlayClipState,
    )

    override fun InspectorInfo.inspectableProperties() {
        name = "trackPaneOverlayClipBounds"
    }
}

private class PaneCoordinatesNode(
    private var paneOverlayClipState: PaneOverlayClipState,
) : Modifier.Node(),
    LayoutAwareModifierNode {

    fun update(
        paneOverlayClipState: PaneOverlayClipState,
    ) {
        if (this.paneOverlayClipState === paneOverlayClipState) return
        this.paneOverlayClipState.paneCoordinates = null
        this.paneOverlayClipState = paneOverlayClipState
        if (isAttached) paneOverlayClipState.paneCoordinates = requireLayoutCoordinates()
    }

    override fun onPlaced(coordinates: LayoutCoordinates) {
        // The coordinates are the same object for the lifetime of the node. Writing them
        // again while clipped invalidates readers of the clip as the pane moves.
        if (paneOverlayClipState.paneCoordinates !== coordinates || paneOverlayClipState.isClipped()) {
            paneOverlayClipState.paneCoordinates = coordinates
        }
    }

    override fun onDetach() {
        paneOverlayClipState.paneCoordinates = null
    }
}

/**
 * Clips drawn content to the bounds defined by [PaneOverlayClipState].
 */
internal fun Modifier.clipToPaneInOverlay(
    paneOverlayClipState: PaneOverlayClipState,
): Modifier = this then PaneOverlayClipElement(
    paneOverlayClipState = paneOverlayClipState,
)

private data class PaneOverlayClipElement(
    val paneOverlayClipState: PaneOverlayClipState,
) : ModifierNodeElement<PaneOverlayClipNode>() {

    override fun create(): PaneOverlayClipNode =
        PaneOverlayClipNode(
            paneOverlayClipState = paneOverlayClipState,
        )

    override fun update(node: PaneOverlayClipNode) {
        node.paneOverlayClipState = paneOverlayClipState
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "clipToPaneInOverlay"
    }
}

private class PaneOverlayClipNode(
    var paneOverlayClipState: PaneOverlayClipState,
) : Modifier.Node(),
    DrawModifierNode {

    override fun ContentDrawScope.draw() {
        val clipPath = paneOverlayClipState.clipPath(
            density = this,
        ) ?: return drawContent()

        // The clip path is in root coordinates, move into them to clip, then back to draw.
        val positionInRoot = requireLayoutCoordinates().positionInRoot()
        translate(
            left = -positionInRoot.x,
            top = -positionInRoot.y,
        ) {
            clipPath(clipPath) {
                translate(
                    left = positionInRoot.x,
                    top = positionInRoot.y,
                ) {
                    this@draw.drawContent()
                }
            }
        }
    }
}

@Stable
internal class PaneOverlayClipState(
    internal val isClipped: () -> Boolean,
) : SharedTransitionScope.OverlayClip {

    internal var paneCoordinates by mutableStateOf<LayoutCoordinates?>(
        value = null,
        policy = neverEqualPolicy(),
    )

    private val path = Path()

    // The path is only rebuilt when the bounds or corner radius it was built with change.
    private var pathPosition = Offset.Unspecified
    private var pathSize = IntSize.Zero
    private var pathCornerRadius = 0f

    override fun getClipPath(
        sharedContentState: SharedTransitionScope.SharedContentState,
        bounds: Rect,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Path? = clipPath(density) ?: sharedContentState.parentSharedContentState?.clipPathInOverlay

    internal fun clipPath(
        density: Density,
    ): Path? {
        if (!isClipped()) return null
        val coordinates = paneCoordinates
            ?.takeIf(LayoutCoordinates::isAttached)
            ?: return null

        val position = coordinates.positionInRoot()
        val size = coordinates.size
        val cornerRadius = with(density) { PaneCornerRadius.toPx() }
        if (
            position == pathPosition &&
            size == pathSize &&
            cornerRadius == pathCornerRadius
        ) return path

        pathPosition = position
        pathSize = size
        pathCornerRadius = cornerRadius

        path.rewind()
        path.addRoundRect(
            RoundRect(
                left = position.x,
                top = position.y,
                right = position.x + size.width,
                bottom = position.y + size.height,
                cornerRadius = CornerRadius(cornerRadius),
            ),
        )
        return path
    }
}

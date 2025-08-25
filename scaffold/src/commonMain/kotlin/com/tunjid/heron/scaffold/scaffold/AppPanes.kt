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

package com.tunjid.heron.scaffold.scaffold

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tunjid.composables.splitlayout.SplitLayoutState
import com.tunjid.treenav.compose.navigation3.ui.NavigationEventHandler
import com.tunjid.treenav.compose.threepane.ThreePane
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.launch

private val PaneSpring = spring(
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = 0.1f,
)

enum class PaneAnchor(
    val fraction: Float,
) {
    Zero(fraction = 0f),
    OneThirds(fraction = 1 / 3f),
    Half(fraction = 1 / 2f),
    TwoThirds(fraction = 2 / 3f),
    Full(fraction = 1f),
}

@Stable
internal class PaneAnchorState {
    var maxWidth by mutableIntStateOf(1000)
        internal set
    val width
        get() = max(
            a = 0,
            b = anchoredDraggableState.offset.roundToInt(),
        )

    val targetPaneAnchor get() = anchoredDraggableState.targetValue

    var hasInteractions by mutableStateOf(false)
        internal set

    val currentPaneAnchor: PaneAnchor
        get() {
            val cappedFraction = max(
                a = min(
                    a = anchoredDraggableState.requireOffset() / maxWidth,
                    b = 1f,
                ),
                b = 0f,
            )
            return when (cappedFraction) {
                in 0f..0.01f -> PaneAnchor.Zero
                in Float.MIN_VALUE..PaneAnchor.OneThirds.fraction -> PaneAnchor.OneThirds
                in PaneAnchor.OneThirds.fraction..PaneAnchor.TwoThirds.fraction -> PaneAnchor.Half
                in PaneAnchor.TwoThirds.fraction..0.99f -> PaneAnchor.TwoThirds
                else -> PaneAnchor.Full
            }
        }

    private val thumbMutableInteractionSource = MutableInteractionSource()

    val thumbInteractionSource: InteractionSource = thumbMutableInteractionSource

    private val anchoredDraggableState = AnchoredDraggableState(
        initialValue = PaneAnchor.OneThirds,
        anchors = currentAnchors(),
    )

    fun updateMaxWidth(
        density: Density,
        maxWidth: Int,
    ) {
        if (maxWidth == this.maxWidth) return
        this.maxWidth = maxWidth
        val newAnchors = currentAnchors()
        anchoredDraggableState.updateAnchors(
            newAnchors = newAnchors,
            newTarget = newAnchors
                .closestAnchor(anchoredDraggableState.offset)
                .takeUnless(PaneAnchor.Zero::equals)
                ?: defaultOpenAnchorPosition(density),
        )
    }

    fun dispatch(delta: Float) {
        anchoredDraggableState.dispatchRawDelta(delta)
    }

    suspend fun moveTo(anchor: PaneAnchor) = anchoredDraggableState.animateTo(
        targetValue = anchor,
    )

    suspend fun settle() {
        anchoredDraggableState.settle(PaneSpring)
    }

    private fun currentAnchors() = DraggableAnchors {
        PaneAnchor.entries.forEach { it at maxWidth * it.fraction }
    }

    private fun defaultOpenAnchorPosition(density: Density): PaneAnchor {
        val layoutSize = with(density) { maxWidth.toDp() }
        val isExpanded = layoutSize >= SecondaryPaneMinWidthBreakpointDp
        return if (isExpanded) {
            PaneAnchor.Half
        } else {
            PaneAnchor.OneThirds
        }
    }

    companion object {

        internal val MinPaneWidth = 1.dp

        @Composable
        internal fun DraggableThumb(
            splitLayoutState: SplitLayoutState,
            paneAnchorState: PaneAnchorState,
            offset: Dp,
        ) {
            val scope = rememberCoroutineScope()
            val isHovered by paneAnchorState.thumbInteractionSource.collectIsHoveredAsState()
            val isPressed by paneAnchorState.thumbInteractionSource.collectIsPressedAsState()
            val isDragged by paneAnchorState.thumbInteractionSource.collectIsDraggedAsState()
            val active = isHovered || isPressed || isDragged

            val thumbWidth by animateDpAsState(
                label = "App Pane Draggable thumb",
                targetValue =
                if (active) {
                    DraggableDividerSizeDp
                } else {
                    when (paneAnchorState.targetPaneAnchor) {
                        PaneAnchor.Zero -> DraggableDividerSizeDp
                        PaneAnchor.OneThirds,
                        PaneAnchor.Half,
                        PaneAnchor.TwoThirds,
                        PaneAnchor.Full,
                        -> 2.dp
                    }
                },
            )
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = offset.roundToPx() - (DraggableDividerSizeDp / 2).roundToPx(),
                            y = 0,
                        )
                    }
                    .fillMaxHeight()
                    .width(DraggableDividerSizeDp)
                    .hoverable(paneAnchorState.thumbMutableInteractionSource)
                    .anchoredDraggable(
                        state = paneAnchorState.anchoredDraggableState,
                        orientation = Orientation.Horizontal,
                        interactionSource = paneAnchorState.thumbMutableInteractionSource,
                        flingBehavior = AnchoredDraggableDefaults.flingBehavior(
                            state = paneAnchorState.anchoredDraggableState,
                            animationSpec = PaneSpring,
                        ),
                    ),
            ) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(thumbWidth)
                        .height(DraggableDividerSizeDp),
                    shape = DraggableDividerShape,
                    color = MaterialTheme.colorScheme.onSurface,
                    onClick = {
                        scope.launch { paneAnchorState.moveTo(PaneAnchor.OneThirds) }
                    },
                ) {
                    Image(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .graphicsLayer {
                                scaleX = 0.6f
                                scaleY = 0.6f
                                rotationZ = 90f
                            },
                        imageVector = Icons.Default.UnfoldMore,
                        contentDescription = "Drag",
                        colorFilter = ColorFilter.tint(
                            color = MaterialTheme.colorScheme.surface,
                        ),
                    )
                }
            }

            val density = LocalDensity.current

            LaunchedEffect(
                paneAnchorState.width,
                splitLayoutState.size,
                splitLayoutState.weightSum,
                density,
            ) {
                val fullWidthPx = with(density) { splitLayoutState.size.roundToPx() }
                val percentage = paneAnchorState.width.toFloat() / fullWidthPx
                val minPercentage = with(density) { MinPaneWidth.toPx() / fullWidthPx }
                val weight = max(percentage, minPercentage) * splitLayoutState.weightSum
                splitLayoutState.setWeightAt(index = 0, weight = weight)
            }

            LaunchedEffect(active) {
                paneAnchorState.hasInteractions = active
            }
        }
    }
}

/**
 * Maps a back gesture to shutting the secondary pane
 */
@Composable
fun PaneScaffoldState.SecondaryPaneCloseBackHandler() {
    val currentlyEnabled = remember {
        derivedStateOf {
            paneState.pane == ThreePane.Primary &&
                splitPaneState.filteredPaneOrder.size > 1 &&
                appState.dismissBehavior != AppState.DismissBehavior.Gesture.Drag
        }
    }

    val paneAnchorState = splitPaneState.paneAnchorState
    var started by remember { mutableStateOf(false) }
    var widthAtStart by remember { mutableIntStateOf(0) }
    var desiredPaneWidth by remember { mutableFloatStateOf(0f) }

    NavigationEventHandler(
        enabled = remember(currentlyEnabled.value) {
            // Force create a new lambda to work around:
            // https://issuetracker.google.com/issues/431534103
            { currentlyEnabled.value }
        },
    ) { events ->
        try {
            events.collectIndexed { index, event ->
                check(appState.dismissBehavior != AppState.DismissBehavior.Gesture.Drag) {
                    "The secondary pane close back handler should not run when dragging to dismiss"
                }
                if (index == 0) {
                    appState.dismissBehavior = AppState.DismissBehavior.Gesture.Slide
                    widthAtStart = paneAnchorState.width
                    started = true
                }
                val progress = event.progress
                val distanceToCover = paneAnchorState.maxWidth - widthAtStart
                desiredPaneWidth = (progress * distanceToCover) + widthAtStart
            }
        } finally {
            appState.dismissBehavior = AppState.DismissBehavior.None
            started = false
        }
    }

    // Make sure desiredPaneWidth is synced with paneSplitState.width before the back gesture
    LaunchedEffect(Unit) {
        snapshotFlow { started to paneAnchorState.width }
            .collect { (isStarted, paneAnchorStateWidth) ->
                if (isStarted) return@collect
                desiredPaneWidth = paneAnchorStateWidth.toFloat()
            }
    }

    // Dispatch changes as the user presses back
    LaunchedEffect(started, desiredPaneWidth) {
        snapshotFlow { started to desiredPaneWidth }
            .collect { (isStarted, paneWidth) ->
                if (!isStarted) return@collect
                paneAnchorState.dispatch(delta = paneWidth - paneAnchorState.width.toFloat())
            }
    }

    // Fling to settle
    LaunchedEffect(Unit) {
        snapshotFlow { started }
            .collect { isStarted ->
                if (isStarted) return@collect
                paneAnchorState.settle()
            }
    }

    // Pop when fully expanded
    LaunchedEffect(Unit) {
        snapshotFlow { splitPaneState.paneAnchorState.currentPaneAnchor }
            .collect { anchor ->
                if (currentlyEnabled.value && anchor == PaneAnchor.Full) {
                    appState.pop()
                }
            }
    }
}

private val DraggableDividerSizeDp = 48.dp
internal val SecondaryPaneMinWidthBreakpointDp = 600.dp
internal val TertiaryPaneMinWidthBreakpointDp = 1200.dp

private val DraggableDividerShape = RoundedCornerShape(DraggableDividerSizeDp)

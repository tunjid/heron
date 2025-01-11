package com.tunjid.heron.scaffold.scaffold

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.Image
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
import com.tunjid.heron.scaffold.navigation.BackHandler
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val PaneSpring = spring(
    visibilityThreshold = 0.1f
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
internal class PaneAnchorState(
    private val density: Density,
) {
    var maxWidth by mutableIntStateOf(1000)
        internal set
    val width
        get() = max(
            a = 0,
            b = anchoredDraggableState.offset.roundToInt()
        )

    val targetPaneAnchor get() = anchoredDraggableState.targetValue

    var hasInteractions by mutableStateOf(false)
        internal set

    val currentPaneAnchor: PaneAnchor
        get() {
            val cappedFraction = max(
                a = min(
                    a = anchoredDraggableState.requireOffset() / maxWidth,
                    b = 1f
                ),
                b = 0f
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
        positionalThreshold = { distance: Float -> distance * 0.5f },
        velocityThreshold = { 100f },
        snapAnimationSpec = PaneSpring,
        decayAnimationSpec = splineBasedDecay(density)
    )

    val modifier = Modifier
        .hoverable(thumbMutableInteractionSource)
        .anchoredDraggable(
            state = anchoredDraggableState,
            orientation = Orientation.Horizontal,
            interactionSource = thumbMutableInteractionSource,
        )

    fun updateMaxWidth(maxWidth: Int) {
        if (maxWidth == this.maxWidth) return
        this.maxWidth = maxWidth
        val newAnchors = currentAnchors()
        anchoredDraggableState.updateAnchors(
            newAnchors = newAnchors,
            newTarget = newAnchors
                .closestAnchor(anchoredDraggableState.offset)
                .takeUnless(PaneAnchor.Zero::equals)
                ?: defaultOpenAnchorPosition(),
        )
    }

    suspend fun onClosed() {
        if (currentPaneAnchor != PaneAnchor.Full) return
        moveTo(defaultOpenAnchorPosition())
    }

    fun dispatch(delta: Float) {
        anchoredDraggableState.dispatchRawDelta(delta)
    }

    suspend fun completeDispatch() = anchoredDraggableState.settle(velocity = 0f)

    suspend fun moveTo(anchor: PaneAnchor) = anchoredDraggableState.animateTo(
        targetValue = anchor,
    )

    private fun currentAnchors() = DraggableAnchors {
        PaneAnchor.entries.forEach { it at maxWidth * it.fraction }
    }

    private fun defaultOpenAnchorPosition(): PaneAnchor {
        val layoutSize = with(density) { maxWidth.toDp() }
        val isExpanded = layoutSize >= SecondaryPaneMinWidthBreakpointDp
        return if (isExpanded) PaneAnchor.Half
        else PaneAnchor.OneThirds
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
                if (active) DraggableDividerSizeDp
                else when (paneAnchorState.targetPaneAnchor) {
                    PaneAnchor.Zero -> DraggableDividerSizeDp
                    PaneAnchor.OneThirds,
                    PaneAnchor.Half,
                    PaneAnchor.TwoThirds,
                    PaneAnchor.Full,
                        -> 2.dp
                }
            )
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = offset.roundToPx() - (DraggableDividerSizeDp / 2).roundToPx(),
                            y = 0
                        )
                    }
                    .fillMaxHeight()
                    .width(DraggableDividerSizeDp)
                    .then(paneAnchorState.modifier)
            ) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(thumbWidth)
                        .height(DraggableDividerSizeDp),
                    shape = RoundedCornerShape(DraggableDividerSizeDp),
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
                            color = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }

            val density = LocalDensity.current

            LaunchedEffect(
                paneAnchorState.width,
                splitLayoutState.size,
                splitLayoutState.weightSum,
                density
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
fun SecondaryPaneCloseBackHandler(enabled: Boolean) {
    val appState = LocalAppState.current
    val paneAnchorState = appState.paneAnchorState
    var started by remember { mutableStateOf(false) }
    var widthAtStart by remember { mutableIntStateOf(0) }
    var desiredPaneWidth by remember { mutableFloatStateOf(0f) }
    val animatedDesiredPanelWidth by animateFloatAsState(
        label = "DesiredAppPanelWidth",
        targetValue = desiredPaneWidth,
    )

    BackHandler(
        enabled = enabled,
        onStarted = {
            paneAnchorState.hasInteractions = true
            widthAtStart = paneAnchorState.width
            started = true
        },
        onProgressed = { progress ->
            val distanceToCover = paneAnchorState.maxWidth - widthAtStart
            desiredPaneWidth = (progress * distanceToCover) + widthAtStart
        },
        onCancelled = {
            paneAnchorState.hasInteractions = false
            started = false
        },
        onBack = {
            paneAnchorState.hasInteractions = false
            started = false
        }
    )

    // Make sure desiredPaneWidth is synced with paneSplitState.width before the back gesture
    LaunchedEffect(Unit) {
        snapshotFlow { started to paneAnchorState.width }
            .collect { (isStarted, paneAnchorStateWidth) ->
                if (isStarted) return@collect
                desiredPaneWidth = paneAnchorStateWidth.toFloat()
            }
    }

    // Dispatch changes as the user presses back
    LaunchedEffect(started, animatedDesiredPanelWidth) {
        snapshotFlow { started to animatedDesiredPanelWidth }
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
                paneAnchorState.completeDispatch()
            }
    }

    // Pop when fully expanded
    LaunchedEffect(Unit) {
        snapshotFlow { appState.paneAnchorState.currentPaneAnchor }
            .collect { anchor ->
                when(anchor){
                    PaneAnchor.Zero -> Unit
                    PaneAnchor.OneThirds -> Unit
                    PaneAnchor.Half -> Unit
                    PaneAnchor.TwoThirds -> Unit
                    PaneAnchor.Full -> appState.pop()
                }
            }
    }
}

private val DraggableDividerSizeDp = 48.dp
internal val SecondaryPaneMinWidthBreakpointDp = 600.dp
internal val TertiaryPaneMinWidthBreakpointDp = 1200.dp
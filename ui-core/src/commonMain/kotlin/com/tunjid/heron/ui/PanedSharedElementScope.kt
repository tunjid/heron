package com.tunjid.heron.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.SharedTransitionScope.PlaceHolderSize
import androidx.compose.animation.SharedTransitionScope.PlaceHolderSize.Companion.contentSize
import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.animation.core.Spring.StiffnessMediumLow
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementScope
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.configurations.requireThreePaneMovableSharedElementScope
import com.tunjid.treenav.strings.Route

@OptIn(ExperimentalSharedTransitionApi::class)
@Stable
interface SharedElementScope :
    SharedTransitionScope, AnimatedVisibilityScope, MovableSharedElementScope {
    fun Modifier.previewBackSharedElement(
        key: Any,
        boundsTransform: BoundsTransform = DefaultBoundsTransform,
        placeHolderSize: PlaceHolderSize = contentSize,
        renderInOverlayDuringTransition: Boolean = true,
        zIndexInOverlay: Float = 0f,
        clipInOverlayDuringTransition: OverlayClip = ParentClip,
    ): Modifier
}

@Stable
private class PanedSharedElementScope(
    val paneScope: PaneScope<ThreePane, Route>,
    val movableSharedElementScope: MovableSharedElementScope,
) : SharedElementScope,
    AnimatedVisibilityScope by paneScope,
    MovableSharedElementScope by movableSharedElementScope {

    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun Modifier.previewBackSharedElement(
        key: Any,
        boundsTransform: BoundsTransform,
        placeHolderSize: PlaceHolderSize,
        renderInOverlayDuringTransition: Boolean,
        zIndexInOverlay: Float,
        clipInOverlayDuringTransition: OverlayClip,
    ): Modifier = composed {

        when (paneScope.paneState.pane) {
            null -> throw IllegalArgumentException(
                "Shared elements may only be used in non null panes"
            )
            // Allow shared elements in the primary or transient primary content only
            ThreePane.Primary -> {
                val state = rememberSharedContentState(key)
                when {
                    paneScope.isPreviewingBack -> sharedElementWithCallerManagedVisibility(
                        sharedContentState = state,
                        visible = !state.isMatchFound,
                        boundsTransform = boundsTransform,
                        placeHolderSize = placeHolderSize,
                        renderInOverlayDuringTransition = renderInOverlayDuringTransition,
                        zIndexInOverlay = zIndexInOverlay,
                        clipInOverlayDuringTransition = clipInOverlayDuringTransition,
                    )
                    // Share the element
                    else -> sharedElementWithCallerManagedVisibility(
                        sharedContentState = rememberSharedContentState(key),
                        visible = true,
                        boundsTransform = boundsTransform,
                        placeHolderSize = placeHolderSize,
                        renderInOverlayDuringTransition = renderInOverlayDuringTransition,
                        zIndexInOverlay = zIndexInOverlay,
                        clipInOverlayDuringTransition = clipInOverlayDuringTransition,
                    )
                }
            }
            // Share the element when in the transient pane
            ThreePane.TransientPrimary -> sharedElement(
                state = rememberSharedContentState(key),
                animatedVisibilityScope = paneScope,
                boundsTransform = boundsTransform,
                placeHolderSize = placeHolderSize,
                renderInOverlayDuringTransition = renderInOverlayDuringTransition,
                zIndexInOverlay = zIndexInOverlay,
                clipInOverlayDuringTransition = clipInOverlayDuringTransition,
            )

            // In the other panes use the element as is
            ThreePane.Secondary,
            ThreePane.Tertiary,
            ThreePane.Overlay,
                -> this
        }
    }
}

@Composable
fun PaneScope<
        ThreePane,
        Route
        >.requirePanedSharedElementScope(): SharedElementScope =
    remember {
        PanedSharedElementScope(
            paneScope = this,
            movableSharedElementScope = requireThreePaneMovableSharedElementScope()
        )
    }

@ExperimentalSharedTransitionApi
private val ParentClip: OverlayClip =
    object : OverlayClip {
        override fun getClipPath(
            state: SharedContentState,
            bounds: Rect,
            layoutDirection: LayoutDirection,
            density: Density,
        ): Path? {
            return state.parentSharedContentState?.clipPathInOverlay
        }
    }

@ExperimentalSharedTransitionApi
private val DefaultBoundsTransform = BoundsTransform { _, _ -> DefaultSpring }

private val DefaultSpring = spring(
    stiffness = StiffnessMediumLow,
    visibilityThreshold = Rect.VisibilityThreshold
)

private val PaneScope<ThreePane, *>.isPreviewingBack: Boolean
    get() = paneState.pane == ThreePane.Primary
            && paneState.adaptations.contains(ThreePane.PrimaryToTransient)
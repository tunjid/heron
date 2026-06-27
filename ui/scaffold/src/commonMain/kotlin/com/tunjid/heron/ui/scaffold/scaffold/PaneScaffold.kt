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

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.tunjid.composables.backpreview.BackPreviewState
import com.tunjid.composables.constrainedsize.constrainedSizePlacement
import com.tunjid.composables.ui.skipIf
import com.tunjid.heron.ui.PaneTransitionScope
import com.tunjid.heron.ui.UiTokens.withDim
import com.tunjid.heron.ui.modifiers.blockClickEvents
import com.tunjid.heron.ui.modifiers.blur
import com.tunjid.heron.ui.modifiers.ifTrue
import com.tunjid.heron.ui.scaffold.identity.isSignedIn
import com.tunjid.heron.ui.scaffold.identity.isStable
import com.tunjid.heron.ui.scaffold.identity.prefersAutoHidingBottomNav
import com.tunjid.heron.ui.scaffold.identity.prefersCompactBottomNav
import com.tunjid.heron.ui.scaffold.scaffold.components.NonSubComposingScaffold
import com.tunjid.heron.ui.stateproduction.RouteViewModel
import com.tunjid.heron.ui.stateproduction.ViewModelInitializer
import com.tunjid.heron.ui.stateproduction.viewModelCoroutineScope
import com.tunjid.heron.ui.text.Memo
import com.tunjid.heron.ui.text.message
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.ThreePaneMovableElementSharedTransitionScope
import com.tunjid.treenav.compose.threepane.rememberThreePaneMovableElementSharedTransitionScope
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Stable
class PaneScaffoldState(
    internal val displayScaffoldState: DisplayScaffoldState,
    viewModelInitializer: ViewModelInitializer,
    paneMovableElementSharedTransitionScope: ThreePaneMovableElementSharedTransitionScope<Route>,
) : PaneTransitionScope,
    ViewModelInitializer by viewModelInitializer,
    ThreePaneMovableElementSharedTransitionScope<Route> by paneMovableElementSharedTransitionScope {

    override val childBoundsTransform: BoundsTransform = { _, _ ->
        BoundsTransformSpring.skipIf {
            displayScaffoldState.dismissBehavior is DisplayScaffoldState.DismissBehavior.Gesture.DragToPop ||
                displayScaffoldState.paneAnchorState.hasInteractions
        }
    }

    internal val snackbarHostState = SnackbarHostState()

    internal val snackbarMessages = mutableStateListOf<Memo>()

    val isMediumScreenWidthOrWider: Boolean
        get() = displayScaffoldState.isMediumScreenWidthOrWider

    internal val dismissBehavior: DisplayScaffoldState.DismissBehavior
        get() = displayScaffoldState.dismissBehavior

    val isSignedOut
        get() = !displayScaffoldState.staticStates.identityState.isSignedIn

    val isSignedIn
        get() = displayScaffoldState.staticStates.identityState.isSignedIn

    val prefersCompactBottomNav
        get() = displayScaffoldState.staticStates.identityState.prefersCompactBottomNav

    val prefersAutoHidingBottomNav
        get() = displayScaffoldState.staticStates.identityState.prefersAutoHidingBottomNav

    internal val nestedNavigationState = PaneNestedNavigationState(
        paneScaffoldState = this,
    )

    internal val canShowNavigationBar: Boolean
        get() = !isMediumScreenWidthOrWider

    internal val canUseMovableNavigationBar: Boolean
        get() = isActive && canShowNavigationBar

    internal val canShowNavigationRail: Boolean
        get() = displayScaffoldState.filteredPaneOrder.firstOrNull() == paneState.pane &&
            isMediumScreenWidthOrWider

    internal val canUseMovableNavigationRail: Boolean
        get() = isActive && canShowNavigationRail

    internal val canShowFab
        get() = when (paneState.pane) {
            ThreePane.Primary -> true
            ThreePane.Secondary -> false
            ThreePane.Tertiary -> false
            ThreePane.Overlay -> false
            null -> false
        }

    internal val backPreviewState = BackPreviewState(
        minScale = 0.75f,
    )

    internal val defaultContainerColor: Color
        @Composable get() {
            val elevation by animateDpAsState(
                if (paneState.pane == ThreePane.Primary &&
                    isActive &&
                    inPredictiveBack
                ) 4.dp
                else 0.dp,
            )

            return MaterialTheme.colorScheme.surfaceColorAtElevation(elevation)
        }

    interface NestedNavigationKey {
        val isRoot: Boolean
    }
}

@Composable
inline fun <reified VM : RouteViewModel> PaneScaffoldState.rememberRouteViewModel(
    route: Route,
): VM = viewModel<VM> {
    routeViewModelInitializer(VM::class).invoke(
        scope = viewModelCoroutineScope(),
        route = route,
    ) as VM
}

@Composable
fun PaneScope<ThreePane, Route>.rememberPaneScaffoldState(
    displayScaffoldState: DisplayScaffoldState = LocalDisplayScaffoldState.current,
    viewModelInitializer: ViewModelInitializer = LocalAppState.current.viewModelInitializer,
    paneMovableElementSharedTransitionScope: ThreePaneMovableElementSharedTransitionScope<Route> = rememberThreePaneMovableElementSharedTransitionScope(),
): PaneScaffoldState {
    val paneScaffoldState = remember(
        displayScaffoldState,
        viewModelInitializer,
        paneMovableElementSharedTransitionScope,
    ) {
        PaneScaffoldState(
            displayScaffoldState = displayScaffoldState,
            viewModelInitializer = viewModelInitializer,
            paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
        )
    }

    val scope = rememberCoroutineScope()

    val navigationEventDispatcher =
        LocalNavigationEventDispatcherOwner.current!!
            .navigationEventDispatcher

    LifecycleStartEffect(
        key1 = scope,
        key2 = navigationEventDispatcher,
    ) {
        val job = scope.launch {
            navigationEventDispatcher.transitionState
                .collectLatest { eventState ->
                    when (eventState) {
                        is NavigationEventTransitionState.Idle -> {
                            // Wait to be visible
                            snapshotFlow { paneScaffoldState.transition.targetState }
                                .first { it == EnterExitState.Visible }
                            paneScaffoldState.backPreviewState.progress = 0f
                        }
                        is NavigationEventTransitionState.InProgress -> paneScaffoldState.backPreviewState.apply {
                            progress = eventState.latestEvent.progress
                            atStart = eventState.latestEvent.swipeEdge == NavigationEvent.EDGE_LEFT
                            pointerOffset = Offset(
                                x = eventState.latestEvent.touchX,
                                y = eventState.latestEvent.touchY,
                            ).round()
                        }
                    }
                }
        }
        onStopOrDispose { job.cancel() }
    }
    return paneScaffoldState
}

@Composable
fun PaneScaffoldState.PaneScaffold(
    modifier: Modifier = Modifier,
    showNavigation: Boolean = true,
    containerColor: Color = defaultContainerColor,
    snackBarMessages: List<Memo> = emptyList(),
    onSnackBarMessageConsumed: (Memo) -> Unit = {},
    topBar: @Composable PaneScaffoldState.() -> Unit = {},
    snackBarHost: @Composable PaneScaffoldState.() -> Unit = { PaneSnackbarHost() },
    floatingActionButton: @Composable PaneScaffoldState.() -> Unit = {},
    navigationBar: @Composable PaneScaffoldState.() -> Unit = {},
    navigationRail: @Composable PaneScaffoldState.() -> Unit = {},
    content: @Composable PaneScaffoldState.(PaddingValues) -> Unit,
) {
    PaneNavigationRailScaffold(
        modifier = modifier,
        navigationRail = {
            navigationRail()
        },
        content = {
            NonSubComposingScaffold(
                modifier = when {
                    displayScaffoldState.paneAnchorState.hasInteractions -> Modifier
                    else -> when (dismissBehavior) {
                        DisplayScaffoldState.DismissBehavior.None,
                        DisplayScaffoldState.DismissBehavior.Gesture.DragToPop,
                        -> Modifier.animateBounds(
                            lookaheadScope = this,
                            boundsTransform = childBoundsTransform,
                        )

                        DisplayScaffoldState.DismissBehavior.Gesture.SlideToPop,
                        DisplayScaffoldState.DismissBehavior.Gesture.ScaleToPop,
                        -> Modifier
                    }
                },
                containerColor = containerColor,
                topBar = {
                    topBar()
                },
                floatingActionButton = {
                    floatingActionButton()
                },
                bottomBar = {
                    navigationBar()
                },
                snackbarHost = {
                    snackBarHost()
                },
                content = { paddingValues ->
                    val isStable = displayScaffoldState.staticStates.identityState.isStable
                    val blurState = animateFloatAsState(
                        if (isStable) 0f else 1f,
                    )
                    Box(
                        modifier = Modifier
                            .ifTrue(
                                predicate = !isStable,
                                block = {
                                    blockClickEvents()
                                        .drawWithContent {
                                            drawContent()
                                            drawRect(Color.Black.withDim(true))
                                        }
                                },
                            )
                            .blur(
                                shape = PaneClipShape,
                                radius = { 32.dp },
                                progress = blurState::value,
                            )
                            .constrainedSizePlacement(
                                orientation = Orientation.Horizontal,
                                minSize = displayScaffoldState.minPaneWidth,
                                atStart = paneState.pane == ThreePane.Secondary,
                            ),
                    ) {
                        PersistentSharedElement()
                        content(paddingValues)
                    }
                },
            )
        },
    )
    SnackbarConsumptionEffect()
    SnackbarDisplayEffect(
        messages = snackBarMessages,
        onMessageConsumed = onSnackBarMessageConsumed,
    )

    if (paneState.pane == ThreePane.Primary) {
        LaunchedEffect(showNavigation) {
            displayScaffoldState.showNavigation = showNavigation
        }
    }
}

@Composable
fun PaneScaffoldState.PaneSnackbarHost(
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        modifier = modifier,
        hostState = snackbarHostState,
        snackbar = { data ->
            Snackbar(
                snackbarData = data,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        },
    )
}

@Composable
fun PaneScaffoldState.SnackbarDisplayEffect(
    messages: List<Memo>,
    onMessageConsumed: (Memo) -> Unit,
) {
    val incomingState = remember {
        mutableStateOf(
            value = messages.firstOrNull(),
            // This is so consecutive identical messages are
            // seen as distinct and do not halt processing
            policy = referentialEqualityPolicy(),
        )
    }.apply { value = messages.firstOrNull() }
    val onConsumedState = rememberUpdatedState(onMessageConsumed)

    LaunchedEffect(this) {
        snapshotFlow { incomingState.value }
            .collect { incoming ->
                if (incoming == null) return@collect
                snackbarMessages += incoming
                onConsumedState.value(incoming)
            }
    }
}

@Composable
private fun PaneScaffoldState.SnackbarConsumptionEffect() {
    LaunchedEffect(this) {
        snapshotFlow { snackbarMessages.isNotEmpty() }
            .filter { it }
            .collect {
                while (isActive) {
                    val message = snackbarMessages.firstOrNull() ?: break
                    snackbarHostState.showSnackbar(message.message())
                    snackbarMessages.remove(message)
                }
            }
    }
}

@Composable
private inline fun PaneNavigationRailScaffold(
    modifier: Modifier = Modifier,
    navigationRail: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier,
        content = {
            Box(
                modifier = Modifier
                    .widthIn(max = 80.dp)
                    .zIndex(2f),
                content = {
                    navigationRail()
                },
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f),
                content = {
                    content()
                },
            )
        },
    )
}

fun Modifier.paneClip() =
    then(PaneClipModifier)

/**
 * A workaround for https://issuetracker.google.com/issues/498497888
 * Sticky shared elements which use Modifier.sharedElementWithUserManagedVisibility
 * need an accompanying Modifier.sharedElement match to animate if the shared element
 * transition is sought. The bug does not affect the transition if it is not seeking.
 */
@Composable
private fun PaneScaffoldState.PersistentSharedElement() {
    if (paneState.pane == ThreePane.Primary) Box(
        Modifier
            .sharedElement(
                sharedContentState = rememberSharedContentState(PersistentSharedElementKey),
                animatedVisibilityScope = this,
            )
            .size(1.dp),
    )
}

private val PaneClipShape = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 16.dp,
)

private val PaneClipModifier = Modifier.clip(
    shape = PaneClipShape,
)

private val BoundsTransformSpring = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = Rect.VisibilityThreshold,
)

private object PersistentSharedElementKey

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

import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tunjid.composables.constrainedsize.constrainedSizePlacement
import com.tunjid.heron.ui.text.Memo
import com.tunjid.heron.ui.text.message
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.ThreePaneMovableElementSharedTransitionScope
import com.tunjid.treenav.compose.threepane.rememberThreePaneMovableElementSharedTransitionScope
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.flow.filterNotNull

class PaneScaffoldState internal constructor(
    internal val appState: AppState,
    internal val splitPaneState: SplitPaneState,
    paneMovableElementSharedTransitionScope: ThreePaneMovableElementSharedTransitionScope<Route>,
) : ThreePaneMovableElementSharedTransitionScope<Route> by paneMovableElementSharedTransitionScope {

    internal val snackbarHostState = SnackbarHostState()

    val isMediumScreenWidthOrWider: Boolean
        get() = splitPaneState.isMediumScreenWidthOrWider

    val dismissBehavior: AppState.DismissBehavior
        get() = appState.dismissBehavior

    val isSignedOut
        get() = !appState.isSignedIn

    val isSignedIn
        get() = appState.isSignedIn

    val prefersCompactBottomNav
        get() = appState.prefersCompactBottomNav

    val prefersAutoHidingBottomNav
        get() = appState.prefersAutoHidingBottomNav

    internal val canShowNavigationBar: Boolean
        get() = !isMediumScreenWidthOrWider

    internal val canUseMovableNavigationBar: Boolean
        get() = isActive && canShowNavigationBar

    internal val canShowNavigationRail: Boolean
        get() = splitPaneState.filteredPaneOrder.firstOrNull() == paneState.pane &&
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

    internal val hasSiblings
        get() = splitPaneState.filteredPaneOrder.size > 1

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
}

@Composable
fun PaneScope<ThreePane, Route>.rememberPaneScaffoldState(): PaneScaffoldState {
    val appState = LocalAppState.current
    val splitPaneState = LocalSplitPaneState.current
    val paneMovableElementSharedTransitionScope =
        rememberThreePaneMovableElementSharedTransitionScope()
    return remember(
        appState,
        splitPaneState,
        paneMovableElementSharedTransitionScope,
    ) {
        PaneScaffoldState(
            appState = appState,
            splitPaneState = splitPaneState,
            paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
        )
    }
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
        modifier = modifier
            .constrainedSizePlacement(
                orientation = Orientation.Horizontal,
                minSize = splitPaneState.minPaneWidth,
                atStart = paneState.pane == ThreePane.Secondary,
            ),
        navigationRail = {
            navigationRail()
        },
        content = {
            Scaffold(
                modifier = if (splitPaneState.paneAnchorState.hasInteractions) Modifier
                else when (dismissBehavior) {
                    AppState.DismissBehavior.None,
                    AppState.DismissBehavior.Gesture.DragToPop,
                    -> Modifier.animateBounds(lookaheadScope = this)
                    AppState.DismissBehavior.Gesture.SlideToPop,
                    AppState.DismissBehavior.Gesture.ScaleToPop,
                    -> Modifier
                }
                    .padding(
                        horizontal = if (hasSiblings) 8.dp else 0.dp,
                    ),
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
                    content(paddingValues)
                },
            )
        },
    )
    val updatedMessages = rememberUpdatedState(snackBarMessages.firstOrNull())
    LaunchedEffect(Unit) {
        snapshotFlow { updatedMessages.value }
            .filterNotNull()
            .collect { message ->
                val text = message.message()
                snackbarHostState.showSnackbar(
                    message = text,
                )
                onSnackBarMessageConsumed(message)
            }
    }

    if (paneState.pane == ThreePane.Primary) {
        LaunchedEffect(showNavigation) {
            appState.showNavigation = showNavigation
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
    )
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

private val PaneClipModifier = Modifier.clip(
    shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
    ),
)

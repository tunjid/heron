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

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
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
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.ThreePaneMovableElementSharedTransitionScope
import com.tunjid.treenav.compose.threepane.rememberThreePaneMovableElementSharedTransitionScope
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull

class  PaneScaffoldState internal constructor(
    internal val appState: AppState,
    internal val splitPaneState: SplitPaneState,
    paneMovableElementSharedTransitionScope: ThreePaneMovableElementSharedTransitionScope<Route>,
) : ThreePaneMovableElementSharedTransitionScope<Route> by paneMovableElementSharedTransitionScope {

    val isMediumScreenWidthOrWider: Boolean
        get() = splitPaneState.isMediumScreenWidthOrWider

    val dismissBehavior: AppState.DismissBehavior
        get() = appState.dismissBehavior

    internal val canShowNavigationBar: Boolean
        get() = !isMediumScreenWidthOrWider

    internal val canUseMovableNavigationBar: Boolean
        get() = isActive && canShowNavigationBar

    internal val canShowNavigationRail: Boolean
        get() = splitPaneState.filteredPaneOrder.firstOrNull() == paneState.pane
                && isMediumScreenWidthOrWider

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
                if (paneState.pane == ThreePane.Primary
                    && isActive
                    && inPredictiveBack
                ) 4.dp
                else 0.dp
            )

            return MaterialTheme.colorScheme.surfaceColorAtElevation(elevation)
        }
}

@Composable
fun PaneScope<ThreePane, Route>.rememberPaneScaffoldState(): PaneScaffoldState {
    val appState = LocalAppState.current
    val splitPaneDisplayScope = LocalSplitPaneState.current
    val paneMovableElementSharedTransitionScope =
        rememberThreePaneMovableElementSharedTransitionScope()
    return remember(appState, splitPaneDisplayScope) {
        PaneScaffoldState(
            appState = appState,
            splitPaneState = splitPaneDisplayScope,
            paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PaneScaffoldState.PaneScaffold(
    modifier: Modifier = Modifier,
    showNavigation: Boolean = true,
    containerColor: Color = defaultContainerColor,
    snackBarMessages: List<String> = emptyList(),
    onSnackBarMessageConsumed: (String) -> Unit = {},
    topBar: @Composable PaneScaffoldState.() -> Unit = {},
    floatingActionButton: @Composable PaneScaffoldState.() -> Unit = {},
    navigationBar: @Composable PaneScaffoldState.() -> Unit = {},
    navigationRail: @Composable PaneScaffoldState.() -> Unit = {},
    content: @Composable PaneScaffoldState.(PaddingValues) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
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
                    AppState.DismissBehavior.Gesture.Drag -> Modifier
                        .animateBounds(
                            lookaheadScope = this,
                            boundsTransform = remember {
                                scaffoldBoundsTransform(
                                    paneScaffoldState = this,
                                )
                            }
                        )
                    AppState.DismissBehavior.Gesture.Slide -> Modifier
                }
                    .padding(
                        horizontal = if (hasSiblings) 8.dp else 0.dp
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
                    SnackbarHost(snackbarHostState)
                },
                content = { paddingValues ->
                    content(paddingValues)
                },
            )
        }
    )
    val updatedMessages = rememberUpdatedState(snackBarMessages.firstOrNull())
    LaunchedEffect(Unit) {
        snapshotFlow { updatedMessages.value }
            .filterNotNull()
            .filterNot(String::isNullOrBlank)
            .collect { message ->
                snackbarHostState.showSnackbar(
                    message = message
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
                }
            )
        },
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
private fun scaffoldBoundsTransform(
    paneScaffoldState: PaneScaffoldState,
): BoundsTransform = BoundsTransform { _, _ ->
    when (paneScaffoldState.paneState.pane) {
        ThreePane.Primary,
        ThreePane.Secondary,
        ThreePane.Tertiary,
            -> if (paneScaffoldState.splitPaneState.paneAnchorState.hasInteractions) snap()
        else spring()

        ThreePane.Overlay,
        null,
            -> snap()
    }
}

fun Modifier.paneClip() =
    then(PaneClipModifier)

private val PaneClipModifier = Modifier.clip(
    shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
    )
)
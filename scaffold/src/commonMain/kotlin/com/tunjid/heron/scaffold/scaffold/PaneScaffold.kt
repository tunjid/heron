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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntSize
import androidx.compose.ui.zIndex
import com.tunjid.composables.ui.skipIf
import com.tunjid.heron.ui.PanedSharedElementScope
import com.tunjid.heron.ui.requirePanedSharedElementScope
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlin.math.abs

class PaneScaffoldState internal constructor(
    private val appState: AppState,
    panedSharedElementScope: PanedSharedElementScope,
) : PanedSharedElementScope by panedSharedElementScope {
    val isMediumScreenWidthOrWider get() = appState.isMediumScreenWidthOrWider

    internal val canShowBottomNavigation get() = !appState.isMediumScreenWidthOrWider

    internal val canShowNavRail
        get() = appState.filteredPaneOrder.firstOrNull() == paneState.pane
                && appState.isMediumScreenWidthOrWider

    internal val canShowFab
        get() = when (paneState.pane) {
            ThreePane.Primary -> true
            ThreePane.TransientPrimary -> true
            ThreePane.Secondary -> false
            ThreePane.Tertiary -> false
            ThreePane.Overlay -> false
            null -> false
        }

    internal var scaffoldTargetSize by mutableStateOf(IntSize.Zero)
    internal var scaffoldCurrentSize by mutableStateOf(IntSize.Zero)

    internal fun hasMatchedSize(): Boolean =
        abs(scaffoldCurrentSize.width - scaffoldTargetSize.width) <= 2
                && abs(scaffoldCurrentSize.height - scaffoldTargetSize.height) <= 2
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PaneScope<ThreePane, Route>.PaneScaffold(
    modifier: Modifier = Modifier,
    showNavigation: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.background,
    snackBarMessages: List<String> = emptyList(),
    onSnackBarMessageConsumed: (String) -> Unit,
    topBar: @Composable PaneScaffoldState.() -> Unit = {},
    floatingActionButton: @Composable PaneScaffoldState.() -> Unit = {},
    navigationBar: @Composable PaneScaffoldState.() -> Unit = {},
    navigationRail: @Composable PaneScaffoldState.() -> Unit = {},
    content: @Composable PaneScaffoldState.(PaddingValues) -> Unit,
) {
    val appState = LocalAppState.current
    val snackbarHostState = remember { SnackbarHostState() }
    val panedSharedElementScope = requirePanedSharedElementScope()
    val paneScaffoldState = remember(appState, panedSharedElementScope) {
        PaneScaffoldState(
            appState = appState,
            panedSharedElementScope = panedSharedElementScope,
        )
    }

    PaneScaffold(
        modifier = modifier,
        navigationRail = {
            if (paneScaffoldState.canShowNavRail) Box(
                modifier = Modifier
                    .zIndex(2f),
            ) {
                paneScaffoldState.navigationRail()
            }
        },
        content = {
            Scaffold(
                modifier = Modifier
                    .animateBounds(
                        lookaheadScope = panedSharedElementScope,
                        boundsTransform = remember {
                            scaffoldBoundsTransform(
                                appState = appState,
                                paneScaffoldState = paneScaffoldState,
                            )
                        }
                    )
                    .padding(
                        horizontal = if (appState.filteredPaneOrder.size > 1) 8.dp else 0.dp
                    )
                    .onSizeChanged {
                        paneScaffoldState.scaffoldCurrentSize = it
                    },
                containerColor = containerColor,
                topBar = {
                    paneScaffoldState.topBar()
                },
                floatingActionButton = {
                    AnimatedVisibility(
                        visible = paneScaffoldState.canShowFab,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it }),
                        content = {
                            paneScaffoldState.floatingActionButton()
                        },
                    )
                },
                bottomBar = {
                    AnimatedVisibility(
                        visible = paneScaffoldState.canShowBottomNavigation,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it }),
                        content = {
                            paneScaffoldState.navigationBar()
                        },
                    )
                },
                snackbarHost = {
                    SnackbarHost(snackbarHostState)
                },
                content = { paddingValues ->
                    paneScaffoldState.content(paddingValues)
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
private inline fun PaneScaffold(
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
            ) {
                navigationRail()
            }
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
    appState: AppState,
    paneScaffoldState: PaneScaffoldState,
): BoundsTransform = BoundsTransform { _, targetBounds ->
    paneScaffoldState.scaffoldTargetSize =
        targetBounds.size.roundToIntSize()

    when (paneScaffoldState.paneState.pane) {
        ThreePane.Primary,
        ThreePane.Secondary,
        ThreePane.Tertiary,
            -> if (appState.paneAnchorState.hasInteractions) snap()
        else spring()

        ThreePane.TransientPrimary,
            -> spring<Rect>().skipIf(paneScaffoldState::hasMatchedSize)

        ThreePane.Overlay,
        null,
            -> snap()
    }
}

val ToolbarHeight = 64.dp

val TabsHeight = 48.dp

val StatusBarHeight: Dp
    @Composable get() = with(LocalDensity.current) {
        statusBarHeight
    }

val BottomNavHeight: Dp = 80.dp

val Density.statusBarHeight: Dp
    @Composable get() {
        val statusBarInsets = WindowInsets.statusBars
        return statusBarInsets.run {
            getTop(this@statusBarHeight) + getBottom(this@statusBarHeight)
        }.toDp()
    }

fun Modifier.paneClip() =
    then(PaneClipModifier)

private val PaneClipModifier = Modifier.clip(
    shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
    )
)
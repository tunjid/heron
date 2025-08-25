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

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.round
import androidx.navigationevent.NavigationEvent
import com.tunjid.composables.splitlayout.SplitLayout
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.scaffold.scaffold.PaneAnchorState.Companion.DraggableThumb
import com.tunjid.heron.scaffold.ui.theme.AppTheme
import com.tunjid.treenav.compose.MultiPaneDisplay
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementHostState
import com.tunjid.treenav.compose.navigation3.ui.NavigationEventHandler
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.panedecorators.threePaneAdaptiveDecorator
import com.tunjid.treenav.compose.threepane.panedecorators.threePaneMovableSharedElementDecorator
import com.tunjid.treenav.strings.Route

/**
 * Root scaffold for the app
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun App(
    modifier: Modifier,
    appState: AppState,
) {
    AppTheme {
        CompositionLocalProvider(
            LocalAppState provides appState,
            LocalVideoPlayerController provides appState.videoPlayerController,
        ) {
            Surface {
                // Root LookaheadScope used to anchor all shared element transitions
                SharedTransitionLayout(
                    modifier = modifier.fillMaxSize(),
                ) {
                    val density = LocalDensity.current
                    val movableSharedElementHostState = remember {
                        MovableSharedElementHostState<ThreePane, Route>(
                            sharedTransitionScope = this,
                        )
                    }
                    val windowWidth = rememberUpdatedState(
                        with(density) {
                            LocalWindowInfo.current.containerSize.width.toDp()
                        },
                    )
                    if (!sharedElementsCoordinatesSet()) return@SharedTransitionLayout

                    val displayState = appState.rememberMultiPaneDisplayState(
                        paneDecorators = remember {
                            listOf(
                                threePaneAdaptiveDecorator(
                                    secondaryPaneBreakPoint = mutableStateOf(
                                        SecondaryPaneMinWidthBreakpointDp,
                                    ),
                                    tertiaryPaneBreakPoint = mutableStateOf(
                                        TertiaryPaneMinWidthBreakpointDp,
                                    ),
                                    windowWidthState = windowWidth,
                                ),
                                threePaneMovableSharedElementDecorator(
                                    movableSharedElementHostState,
                                ),
                            )
                        },
                    )
                    MultiPaneDisplay(
                        modifier = Modifier.fillMaxSize(),
                        state = displayState,
                    ) {
                        val splitPaneState = remember {
                            SplitPaneState(
                                paneNavigationState = paneNavigationState,
                                density = density,
                                windowWidth = windowWidth,
                            )
                        }.also {
                            it.update(
                                paneNavigationState = paneNavigationState,
                                density = density,
                            )
                        }
                        CompositionLocalProvider(
                            LocalSplitPaneState provides splitPaneState,
                        ) {
                            SplitLayout(
                                state = splitPaneState.splitLayoutState,
                                modifier = modifier
                                    .fillMaxSize(),
                                itemSeparators = { _, offset ->
                                    DraggableThumb(
                                        splitLayoutState = splitPaneState.splitLayoutState,
                                        paneAnchorState = splitPaneState.paneAnchorState,
                                        offset = offset,
                                    )
                                },
                                itemContent = { index ->
                                    Destination(splitPaneState.filteredPaneOrder[index])
                                },
                            )
                        }
                        NavigationEventHandler(
                            enabled = displayState::canPop,
                            passThrough = true,
                        ) { progress ->
                            try {
                                progress.collect { event ->
                                    appState.backPreviewState.progress = event.progress
                                    appState.backPreviewState.atStart =
                                        event.swipeEdge == NavigationEvent.EDGE_LEFT
                                    appState.backPreviewState.pointerOffset =
                                        Offset(event.touchX, event.touchY).round()
                                }
                                appState.backPreviewState.progress = 0f
                            } finally {
                                appState.backPreviewState.progress = 0f
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun sharedElementsCoordinatesSet(): Boolean {
    var coordinatesSet by remember {
        mutableStateOf(false)
    }
    Spacer(
        Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                if (coordinates != null && isLookingAhead) {
                    coordinatesSet = true
                }
                placeable.place(0, 0)
            }
        },
    )
    return coordinatesSet
}

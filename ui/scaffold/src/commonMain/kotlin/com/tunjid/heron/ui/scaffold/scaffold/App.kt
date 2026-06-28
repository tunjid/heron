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

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.tunjid.composables.splitlayout.SplitLayout
import com.tunjid.heron.images.LocalImageLoader
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.scaffold.scaffold.AppScaffoldState.StaticStates.Companion.rememberMultiPaneDisplayState
import com.tunjid.heron.ui.scaffold.scaffold.AppState.Companion.staticStates
import com.tunjid.heron.ui.scaffold.scaffold.PaneAnchorState.Companion.DraggableThumb
import com.tunjid.heron.ui.scaffold.ui.theme.AppTheme
import com.tunjid.heron.ui.scaffold.ui.theme.DarkThemeConfig
import com.tunjid.heron.ui.scaffold.ui.theme.Theme
import com.tunjid.treenav.compose.MovableSharedElementHostState
import com.tunjid.treenav.compose.MultiPaneDisplay
import com.tunjid.treenav.compose.PaneEntry
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.panedecorators.threePaneAdaptiveDecorator
import com.tunjid.treenav.compose.threepane.panedecorators.threePaneMovableSharedElementDecorator
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine

/**
 * Root scaffold for the app
 */
@Composable
fun App(
    modifier: Modifier,
    appState: AppState,
) {
    val staticStates = remember(appState) {
        appState.staticStates()
    }
    AppScaffold(
        modifier = modifier,
        staticStates = staticStates,
        entryDecorator = appState.splashVisibilityNavEntryDecorator,
        entryProvider = appState::entry,
    )
}

@Composable
fun AppScaffold(
    modifier: Modifier,
    staticStates: AppScaffoldState.StaticStates,
    entryDecorator: NavEntryDecorator<Route>? = null,
    entryProvider: (Route) -> PaneEntry<ThreePane, Route>,
) {
    val localPrefs = staticStates.identityState.preferences?.local
    AppTheme(
        useDarkTheme = when (
            DarkThemeConfig.fromOrdinal(localPrefs?.darkThemeConfigOrdinal ?: 0)
        ) {
            DarkThemeConfig.System -> isSystemInDarkTheme()
            DarkThemeConfig.Light -> false
            DarkThemeConfig.Dark -> true
        },
        theme = Theme.fromOrdinal(localPrefs?.currentThemeOrdinal ?: 0),
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

                val saveableStateHolderNavEntryDecorator =
                    rememberSaveableStateHolderNavEntryDecorator<Route>()
                val viewModelStoreNavEntryDecorator =
                    rememberViewModelStoreNavEntryDecorator<Route>()

                val displayState = staticStates.rememberMultiPaneDisplayState(
                    paneDecorators = remember(
                        movableSharedElementHostState,
                    ) {
                        listOf(
                            threePaneAdaptiveDecorator(
                                secondaryPaneBreakPoint = mutableStateOf(
                                    UiTokens.SecondaryPaneMinWidthBreakpoint,
                                ),
                                tertiaryPaneBreakPoint = mutableStateOf(
                                    UiTokens.TertiaryPaneMinWidthBreakpoint,
                                ),
                                windowWidthState = windowWidth,
                            ),
                            threePaneMovableSharedElementDecorator(
                                movableSharedElementHostState,
                            ),
                        )
                    },
                    entryDecorators = remember(
                        saveableStateHolderNavEntryDecorator,
                        viewModelStoreNavEntryDecorator,
                        entryDecorator,
                    ) {
                        listOfNotNull(
                            saveableStateHolderNavEntryDecorator,
                            viewModelStoreNavEntryDecorator,
                            entryDecorator,
                        )
                    },
                    entryProvider = entryProvider,
                )
                MultiPaneDisplay(
                    modifier = Modifier.fillMaxSize(),
                    state = displayState,
                ) {
                    val displayScope = this
                    val appScaffoldState = remember(
                        staticStates,
                        displayScope,
                    ) {
                        AppScaffoldState(
                            paneNavigationState = { displayScope.paneNavigationState },
                            density = density,
                            windowWidth = windowWidth,
                            staticStates = staticStates,
                        )
                    }.also {
                        it.update(
                            density = density,
                        )
                    }
                    CompositionLocalProvider(
                        LocalAppScaffoldState provides appScaffoldState,
                        LocalImageLoader provides staticStates.imageLoader,
                        LocalVideoPlayerController provides staticStates.videoPlayerController,
                    ) {
                        SplitLayout(
                            state = appScaffoldState.splitLayoutState,
                            modifier = Modifier
                                .fillMaxSize(),
                            itemSeparators = { _, offset ->
                                DraggableThumb(
                                    splitLayoutState = appScaffoldState.splitLayoutState,
                                    paneAnchorState = appScaffoldState.paneAnchorState,
                                    offset = offset,
                                )
                            },
                            itemContent = { index ->
                                Destination(appScaffoldState.filteredPaneOrder[index])
                            },
                        )
                    }
                    LaunchedEffect(
                        appScaffoldState,
                        displayScope,
                    ) {
                        snapshotFlow {
                            appScaffoldState.paneAnchorState.currentPaneAnchor
                        }.collect { anchor ->
                            appScaffoldState.onPaneAnchorChanged(
                                anchor = anchor,
                                destinationId = paneNavigationState.destinationId,
                            )
                        }
                    }

                    val navigationEventDispatcher =
                        LocalNavigationEventDispatcherOwner.current!!
                            .navigationEventDispatcher

                    LaunchedEffect(
                        navigationEventDispatcher,
                        appScaffoldState,
                    ) {
                        combine(
                            navigationEventDispatcher.transitionState,
                            navigationEventDispatcher.history,
                        ) { transitionState, navigationEventHistory ->
                            val navigationEventInfo = navigationEventHistory.mergedHistory
                                .getOrNull(navigationEventHistory.currentIndex)
                            when (transitionState) {
                                NavigationEventTransitionState.Idle -> AppScaffoldState.DismissBehavior.None
                                is NavigationEventTransitionState.InProgress -> when {
                                    navigationEventInfo is SecondaryPaneCloseNavigationEventInfo -> AppScaffoldState.DismissBehavior.Gesture.SlideToPop
                                    transitionState.latestEvent.swipeEdge == NavigationEvent.EDGE_NONE -> AppScaffoldState.DismissBehavior.Gesture.DragToPop
                                    else -> AppScaffoldState.DismissBehavior.Gesture.ScaleToPop
                                }
                            }
                        }
                            .collectLatest {
                                appScaffoldState.dismissBehavior = it
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

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

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.tunjid.composables.backpreview.BackPreviewState
import com.tunjid.composables.splitlayout.SplitLayoutState
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.media.video.VideoPlayerController
import com.tunjid.heron.scaffold.navigation.NavItem
import com.tunjid.heron.scaffold.navigation.NavigationStateHolder
import com.tunjid.heron.scaffold.navigation.navItemSelected
import com.tunjid.heron.scaffold.navigation.navItems
import com.tunjid.heron.scaffold.navigation.unknownRoute
import com.tunjid.heron.scaffold.scaffold.PaneAnchorState.Companion.MinPaneWidth
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.backStack
import com.tunjid.treenav.compose.MultiPaneDisplayScope
import com.tunjid.treenav.compose.MultiPaneDisplayState
import com.tunjid.treenav.compose.PaneEntry
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.compose.transforms.Transform
import com.tunjid.treenav.current
import com.tunjid.treenav.pop
import com.tunjid.treenav.strings.PathPattern
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteTrie
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@Stable
class AppState @Inject constructor(
    private val routeConfigurationMap: Map<String, PaneEntry<ThreePane, Route>>,
    private val navigationStateHolder: NavigationStateHolder,
    internal val videoPlayerController: VideoPlayerController,
    private val writeQueue: WriteQueue,
) {

    private var density = Density(1f)
    private val multiStackNavState = mutableStateOf(navigationStateHolder.state.value)
    private val paneRenderOrder = listOf(
        ThreePane.Tertiary,
        ThreePane.Secondary,
        ThreePane.Primary,
    )

    internal var showNavigation by mutableStateOf(false)
    internal val navItems by derivedStateOf { multiStackNavState.value.navItems }
    internal val navigation by multiStackNavState
    internal val backPreviewState = BackPreviewState(
        minScale = 0.75f,
    )
    internal val splitLayoutState = SplitLayoutState(
        orientation = Orientation.Horizontal,
        maxCount = paneRenderOrder.size,
        minSize = MinPaneWidth,
        keyAtIndex = { index ->
            val indexDiff = paneRenderOrder.size - visibleCount
            paneRenderOrder[index + indexDiff]
        }
    )

    internal val paneAnchorState by lazy { PaneAnchorState(density) }
    internal val dragToPopState = DragToPopState()

    internal val isPreviewingBack
        get() = !backPreviewState.progress.isNaN()
                || dragToPopState.isDraggingToPop

    internal val isMediumScreenWidthOrWider get() = splitLayoutState.size >= SecondaryPaneMinWidthBreakpointDp

    internal var displayScope by mutableStateOf<MultiPaneDisplayScope<ThreePane, Route>?>(null)

    internal val filteredPaneOrder: List<ThreePane> by derivedStateOf {
        paneRenderOrder.filter { displayScope?.destinationIn(it) != null }
    }

    private val configurationTrie = RouteTrie<PaneEntry<ThreePane, Route>>().apply {
        routeConfigurationMap
            .mapKeys { (template) -> PathPattern(template) }
            .forEach { set(it.key, it.value) }
    }

    @Composable
    internal fun rememberMultiPaneDisplayState(
        transforms: List<Transform<ThreePane, MultiStackNav, Route>>,
    ): MultiPaneDisplayState<ThreePane, MultiStackNav, Route> {
        LocalDensity.current.also { density = it }
        val displayState = remember {
            MultiPaneDisplayState(
                panes = ThreePane.entries.toList(),
                navigationState = multiStackNavState,
                backStackTransform = { multiStackNav ->
                    multiStackNav.backStack(
                        includeCurrentDestinationChildren = true,
                        placeChildrenBeforeParent = true,
                    )
                        .filterIsInstance<Route>()
                        .toList()
                },
                destinationTransform = { multiStackNav ->
                    multiStackNav.current as? Route ?: unknownRoute("")
                },
                entryProvider = { node ->
                    configurationTrie[node] ?: threePaneEntry(
                        render = { },
                    )
                },
                transforms = transforms,
            )
        }
        DisposableEffect(Unit) {
            val job = CoroutineScope(Dispatchers.Main.immediate).launch {
                navigationStateHolder.state.collect { multiStackNav ->
                    multiStackNavState.value = multiStackNav
                }
            }
            onDispose { job.cancel() }
        }

        // TODO: Figure out a way to do this in the background with KMP
        LaunchedEffect(Unit) {
            writeQueue.drain()
        }

        return displayState
    }


    internal fun onNavItemSelected(navItem: NavItem) {
        navigationStateHolder.accept { navState.navItemSelected(item = navItem) }
    }

    internal fun pop() =
        navigationStateHolder.accept {
            navState.pop()
        }
}

internal val LocalAppState = staticCompositionLocalOf<AppState> {
    TODO()
}

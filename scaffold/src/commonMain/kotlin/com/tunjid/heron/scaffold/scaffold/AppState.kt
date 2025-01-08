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
import com.tunjid.heron.scaffold.navigation.NavItem
import com.tunjid.heron.scaffold.navigation.NavigationStateHolder
import com.tunjid.heron.scaffold.navigation.navItemSelected
import com.tunjid.heron.scaffold.navigation.navItems
import com.tunjid.heron.scaffold.navigation.unknownRoute
import com.tunjid.heron.scaffold.scaffold.PaneAnchorState.Companion.MinPaneWidth
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.compose.PaneStrategy
import com.tunjid.treenav.compose.PanedNavHostConfiguration
import com.tunjid.treenav.compose.PanedNavHostScope
import com.tunjid.treenav.compose.SavedStatePanedNavHostState
import com.tunjid.treenav.compose.panedNavHostConfiguration
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneListDetailStrategy
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
    private val routeConfigurationMap: Map<String, PaneStrategy<ThreePane, Route>>,
    private val navigationStateHolder: NavigationStateHolder,
    private val writeQueue: WriteQueue,
) {

    private var density = Density(1f)
    private val multiStackNavState = mutableStateOf(navigationStateHolder.state.value)
    private val paneRenderOrder = listOf(
        ThreePane.Secondary,
        ThreePane.Primary,
    )

    internal var showNavigation by mutableStateOf(false)
    internal val navItems by derivedStateOf { multiStackNavState.value.navItems }
    internal val navigation by multiStackNavState
    internal val backPreviewState = BackPreviewState()
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

    internal val usesNavRail get() = splitLayoutState.size >= SecondaryPaneMinWidthBreakpointDp

    internal fun filteredPaneOrder(
        panedNavHostScope: PanedNavHostScope<ThreePane, Route>,
    ): List<ThreePane> {
        val order = paneRenderOrder.filter { panedNavHostScope.nodeFor(it) != null }
        return order
    }

    private val configurationTrie = RouteTrie<PaneStrategy<ThreePane, Route>>().apply {
        routeConfigurationMap
            .mapKeys { (template) -> PathPattern(template) }
            .forEach { set(it.key, it.value) }
    }

    private val navHostConfiguration = panedNavHostConfiguration(
        navigationState = multiStackNavState,
        destinationTransform = { multiStackNav ->
            multiStackNav.current as? Route ?: unknownRoute("")
        },
        strategyTransform = { node ->
            configurationTrie[node] ?: threePaneListDetailStrategy(
                render = { },
            )
        }
    )

    @Composable
    internal fun rememberPanedNavHostState(
        configurationBlock: PanedNavHostConfiguration<
                ThreePane,
                MultiStackNav,
                Route
                >.() -> PanedNavHostConfiguration<ThreePane, MultiStackNav, Route>,
    ): SavedStatePanedNavHostState<ThreePane, Route> {
        LocalDensity.current.also { density = it }
        val adaptiveNavHostState = remember {
            SavedStatePanedNavHostState(
                panes = ThreePane.entries.toList(),
                configuration = navHostConfiguration.configurationBlock(),
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

        return adaptiveNavHostState
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

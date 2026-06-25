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

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.tunjid.composables.splitlayout.SplitLayoutState
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.images.ImageLoader
import com.tunjid.heron.media.video.VideoPlayerController
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.coroutines.RouteViewModelInitializer
import com.tunjid.heron.ui.coroutines.SheetViewModel
import com.tunjid.heron.ui.coroutines.SheetViewModelInitializer
import com.tunjid.heron.ui.coroutines.withSnapshotNotifications
import com.tunjid.heron.ui.scaffold.identity.IdentityAction
import com.tunjid.heron.ui.scaffold.identity.IdentityStateHolder
import com.tunjid.heron.ui.scaffold.identity.isSignedIn
import com.tunjid.heron.ui.scaffold.navigation.AppStack
import com.tunjid.heron.ui.scaffold.navigation.NavItem
import com.tunjid.heron.ui.scaffold.navigation.NavigationStateHolder
import com.tunjid.heron.ui.scaffold.navigation.deepLinkTo
import com.tunjid.heron.ui.scaffold.navigation.isShowingSplashScreen
import com.tunjid.heron.ui.scaffold.navigation.navItemSelected
import com.tunjid.heron.ui.scaffold.navigation.signInDestination
import com.tunjid.heron.ui.scaffold.navigation.tasksDestination
import com.tunjid.heron.ui.scaffold.notifications.NotificationAction
import com.tunjid.heron.ui.scaffold.notifications.NotificationStateHolder
import com.tunjid.heron.ui.scaffold.scaffold.PaneAnchorState.Companion.MinPaneWidth
import com.tunjid.mutator.compose.produceState
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.compose.MultiPaneDisplayState
import com.tunjid.treenav.compose.PaneEntry
import com.tunjid.treenav.compose.PaneNavigationState
import com.tunjid.treenav.compose.multiPaneDisplayBackstack
import com.tunjid.treenav.compose.panedecorators.PaneDecorator
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.current
import com.tunjid.treenav.pop
import com.tunjid.treenav.requireCurrent
import com.tunjid.treenav.strings.PathPattern
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.toRouteTrie
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

@Stable
class AppState(
    entryMap: Map<String, PaneEntry<ThreePane, Route>>,
    private val identityStateHolder: IdentityStateHolder,
    private val navigationStateHolder: NavigationStateHolder,
    private val notificationStateHolder: NotificationStateHolder,
    internal val imageLoader: ImageLoader,
    internal val videoPlayerController: VideoPlayerController,
    internal val sheetViewModelInitializers: Map<KClass<*>, SheetViewModelInitializer>,
    internal val routeViewModelInitializers: Map<KClass<*>, RouteViewModelInitializer>,
) {
    internal val identityState
        get() = identityStateHolder.state

    internal val navigationState
        get() = navigationStateHolder.state

    private val notificationState
        get() = notificationStateHolder.state

    internal var showNavigation by mutableStateOf(false)

    var showPlatformSplashScreen by mutableStateOf(true)
        internal set

    internal val navItems by derivedStateOf {
        currentNavItems()
    }

    internal var dismissBehavior by mutableStateOf<DismissBehavior>(DismissBehavior.None)
        private set

    internal var lastPaneAnchor by mutableStateOf(PaneAnchor.Half)
        private set

    internal val movableNavigationBar =
        movableContentOf<Modifier, () -> Boolean> { modifier, onNavItemReselected ->
            PaneNavigationBar(
                modifier = modifier,
                onNavItemReselected = onNavItemReselected,
            )
        }

    internal val movableNavigationRail =
        movableContentOf<Modifier, () -> Boolean> { modifier, onNavItemReselected ->
            PaneNavigationRail(
                modifier = modifier,
                onNavItemReselected = onNavItemReselected,
            )
        }

    private val entryTrie = entryMap
        .mapKeys { (template) -> PathPattern(template) }
        .toRouteTrie()

    @Composable
    internal fun rememberMultiPaneDisplayState(
        paneDecorators: List<PaneDecorator<MultiStackNav, Route, ThreePane>>,
    ): MultiPaneDisplayState<MultiStackNav, Route, ThreePane> {
        val saveableStateHolderNavEntryDecorator =
            rememberSaveableStateHolderNavEntryDecorator<Route>()
        val viewModelStoreNavEntryDecorator =
            rememberViewModelStoreNavEntryDecorator<Route>()

        val displayState = remember {
            MultiPaneDisplayState(
                panes = ThreePane.entries.toList(),
                paneDecorators = paneDecorators,
                navigationState = derivedStateOf(navigationState::multiStackNav),
                backStackTransform = MultiStackNav::multiPaneDisplayBackstack,
                destinationTransform = MultiStackNav::requireCurrent,
                popTransform = MultiStackNav::pop,
                onPopped = { poppedNavigationState ->
                    navigationStateHolder.accept {
                        poppedNavigationState
                    }
                },
                navEntryDecorators = listOf(
                    saveableStateHolderNavEntryDecorator,
                    viewModelStoreNavEntryDecorator,
                    splashVisibilityNavEntryDecorator(),
                ),
                entryProvider = { node ->
                    entryTrie[node] ?: threePaneEntry(
                        render = { },
                    )
                },
            )
        }

        identityStateHolder.produceState()
        navigationStateHolder.produceState()
        notificationStateHolder.produceState()

        // TODO: Figure out a way to do this in the background with KMP
        LifecycleResumeEffect(Unit) {
            notificationStateHolder.accept(
                NotificationAction.ToggleUnreadNotificationsMonitor(monitor = true),
            )
            onPauseOrDispose {
                notificationStateHolder.accept(
                    NotificationAction.ToggleUnreadNotificationsMonitor(monitor = false),
                )
            }
        }

        LifecycleStartEffect(videoPlayerController) {
            onStopOrDispose { videoPlayerController.pauseActiveVideo() }
        }

        val navigationEventDispatcher = LocalNavigationEventDispatcherOwner.current!!
            .navigationEventDispatcher

        LaunchedEffect(navigationEventDispatcher) {
            combine(
                navigationEventDispatcher.transitionState,
                navigationEventDispatcher.history,
            ) { transitionState, navigationEventHistory ->
                val navigationEventInfo = navigationEventHistory.mergedHistory
                    .getOrNull(navigationEventHistory.currentIndex)
                when (transitionState) {
                    NavigationEventTransitionState.Idle -> DismissBehavior.None
                    is NavigationEventTransitionState.InProgress -> when {
                        navigationEventInfo is SecondaryPaneCloseNavigationEventInfo -> DismissBehavior.Gesture.SlideToPop
                        transitionState.latestEvent.swipeEdge == NavigationEvent.EDGE_NONE -> DismissBehavior.Gesture.DragToPop
                        else -> DismissBehavior.Gesture.ScaleToPop
                    }
                }
            }
                .collectLatest(::dismissBehavior::set)
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

    internal fun onNavigateToTasks() =
        navigationStateHolder.accept(
            tasksDestination(showFailedWrites = true).navigationMutation,
        )

    internal fun addAccount() {
        identityStateHolder.accept(
            IdentityAction.Switch.Cancel,
        )
        navigationStateHolder.accept(
            signInDestination().navigationMutation,
        )
    }

    internal fun onPaneAnchorChanged(
        anchor: PaneAnchor,
        destinationId: String,
    ) {
        if (destinationId != navigationState.multiStackNav.current?.id || anchor == PaneAnchor.Full) return
        lastPaneAnchor = anchor
    }

    fun onDeepLink(uri: GenericUri) =
        navigationStateHolder.accept(deepLinkTo(uri))

    fun onNotificationAction(action: NotificationAction) =
        notificationStateHolder.accept(action)

    fun onIdentityAction(action: IdentityAction) =
        identityStateHolder.accept(action)

    /**
     * This method is called from outside compose and
     * needs manual snapshot observation.
     */
    suspend fun awaitNotificationProcessing(
        recordUri: RecordUri,
    ) = withSnapshotNotifications {
        snapshotFlow {
            notificationState.processedNotificationRecordUris
        }.first {
            recordUri in it
        }
    }

    private fun currentNavItems(): List<NavItem> {
        return navigationState.multiStackNav
            .stacks
            .map(StackNav::name)
            .mapIndexedNotNull { index, name ->
                val stack = AppStack.entries.firstOrNull { stack ->
                    when (stack) {
                        AppStack.Home -> stack.stackName == name
                        AppStack.Search -> stack.stackName == name
                        AppStack.Messages -> identityState.isSignedIn && stack.stackName == name
                        AppStack.Notifications -> identityState.isSignedIn && stack.stackName == name
                        AppStack.Auth -> stack.stackName == name
                        AppStack.Splash -> stack.stackName == name
                    }
                } ?: return@mapIndexedNotNull null

                NavItem(
                    stack = stack,
                    index = index,
                    selected = navigationState.multiStackNav.currentIndex == index,
                    badgeCount = if (stack == AppStack.Notifications) notificationState.unreadCount else 0L,
                )
            }
    }

    sealed class DismissBehavior {
        data object None : DismissBehavior()
        sealed class Gesture : DismissBehavior() {
            data object DragToPop : Gesture()
            data object SlideToPop : Gesture()
            data object ScaleToPop : Gesture()
        }
    }

    companion object {
        val NOTIFICATION_PROCESSING_TIMEOUT_SECONDS = 10.seconds
    }
}

val AppState.isShowingSplashScreen: Boolean
    get() = navigationState.multiStackNav.isShowingSplashScreen

private fun AppState.splashVisibilityNavEntryDecorator(): NavEntryDecorator<Route> = NavEntryDecorator { entry ->
    entry.Content()
    if (showPlatformSplashScreen) {
        LifecycleStartEffect(Unit) {
            showPlatformSplashScreen = false
            onStopOrDispose { }
        }
    }
}

@Stable
internal class SplitPaneState(
    paneNavigationState: () -> PaneNavigationState<ThreePane, Route>,
    density: Density,
    initialAnchor: PaneAnchor,
    private val windowWidth: State<Dp>,
    private val hasCompatBottomNav: () -> Boolean,
) {
    internal var density by mutableStateOf(density)

    internal val paneAnchorState = PaneAnchorState(
        initialMaxWidth = with(density) { windowWidth.value.roundToPx() },
        initialAnchor = initialAnchor,
    )

    internal val filteredPaneOrder by derivedStateOf {
        PaneRenderOrder.filter { paneNavigationState().destinationIn(it) != null }
    }

    internal val minPaneWidth: Dp
        get() = (windowWidth.value * 0.5f) - UiTokens.bottomNavHeight(
            isCompact = hasCompatBottomNav(),
        )

    internal val splitLayoutState = SplitLayoutState(
        orientation = Orientation.Horizontal,
        maxCount = PaneRenderOrder.size,
        minSize = MinPaneWidth,
        visibleCount = {
            filteredPaneOrder.size
        },
        keyAtIndex = { index ->
            filteredPaneOrder[index]
        },
    )

    internal val isMediumScreenWidthOrWider
        get() = windowWidth.value >= UiTokens.SecondaryPaneMinWidthBreakpoint

    fun update(
        density: Density,
    ) {
        this.density = density
        paneAnchorState.updateMaxWidth(
            density = density,
            maxWidth = with(density) { windowWidth.value.roundToPx() },
        )
    }
}

internal val AppState.prefersCompactBottomNav: Boolean
    get() = identityState.preferences?.local?.useCompactNavigation ?: false

internal val AppState.prefersAutoHidingBottomNav: Boolean
    get() = identityState.preferences?.local?.autoHideBottomNavigation ?: true

private val PaneRenderOrder = listOf(
    ThreePane.Tertiary,
    ThreePane.Secondary,
    ThreePane.Primary,
)

internal val LocalSplitPaneState = staticCompositionLocalOf<SplitPaneState> {
    throw IllegalStateException("No SplitPaneState provided")
}

internal val LocalAppState = staticCompositionLocalOf<AppState> {
    throw IllegalStateException("No AppState provided")
}

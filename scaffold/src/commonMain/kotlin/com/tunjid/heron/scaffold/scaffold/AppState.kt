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
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.tunjid.composables.backpreview.BackPreviewState
import com.tunjid.composables.splitlayout.SplitLayoutState
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.images.ImageLoader
import com.tunjid.heron.media.video.VideoPlayerController
import com.tunjid.heron.scaffold.navigation.AppStack
import com.tunjid.heron.scaffold.navigation.NavItem
import com.tunjid.heron.scaffold.navigation.NavigationStateHolder
import com.tunjid.heron.scaffold.navigation.deepLinkTo
import com.tunjid.heron.scaffold.navigation.isShowingSplashScreen
import com.tunjid.heron.scaffold.navigation.navItemSelected
import com.tunjid.heron.scaffold.notifications.NotificationAction
import com.tunjid.heron.scaffold.notifications.NotificationStateHolder
import com.tunjid.heron.scaffold.scaffold.PaneAnchorState.Companion.MinPaneWidth
import com.tunjid.heron.ui.UiTokens
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.compose.MultiPaneDisplayState
import com.tunjid.treenav.compose.PaneEntry
import com.tunjid.treenav.compose.PaneNavigationState
import com.tunjid.treenav.compose.multiPaneDisplayBackstack
import com.tunjid.treenav.compose.panedecorators.PaneDecorator
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.pop
import com.tunjid.treenav.requireCurrent
import com.tunjid.treenav.strings.PathPattern
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.toRouteTrie
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Stable
class AppState(
    entryMap: Map<String, PaneEntry<ThreePane, Route>>,
    private val authRepository: AuthRepository,
    private val userDataRepository: UserDataRepository,
    private val navigationStateHolder: NavigationStateHolder,
    private val notificationStateHolder: NotificationStateHolder,
    internal val imageLoader: ImageLoader,
    internal val videoPlayerController: VideoPlayerController,
    private val writeQueue: WriteQueue,
) {
    private var notificationCount by mutableStateOf(0L)

    internal var isSignedIn by mutableStateOf(false)

    internal var preferences by mutableStateOf<Preferences?>(null)

    private val multiStackNavState = mutableStateOf(navigationStateHolder.state.value)

    internal var showNavigation by mutableStateOf(false)

    var showPlatformSplashScreen by mutableStateOf(true)
        internal set
    internal val navItems by derivedStateOf {
        currentNavItems()
    }

    internal val navigation by multiStackNavState
    internal val backPreviewState = BackPreviewState(
        minScale = 0.75f,
    )

    internal var dismissBehavior by mutableStateOf<DismissBehavior>(DismissBehavior.None)
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
                navigationState = multiStackNavState,
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
            launch {
                writeQueue.drain()
            }
            launch {
                notificationStateHolder.state.collect { notificationState ->
                    notificationCount = notificationState.unreadCount
                }
            }
            launch {
                authRepository.isSignedIn.collect { signedIn ->
                    isSignedIn = signedIn
                }
            }
            launch {
                userDataRepository.preferences.collect { currentPreferences ->
                    preferences = currentPreferences
                }
            }
        }
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

    fun onDeepLink(uri: GenericUri) =
        navigationStateHolder.accept(deepLinkTo(uri))

    fun onNotificationAction(action: NotificationAction) =
        notificationStateHolder.accept(action)

    suspend fun awaitNotificationProcessing(recordUri: RecordUri) {
        notificationStateHolder.state.first { state ->
            recordUri in state.processedNotificationRecordUris
        }
    }

    private fun currentNavItems(): List<NavItem> {
        val multiStackNav = multiStackNavState.value
        return multiStackNav.stacks
            .map(StackNav::name)
            .mapIndexedNotNull { index, name ->
                val stack = AppStack.entries.firstOrNull { stack ->
                    when (stack) {
                        AppStack.Home -> stack.stackName == name
                        AppStack.Search -> stack.stackName == name
                        AppStack.Messages -> isSignedIn && stack.stackName == name
                        AppStack.Notifications -> isSignedIn && stack.stackName == name
                        AppStack.Auth -> stack.stackName == name
                        AppStack.Splash -> stack.stackName == name
                    }
                } ?: return@mapIndexedNotNull null

                NavItem(
                    stack = stack,
                    index = index,
                    selected = multiStackNav.currentIndex == index,
                    badgeCount = if (stack == AppStack.Notifications) notificationCount else 0L,
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
    get() = navigation.isShowingSplashScreen

private fun AppState.splashVisibilityNavEntryDecorator(): NavEntryDecorator<Route> = NavEntryDecorator { entry ->
    entry.Content()
    LifecycleStartEffect(Unit) {
        if (entry.contentKey == AppStack.Splash.rootRoute.id) {
            showPlatformSplashScreen = false
        }
        onStopOrDispose { }
    }
}

@Stable
internal class SplitPaneState(
    paneNavigationState: () -> PaneNavigationState<ThreePane, Route>,
    density: Density,
    private val windowWidth: State<Dp>,
    private val hasCompatBottomNav: () -> Boolean,
) {
    internal var density by mutableStateOf(density)

    internal val paneAnchorState = PaneAnchorState()

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
        get() = windowWidth.value >= SecondaryPaneMinWidthBreakpointDp

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
    get() = preferences?.local?.useCompactNavigation ?: false

internal val AppState.prefersAutoHidingBottomNav: Boolean
    get() = preferences?.local?.autoHideBottomNavigation ?: true

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

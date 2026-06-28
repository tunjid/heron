package com.tunjid.heron.ui.scaffold.scaffold

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentWithReceiverOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.navigation3.runtime.NavEntryDecorator
import com.tunjid.composables.splitlayout.SplitLayoutState
import com.tunjid.heron.images.ImageLoader
import com.tunjid.heron.media.video.VideoPlayerController
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.scaffold.identity.IdentityAction
import com.tunjid.heron.ui.scaffold.identity.IdentityStateHolder
import com.tunjid.heron.ui.scaffold.identity.isSignedIn
import com.tunjid.heron.ui.scaffold.identity.prefersCompactBottomNav
import com.tunjid.heron.ui.scaffold.navigation.AppStack
import com.tunjid.heron.ui.scaffold.navigation.NavItem
import com.tunjid.heron.ui.scaffold.navigation.NavigationMutation
import com.tunjid.heron.ui.scaffold.navigation.NavigationStateHolder
import com.tunjid.heron.ui.scaffold.navigation.navItemSelected
import com.tunjid.heron.ui.scaffold.navigation.signInDestination
import com.tunjid.heron.ui.scaffold.navigation.tasksDestination
import com.tunjid.heron.ui.scaffold.notifications.NotificationAction
import com.tunjid.heron.ui.scaffold.notifications.NotificationStateHolder
import com.tunjid.heron.ui.stateproduction.StateHolderInitializer
import com.tunjid.mutator.compose.produceState
import com.tunjid.mutator.invoke
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.compose.MultiPaneDisplayState
import com.tunjid.treenav.compose.PaneEntry
import com.tunjid.treenav.compose.PaneNavigationState
import com.tunjid.treenav.compose.multiPaneDisplayBackstack
import com.tunjid.treenav.compose.panedecorators.PaneDecorator
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.current
import com.tunjid.treenav.pop
import com.tunjid.treenav.requireCurrent
import com.tunjid.treenav.strings.Route

/**
 * The scaffold state for the app hosting UI logic.
 *
 * A new [AppScaffoldState] is created per [com.tunjid.treenav.compose.MultiPaneDisplayScope].
 * Elements that are retained across scopes are in [AppScaffoldState.StaticStates].
 */
@Stable
class AppScaffoldState internal constructor(
    paneNavigationState: () -> PaneNavigationState<ThreePane, Route>,
    density: Density,
    internal val staticStates: StaticStates,
    private val windowWidth: State<Dp>,
) {
    internal var density by mutableStateOf(density)

    internal val paneAnchorState = PaneAnchorState(
        initialMaxWidth = with(density) { windowWidth.value.roundToPx() },
        initialAnchor = staticStates.currentPaneAnchor,
    )

    internal val filteredPaneOrder by derivedStateOf {
        PaneRenderOrder.filter { paneNavigationState().destinationIn(it) != null }
    }

    internal val minPaneWidth: Dp
        get() = (windowWidth.value * 0.5f) - UiTokens.bottomNavHeight(
            isCompact = staticStates.identityState.prefersCompactBottomNav,
        )

    internal val splitLayoutState = SplitLayoutState(
        orientation = Orientation.Horizontal,
        maxCount = PaneRenderOrder.size,
        minSize = PaneAnchorState.MinPaneWidth,
        visibleCount = {
            filteredPaneOrder.size
        },
        keyAtIndex = { index ->
            filteredPaneOrder[index]
        },
    )

    internal val isMediumScreenWidthOrWider
        get() = windowWidth.value >= UiTokens.SecondaryPaneMinWidthBreakpoint

    internal fun update(
        density: Density,
    ) {
        this.density = density
        paneAnchorState.updateMaxWidth(
            density = density,
            maxWidth = with(density) { windowWidth.value.roundToPx() },
        )
    }

    internal fun onNavigateToTasks() =
        staticStates.onNavigationAction(
            tasksDestination(showFailedWrites = true).navigationMutation,
        )

    internal fun addAccount() {
        staticStates.onIdentityAction(
            IdentityAction.Switch.Cancel,
        )
        staticStates.onNavigationAction(
            signInDestination().navigationMutation,
        )
    }

    internal fun pop() =
        staticStates.onNavigationAction {
            navState.pop()
        }

    sealed class DismissBehavior {
        data object None : DismissBehavior()
        sealed class Gesture : DismissBehavior() {
            data object DragToPop : Gesture()
            data object SlideToPop : Gesture()
            data object ScaleToPop : Gesture()
        }
    }

    /**
     * Elements on [AppScaffoldState] that are effectively singletons from
     * the App's UI scaffold POV, though not necessarily from the [AppState]'s.
     * i.e, all [AppScaffoldState] instances share identical [AppScaffoldState.StaticStates]
     * instances.
     *
     * It also acts as a mixin for app level properties.
     */
    @Stable
    class StaticStates(
        private val identityStateHolder: IdentityStateHolder,
        private val navigationStateHolder: NavigationStateHolder,
        private val notificationStateHolder: NotificationStateHolder,
        internal val imageLoader: ImageLoader,
        internal val videoPlayerController: VideoPlayerController,
        internal val stateHolderInitializer: StateHolderInitializer,
    ) {
        internal var showNavigation by mutableStateOf(false)
        internal var dismissBehavior by mutableStateOf<DismissBehavior>(DismissBehavior.None)

        internal var currentPaneAnchor by mutableStateOf(PaneAnchor.Half)
            private set

        internal val identityState
            get() = identityStateHolder.state

        internal val navigationState
            get() = navigationStateHolder.state

        internal val notificationState
            get() = notificationStateHolder.state

        internal val movableNavigationBar =
            movableContentWithReceiverOf<AppScaffoldState, Modifier, () -> Boolean> { modifier, onNavItemReselected ->
                PaneNavigationBar(
                    modifier = modifier,
                    onNavItemReselected = onNavItemReselected,
                )
            }

        internal val movableNavigationRail =
            movableContentWithReceiverOf<AppScaffoldState, Modifier, () -> Boolean> { modifier, onNavItemReselected ->
                PaneNavigationRail(
                    modifier = modifier,
                    onNavItemReselected = onNavItemReselected,
                )
            }

        internal val navItems by derivedStateOf {
            currentNavItems()
        }

        internal fun onNavItemSelected(navItem: NavItem) {
            onNavigationAction { navState.navItemSelected(item = navItem) }
        }

        internal fun onPaneAnchorChanged(
            anchor: PaneAnchor,
            destinationId: String,
        ) {
            if (destinationId != navigationState.multiStackNav.current?.id || anchor == PaneAnchor.Full) return
            currentPaneAnchor = anchor
        }

        fun onIdentityAction(
            action: IdentityAction,
        ) = identityStateHolder(action)

        fun onNavigationAction(
            action: NavigationMutation,
        ) = navigationStateHolder(action)

        fun onNotificationAction(
            action: NotificationAction,
        ) = notificationStateHolder(action)

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

        companion object {
            @Composable
            internal fun StaticStates.rememberMultiPaneDisplayState(
                paneDecorators: List<PaneDecorator<MultiStackNav, Route, ThreePane>>,
                entryDecorators: List<NavEntryDecorator<Route>>,
                entryProvider: (Route) -> PaneEntry<ThreePane, Route>,
            ): MultiPaneDisplayState<MultiStackNav, Route, ThreePane> {
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
                        navEntryDecorators = entryDecorators,
                        entryProvider = entryProvider,
                    )
                }

                identityStateHolder.produceState()
                navigationStateHolder.produceState()
                notificationStateHolder.produceState()

                // TODO: Figure out a way to do this in the background with KMP
                LifecycleResumeEffect(Unit) {
                    onNotificationAction(
                        NotificationAction.ToggleUnreadNotificationsMonitor(monitor = true),
                    )
                    onPauseOrDispose {
                        onNotificationAction(
                            NotificationAction.ToggleUnreadNotificationsMonitor(monitor = false),
                        )
                    }
                }

                LifecycleStartEffect(videoPlayerController) {
                    onStopOrDispose { videoPlayerController.pauseActiveVideo() }
                }

                return displayState
            }
        }
    }
}

private val PaneRenderOrder = listOf(
    ThreePane.Tertiary,
    ThreePane.Secondary,
    ThreePane.Primary,
)

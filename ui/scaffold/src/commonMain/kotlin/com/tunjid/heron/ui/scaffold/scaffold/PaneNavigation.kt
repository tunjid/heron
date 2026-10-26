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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.roundedMaxDelta
import com.tunjid.heron.ui.scaffold.identity.isStable
import com.tunjid.heron.ui.scaffold.navigation.AppStack
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction
import com.tunjid.treenav.compose.Adaptation

@Composable
fun PaneScaffoldState.PaneNavigationBar(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    enterTransition: EnterTransition = slideInVertically(initialOffsetY = { it }),
    exitTransition: ExitTransition = slideOutVertically(targetOffsetY = { it }),
    onNavItemReselected: () -> Boolean = { false },
) {
    val status = appScaffoldState.staticStates.uiState.transientBottomNavStatus
        ?: if (expanded) BottomNavStatus.Expanded
        else when (bottomNavConfig) {
            PaneScaffoldState.BottomNavConfig.PartiallyCollapse -> BottomNavStatus.Collapsed.Partially
            PaneScaffoldState.BottomNavConfig.Hide -> BottomNavStatus.Expanded
            PaneScaffoldState.BottomNavConfig.FullyCollapse -> BottomNavStatus.Collapsed.Fully
        }
    SideEffect(expanded) {
        if (!expanded) appScaffoldState.updateTransientBottomNav(
            show = false,
        )
    }
    AnimatedVisibility(
        modifier = modifier,
        visible = canShowNavigationBar,
        enter = enterTransition,
        exit = exitTransition,
        content = {
            with(this@PaneNavigationBar) {
                if (canUseMovableNavigationBar) appScaffoldState.staticStates.movableNavigationBar(
                    this,
                    Modifier,
                    status,
                    onNavItemReselected,
                )
                else PlatformNavigationBar(
                    modifier = Modifier,
                    status = status,
                    onNavItemReselected = onNavItemReselected,
                )
            }
        },
    )
}

/**
 * TODO: This is a workaround to being aware of system vertical bar insets.
 *
 * Takes the place of the navigation rail for panes that host their own vertical chrome.
 *
 * When it sits on the same side as a system vertical bar, it is as wide as the bar and centers
 * [content] on it, below the bar's status content, wherever the container itself is placed.
 * Otherwise it is a plain [Box].
 *
 * @param side the layout relative side of the pane this container sits on, either
 * [Alignment.Start] or [Alignment.End]. Defaults to the navigation rail's side.
 */
@Composable
fun PaneScaffoldState.PaneNavigationRailSpacer(
    modifier: Modifier = Modifier,
    side: Alignment.Horizontal = navigationRailSide(),
    content: @Composable BoxScope.() -> Unit = {},
) {
    val systemVerticalBar = appScaffoldState.staticStates.uiState.systemVerticalBar
    val sitsOnBar = when (side) {
        Alignment.Start,
        Alignment.End,
        -> side == navigationRailSide()
        else -> false
    }
    if (canShowNavigationRail && systemVerticalBar != null && sitsOnBar) Box(
        modifier = modifier
            .width(systemVerticalBar.width),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(
                    top = animateDpAsState(systemVerticalBarContentTop).value + 8.dp,
                )
                .centerOnSystemVerticalBarInWindow(),
            content = content,
        )
    }
    else Box(
        modifier = modifier,
        content = content,
    )
}

@Composable
fun PaneScaffoldState.PaneNavigationRail(
    modifier: Modifier = Modifier,
    enterTransition: EnterTransition = slideInHorizontally(initialOffsetX = { -it }),
    exitTransition: ExitTransition = slideOutHorizontally(targetOffsetX = { -it }),
    onNavItemReselected: () -> Boolean = { false },
    onNavigationIconClicked: (() -> Unit)? = null,
    actions: @Composable ColumnScope.() -> Unit = {},
) = Box(
    modifier = modifier,
) {
    val isHandedBetweenPanes = paneState.adaptations.any {
        it is Adaptation.Swap<*> || it is Adaptation.Same
    }
    AnimatedVisibility(
        visible = canShowNavigationRail,
        enter = when {
            canShowNavigationRail && !isHandedBetweenPanes -> enterTransition
            else -> EnterTransition.None
        },
        exit = when {
            isHandedBetweenPanes -> ExitTransition.None
            else -> exitTransition
        },
        content = {
            with(this@PaneNavigationRail) {
                if (canUseMovableNavigationRail) appScaffoldState.staticStates.movableNavigationRail(
                    this,
                    Modifier,
                    onNavItemReselected,
                )
                else PlatformNavigationRail(
                    modifier = Modifier,
                    onNavItemReselected = onNavItemReselected,
                )
            }
        },
    )
    // Outside the rail's shared element, as the actions belong to this pane.
    if (placesAppBarActionsInNavigationRail) Column(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(
                top = animateDpAsState(systemVerticalBarContentTop).value + 8.dp,
            )
            .centerOnSystemVerticalBar(
                railBoxWidth = navigationRailWidth,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NavigationRailNavigationIcon(
            onClicked = onNavigationIconClicked,
        )
        actions()
    }
}

@Composable
private fun PaneScaffoldState.navigationRailSide(): Alignment.Horizontal =
    if (navRailPlacesAtStart(layoutDirection = LocalLayoutDirection.current)) Alignment.Start
    else Alignment.End

@Composable
private fun PaneScaffoldState.NavigationRailNavigationIcon(
    onClicked: (() -> Unit)?,
) {
    val isAtStackRoot = AppStack.entries.any {
        it.rootRoute.id == paneState.currentDestination?.id
    }
    val isStable = appScaffoldState.staticStates.identityState.isStable
    NavigationIconBox {
        AppLogo(
            modifier = Modifier
                .padding(8.dp)
                .clip(CircleShape)
                .clickable(
                    enabled = !isAtStackRoot || isStable,
                    onClick = click@{
                        if (onClicked != null) return@click onClicked()
                        val action = if (isAtStackRoot) NavigationAction.Home.navigationMutation
                        else NavigationAction.Pop.navigationMutation
                        appScaffoldState.staticStates.onNavigationAction(
                            action = action,
                        )
                    },
                )
                .size(UiTokens.avatarSize),
            presentation = remember(isAtStackRoot) {
                if (isAtStackRoot) LogoPresentation.Destination.Root()
                else LogoPresentation.Destination.Poppable
            },
        )
    }
}

fun PaneScaffoldState.bottomNavVisibleInset(): IntOffset =
    bottomNavigationNestedScrollConnection.roundedMaxDelta

fun PaneScaffoldState.bottomNavOffset(): IntOffset {
    return if (prefersAutoHidingBottomNav) bottomNavigationNestedScrollConnection.offset.round()
    else IntOffset.Zero
}

@Composable
internal fun Badge(
    count: Long,
) {
    when (count) {
        in 1..<MaxBadgeCount -> Badge { Text("$count") }
        in MaxBadgeCount..Long.MAX_VALUE -> Badge(Modifier.size(4.dp))
    }
}

private const val MaxBadgeCount = 100L

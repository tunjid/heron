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
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.times
import com.tunjid.composables.constrainedsize.constrainedSizePlacement
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.scaffold.identity.isStable
import com.tunjid.treenav.compose.Adaptation
import org.jetbrains.compose.resources.stringResource

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

@Composable
fun PaneScaffoldState.PaneNavigationRail(
    modifier: Modifier = Modifier,
    enterTransition: EnterTransition = slideInHorizontally(initialOffsetX = { -it }),
    exitTransition: ExitTransition = slideOutHorizontally(targetOffsetX = { -it }),
    onNavItemReselected: () -> Boolean = { false },
) {
    AnimatedVisibility(
        modifier = modifier
            .sharedElement(
                sharedContentState = rememberSharedContentState(NavigationRailSharedElementKey),
                animatedVisibilityScope = this,
                zIndexInOverlay = UiTokens.navigationBarSharedElementZIndex,
                boundsTransform = NavigationRailBoundsTransform,
            ),
        visible = canShowNavigationRail,
        enter = if (
            canShowNavigationRail &&
            paneState.adaptations.none { it is Adaptation.Swap<*> || it is Adaptation.Same }
        ) enterTransition else EnterTransition.None,
        exit = exitTransition,
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
}

fun PaneScaffoldState.bottomNavOffset(offset: Offset): IntOffset {
    return if (prefersAutoHidingBottomNav) offset.round()
    else IntOffset.Zero
}

@Composable
internal fun PaneScaffoldState.PlatformNavigationRail(
    modifier: Modifier = Modifier,
    onNavItemReselected: () -> Boolean,
) = with(appScaffoldState.staticStates) {
    Box(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .widthIn(max = navigationRailWidth)
            .fillMaxSize(),
    ) {
        val color by animateColorAsState(
            if (appScaffoldState.shouldElevateNavRail()) MaterialTheme.colorScheme.surfaceContainerHigh
            else MaterialTheme.colorScheme.surface,
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .constrainedSizePlacement(
                    orientation = Orientation.Horizontal,
                    minSize = navigationRailWidth,
                    atStart = true,
                )
                .background(
                    color = color,
                    shape = NavRailShape,
                )
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            navItems.forEach { item ->
                NavigationRailItem(
                    enabled = identityState.isStable,
                    selected = item.selected,
                    icon = {
                        BadgedBox(
                            badge = {
                                Badge(item.badgeCount)
                            },
                            content = {
                                Icon(
                                    imageVector = item.stack.icon,
                                    contentDescription = stringResource(item.stack.titleRes),
                                )
                            },
                        )
                    },
                    onClick = {
                        if (item.selected && onNavItemReselected()) return@NavigationRailItem
                        onNavItemSelected(item)
                    },
                )
            }
        }
    }
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

@Composable
private fun AppScaffoldState.shouldElevateNavRail(): Boolean = remember(this) {
    derivedStateOf {
        splitLayoutState.weightAt(0)
            .times(splitLayoutState.size)
            .minus(navigationRailWidth) < minPaneWidth
    }
}.value

private data object NavigationRailSharedElementKey

private val NavRailShape = RoundedCornerShape(UiTokens.NavRailWidth)

private const val MaxBadgeCount = 100L

private val NavigationRailBoundsTransform = BoundsTransform { _, _ -> snap() }

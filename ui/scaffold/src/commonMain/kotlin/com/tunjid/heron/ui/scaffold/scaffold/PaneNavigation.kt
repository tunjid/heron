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
import androidx.compose.animation.core.snap
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.roundedMaxDelta
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

private data object NavigationRailSharedElementKey

private const val MaxBadgeCount = 100L

private val NavigationRailBoundsTransform = BoundsTransform { _, _ -> snap() }

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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.snap
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.unit.dp
import com.tunjid.heron.ui.UiTokens
import com.tunjid.treenav.compose.Adaptation
import com.tunjid.treenav.compose.threepane.ThreePane
import org.jetbrains.compose.resources.stringResource

@Composable
fun PaneScaffoldState.PaneNavigationBar(
    modifier: Modifier = Modifier,
    enterTransition: EnterTransition = slideInVertically(initialOffsetY = { it }),
    exitTransition: ExitTransition = slideOutVertically(targetOffsetY = { it }),
    onNavItemReselected: () -> Boolean = { false },
) {
    AnimatedVisibility(
        modifier = modifier
            .sharedElement(
                sharedContentState = rememberSharedContentState(NavigationBarSharedElementKey),
                animatedVisibilityScope = this,
                zIndexInOverlay = NavigationSharedElementZIndex,
            ),
        visible = canShowNavigationBar,
        enter = enterTransition,
        exit = exitTransition,
        content = {
            if (canUseMovableNavigationBar) appState.movableNavigationBar(
                Modifier,
                onNavItemReselected,
            )
            else appState.PaneNavigationBar(
                modifier = Modifier,
                onNavItemReselected = onNavItemReselected,
            )
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
                zIndexInOverlay = NavigationSharedElementZIndex,
                boundsTransform = NavigationRailBoundsTransform,
            ),
        visible = canShowNavigationRail,
        enter = if (
            canShowNavigationRail &&
            paneState.adaptations.none { it is Adaptation.Swap<*> || it is Adaptation.Same }
        ) enterTransition else EnterTransition.None,
        exit = exitTransition,
        content = {
            if (canUseMovableNavigationRail) appState.movableNavigationRail(
                Modifier,
                onNavItemReselected,
            )
            else appState.PaneNavigationRail(
                modifier = Modifier,
                onNavItemReselected = onNavItemReselected,
            )
        },
    )
}

@Composable
internal fun AppState.PaneNavigationBar(
    modifier: Modifier = Modifier,
    onNavItemReselected: () -> Boolean,
) {
    LookaheadScope {
        Surface(
            modifier = modifier
                .animateBounds(lookaheadScope = this@LookaheadScope),
            color = BottomAppBarDefaults.containerColor,
            contentColor = contentColorFor(BottomAppBarDefaults.containerColor),
        ) {
            Row(
                modifier = Modifier
                    .navigationBarsPadding()
                    .height(UiTokens.bottomNavHeight(isCompact = prefersCompactBottomNav)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                navItems.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (item.hasBadge) {
                                        if ((item.badgeCount ?: 0) > 0) Badge(content = { Text("${item.badgeCount}") })
                                        else Badge(Modifier.size(4.dp))
                                    }
                                },
                                content = {
                                    Icon(
                                        imageVector = item.stack.icon,
                                        contentDescription = stringResource(item.stack.titleRes),
                                    )
                                },
                            )
                        },
                        selected = item.selected,
                        onClick = {
                            if (item.selected && onNavItemReselected()) return@NavigationBarItem
                            onNavItemSelected(item)
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun AppState.PaneNavigationRail(
    modifier: Modifier = Modifier,
    onNavItemReselected: () -> Boolean,
) {
    NavigationRail(
        modifier = modifier,
    ) {
        navItems.forEach { item ->
            NavigationRailItem(
                selected = item.selected,
                icon = {
                    BadgedBox(
                        badge = {
                            if (item.hasBadge) Badge(Modifier.size(4.dp))
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

@Composable
fun Modifier.bottomNavigationSharedBounds(
    paneScaffoldState: PaneScaffoldState,
): Modifier = with(paneScaffoldState) {
    when (paneState.pane) {
        ThreePane.Primary -> if (inPredictiveBack) this@bottomNavigationSharedBounds else sharedBounds(
            sharedContentState = rememberSharedContentState(NavigationBarSharedElementKey),
            animatedVisibilityScope = this,
        )

        ThreePane.Secondary,
        ThreePane.Tertiary,
        ThreePane.Overlay,
        null,
        -> this@bottomNavigationSharedBounds
    }
}

private data object NavigationBarSharedElementKey
private data object NavigationRailSharedElementKey

private const val NavigationSharedElementZIndex = 2f

private val NavigationRailBoundsTransform = BoundsTransform { _, _ -> snap() }

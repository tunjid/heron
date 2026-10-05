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

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.modifiers.ifTrue
import com.tunjid.heron.ui.scaffold.identity.isStable
import com.tunjid.heron.ui.scaffold.identity.prefersCompactBottomNav
import com.tunjid.heron.ui.scaffold.navigation.NavItem
import com.tunjid.treenav.compose.NavigationEventStatus
import com.tunjid.treenav.compose.threepane.ThreePane
import org.jetbrains.compose.resources.stringResource

/**
 * The bottom navigation surface. Most platforms render [CommonNavigationBar] (the Material
 * `NavigationBar`). iOS renders a native Liquid Glass `UITabBar` on iOS 26+, which is self contained
 * and so is not wrapped in a Compose surface, and renders [CommonNavigationBar] on older systems.
 *
 * The nav items already live on [AppScaffoldState], so this reads them directly rather than taking
 * them as parameters.
 */
@Composable
internal expect fun PaneScaffoldState.PlatformNavigationBar(
    modifier: Modifier,
    status: BottomNavStatus,
    onNavItemReselected: () -> Boolean,
)

@Composable
internal fun PaneScaffoldState.CommonNavigationBar(
    modifier: Modifier,
    status: BottomNavStatus,
    onNavItemReselected: () -> Boolean,
) = with(appScaffoldState.staticStates) {
    val bottomNavHeight = UiTokens.bottomNavHeight(
        isCompact = identityState.prefersCompactBottomNav,
    )
    val navigationBarHeight = UiTokens.navigationBarHeight
    val totalHeight = bottomNavHeight + navigationBarHeight

    Box(
        modifier = modifier
            .height(totalHeight)
            .commonBottomNavWidth(
                width = totalHeight,
                bottomNavStatus = status,
            )
            .commonBottomNavPadding(
                bottomNavStatus = status,
                navigationBarHeight = navigationBarHeight,
            )
            .animateBounds(
                lookaheadScope = this@CommonNavigationBar,
            ),
    ) {
        Surface(
            modifier = Modifier
                .commonBottomNavSharedElement(
                    sharedContentState = rememberSharedContentState(NavigationBarSharedElementKey),
                )
                .matchParentSize()
                .animateBounds(
                    lookaheadScope = this@CommonNavigationBar,
                ),
            color = BottomAppBarDefaults.containerColor.copy(alpha = BackgroundAlpha),
            contentColor = contentColorFor(BottomAppBarDefaults.containerColor),
            shape = status.commonNavigationBarShape(
                prefersCompactBottomNav = identityState.prefersCompactBottomNav,
            ),
        ) {
            Box {
                val fillsMaxWidth = status is BottomNavStatus.Expanded || status is BottomNavStatus.Collapsed.Partially
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .ifTrue(fillsMaxWidth) {
                            fillMaxWidth()
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    navItems.forEach { item ->
                        if (status.canShow(item)) key(item.stack.name) {
                            NavigationBarItem(
                                modifier = Modifier
                                    .weight(1f)
                                    .animateBounds(
                                        lookaheadScope = this@CommonNavigationBar,
                                    ),
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
                                enabled = identityState.isStable,
                                selected = item.selected && status != BottomNavStatus.Collapsed.Fully,
                                onClick = click@{
                                    when (status) {
                                        BottomNavStatus.Collapsed.Fully -> appScaffoldState.updateTransientBottomNav(
                                            show = true,
                                        )
                                        BottomNavStatus.Collapsed.Partially,
                                        BottomNavStatus.Expanded,
                                        -> {
                                            // Collapse just in case its expanded
                                            if (item.selected && onNavItemReselected()) appScaffoldState.updateTransientBottomNav(
                                                show = false,
                                            )
                                            onNavItemSelected(item)
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
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

context(paneScaffoldState: PaneScaffoldState)
private fun Modifier.commonBottomNavSharedElement(
    sharedContentState: SharedTransitionScope.SharedContentState,
) = with(paneScaffoldState) {
    sharedElement(
        sharedContentState = sharedContentState,
        animatedVisibilityScope = this,
        zIndexInOverlay = UiTokens.navigationBarSharedElementZIndex,
    )
        .renderInSharedTransitionScopeOverlay(
            zIndexInOverlay = UiTokens.navigationBarSharedElementZIndex,
            renderInOverlay = {
                isActive &&
                    isTransitionActive &&
                    !sharedContentState.isMatchFound &&
                    navigationEventStatus !is NavigationEventStatus.Completed.Cancelled
            },
        )
}

@Composable
private fun BottomNavStatus.commonNavigationBarShape(
    prefersCompactBottomNav: Boolean,
): Shape {
    val topCornerSize by animateDpAsState(
        when (this) {
            BottomNavStatus.Collapsed.Fully,
            BottomNavStatus.Collapsed.Partially,
            -> FullCornerSize
            BottomNavStatus.Expanded -> if (prefersCompactBottomNav) CompactCornerSize else RegularCornerSize
        },
    )
    val bottomCornerSize by animateDpAsState(
        when (this) {
            BottomNavStatus.Expanded -> 0.dp
            is BottomNavStatus.Collapsed -> FullCornerSize
        },
    )

    return RoundedCornerShape(
        topStart = topCornerSize,
        topEnd = topCornerSize,
        bottomStart = bottomCornerSize,
        bottomEnd = bottomCornerSize,
    )
}

private fun BottomNavStatus.canShow(
    item: NavItem,
) = when (this) {
    BottomNavStatus.Collapsed.Fully -> item.selected
    BottomNavStatus.Expanded,
    BottomNavStatus.Collapsed.Partially,
    -> true
}

private fun Modifier.commonBottomNavWidth(
    width: Dp,
    bottomNavStatus: BottomNavStatus,
) = when (bottomNavStatus) {
    BottomNavStatus.Collapsed.Fully -> width(width)
    BottomNavStatus.Expanded,
    BottomNavStatus.Collapsed.Partially,
    -> fillMaxWidth()
}

private fun Modifier.commonBottomNavPadding(
    bottomNavStatus: BottomNavStatus,
    navigationBarHeight: Dp,
): Modifier {
    return padding(
        start = if (bottomNavStatus is BottomNavStatus.Collapsed) 16.dp else 0.dp,
        end = if (bottomNavStatus is BottomNavStatus.Collapsed.Partially) 80.dp else 0.dp,
        bottom = if (bottomNavStatus is BottomNavStatus.Collapsed) navigationBarHeight
        else 0.dp,
    )
}

internal data object NavigationBarSharedElementKey

sealed class BottomNavStatus {
    internal data object Expanded : BottomNavStatus()

    internal sealed class Collapsed : BottomNavStatus() {
        data object Partially : Collapsed()
        data object Fully : Collapsed()
    }
}

private val CompactCornerSize = 0.dp
private val RegularCornerSize = 16.dp

private val FullCornerSize = 64.dp
private const val BackgroundAlpha = 0.98f

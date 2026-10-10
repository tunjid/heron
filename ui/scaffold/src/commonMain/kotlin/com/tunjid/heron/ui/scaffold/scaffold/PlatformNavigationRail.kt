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

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.tunjid.composables.constrainedsize.constrainedSizePlacement
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.scaffold.identity.isStable
import org.jetbrains.compose.resources.stringResource

@Composable
internal expect fun PaneScaffoldState.PlatformNavigationRail(
    modifier: Modifier,
    onNavItemReselected: () -> Boolean,
)

@Composable
internal fun PaneScaffoldState.CommonNavigationRail(
    modifier: Modifier,
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
                .navigationRailPosition(
                    bottomAligned = bottomAlignsNavigationRail,
                )
                .constrainedSizePlacement(
                    orientation = Orientation.Horizontal,
                    minSize = navigationRailWidth,
                    atStart = navRailPlacesAtStart(
                        layoutDirection = LocalLayoutDirection.current,
                    ),
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

/**
 * Centers the rail vertically, or aligns it to the bottom above the navigation bar inset when
 * [bottomAligned].
 */
@Composable
context(boxScope: BoxScope)
internal fun Modifier.navigationRailPosition(
    bottomAligned: Boolean,
): Modifier = with(boxScope) {
    if (bottomAligned) align(Alignment.BottomCenter)
        .padding(bottom = UiTokens.navigationBarHeight)
    else align(Alignment.Center)
}

internal fun PaneScaffoldState.navRailPlacesAtStart(
    layoutDirection: LayoutDirection,
): Boolean = (navRailEdge == NavRailEdge.Start) == (layoutDirection == LayoutDirection.Ltr)

@Composable
private fun AppScaffoldState.shouldElevateNavRail(): Boolean = remember(this) {
    derivedStateOf {
        splitLayoutState.weightAt(0)
            .times(splitLayoutState.size)
            .minus(navigationRailWidth) < minPaneWidth
    }
}.value

private val NavRailShape = RoundedCornerShape(UiTokens.NavRailWidth)

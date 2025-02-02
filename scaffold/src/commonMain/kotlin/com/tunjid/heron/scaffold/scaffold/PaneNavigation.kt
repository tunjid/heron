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
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import com.tunjid.heron.scaffold.navigation.NavItem
import com.tunjid.heron.ui.PanedSharedElementScope
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NavScaffold(
    isVisible: Boolean,
    useRail: Boolean,
    modifier: Modifier = Modifier,
    navItems: List<NavItem>,
    onNavItemSelected: (NavItem) -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier,
    ) {
        AnimatedVisibility(
            modifier = Modifier
                .zIndex(2f),
            visible = isVisible && useRail,
            enter = expandHorizontally(),
            exit = shrinkHorizontally(),
        ) {
            NavigationRail(
                navItems = navItems,
                onNavItemSelected = onNavItemSelected
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f)
        ) {
            content()
        }
    }
}


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PaneBottomAppBar(
    modifier: Modifier = Modifier,
    panedSharedElementScope: PanedSharedElementScope,
    onNavItemReselected: () -> Boolean = { false },
) = with(panedSharedElementScope) {
    val appState = LocalAppState.current
    val sharedContentState = rememberSharedContentState(BottomNavSharedElementKey)
    BottomAppBar(
        modifier = modifier
            .sharedElement(
                sharedContentState = sharedContentState,
                animatedVisibilityScope = panedSharedElementScope,
                zIndexInOverlay = BottomNavSharedElementZIndex,
            ),
    ) {
        appState.navItems.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.stack.icon,
                        contentDescription = stringResource(item.stack.titleRes),
                    )
                },
                selected = item.selected,
                onClick = {
                    if (item.selected && onNavItemReselected()) return@NavigationBarItem
                    appState.onNavItemSelected(item)
                }
            )
        }
    }
}

@Composable
private fun NavigationRail(
    modifier: Modifier = Modifier,
    navItems: List<NavItem>,
    onNavItemSelected: (NavItem) -> Unit,
) {
    NavigationRail(
        modifier = modifier,
    ) {
        navItems.forEach { item ->
            NavigationRailItem(
                selected = item.selected,
                icon = {
                    Icon(
                        imageVector = item.stack.icon,
                        contentDescription = stringResource(item.stack.titleRes),
                    )
                },
                onClick = {
                    onNavItemSelected(item)
                }
            )
        }
    }
}

private data object BottomNavSharedElementKey

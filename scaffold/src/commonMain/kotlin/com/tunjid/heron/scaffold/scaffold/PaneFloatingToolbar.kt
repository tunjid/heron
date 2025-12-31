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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.tunjid.heron.scaffold.scaffold

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarDefaults.floatingToolbarVerticalNestedScroll
import androidx.compose.material3.FloatingToolbarExitDirection.Companion.Bottom
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource

/**
 * Creates a [FloatingToolbarScrollBehavior] that exits toward the bottom.
 */
@Composable
fun rememberFloatingToolbarScrollBehavior(): FloatingToolbarScrollBehavior {
    return FloatingToolbarDefaults.exitAlwaysScrollBehavior(exitDirection = Bottom)
}

/**
 * Creates a nested scroll modifier for controlling floating toolbar expansion/collapse.
 */
@Composable
fun Modifier.floatingToolbarNestedScroll(
    expanded: Boolean,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
): Modifier = this.floatingToolbarVerticalNestedScroll(
    expanded = expanded,
    onExpand = onExpand,
    onCollapse = onCollapse,
)

/**
 * A floating toolbar that combines the navigation bar and FAB using Material 3 Expressive.
 * This version uses scroll behavior to hide/show the entire toolbar.
 */
@Composable
fun PaneScaffoldState.PaneFloatingToolbar(
    modifier: Modifier = Modifier,
    scrollBehavior: FloatingToolbarScrollBehavior? = null,
    onNavItemReselected: () -> Boolean = { false },
    fab: (@Composable () -> Unit)? = null,
) {
    if (!canShowNavigationBar) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        val floatingActionButton: @Composable () -> Unit = if (fab != null && canShowFab) fab else {
            {}
        }
        HorizontalFloatingToolbar(
            expanded = true,
            floatingActionButton = floatingActionButton,
            scrollBehavior = scrollBehavior,
            colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
            content = {
                appState.navItems.forEach { item ->
                    val selected = item.selected
                    NavItemButton(
                        selected = selected,
                        icon = item.stack.icon,
                        contentDescription = stringResource(item.stack.titleRes),
                        hasBadge = item.hasBadge,
                        onClick = {
                            if (selected && onNavItemReselected()) return@NavItemButton
                            appState.onNavItemSelected(item)
                        },
                    )
                }
            },
        )
    }
}

/**
 * An expandable floating toolbar that combines the navigation bar and FAB using Material 3 Expressive.
 * This version expands/collapses based on scroll direction.
 */
@Composable
fun PaneScaffoldState.PaneExpandableFloatingToolbar(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    onNavItemReselected: () -> Boolean = { false },
    fab: (@Composable () -> Unit)? = null,
) {
    if (!canShowNavigationBar) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        val floatingActionButton: @Composable () -> Unit = if (fab != null && canShowFab) fab else {
            {}
        }
        HorizontalFloatingToolbar(
            expanded = expanded,
            floatingActionButton = floatingActionButton,
            colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
            content = {
                appState.navItems.forEach { item ->
                    val selected = item.selected
                    NavItemButton(
                        selected = selected,
                        icon = item.stack.icon,
                        contentDescription = stringResource(item.stack.titleRes),
                        hasBadge = item.hasBadge,
                        onClick = {
                            if (selected && onNavItemReselected()) return@NavItemButton
                            appState.onNavItemSelected(item)
                        },
                    )
                }
            },
        )
    }
}

/**
 * A navigation item button that shows selected state with a circle background.
 */
@Composable
private fun NavItemButton(
    selected: Boolean,
    icon: ImageVector,
    contentDescription: String,
    hasBadge: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Box(
            modifier = if (selected) {
                Modifier
                    .background(
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = CircleShape,
                    )
                    .padding(8.dp)
            } else {
                Modifier.padding(8.dp)
            },
            contentAlignment = Alignment.Center,
        ) {
            BadgedBox(
                badge = {
                    if (hasBadge) Badge(Modifier.size(4.dp))
                },
                content = {
                    Icon(
                        imageVector = icon,
                        contentDescription = contentDescription,
                        tint = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                },
            )
        }
    }
}

/**
 * A FAB that can be used within the floating toolbar.
 * Uses the vibrant style to match the toolbar colors.
 */
@Composable
fun PaneScaffoldState.PaneFloatingToolbarFab(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val fabAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.6f,
        label = "fabAlpha"
    )
    FloatingToolbarDefaults.VibrantFloatingActionButton(
        modifier = modifier.graphicsLayer { alpha = fabAlpha },
        onClick = { if (enabled) onClick() },
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
        )
    }
}

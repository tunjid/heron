package com.tunjid.heron.scaffold.scaffold

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tunjid.heron.scaffold.navigation.NavItem
import com.tunjid.heron.ui.SharedElementScope
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
            modifier = Modifier,
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
        ) {
            content()
        }
    }
}


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BottomAppBar(
    modifier: Modifier = Modifier,
    sharedElementScope: SharedElementScope,
) = with(sharedElementScope) {
    val appState = LocalAppState.current
    AnimatedVisibility(
        modifier = modifier,
        visible = appState.splitLayoutState.size <= SecondaryPaneMinWidthBreakpointDp,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        content = {
            val sharedContentState = rememberSharedContentState(BottomNavSharedElementKey)
            BottomAppBar(
                modifier = Modifier
                    .sharedElement(
                        state = sharedContentState,
                        animatedVisibilityScope = sharedElementScope,
                        zIndexInOverlay = 2f,
                    )
                    .renderInSharedTransitionScopeOverlay(
                        renderInOverlay = {
                            sharedElementScope.isTransitionActive && !sharedContentState.isMatchFound
                        }
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
                            appState.onNavItemSelected(item)
                        }
                    )
                }
            }
        }
    )
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
                        contentDescription = null,
                    )
                },
                label = {
                    Text(text = stringResource(item.stack.titleRes))
                },
                onClick = {
                    onNavItemSelected(item)
                }
            )
        }
    }
}

private data object BottomNavSharedElementKey

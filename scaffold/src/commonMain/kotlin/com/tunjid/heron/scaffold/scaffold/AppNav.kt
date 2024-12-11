package com.tunjid.heron.scaffold.scaffold

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tunjid.heron.scaffold.navigation.NavItem
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
    Column(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.weight(1f)
        ) {
            AnimatedVisibility(
                modifier = Modifier
                    .wrapContentWidth(),
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
                    .animateContentSize()
            ) {
                content()
            }
        }
        AnimatedVisibility(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            visible = isVisible && !useRail,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            BottomAppBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                navItems = navItems,
                onNavItemSelected = onNavItemSelected
            )
        }
    }
}


@Composable
private fun BottomAppBar(
    modifier: Modifier = Modifier,
    navItems: List<NavItem>,
    onNavItemSelected: (NavItem) -> Unit
) {
    BottomAppBar(
        modifier = modifier,
    ) {
        navItems.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.stack.icon,
                        contentDescription = null,
                    )
                },
                label = { Text(text = stringResource(item.stack.titleRes)) },
                selected = item.selected,
                onClick = {
                    onNavItemSelected(item)
                }
            )
        }
    }
}

@Composable
private fun NavigationRail(
    modifier: Modifier = Modifier,
    navItems: List<NavItem>,
    onNavItemSelected: (NavItem) -> Unit
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

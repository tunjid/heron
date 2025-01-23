package com.tunjid.heron.scaffold.scaffold

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.spring
import androidx.compose.animation.defaultDecayAnimationSpec
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
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
fun BottomAppBar(
    modifier: Modifier = Modifier,
    sharedElementScope: SharedElementScope,
) = with(sharedElementScope) {
    val appState = LocalAppState.current
    val sharedContentState = rememberSharedContentState(BottomNavSharedElementKey)
    BottomAppBar(
        modifier = modifier
            .sharedElement(
                sharedContentState = sharedContentState,
                animatedVisibilityScope = sharedElementScope,
                zIndexInOverlay = BottomNavSharedElementZIndex,
            ),
    ) {
        appState.navItems.forEach { item ->
            AnchoredDraggableState(
                initialValue = 1f,
                positionalThreshold = { distance: Float -> distance  },
                velocityThreshold = { 100f },
                snapAnimationSpec = spring(),
                decayAnimationSpec = rememberSplineBasedDecay(),
                anchors = DraggableAnchors {}
            )
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

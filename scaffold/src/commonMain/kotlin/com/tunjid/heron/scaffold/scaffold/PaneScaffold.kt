package com.tunjid.heron.scaffold.scaffold

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull

@Composable
fun PaneScope<ThreePane, Route>.PaneScaffold(
    modifier: Modifier = Modifier,
    showNavigation: Boolean = true,
    snackBarMessages: List<String> = emptyList(),
    onSnackBarMessageConsumed: (String) -> Unit,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = {
            if (paneState.pane == ThreePane.Primary) bottomBar()
        },
        floatingActionButton = {
            if (paneState.pane == ThreePane.Primary) floatingActionButton()
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        contentWindowInsets = contentWindowInsets,
        content = content,
    )

    val updatedMessages = rememberUpdatedState(snackBarMessages.firstOrNull())
    LaunchedEffect(Unit) {
        snapshotFlow { updatedMessages.value }
            .filterNotNull()
            .filterNot(String::isNullOrBlank)
            .collect { message ->
                snackbarHostState.showSnackbar(
                    message = message
                )
                onSnackBarMessageConsumed(message)
            }
    }

    if (paneState.pane == ThreePane.Primary) {
        val appState = LocalAppState.current
        LaunchedEffect(showNavigation) {
            appState.showNavigation = showNavigation
        }
    }
}

val ToolbarHeight = 64.dp

val TabsHeight = 48.dp

val StatusBarHeight: Dp
    @Composable get() = with(LocalDensity.current) {
        statusBarHeight
    }

val Density.statusBarHeight: Dp
    @Composable get() {
        val statusBarInsets = WindowInsets.statusBars
        return statusBarInsets.run {
            getTop(this@statusBarHeight) + getBottom(this@statusBarHeight)
        }.toDp()
    }
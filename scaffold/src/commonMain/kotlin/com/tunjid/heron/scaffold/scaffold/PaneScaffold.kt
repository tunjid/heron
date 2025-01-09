package com.tunjid.heron.scaffold.scaffold

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull

class PaneScaffoldState internal constructor(
    private val appState: AppState,
) {
    val isExpanded get() = appState.isExpanded
}

@Composable
fun PaneScope<ThreePane, Route>.PaneScaffold(
    modifier: Modifier = Modifier,
    showNavigation: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.background,
    snackBarMessages: List<String> = emptyList(),
    onSnackBarMessageConsumed: (String) -> Unit,
    topBar: @Composable PaneScaffoldState.() -> Unit = {},
    floatingActionButton: @Composable PaneScaffoldState.() -> Unit = {},
    bottomBar: @Composable PaneScaffoldState.() -> Unit = {},
    content: @Composable PaneScaffoldState.(PaddingValues) -> Unit,
) {
    val appState = LocalAppState.current
    val snackbarHostState = remember { SnackbarHostState() }
    val paneScaffoldState = remember(appState) { PaneScaffoldState(appState) }

    Scaffold(
        modifier = modifier,
        containerColor = containerColor,
        topBar = {
            paneScaffoldState.topBar()
        },
        floatingActionButton = {
            paneScaffoldState.floatingActionButton()
        },
        bottomBar = {
            paneScaffoldState.bottomBar()
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        content = { paddingValues ->
            paneScaffoldState.content(paddingValues)
        },
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

val BottomNavHeight: Dp = 80.dp

val Density.statusBarHeight: Dp
    @Composable get() {
        val statusBarInsets = WindowInsets.statusBars
        return statusBarInsets.run {
            getTop(this@statusBarHeight) + getBottom(this@statusBarHeight)
        }.toDp()
    }
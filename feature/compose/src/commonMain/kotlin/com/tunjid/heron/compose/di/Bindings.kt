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

package com.tunjid.heron.compose.di

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.compose.Action
import com.tunjid.heron.compose.ActualComposeViewModel
import com.tunjid.heron.compose.ComposeScreen
import com.tunjid.heron.compose.ComposeStateHolder
import com.tunjid.heron.compose.RouteViewModelInitializer
import com.tunjid.heron.compose.ui.ComposePostBottomBar
import com.tunjid.heron.compose.ui.ComposePostFabRow
import com.tunjid.heron.compose.ui.TopAppBarFab
import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.coroutines.viewModelCoroutineScope
import com.tunjid.heron.ui.scaffold.di.ScaffoldBindings
import com.tunjid.heron.ui.scaffold.scaffold.NavigationContentTransformer
import com.tunjid.heron.ui.scaffold.scaffold.PaneNavigationRail
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.ui.scaffold.scaffold.PoppableDestinationTopAppBar
import com.tunjid.heron.ui.scaffold.scaffold.predictiveBackPlacement
import com.tunjid.heron.ui.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.mutator.compose.produceStateWithLifecycle
import com.tunjid.treenav.compose.PaneEntry
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.urlRouteMatcher
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.StringKey

private const val RoutePattern = "/compose"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
)

@BindingContainer
object ComposeNavigationBindings {

    @Provides
    @IntoMap
    @StringKey(RoutePattern)
    fun provideRouteMatcher(): RouteMatcher =
        urlRouteMatcher(
            routePattern = RoutePattern,
            routeMapper = ::createRoute,
        )
}

@BindingContainer
class ComposeBindings(
    @Includes dataBindings: DataBindings,
    @Includes scaffoldBindings: ScaffoldBindings,
) {

    @Provides
    @IntoMap
    @StringKey(RoutePattern)
    fun providePaneEntry(
        viewModelInitializer: RouteViewModelInitializer,
        navigationContentTransformer: NavigationContentTransformer,
    ): PaneEntry<ThreePane, Route> = routePaneEntry(
        viewModelInitializer = viewModelInitializer,
        navigationContentTransformer = navigationContentTransformer,
    )

    private fun routePaneEntry(
        viewModelInitializer: RouteViewModelInitializer,
        navigationContentTransformer: NavigationContentTransformer,
    ) = threePaneEntry(
        contentTransform = navigationContentTransformer::contentTransform,
        render = { route ->
            val stateHolder: ComposeStateHolder = viewModel<ActualComposeViewModel> {
                viewModelInitializer.invoke(
                    scope = viewModelCoroutineScope(),
                    route = route,
                )
            }
            val state = stateHolder.produceStateWithLifecycle()
            val paneScaffoldState = rememberPaneScaffoldState()

            paneScaffoldState.PaneScaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .predictiveBackPlacement(paneScaffoldState = paneScaffoldState),
                showNavigation = true,
                snackBarMessages = state.messages,
                onSnackBarMessageConsumed = {
                    stateHolder.accept(Action.SnackbarDismissed(it))
                },
                topBar = {
                    PoppableDestinationTopAppBar(
                        actions = {
                            TopAppBarFab(
                                modifier = Modifier,
                                state = state,
                                onCreatePost = stateHolder.accept,
                            )
                            Spacer(Modifier.width(16.dp))
                        },
                        onBackPressed = {
                            stateHolder.accept(Action.Navigate.Pop)
                        },
                    )
                },
                floatingActionButton = {
                    ComposePostFabRow(
                        modifier = Modifier,
                        state = state,
                        onAction = stateHolder.accept,
                    )
                },
                navigationBar = {
                    val borderColor = MaterialTheme.colorScheme.outline
                    val imePadding = WindowInsets.ime.asPaddingValues()
                    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()
                    val imeShowing by remember {
                        derivedStateOf {
                            imePadding.calculateBottomPadding() > navBarPadding.calculateBottomPadding()
                        }
                    }
                    val hasBlankText by remember {
                        derivedStateOf { state.postText.text.isBlank() }
                    }
                    ComposePostBottomBar(
                        modifier = Modifier
                            .drawBehind {
                                drawLine(
                                    color = borderColor,
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, 0f),
                                    strokeWidth = 1f,
                                )
                            }
                            .padding(horizontal = 8.dp)
                            .imePadding()
                            .windowInsetsPadding(WindowInsets.navigationBars),
                        postText = state.postText,
                        photos = state.photos,
                        onMediaEdited = stateHolder.accept,
                    )

                    DisposableEffect(hasBlankText, imeShowing) {
                        val fabExpanded = hasBlankText || !imeShowing
                        stateHolder.accept(Action.SetFabExpanded(expanded = fabExpanded))
                        onDispose { }
                    }
                },
                navigationRail = {
                    PaneNavigationRail()
                },
                content = { paddingValues ->
                    ComposeScreen(
                        paneScaffoldState = this,
                        modifier = Modifier
                            .padding(paddingValues)
                            .padding(
                                // This padding is solely for the post interaction button
                                bottom = UiTokens.toolbarHeight,
                            ),
                        state = state,
                        actions = stateHolder.accept,
                    )
                },
            )
        },
    )
}

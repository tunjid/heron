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

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.compose.Action
import com.tunjid.heron.compose.ActualComposeViewModel
import com.tunjid.heron.compose.ComposeScreen
import com.tunjid.heron.compose.RouteViewModelInitializer
import com.tunjid.heron.compose.ui.BottomAppBarFab
import com.tunjid.heron.compose.ui.ComposePostBottomBar
import com.tunjid.heron.compose.ui.TopAppBarFab
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.fromBase64EncodedUrl
import com.tunjid.heron.data.di.DataComponent
import com.tunjid.heron.scaffold.di.ScaffoldComponent
import com.tunjid.heron.scaffold.navigation.routePatternAndMatcher
import com.tunjid.heron.scaffold.scaffold.PaneNavigationRail
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.PoppableDestinationTopAppBar
import com.tunjid.heron.scaffold.scaffold.predictiveBackBackgroundModifier
import com.tunjid.heron.scaffold.scaffold.predictiveBackContentTransform
import com.tunjid.heron.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.viewModelCoroutineScope
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.optionalMappedRouteQuery
import com.tunjid.treenav.strings.optionalRouteQuery
import com.tunjid.treenav.strings.routeOf
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.KmpComponentCreate
import me.tatarka.inject.annotations.Provides

private const val RoutePattern = "/compose"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
)

internal val Route.creationType: Post.Create? by optionalMappedRouteQuery(
    mapper = String::fromBase64EncodedUrl
)

internal val Route.sharedElementPrefix by optionalRouteQuery()

@KmpComponentCreate
expect fun ComposeNavigationComponent.Companion.create(): ComposeNavigationComponent

@KmpComponentCreate
expect fun ComposeComponent.Companion.create(
    dataComponent: DataComponent,
    scaffoldComponent: ScaffoldComponent,
): ComposeComponent

@Component
abstract class ComposeNavigationComponent {
    companion object

    @IntoMap
    @Provides
    fun profileRouteParser(): Pair<String, RouteMatcher> =
        routePatternAndMatcher(
            routePattern = RoutePattern,
            routeMapper = ::createRoute,
        )

}

@Component
abstract class ComposeComponent(
    @Component val dataComponent: DataComponent,
    @Component val scaffoldComponent: ScaffoldComponent,
) {
    companion object

    @IntoMap
    @Provides
    fun routePattern(
        viewModelInitializer: RouteViewModelInitializer,
    ) = RoutePattern to routePaneEntry(
        viewModelInitializer = viewModelInitializer,
    )

    private fun routePaneEntry(
        viewModelInitializer: RouteViewModelInitializer,
    ) = threePaneEntry(
        contentTransform = predictiveBackContentTransform,
        render = { route ->
            val viewModel = viewModel<ActualComposeViewModel> {
                viewModelInitializer.invoke(
                    scope = viewModelCoroutineScope(),
                    route = route,
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()

            rememberPaneScaffoldState().PaneScaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .predictiveBackBackgroundModifier(paneScope = this),
                showNavigation = true,
                snackBarMessages = state.messages,
                onSnackBarMessageConsumed = {
                },
                topBar = {
                    PoppableDestinationTopAppBar(
                        actions = {
                            TopAppBarFab(
                                modifier = Modifier,
                                state = state,
                                onCreatePost = viewModel.accept,
                            )
                        },
                        onBackPressed = {
                            viewModel.accept(Action.Navigate.Pop)
                        }
                    )
                },
                floatingActionButton = {
                    BottomAppBarFab(
                        modifier = Modifier,
                        state = state,
                        onCreatePost = viewModel.accept,
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
                        onMediaEdited = viewModel.accept,
                    )

                    DisposableEffect(hasBlankText, imeShowing) {
                        val fabExpanded = hasBlankText || !imeShowing
                        viewModel.accept(Action.SetFabExpanded(expanded = fabExpanded))
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
                            .padding(
                                top = paddingValues.calculateTopPadding()
                            )
                            .imePadding(),
                        state = state,
                        actions = viewModel.accept,
                    )
                }
            )
        }
    )
}

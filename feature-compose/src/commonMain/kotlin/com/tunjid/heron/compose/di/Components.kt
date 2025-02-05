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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.compose.Action
import com.tunjid.heron.compose.ActualComposeViewModel
import com.tunjid.heron.compose.ComposeScreen
import com.tunjid.heron.compose.ComposeViewModelCreator
import com.tunjid.heron.compose.ui.BottomAppBarFab
import com.tunjid.heron.compose.ui.ComposePostBottomBar
import com.tunjid.heron.compose.ui.TopAppBarFab
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.fromBase64EncodedUrl
import com.tunjid.heron.data.di.DataComponent
import com.tunjid.heron.scaffold.di.ScaffoldComponent
import com.tunjid.heron.scaffold.navigation.AppStack
import com.tunjid.heron.scaffold.navigation.routeAndMatcher
import com.tunjid.heron.scaffold.navigation.routeOf
import com.tunjid.heron.scaffold.scaffold.PaneNavigationRail
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.PoppableDestinationTopAppBar
import com.tunjid.heron.scaffold.scaffold.predictiveBackBackgroundModifier
import com.tunjid.treenav.compose.threepane.threePaneListDetailStrategy
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
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

internal val Route.creationType
    get(): Post.Create? = routeParams.queryParams["type"]?.firstOrNull()?.fromBase64EncodedUrl()

internal val Route.sharedElementPrefix
    get() = routeParams.queryParams["sharedElementPrefix"]?.firstOrNull()

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
        routeAndMatcher(
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
    fun routeAdaptiveConfiguration(
        creator: ComposeViewModelCreator,
    ) = RoutePattern to threePaneListDetailStrategy(
        render = { route ->
            val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
            val viewModel = viewModel<ActualComposeViewModel> {
                creator.invoke(
                    scope = lifecycleCoroutineScope,
                    route = route,
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()

            PaneScaffold(
                modifier = Modifier
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
                            .padding(
                                imePadding.takeIf {
                                    imeShowing
                                } ?: navBarPadding
                            ),
                        postText = state.postText,
                        onMediaEdited = viewModel.accept,
                    )

                    DisposableEffect(hasBlankText, imeShowing) {
                        val fabExpanded = hasBlankText || !imeShowing
                        viewModel.accept(Action.SetFabExpanded(expanded = fabExpanded))
                        onDispose { }
                    }
                },
                navigationRail = {
                    PaneNavigationRail(
                        badge = { stack ->
                            if (stack == AppStack.Notifications && state.unreadNotificationCount != 0L) {
                                Badge(Modifier.size(4.dp))
                            }
                        },
                    )
                },
                content = { paddingValues ->
                    ComposeScreen(
                        paneScaffoldState = this,
                        modifier = Modifier
                            .padding(
                                top = paddingValues.calculateTopPadding()
                            ),
                        state = state,
                        actions = viewModel.accept,
                    )
                }
            )
        }
    )
}

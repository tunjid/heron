/*
 * Copyright 2024 Adetunji Dahunsi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.heron.home.di

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.di.DataComponent
import com.tunjid.heron.home.Action
import com.tunjid.heron.home.ActualHomeStateHolder
import com.tunjid.heron.home.HomeScreen
import com.tunjid.heron.home.HomeStateHolderCreator
import com.tunjid.heron.scaffold.di.ScaffoldComponent
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.routeAndMatcher
import com.tunjid.heron.scaffold.navigation.routeOf
import com.tunjid.heron.scaffold.scaffold.BottomAppBar
import com.tunjid.heron.scaffold.scaffold.ComposeFab
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.RootDestinationTopAppBar
import com.tunjid.heron.scaffold.scaffold.StatusBarHeight
import com.tunjid.heron.scaffold.scaffold.ToolbarHeight
import com.tunjid.heron.scaffold.scaffold.isFabExpanded
import com.tunjid.heron.scaffold.scaffold.predictiveBackBackgroundModifier
import com.tunjid.heron.scaffold.ui.bottomAppBarAccumulatedOffsetNestedScrollConnection
import com.tunjid.heron.scaffold.ui.rememberAccumulatedOffsetNestedScrollConnection
import com.tunjid.heron.ui.SharedElementScope
import com.tunjid.heron.ui.requirePanedSharedElementScope
import com.tunjid.treenav.compose.threepane.threePaneListDetailStrategy
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides

internal const val RoutePattern = "/home"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
)

@Component
abstract class HomeNavigationComponent {

    @IntoMap
    @Provides
    fun profileRouteParser(): Pair<String, RouteMatcher> =
        routeAndMatcher(
            routePattern = RoutePattern,
            routeMapper = ::createRoute,
        )

}

@Component
abstract class HomeComponent(
    @Component val dataComponent: DataComponent,
    @Component val scaffoldComponent: ScaffoldComponent,
) {

    @IntoMap
    @Provides
    fun routeAdaptiveConfiguration(
        creator: HomeStateHolderCreator,
    ) = RoutePattern to threePaneListDetailStrategy(
        render = { route ->
            val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
            val viewModel = viewModel<ActualHomeStateHolder> {
                creator.invoke(
                    scope = lifecycleCoroutineScope,
                    route = route,
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()

            val sharedElementScope = requirePanedSharedElementScope()

            val statusBarHeight = StatusBarHeight
            val topAppBarOffsetNestedScrollConnection =
                rememberAccumulatedOffsetNestedScrollConnection(
                    maxOffset = { Offset.Zero },
                    minOffset = { Offset(x = 0f, y = -(statusBarHeight + ToolbarHeight).toPx()) },
                )
            val bottomNavAccumulatedOffsetNestedScrollConnection =
                bottomAppBarAccumulatedOffsetNestedScrollConnection()

            PaneScaffold(
                modifier = Modifier
                    .predictiveBackBackgroundModifier(paneScope = this)
                    .nestedScroll(topAppBarOffsetNestedScrollConnection)
                    .nestedScroll(bottomNavAccumulatedOffsetNestedScrollConnection),
                showNavigation = true,
                snackBarMessages = state.messages,
                onSnackBarMessageConsumed = {
                },
                topBar = {
                    RootDestinationTopAppBar(
                        modifier = Modifier.offset {
                            topAppBarOffsetNestedScrollConnection.offset.round()
                        },
                        sharedElementScope = sharedElementScope,
                        signedInProfile = state.signedInProfile,
                        onSignedInProfileClicked = { profile, sharedElementKey ->
                            viewModel.accept(
                                Action.Navigate.DelegateTo(
                                    NavigationAction.Common.ToProfile(
                                        referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                                        profile = profile,
                                        avatarSharedElementKey = sharedElementKey,
                                    )
                                )
                            )
                        },
                    )
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .height(statusBarHeight)
                            .fillMaxWidth()
                    )
                },
                floatingActionButton = {
                    ComposeFab(
                        modifier = Modifier
                            .offset {
                                if (usesNavRail) IntOffset.Zero
                                else bottomNavAccumulatedOffsetNestedScrollConnection.offset.round()
                            },
                        sharedElementScope = sharedElementScope,
                        expanded = isFabExpanded(
                            offset = bottomNavAccumulatedOffsetNestedScrollConnection.offset
                        ),
                        onClick = {
                            viewModel.accept(
                                Action.Navigate.DelegateTo(
                                    NavigationAction.Common.ComposePost(
                                        type = Post.Create.Timeline
                                    )
                                )
                            )
                        }
                    )
                },
                bottomBar = {
                    BottomBar(
                        sharedElementScope = sharedElementScope,
                        modifier = Modifier
                            .offset {
                                bottomNavAccumulatedOffsetNestedScrollConnection.offset.round()
                            }
                    )
                },
                content = {
                    HomeScreen(
                        sharedElementScope = requirePanedSharedElementScope(),
                        state = state,
                        actions = viewModel.accept,
                        modifier = Modifier,
                    )
                }
            )
        }
    )
}

@Composable
private fun BottomBar(
    modifier: Modifier = Modifier,
    sharedElementScope: SharedElementScope,
) {
    BottomAppBar(
        modifier = modifier,
        sharedElementScope = sharedElementScope,
    )
}

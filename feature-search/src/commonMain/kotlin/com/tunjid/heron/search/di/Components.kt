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

package com.tunjid.heron.search.di

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.data.di.DataComponent
import com.tunjid.heron.scaffold.di.ScaffoldComponent
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.routeAndMatcher
import com.tunjid.heron.scaffold.navigation.routeOf
import com.tunjid.heron.scaffold.scaffold.BottomAppBar
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.RootDestinationTopAppBar
import com.tunjid.heron.scaffold.scaffold.predictiveBackBackgroundModifier
import com.tunjid.heron.scaffold.ui.bottomAppBarAccumulatedOffsetNestedScrollConnection
import com.tunjid.heron.search.Action
import com.tunjid.heron.search.ActualSearchStateHolder
import com.tunjid.heron.search.ScreenLayout
import com.tunjid.heron.search.SearchScreen
import com.tunjid.heron.search.SearchStateHolderCreator
import com.tunjid.heron.search.ui.SearchBar
import com.tunjid.heron.ui.SharedElementScope
import com.tunjid.heron.ui.requirePanedSharedElementScope
import com.tunjid.treenav.compose.threepane.threePaneListDetailStrategy
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import heron.feature_search.generated.resources.Res
import heron.feature_search.generated.resources.close_search
import heron.feature_search.generated.resources.search
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import org.jetbrains.compose.resources.stringResource

private const val RoutePattern = "/search"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
)

@Component
abstract class SearchNavigationComponent {

    @IntoMap
    @Provides
    fun profileRouteParser(): Pair<String, RouteMatcher> =
        routeAndMatcher(
            routePattern = RoutePattern,
            routeMapper = ::createRoute,
        )

}

@Component
abstract class SearchComponent(
    @Component val dataComponent: DataComponent,
    @Component val scaffoldComponent: ScaffoldComponent,
) {

    @IntoMap
    @Provides
    fun routeAdaptiveConfiguration(
        creator: SearchStateHolderCreator,
    ) = RoutePattern to threePaneListDetailStrategy(
        render = { route ->
            val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
            val viewModel = viewModel<ActualSearchStateHolder> {
                creator.invoke(
                    scope = lifecycleCoroutineScope,
                    route = route,
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()

            val sharedElementScope = requirePanedSharedElementScope()

            val bottomNavAccumulatedOffsetNestedScrollConnection =
                bottomAppBarAccumulatedOffsetNestedScrollConnection()

            PaneScaffold(
                modifier = Modifier
                    .predictiveBackBackgroundModifier(paneScope = this)
                    .nestedScroll(bottomNavAccumulatedOffsetNestedScrollConnection),
                showNavigation = true,
                snackBarMessages = state.messages,
                onSnackBarMessageConsumed = {
                },
                topBar = {
                    RootDestinationTopAppBar(
                        modifier = Modifier,
                        sharedElementScope = sharedElementScope,
                        signedInProfile = state.signedInProfile,
                        title = {
                            SearchBar(
                                searchQuery = state.currentQuery,
                                layout = state.layout,
                                onSearchAction = viewModel.accept,
                            )
                        },
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
                    SearchScreen(
                        sharedElementScope = requirePanedSharedElementScope(),
                        modifier = Modifier,
                        state = state,
                        actions = viewModel.accept,
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

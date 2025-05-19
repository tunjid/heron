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

package com.tunjid.heron.search.di

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.data.di.DataComponent
import com.tunjid.heron.scaffold.di.ScaffoldComponent
import com.tunjid.heron.scaffold.navigation.AppStack
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.routeAndMatcher
import com.tunjid.heron.scaffold.navigation.routeOf
import com.tunjid.heron.scaffold.scaffold.PaneNavigationBar
import com.tunjid.heron.scaffold.scaffold.PaneNavigationRail
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.RootDestinationTopAppBar
import com.tunjid.heron.scaffold.scaffold.predictiveBackBackgroundModifier
import com.tunjid.heron.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.scaffold.ui.bottomNavigationNestedScrollConnection
import com.tunjid.heron.search.Action
import com.tunjid.heron.search.ActualSearchViewModel
import com.tunjid.heron.search.SearchScreen
import com.tunjid.heron.search.SearchViewModelCreator
import com.tunjid.heron.search.ui.SearchBar
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.KmpComponentCreate
import me.tatarka.inject.annotations.Provides

private const val RoutePattern = "/search"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
)

@KmpComponentCreate
expect fun SearchNavigationComponent.Companion.create(): SearchNavigationComponent

@KmpComponentCreate
expect fun SearchComponent.Companion.create(
    dataComponent: DataComponent,
    scaffoldComponent: ScaffoldComponent,
): SearchComponent

@Component
abstract class SearchNavigationComponent {
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
abstract class SearchComponent(
    @Component val dataComponent: DataComponent,
    @Component val scaffoldComponent: ScaffoldComponent,
) {
    companion object

    @IntoMap
    @Provides
    fun routeAdaptiveConfiguration(
        creator: SearchViewModelCreator,
    ) = RoutePattern to threePaneEntry(
        render = { route ->
            val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
            val viewModel = viewModel<ActualSearchViewModel> {
                creator.invoke(
                    scope = lifecycleCoroutineScope,
                    route = route,
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()

            val bottomNavigationNestedScrollConnection =
                bottomNavigationNestedScrollConnection()

            rememberPaneScaffoldState().PaneScaffold(
                modifier = Modifier
                    .predictiveBackBackgroundModifier(paneScope = this)
                    .nestedScroll(bottomNavigationNestedScrollConnection),
                showNavigation = true,
                snackBarMessages = state.messages,
                onSnackBarMessageConsumed = {
                },
                topBar = {
                    RootDestinationTopAppBar(
                        modifier = Modifier,
                        signedInProfile = state.signedInProfile,
                        title = {
                            SearchBar(
                                searchQuery = state.currentQuery,
                                onQueryChanged = { query ->
                                    viewModel.accept(
                                        Action.Search.OnSearchQueryChanged(query)
                                    )
                                },
                                onQueryConfirmed = {
                                    viewModel.accept(
                                        Action.Search.OnSearchQueryConfirmed(isLocalOnly = false)
                                    )
                                }
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
                navigationBar = {
                    PaneNavigationBar(
                        modifier = Modifier
                            .offset {
                                bottomNavigationNestedScrollConnection.offset.round()
                            },
                    )
                },
                navigationRail = {
                    PaneNavigationRail()
                },
                content = {
                    SearchScreen(
                        paneScaffoldState = this,
                        modifier = Modifier,
                        state = state,
                        actions = viewModel.accept,
                    )
                }
            )
        }
    )
}
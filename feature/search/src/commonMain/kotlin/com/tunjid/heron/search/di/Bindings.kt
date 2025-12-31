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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.round
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.scaffold.di.ScaffoldBindings
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.profileDestination
import com.tunjid.heron.scaffold.scaffold.PaneExpandableFloatingToolbar
import com.tunjid.heron.scaffold.scaffold.PaneFloatingToolbarFab
import com.tunjid.heron.scaffold.scaffold.PaneNavigationRail
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.PaneSnackbarHost
import com.tunjid.heron.scaffold.scaffold.PoppableDestinationTopAppBar
import com.tunjid.heron.scaffold.scaffold.RootDestinationTopAppBar
import com.tunjid.heron.scaffold.scaffold.floatingToolbarNestedScroll
import com.tunjid.heron.scaffold.scaffold.predictiveBackContentTransform
import com.tunjid.heron.scaffold.scaffold.predictiveBackPlacement
import com.tunjid.heron.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.viewModelCoroutineScope
import com.tunjid.heron.search.Action
import com.tunjid.heron.search.RouteViewModelInitializer
import com.tunjid.heron.search.SearchScreen
import com.tunjid.heron.search.SearchViewModel
import com.tunjid.heron.search.ui.SearchBar
import com.tunjid.heron.ui.topAppBarNestedScrollConnection
import com.tunjid.heron.ui.verticalOffsetProgress
import com.tunjid.treenav.compose.PaneEntry
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.routePath
import com.tunjid.treenav.strings.urlRouteMatcher
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.StringKey

private const val RoutePattern = "/search"
private const val RouteQueryPattern = "/search/{query}"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
)

internal val Route.query by routePath(default = "")

@BindingContainer
object SearchNavigationBindings {

    @Provides
    @IntoMap
    @StringKey(RoutePattern)
    fun provideRouteMatcher(): RouteMatcher =
        urlRouteMatcher(
            routePattern = RoutePattern,
            routeMapper = ::createRoute,
        )

    @Provides
    @IntoMap
    @StringKey(RouteQueryPattern)
    fun provideRouteQueryMatcher(): RouteMatcher =
        urlRouteMatcher(
            routePattern = RouteQueryPattern,
            routeMapper = ::createRoute,
        )
}

@BindingContainer
class SearchBindings(
    @Includes dataBindings: DataBindings,
    @Includes scaffoldBindings: ScaffoldBindings,
) {

    @Provides
    @IntoMap
    @StringKey(RoutePattern)
    fun providePaneEntry(
        viewModelInitializer: RouteViewModelInitializer,
    ): PaneEntry<ThreePane, Route> = routePaneEntry(
        viewModelInitializer = viewModelInitializer,
    )

    @Provides
    @IntoMap
    @StringKey(RouteQueryPattern)
    fun providePaneQueryEntry(
        viewModelInitializer: RouteViewModelInitializer,
    ): PaneEntry<ThreePane, Route> = routePaneEntry(
        viewModelInitializer = viewModelInitializer,
    )

    private fun routePaneEntry(
        viewModelInitializer: RouteViewModelInitializer,
    ) = threePaneEntry(
        contentTransform = predictiveBackContentTransform,
        render = { route ->
            val viewModel = viewModel<SearchViewModel> {
                viewModelInitializer.invoke(
                    scope = viewModelCoroutineScope(),
                    route = route,
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()

            val topAppBarNestedScrollConnection =
                topAppBarNestedScrollConnection()

            var floatingToolbarExpanded by rememberSaveable { mutableStateOf(true) }
            val searchBarFocusRequester = remember { FocusRequester() }

            rememberPaneScaffoldState().PaneScaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .predictiveBackPlacement(paneScope = this)
                    .nestedScroll(topAppBarNestedScrollConnection)
                    .floatingToolbarNestedScroll(
                        expanded = floatingToolbarExpanded,
                        onExpand = { floatingToolbarExpanded = true },
                        onCollapse = { floatingToolbarExpanded = false },
                    ),
                showNavigation = true,
                snackBarMessages = state.messages,
                onSnackBarMessageConsumed = {
                    viewModel.accept(Action.SnackbarDismissed(it))
                },
                topBar = {
                    if (state.isQueryEditable) RootDestinationTopAppBar(
                        modifier = Modifier.offset {
                            topAppBarNestedScrollConnection.offset.round()
                        },
                        signedInProfile = state.signedInProfile,
                        title = {
                            SearchBar(
                                searchQuery = state.currentQuery,
                                onQueryChanged = { query ->
                                    viewModel.accept(
                                        Action.Search.OnSearchQueryChanged(query),
                                    )
                                },
                                onQueryConfirmed = {
                                    viewModel.accept(
                                        Action.Search.OnSearchQueryConfirmed(isLocalOnly = false),
                                    )
                                },
                                focusRequester = searchBarFocusRequester,
                            )
                        },
                        transparencyFactor = topAppBarNestedScrollConnection::verticalOffsetProgress,
                        onSignedInProfileClicked = { profile, sharedElementKey ->
                            viewModel.accept(
                                Action.Navigate.To(
                                    profileDestination(
                                        referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                                        profile = profile,
                                        avatarSharedElementKey = sharedElementKey,
                                    ),
                                ),
                            )
                        },
                    )
                    else PoppableDestinationTopAppBar(
                        title = {
                            Text(
                                text = state.currentQuery,
                                style = MaterialTheme.typography.titleSmallEmphasized,
                            )
                        },
                        transparencyFactor = topAppBarNestedScrollConnection::verticalOffsetProgress,
                        onBackPressed = {
                            viewModel.accept(Action.Navigate.Pop)
                        },
                    )
                },
                snackBarHost = {
                    PaneSnackbarHost()
                },
                useFloatingToolbar = true,
                floatingToolbar = {
                    PaneExpandableFloatingToolbar(
                        expanded = floatingToolbarExpanded,
                        fab = if (state.isQueryEditable) {
                            {
                                PaneFloatingToolbarFab(
                                    icon = Icons.Rounded.Search,
                                    onClick = {
                                        searchBarFocusRequester.requestFocus()
                                    },
                                )
                            }
                        } else null,
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
                },
            )
        },
    )
}

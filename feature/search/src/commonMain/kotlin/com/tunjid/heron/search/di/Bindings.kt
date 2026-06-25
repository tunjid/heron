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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.round
import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.search.Action
import com.tunjid.heron.search.RouteQuery
import com.tunjid.heron.search.SearchScreen
import com.tunjid.heron.search.SearchStateHolder
import com.tunjid.heron.search.SearchViewModel
import com.tunjid.heron.search.SearchViewModelInitializer
import com.tunjid.heron.search.isQueryEditable
import com.tunjid.heron.search.isRoot
import com.tunjid.heron.search.profileHandle
import com.tunjid.heron.ui.SearchBar
import com.tunjid.heron.ui.bottomNavigationNestedScrollConnection
import com.tunjid.heron.ui.coroutines.RouteViewModelInitializer
import com.tunjid.heron.ui.modifiers.ifTrue
import com.tunjid.heron.ui.scaffold.di.ScaffoldBindings
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction
import com.tunjid.heron.ui.scaffold.navigation.profileDestination
import com.tunjid.heron.ui.scaffold.scaffold.NavigationContentTransformer
import com.tunjid.heron.ui.scaffold.scaffold.PaneNavigationBar
import com.tunjid.heron.ui.scaffold.scaffold.PaneNavigationRail
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.ui.scaffold.scaffold.PaneSnackbarHost
import com.tunjid.heron.ui.scaffold.scaffold.PoppableDestinationTopAppBar
import com.tunjid.heron.ui.scaffold.scaffold.RootDestinationTopAppBar
import com.tunjid.heron.ui.scaffold.scaffold.fabOffset
import com.tunjid.heron.ui.scaffold.scaffold.predictiveBackPlacement
import com.tunjid.heron.ui.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.rememberRouteViewModel
import com.tunjid.heron.ui.topAppBarNestedScrollConnection
import com.tunjid.heron.ui.verticalOffsetProgress
import com.tunjid.mutator.compose.produceStateWithLifecycle
import com.tunjid.treenav.compose.PaneEntry
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.mappedRoutePath
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.urlRouteMatcher
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ClassKey
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.StringKey
import heron.feature.search.generated.resources.Res
import heron.feature.search.generated.resources.hint_general_search
import heron.feature.search.generated.resources.hint_profile_post_search
import org.jetbrains.compose.resources.stringResource

private const val RoutePattern = "/search"
private const val RouteQueryPattern = "/search/{query}"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
)

internal val Route.query by mappedRoutePath(default = RouteQuery.FullSearch) { query ->
    when {
        query.isBlank() -> RouteQuery.FullSearch
        query.startsWith("from:") -> RouteQuery.ProfilePostSearch(query)
        else -> RouteQuery.HashtaggedPostsSearch(query)
    }
}

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
    @ClassKey(SearchViewModel::class)
    fun provideRouteViewModelInitializer(
        initializer: SearchViewModelInitializer,
    ): RouteViewModelInitializer = RouteViewModelInitializer(initializer::invoke)

    @Provides
    @IntoMap
    @StringKey(RoutePattern)
    fun providePaneEntry(
        navigationContentTransformer: NavigationContentTransformer,
    ): PaneEntry<ThreePane, Route> = routePaneEntry(
        navigationContentTransformer = navigationContentTransformer,
    )

    @Provides
    @IntoMap
    @StringKey(RouteQueryPattern)
    fun providePaneQueryEntry(
        navigationContentTransformer: NavigationContentTransformer,
    ): PaneEntry<ThreePane, Route> = routePaneEntry(
        navigationContentTransformer = navigationContentTransformer,
    )

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    private fun routePaneEntry(
        navigationContentTransformer: NavigationContentTransformer,
    ) = threePaneEntry(
        contentTransform = navigationContentTransformer::contentTransform,
        render = { route ->
            val paneScaffoldState = rememberPaneScaffoldState()
            val stateHolder: SearchStateHolder = paneScaffoldState.rememberRouteViewModel<SearchViewModel>(
                route = route,
            )
            val state = stateHolder.produceStateWithLifecycle()

            val searchFocusRequester = remember { FocusRequester() }

            LaunchedEffect(Unit) {
                searchFocusRequester.requestFocus()
            }

            val topAppBarNestedScrollConnection =
                topAppBarNestedScrollConnection()

            val bottomNavigationNestedScrollConnection =
                bottomNavigationNestedScrollConnection(
                    isCompact = paneScaffoldState.prefersCompactBottomNav,
                )

            paneScaffoldState.PaneScaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .predictiveBackPlacement(paneScaffoldState = paneScaffoldState)
                    .nestedScroll(topAppBarNestedScrollConnection)
                    .ifTrue(paneScaffoldState.prefersAutoHidingBottomNav) {
                        nestedScroll(bottomNavigationNestedScrollConnection)
                    },
                showNavigation = true,
                snackBarMessages = state.messages,
                onSnackBarMessageConsumed = {
                    stateHolder.accept(Action.SnackbarDismissed(it))
                },
                topBar = {
                    val searchHint = when (val query = state.query) {
                        is RouteQuery.ProfilePostSearch -> stringResource(
                            Res.string.hint_profile_post_search,
                            query.profileHandle,
                        )
                        RouteQuery.FullSearch -> stringResource(
                            Res.string.hint_general_search,
                        )
                        is RouteQuery.HashtaggedPostsSearch -> state.searchBarText
                    }
                    if (state.query.isRoot) RootDestinationTopAppBar(
                        modifier = Modifier.offset {
                            topAppBarNestedScrollConnection.offset.round()
                        },
                        title = {
                            SearchBar(
                                searchQuery = state.searchBarText,
                                searchHint = searchHint,
                                focusRequester = searchFocusRequester,
                                onQueryChanged = { query ->
                                    stateHolder.accept(
                                        Action.Search.OnSearchQueryChanged(query),
                                    )
                                },
                                onQueryConfirmed = {
                                    stateHolder.accept(
                                        Action.Search.OnSearchQueryConfirmed(isLocalOnly = false),
                                    )
                                },
                            )
                        },
                        transparencyFactor = topAppBarNestedScrollConnection::verticalOffsetProgress,
                        onSignedInProfileClicked = { profile, sharedElementKey ->
                            stateHolder.accept(
                                Action.Navigate.To(
                                    profileDestination(
                                        referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                                        profile = profile,
                                        avatarSharedElementKey = sharedElementKey,
                                    ),
                                ),
                            )
                        },
                        onLogoClicked = {
                            stateHolder.accept(Action.Navigate.Home)
                        },
                    )
                    else PoppableDestinationTopAppBar(
                        title = {
                            if (state.query.isQueryEditable) SearchBar(
                                searchQuery = state.searchBarText,
                                searchHint = searchHint,
                                focusRequester = searchFocusRequester,
                                onQueryChanged = { query ->
                                    stateHolder.accept(
                                        Action.Search.OnSearchQueryChanged(query),
                                    )
                                },
                                onQueryConfirmed = {
                                    stateHolder.accept(
                                        Action.Search.OnSearchQueryConfirmed(isLocalOnly = false),
                                    )
                                },
                            )
                            else Text(
                                text = searchHint,
                                style = MaterialTheme.typography.titleSmallEmphasized,
                            )
                        },
                        transparencyFactor = topAppBarNestedScrollConnection::verticalOffsetProgress,
                        onBackPressed = {
                            stateHolder.accept(Action.Navigate.Pop)
                        },
                    )
                },
                snackBarHost = {
                    PaneSnackbarHost(
                        modifier = Modifier
                            .offset {
                                fabOffset(bottomNavigationNestedScrollConnection.offset)
                            },
                    )
                },
                navigationBar = {
                    PaneNavigationBar(
                        modifier = Modifier
                            .offset {
                                bottomNavigationNestedScrollConnection.offset.round()
                            },
                        onNavItemReselected = {
                            searchFocusRequester.requestFocus()
                            true
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
                        actions = stateHolder.accept,
                    )
                },
            )
        },
    )
}

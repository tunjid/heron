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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.round
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.tunjid.heron.search.Action
import com.tunjid.heron.search.GrazeFeedPreviewPrefix
import com.tunjid.heron.search.ProfilePostSearchPrefix
import com.tunjid.heron.search.RouteQuery
import com.tunjid.heron.search.SearchScreen
import com.tunjid.heron.search.SearchStateHolder
import com.tunjid.heron.search.SearchViewModelInitializer
import com.tunjid.heron.search.State
import com.tunjid.heron.search.canShowFab
import com.tunjid.heron.search.isQueryEditable
import com.tunjid.heron.search.isRoot
import com.tunjid.heron.search.profileHandle
import com.tunjid.heron.search.ui.PaneActions
import com.tunjid.heron.search.ui.filter.rememberUpdatedSearchFilterSheetState
import com.tunjid.heron.ui.SearchBar
import com.tunjid.heron.ui.modifiers.ifTrue
import com.tunjid.heron.ui.scaffold.di.NavigationScope
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction
import com.tunjid.heron.ui.scaffold.navigation.grazeEditorDestination
import com.tunjid.heron.ui.scaffold.navigation.profileDestination
import com.tunjid.heron.ui.scaffold.scaffold.NavigationContentTransformer
import com.tunjid.heron.ui.scaffold.scaffold.PaneFab
import com.tunjid.heron.ui.scaffold.scaffold.PaneNavigationBar
import com.tunjid.heron.ui.scaffold.scaffold.PaneNavigationRail
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.PaneSnackbarHost
import com.tunjid.heron.ui.scaffold.scaffold.PoppableDestinationTopAppBar
import com.tunjid.heron.ui.scaffold.scaffold.RootDestinationTopAppBar
import com.tunjid.heron.ui.scaffold.scaffold.fabOffset
import com.tunjid.heron.ui.scaffold.scaffold.isFabExpanded
import com.tunjid.heron.ui.scaffold.scaffold.predictiveBackPlacement
import com.tunjid.heron.ui.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.retainRouteStateHolder
import com.tunjid.heron.ui.stateproduction.RouteStateHolderInitializer
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
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ClassKey
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.StringKey
import heron.feature.search.generated.resources.Res
import heron.feature.search.generated.resources.adapt_to_feed
import heron.feature.search.generated.resources.hint_general_search
import heron.feature.search.generated.resources.hint_graze_feed_preview
import heron.feature.search.generated.resources.hint_profile_post_search
import org.jetbrains.compose.resources.stringResource

private const val RoutePattern = "/search"
private const val RouteQueryPattern = "/search/{query}"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
)

internal val Route.query by mappedRoutePath(
    default = RouteQuery.FullSearch,
) { query ->
    when {
        query.isBlank() -> RouteQuery.FullSearch
        query.startsWith(ProfilePostSearchPrefix) -> RouteQuery.ProfilePostSearch(query)
        query.startsWith(GrazeFeedPreviewPrefix) -> RouteQuery.GrazeFeedPreview(query)
        else -> RouteQuery.HashtaggedPostsSearch(query)
    }
}

@BindingContainer
@ContributesTo(NavigationScope::class)
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
@ContributesTo(AppScope::class)
object SearchBindings {

    @Provides
    @IntoMap
    @ClassKey(SearchStateHolder::class)
    fun provideRouteStateHolderInitializer(
        initializer: SearchViewModelInitializer,
    ): RouteStateHolderInitializer = RouteStateHolderInitializer(initializer::invoke)

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
            Route(
                route = route,
                paneScaffoldState = rememberPaneScaffoldState(),
            )
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun Route(
    route: Route,
    paneScaffoldState: PaneScaffoldState,
) {
    val stateHolder = paneScaffoldState.retainRouteStateHolder<SearchStateHolder>(
        route = route,
    )
    val state = stateHolder.produceStateWithLifecycle()

    val focusManager = LocalFocusManager.current

    val searchFilterSheetState = paneScaffoldState.rememberUpdatedSearchFilterSheetState(
        queryText = state.searchBarText,
        filter = state.draftFilter,
        onQueryTextChanged = { query ->
            stateHolder.accept(Action.Search.OnSearchQueryChanged(query))
        },
        onFilterChanged = { filter ->
            stateHolder.accept(Action.Filter.Edit(filter))
        },
        onClear = {
            stateHolder.accept(Action.Filter.Clear)
        },
        onApply = {
            focusManager.clearFocus()
            stateHolder.accept(Action.Filter.Apply)
        },
    )

    val keyboard = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }
    KeyboardPopupEffect(state, searchFocusRequester)

    val topAppBarNestedScrollConnection =
        paneScaffoldState.topAppBarNestedScrollConnection

    val bottomNavigationNestedScrollConnection =
        paneScaffoldState.bottomNavigationNestedScrollConnection

    paneScaffoldState.PaneScaffold(
        modifier = Modifier
            .fillMaxSize()
            .predictiveBackPlacement(paneScaffoldState = paneScaffoldState)
            .nestedScroll(topAppBarNestedScrollConnection)
            .ifTrue(paneScaffoldState.prefersAutoHidingBottomNav) {
                nestedScroll(bottomNavigationNestedScrollConnection)
            },
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
                is RouteQuery.GrazeFeedPreview -> stringResource(
                    Res.string.hint_graze_feed_preview,
                )
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
                            focusManager.clearFocus()
                            stateHolder.accept(
                                Action.Search.OnSearchQueryConfirmed(isLocalOnly = false),
                            )
                        },
                    )
                },
                actions = {
                    PaneActions(
                        state = state,
                        focusManager = focusManager,
                        actions = stateHolder.accept,
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
                actions = {
                    PaneActions(
                        state = state,
                        focusManager = focusManager,
                        actions = stateHolder.accept,
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
        floatingActionButton = {
            AnimatedVisibility(
                visible = state.canShowFab,
            ) {
                PaneFab(
                    modifier = Modifier
                        .offset {
                            fabOffset(bottomNavigationNestedScrollConnection.offset)
                        },
                    text = stringResource(Res.string.adapt_to_feed),
                    icon = Icons.Rounded.SwapHoriz,
                    expanded = isFabExpanded {
                        if (prefersAutoHidingBottomNav) bottomNavigationNestedScrollConnection.offset
                        else topAppBarNestedScrollConnection.offset * -1f
                    },
                    onClick = {
                        stateHolder.accept(
                            Action.Navigate.To(
                                grazeEditorDestination(
                                    searchQuery = state.searchBarText,
                                    searchFilter = state.appliedFilter,
                                ),
                            ),
                        )
                    },
                )
            }
        },
        navigationBar = {
            PaneNavigationBar(
                modifier = Modifier
                    .offset {
                        bottomNavigationNestedScrollConnection.offset.round()
                    },
                onNavItemReselected = {
                    searchFocusRequester.requestFocus()
                    keyboard?.show()
                    true
                },
            )
        },
        navigationRail = {
            PaneNavigationRail(
                onNavItemReselected = {
                    searchFocusRequester.requestFocus()
                    keyboard?.show()
                    true
                },
            )
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
}

@Composable
private fun KeyboardPopupEffect(
    state: State,
    searchFocusRequester: FocusRequester,
) {
    var restored by rememberSaveable {
        mutableStateOf(false)
    }
    LifecycleResumeEffect(Unit) {
        onPauseOrDispose { restored = true }
    }
    LaunchedEffect(Unit) {
        if (state.searchBarText.isBlank() && !restored) searchFocusRequester.requestFocus()
    }
}

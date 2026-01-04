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

package com.tunjid.heron.posts.di

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.round
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.data.core.types.ProfileHandleOrId
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.posts.Action
import com.tunjid.heron.posts.ActualPostsViewModel
import com.tunjid.heron.posts.PostsScreen
import com.tunjid.heron.posts.RouteViewModelInitializer
import com.tunjid.heron.scaffold.di.ScaffoldBindings
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.decodeReferringRoute
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.hydrate
import com.tunjid.heron.scaffold.scaffold.AppBarTitle
import com.tunjid.heron.scaffold.scaffold.LocalAppState
import com.tunjid.heron.scaffold.scaffold.PaneNavigationBar
import com.tunjid.heron.scaffold.scaffold.PaneNavigationRail
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.PoppableDestinationTopAppBar
import com.tunjid.heron.scaffold.scaffold.SecondaryPaneCloseBackHandler
import com.tunjid.heron.scaffold.scaffold.predictiveBackContentTransform
import com.tunjid.heron.scaffold.scaffold.predictiveBackPlacement
import com.tunjid.heron.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.viewModelCoroutineScope
import com.tunjid.heron.ui.bottomNavigationNestedScrollConnection
import com.tunjid.heron.ui.navigationBarOffset
import com.tunjid.heron.ui.topAppBarNestedScrollConnection
import com.tunjid.heron.ui.verticalOffsetProgress
import com.tunjid.treenav.compose.PaneEntry
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.PathPattern
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.RouteParser
import com.tunjid.treenav.strings.mappedRoutePath
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.toRouteTrie
import com.tunjid.treenav.strings.urlRouteMatcher
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.StringKey
import heron.feature.posts.generated.resources.Res
import heron.feature.posts.generated.resources.bookmarks
import heron.feature.posts.generated.resources.quotes
import org.jetbrains.compose.resources.stringResource

private const val SavedRoutePattern = "/saved"
private const val QuotesRoutePattern = "/profile/{profileHandleOrId}/post/{postRecordKey}/quotes"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
    children = listOfNotNull(
        routeParams.decodeReferringRoute(),
    ),
)

internal sealed class PostsRequest {
    data object Saved : PostsRequest()
    data class Quotes(
        val profileHandleOrId: ProfileHandleOrId,
        val postRecordKey: RecordKey,
    ) : PostsRequest()
}

private fun postsRouteMatcher(pattern: String) = urlRouteMatcher(
    routePattern = pattern,
    routeMapper = ::createRoute,
)

private val Route.profileHandleOrId by mappedRoutePath(
    mapper = ::ProfileHandleOrId,
)
private val Route.postRecordKey by mappedRoutePath(
    mapper = ::RecordKey,
)

private val RequestTrie = mapOf(
    PathPattern(SavedRoutePattern) to { route: Route ->
        PostsRequest.Saved
    },
    PathPattern(QuotesRoutePattern) to { route: Route ->
        PostsRequest.Quotes(
            profileHandleOrId = route.profileHandleOrId,
            postRecordKey = route.postRecordKey,
        )
    },
).toRouteTrie()

internal val Route.postsRequest: PostsRequest
    get() = checkNotNull(RequestTrie[this]).invoke(this)

@BindingContainer
object PostsNavigationBindings {

    @Provides
    @IntoMap
    @StringKey(SavedRoutePattern)
    fun provideSavedRouteMatcher(): RouteMatcher = postsRouteMatcher(SavedRoutePattern)

    @Provides
    @IntoMap
    @StringKey(QuotesRoutePattern)
    fun provideQuotesRouteMatcher(): RouteMatcher = postsRouteMatcher(QuotesRoutePattern)
}

@BindingContainer
class PostsBindings(
    @Includes dataBindings: DataBindings,
    @Includes scaffoldBindings: ScaffoldBindings,
) {

    @Provides
    @IntoMap
    @StringKey(SavedRoutePattern)
    fun provideSavedPaneEntry(
        routeParser: RouteParser,
        viewModelInitializer: RouteViewModelInitializer,
    ): PaneEntry<ThreePane, Route> = routePaneEntry(
        routeParser = routeParser,
        viewModelInitializer = viewModelInitializer,
    )

    @Provides
    @IntoMap
    @StringKey(QuotesRoutePattern)
    fun provideQuotesPaneEntry(
        routeParser: RouteParser,
        viewModelInitializer: RouteViewModelInitializer,
    ): PaneEntry<ThreePane, Route> = routePaneEntry(
        routeParser = routeParser,
        viewModelInitializer = viewModelInitializer,
    )

    private fun routePaneEntry(
        routeParser: RouteParser,
        viewModelInitializer: RouteViewModelInitializer,
    ) = threePaneEntry<Route>(
        contentTransform = predictiveBackContentTransform,
        paneMapping = { route ->
            mapOf(
                ThreePane.Primary to route,
                ThreePane.Secondary to route.children.firstOrNull() as? Route,
            )
        },
        render = { route ->
            val viewModel = viewModel<ActualPostsViewModel> {
                viewModelInitializer.invoke(
                    scope = viewModelCoroutineScope(),
                    route = routeParser.hydrate(route),
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()
            val paneScaffoldState = rememberPaneScaffoldState()

            val topAppBarNestedScrollConnection =
                topAppBarNestedScrollConnection()

            val appState = LocalAppState.current

            val autoHideNavigationBar = appState.preferences?.autoHideNavigationBar ?: true

            val bottomNavigationNestedScrollConnection =
                bottomNavigationNestedScrollConnection(
                    isCompact = paneScaffoldState.prefersCompactBottomNav,
                    enabled = autoHideNavigationBar,
                )

            paneScaffoldState.PaneScaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .predictiveBackPlacement(paneScope = this)
                    .nestedScroll(topAppBarNestedScrollConnection)
                    .nestedScroll(bottomNavigationNestedScrollConnection),
                showNavigation = true,
                snackBarMessages = state.messages,
                onSnackBarMessageConsumed = {
                    viewModel.accept(Action.SnackbarDismissed(it))
                },
                topBar = {
                    PoppableDestinationTopAppBar(
                        title = {
                            AppBarTitle(
                                stringResource(
                                    when (route.postsRequest) {
                                        is PostsRequest.Quotes -> Res.string.quotes
                                        PostsRequest.Saved -> Res.string.bookmarks
                                    },
                                ),
                            )
                        },
                        transparencyFactor = topAppBarNestedScrollConnection::verticalOffsetProgress,
                        onBackPressed = { viewModel.accept(Action.Navigate.Pop) },
                    )
                },
                navigationBar = {
                    PaneNavigationBar(
                        modifier = Modifier.offset {
                            bottomNavigationNestedScrollConnection.navigationBarOffset(autoHideNavigationBar).round()
                        },
                    )
                },
                navigationRail = {
                    PaneNavigationRail()
                },
                content = {
                    PostsScreen(
                        paneScaffoldState = this,
                        state = state,
                        actions = viewModel.accept,
                        modifier = Modifier,
                    )
                    SecondaryPaneCloseBackHandler()
                },
            )
        },
    )
}

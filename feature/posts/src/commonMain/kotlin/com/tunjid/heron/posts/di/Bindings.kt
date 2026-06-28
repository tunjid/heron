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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.round
import com.tunjid.heron.data.core.types.ProfileHandleOrId
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.posts.Action
import com.tunjid.heron.posts.ActualPostsViewModel
import com.tunjid.heron.posts.PostsScreen
import com.tunjid.heron.posts.PostsStateHolder
import com.tunjid.heron.posts.PostsViewModelInitializer
import com.tunjid.heron.ui.bottomNavigationNestedScrollConnection
import com.tunjid.heron.ui.modifiers.ifTrue
import com.tunjid.heron.ui.scaffold.di.ScaffoldBindings
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.decodeReferringRoute
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.hydrate
import com.tunjid.heron.ui.scaffold.scaffold.AppBarTitle
import com.tunjid.heron.ui.scaffold.scaffold.NavigationContentTransformer
import com.tunjid.heron.ui.scaffold.scaffold.PaneNavigationBar
import com.tunjid.heron.ui.scaffold.scaffold.PaneNavigationRail
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.PoppableDestinationTopAppBar
import com.tunjid.heron.ui.scaffold.scaffold.SecondaryPaneCloseBackHandler
import com.tunjid.heron.ui.scaffold.scaffold.predictiveBackPlacement
import com.tunjid.heron.ui.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.retainRouteStateHolder
import com.tunjid.heron.ui.stateproduction.RouteStateHolderInitializer
import com.tunjid.heron.ui.topAppBarNestedScrollConnection
import com.tunjid.heron.ui.verticalOffsetProgress
import com.tunjid.mutator.compose.produceStateWithLifecycle
import com.tunjid.treenav.compose.PaneEntry
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.RouteParser
import com.tunjid.treenav.strings.mappedRoutePath
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.trieOf
import com.tunjid.treenav.strings.urlRouteMatcher
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ClassKey
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

private val RequestTrie = trieOf(
    SavedRoutePattern to { route: Route ->
        PostsRequest.Saved
    },
    QuotesRoutePattern to { route: Route ->
        PostsRequest.Quotes(
            profileHandleOrId = route.profileHandleOrId,
            postRecordKey = route.postRecordKey,
        )
    },
)

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
    @ClassKey(PostsStateHolder::class)
    fun provideRouteStateHolderInitializer(
        initializer: PostsViewModelInitializer,
    ): RouteStateHolderInitializer = RouteStateHolderInitializer(initializer::invoke)

    @Provides
    @IntoMap
    @StringKey(SavedRoutePattern)
    fun provideSavedPaneEntry(
        routeParser: RouteParser,
        navigationContentTransformer: NavigationContentTransformer,
    ): PaneEntry<ThreePane, Route> = routePaneEntry(
        routeParser = routeParser,
        navigationContentTransformer = navigationContentTransformer,
    )

    @Provides
    @IntoMap
    @StringKey(QuotesRoutePattern)
    fun provideQuotesPaneEntry(
        routeParser: RouteParser,
        navigationContentTransformer: NavigationContentTransformer,
    ): PaneEntry<ThreePane, Route> = routePaneEntry(
        routeParser = routeParser,
        navigationContentTransformer = navigationContentTransformer,
    )

    private fun routePaneEntry(
        routeParser: RouteParser,
        navigationContentTransformer: NavigationContentTransformer,
    ) = threePaneEntry<Route>(
        contentTransform = navigationContentTransformer::contentTransform,
        paneMapping = { route ->
            mapOf(
                ThreePane.Primary to route,
                ThreePane.Secondary to route.children.firstOrNull() as? Route,
            )
        },
        render = { route ->
            Route(
                route = routeParser.hydrate(route),
                paneScaffoldState = rememberPaneScaffoldState(),
            )
        },
    )
}

@Composable
internal fun Route(
    route: Route,
    paneScaffoldState: PaneScaffoldState,
) {
    val stateHolder = paneScaffoldState.retainRouteStateHolder<PostsStateHolder>(
        route = route,
    )
    val state = stateHolder.produceStateWithLifecycle()

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
            PoppableDestinationTopAppBar(
                title = {
                    AppBarTitle(
                        title = stringResource(
                            when (route.postsRequest) {
                                is PostsRequest.Quotes -> Res.string.quotes
                                PostsRequest.Saved -> Res.string.bookmarks
                            },
                        ),
                    )
                },
                transparencyFactor = topAppBarNestedScrollConnection::verticalOffsetProgress,
                onBackPressed = { stateHolder.accept(Action.Navigate.Pop) },
            )
        },
        navigationBar = {
            PaneNavigationBar(
                modifier = Modifier.offset {
                    bottomNavigationNestedScrollConnection.offset.round()
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
                actions = stateHolder.accept,
                modifier = Modifier,
            )
            SecondaryPaneCloseBackHandler()
        },
    )
}

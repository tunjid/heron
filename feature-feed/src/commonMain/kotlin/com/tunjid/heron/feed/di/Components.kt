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

package com.tunjid.heron.feed.di

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.ProfileHandleOrId
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.di.DataComponent
import com.tunjid.heron.data.repository.TimelineRequest
import com.tunjid.heron.data.utilities.getAsRawUri
import com.tunjid.heron.domain.timeline.TimelineLoadAction
import com.tunjid.heron.feed.Action
import com.tunjid.heron.feed.ActualFeedViewModel
import com.tunjid.heron.feed.FeedScreen
import com.tunjid.heron.feed.RouteViewModelInitializer
import com.tunjid.heron.feed.ui.TimelineTitle
import com.tunjid.heron.scaffold.di.ScaffoldComponent
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.decodeReferringRoute
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.hydrate
import com.tunjid.heron.scaffold.navigation.routePatternAndMatcher
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.PoppableDestinationTopAppBar
import com.tunjid.heron.scaffold.scaffold.SecondaryPaneCloseBackHandler
import com.tunjid.heron.scaffold.scaffold.predictiveBackPlacement
import com.tunjid.heron.scaffold.scaffold.predictiveBackContentTransform
import com.tunjid.heron.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.viewModelCoroutineScope
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.PathPattern
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.RouteParser
import com.tunjid.treenav.strings.mappedRoutePath
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.routePath
import com.tunjid.treenav.strings.toRouteTrie
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.KmpComponentCreate
import me.tatarka.inject.annotations.Provides

private const val RoutePattern = "/profile/{profileId}/feed/{feedUriSuffix}"
private const val RouteUriPattern = "/{feedUriPrefix}/app.bsky.feed.generator/{feedUriSuffix}"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
    children = listOfNotNull(
        routeParams.decodeReferringRoute()
    ),
)

private val Route.profileId by mappedRoutePath(
    mapper = ::ProfileHandleOrId
)

private val Route.feedUriSuffix by routePath()

private val RequestTrie = mapOf(
    PathPattern(RoutePattern) to { route: Route ->
        TimelineRequest.OfFeed.WithProfile(
            profileHandleOrDid = route.profileId,
            feedUriSuffix = route.feedUriSuffix,
        )
    },
    PathPattern(RouteUriPattern) to { route: Route ->
        TimelineRequest.OfFeed.WithUri(
            uri = route.routeParams.pathAndQueries
                .getAsRawUri(Uri.Host.AtProto)
                .let(::FeedGeneratorUri)
        )
    },
).toRouteTrie()

internal val Route.timelineRequest: TimelineRequest.OfFeed
    get() = checkNotNull(RequestTrie[this]).invoke(this)

@KmpComponentCreate
expect fun FeedNavigationComponent.Companion.create(): FeedNavigationComponent

@KmpComponentCreate
expect fun FeedComponent.Companion.create(
    dataComponent: DataComponent,
    scaffoldComponent: ScaffoldComponent,
): FeedComponent

@Component
abstract class FeedNavigationComponent {
    companion object

    @IntoMap
    @Provides
    fun profileRouteParser(): Pair<String, RouteMatcher> =
        routePatternAndMatcher(
            routePattern = RoutePattern,
            routeMapper = ::createRoute,
        )

    @IntoMap
    @Provides
    fun uriRouteParser(): Pair<String, RouteMatcher> =
        routePatternAndMatcher(
            routePattern = RouteUriPattern,
            routeMapper = ::createRoute,
        )

}

@Component
abstract class FeedComponent(
    @Component val dataComponent: DataComponent,
    @Component val scaffoldComponent: ScaffoldComponent,
) {
    companion object

    @IntoMap
    @Provides
    fun routePattern(
        routeParser: RouteParser,
        viewModelInitializer: RouteViewModelInitializer,
    ) = RoutePattern to routePaneEntry(
        routeParser = routeParser,
        viewModelInitializer = viewModelInitializer,
    )

    @IntoMap
    @Provides
    fun routeUriPattern(
        routeParser: RouteParser,
        viewModelInitializer: RouteViewModelInitializer,
    ) = RouteUriPattern to routePaneEntry(
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
                ThreePane.Secondary to route.children.firstOrNull() as? Route
            )
        },
        render = { route ->
            val viewModel = viewModel<ActualFeedViewModel> {
                viewModelInitializer.invoke(
                    scope = viewModelCoroutineScope(),
                    route = routeParser.hydrate(route),
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()

            rememberPaneScaffoldState().PaneScaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .predictiveBackPlacement(paneScope = this),
                showNavigation = true,
                snackBarMessages = state.messages,
                onSnackBarMessageConsumed = {
                },
                topBar = {
                    PoppableDestinationTopAppBar(
                        title = {
                            TimelineTitle(
                                timeline = state.timelineState?.timeline,
                                creator = state.creator,
                                hasUpdates = state.timelineState?.hasUpdates == true,
                                onPresentationSelected = { timeline, presentation ->
                                    state.timelineStateHolder
                                        ?.accept
                                        ?.invoke(
                                            TimelineLoadAction.UpdatePreferredPresentation(
                                                timeline = timeline,
                                                presentation = presentation
                                            )
                                        )
                                }
                            )
                        },
                        onBackPressed = { viewModel.accept(Action.Navigate.Pop) }
                    )
                },
                content = { paddingValues ->
                    FeedScreen(
                        paneScaffoldState = this,
                        modifier = Modifier
                            .padding(
                                top = paddingValues.calculateTopPadding()
                            ),
                        state = state,
                        actions = viewModel.accept,
                    )
                    SecondaryPaneCloseBackHandler(
                        enabled = paneState.pane == ThreePane.Primary
                                && route.children.isNotEmpty()
                                && isMediumScreenWidthOrWider
                    )
                }
            )
        }
    )
}

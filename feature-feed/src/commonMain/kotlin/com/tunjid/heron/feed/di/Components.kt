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

import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.data.core.models.UriLookup
import com.tunjid.heron.data.di.DataComponent
import com.tunjid.heron.feed.Action
import com.tunjid.heron.feed.ActualFeedViewModel
import com.tunjid.heron.feed.FeedScreen
import com.tunjid.heron.feed.FeedViewModelCreator
import com.tunjid.heron.feed.ui.TimelineTitle
import com.tunjid.heron.scaffold.di.ScaffoldComponent
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.hydrate
import com.tunjid.heron.scaffold.navigation.routeAndMatcher
import com.tunjid.heron.scaffold.navigation.routeOf
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.PoppableDestinationTopAppBar
import com.tunjid.heron.scaffold.scaffold.predictiveBackBackgroundModifier
import com.tunjid.treenav.compose.threepane.threePaneListDetailStrategy
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.RouteParser
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.KmpComponentCreate
import me.tatarka.inject.annotations.Provides

private const val RoutePattern = "/profile/{profileId}/feed/{feedId}"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
)

internal val Route.feedLookup
    get() = UriLookup.Timeline.FeedGenerator(
        profileHandleOrDid = routeParams.pathArgs.getValue("profileId"),
        feedUriSuffix = routeParams.pathArgs.getValue("feedId"),
    )

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
        routeAndMatcher(
            routePattern = RoutePattern,
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
    fun routeAdaptiveConfiguration(
        routeParser: RouteParser,
        creator: FeedViewModelCreator,
    ) = RoutePattern to threePaneListDetailStrategy(
        render = { route ->
            val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
            val viewModel = viewModel<ActualFeedViewModel> {
                creator.invoke(
                    scope = lifecycleCoroutineScope,
                    route = routeParser.hydrate(route),
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
                        title = {
                            TimelineTitle(
                                timeline = state.timelineState?.timeline,
                                creator = state.creator,
                            )
                        },
                        onBackPressed = { viewModel.accept(Action.Navigate.Pop) }
                    )
                },
                content = {
                    FeedScreen(
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

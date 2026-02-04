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

package com.tunjid.heron.list.di

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.data.core.models.uri
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.ProfileHandleOrId
import com.tunjid.heron.data.core.types.StarterPackUri
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.core.types.asEmbeddableRecordUriOrNull
import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.data.repository.TimelineRequest
import com.tunjid.heron.data.utilities.asGenericUri
import com.tunjid.heron.data.utilities.getAsRawUri
import com.tunjid.heron.list.Action
import com.tunjid.heron.list.ActualListViewModel
import com.tunjid.heron.list.ListScreen
import com.tunjid.heron.list.ListScreenStateHolders
import com.tunjid.heron.list.RouteViewModelInitializer
import com.tunjid.heron.list.withListTimelineOrNull
import com.tunjid.heron.scaffold.di.ScaffoldBindings
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.decodeReferringRoute
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.hydrate
import com.tunjid.heron.scaffold.navigation.composePostDestination
import com.tunjid.heron.scaffold.navigation.conversationDestination
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.PoppableDestinationTopAppBar
import com.tunjid.heron.scaffold.scaffold.SecondaryPaneCloseBackHandler
import com.tunjid.heron.scaffold.scaffold.predictiveBackContentTransformProvider
import com.tunjid.heron.scaffold.scaffold.predictiveBackPlacement
import com.tunjid.heron.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.viewModelCoroutineScope
import com.tunjid.heron.timeline.state.TimelineState
import com.tunjid.heron.timeline.ui.EmbeddableRecordOptionsSheetState.Companion.rememberUpdatedEmbeddableRecordOptionsState
import com.tunjid.heron.timeline.ui.ShareRecordButton
import com.tunjid.heron.timeline.ui.list.FeedListStatus
import com.tunjid.heron.timeline.utilities.TimelineTitle
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
import com.tunjid.treenav.strings.routePath
import com.tunjid.treenav.strings.toRouteTrie
import com.tunjid.treenav.strings.urlRouteMatcher
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.StringKey

private const val ListRoutePattern = "/profile/{profileId}/lists/{listUriSuffix}"
private const val ListRouteUriPattern = "/{listUriPrefix}/app.bsky.graph.list/{listUriSuffix}"

private const val StarterPackRoutePattern = "/starter-pack/{profileId}/{starterPackUriSuffix}"
private const val StarterPackRouteUriPattern =
    "/{starterPackUriPrefix}/app.bsky.graph.starterpack/{starterPackUriSuffix}"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
    children = listOfNotNull(
        routeParams.decodeReferringRoute(),
    ),
)

private val Route.profileId by mappedRoutePath(
    mapper = ::ProfileHandleOrId,
)

private val Route.listUriSuffix by routePath()

private val Route.starterPackUriSuffix by routePath()

private val RequestTrie = mapOf(
    PathPattern(ListRoutePattern) to { route: Route ->
        TimelineRequest.OfList.WithProfile(
            profileHandleOrDid = route.profileId,
            listUriSuffix = route.listUriSuffix,
        )
    },
    PathPattern(ListRouteUriPattern) to { route: Route ->
        TimelineRequest.OfList.WithUri(
            uri = route.routeParams.pathAndQueries
                .getAsRawUri(Uri.Host.AtProto)
                .let(::ListUri),
        )
    },
    PathPattern(StarterPackRoutePattern) to { route: Route ->
        TimelineRequest.OfStarterPack.WithProfile(
            profileHandleOrDid = route.profileId,
            starterPackUriSuffix = route.starterPackUriSuffix,
        )
    },
    PathPattern(StarterPackRouteUriPattern) to { route: Route ->
        TimelineRequest.OfStarterPack.WithUri(
            uri = route.routeParams.pathAndQueries
                .getAsRawUri(Uri.Host.AtProto)
                .let(::StarterPackUri),
        )
    },
).toRouteTrie()

internal val Route.timelineRequest: TimelineRequest.OfList
    get() = checkNotNull(RequestTrie[this]).invoke(this)

@BindingContainer
object ListNavigationBindings {

    @Provides
    @IntoMap
    @StringKey(ListRoutePattern)
    fun provideRouteMatcher(): RouteMatcher =
        urlRouteMatcher(
            routePattern = ListRoutePattern,
            routeMapper = ::createRoute,
        )

    @Provides
    @IntoMap
    @StringKey(ListRouteUriPattern)
    fun provideRouteUriMatcher(): RouteMatcher =
        urlRouteMatcher(
            routePattern = ListRouteUriPattern,
            routeMapper = ::createRoute,
        )

    @Provides
    @IntoMap
    @StringKey(StarterPackRoutePattern)
    fun provideStarterPackRouteMatcher(): RouteMatcher =
        urlRouteMatcher(
            routePattern = StarterPackRoutePattern,
            routeMapper = ::createRoute,
        )

    @Provides
    @IntoMap
    @StringKey(StarterPackRouteUriPattern)
    fun provideRouteStarterPackUriMatcher(): RouteMatcher =
        urlRouteMatcher(
            routePattern = StarterPackRouteUriPattern,
            routeMapper = ::createRoute,
        )
}

@BindingContainer
class ListBindings(
    @Includes dataBindings: DataBindings,
    @Includes scaffoldBindings: ScaffoldBindings,
) {

    @Provides
    @IntoMap
    @StringKey(ListRoutePattern)
    fun providePaneEntry(
        routeParser: RouteParser,
        viewModelInitializer: RouteViewModelInitializer,
    ): PaneEntry<ThreePane, Route> = routePaneEntry(
        routeParser = routeParser,
        viewModelInitializer = viewModelInitializer,
    )

    @Provides
    @IntoMap
    @StringKey(ListRouteUriPattern)
    fun provideUriPaneEntry(
        routeParser: RouteParser,
        viewModelInitializer: RouteViewModelInitializer,
    ): PaneEntry<ThreePane, Route> = routePaneEntry(
        routeParser = routeParser,
        viewModelInitializer = viewModelInitializer,
    )

    @Provides
    @IntoMap
    @StringKey(StarterPackRoutePattern)
    fun provideStarterPackRoutePaneEntry(
        routeParser: RouteParser,
        viewModelInitializer: RouteViewModelInitializer,
    ): PaneEntry<ThreePane, Route> = routePaneEntry(
        routeParser = routeParser,
        viewModelInitializer = viewModelInitializer,
    )

    @Provides
    @IntoMap
    @StringKey(StarterPackRouteUriPattern)
    fun provideStarterPackUriPaneEntry(
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
        contentTransform = predictiveBackContentTransformProvider(),
        paneMapping = { route ->
            mapOf(
                ThreePane.Primary to route,
                ThreePane.Secondary to route.children.firstOrNull() as? Route,
            )
        },
        render = { route ->
            val viewModel = viewModel<ActualListViewModel> {
                viewModelInitializer.invoke(
                    scope = viewModelCoroutineScope(),
                    route = routeParser.hydrate(route),
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()
            val paneScaffoldState = rememberPaneScaffoldState()

            val recordOptionsSheetState = rememberUpdatedEmbeddableRecordOptionsState(
                signedInProfileId = state.signedInProfileId,
                recentConversations = state.recentConversations,
                onShareInConversationClicked = { recordUri, conversation ->
                    viewModel.accept(
                        Action.Navigate.To(
                            conversationDestination(
                                id = conversation.id,
                                members = conversation.members,
                                sharedElementPrefix = conversation.id.id,
                                sharedUri = recordUri.asGenericUri(),
                                referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                            ),
                        ),
                    )
                },
                onShareInPostClicked = { recordUri ->
                    viewModel.accept(
                        Action.Navigate.To(
                            composePostDestination(
                                sharedUri = recordUri.asGenericUri(),
                            ),
                        ),
                    )
                },
            )

            paneScaffoldState.PaneScaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .predictiveBackPlacement(paneScaffoldState = paneScaffoldState),
                showNavigation = true,
                snackBarMessages = state.messages,
                onSnackBarMessageConsumed = {
                    viewModel.accept(Action.SnackbarDismissed(it))
                },
                topBar = {
                    PoppableDestinationTopAppBar(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface),
                        title = {
                            TimelineTitle(
                                movableElementSharedTransitionScope = this,
                                timeline = state.timelineState?.timeline,
                                sharedElementPrefix = state.sharedElementPrefix,
                                // Indicated on the tab instead
                                hasUpdates = false,
                                onPresentationSelected = { timeline, presentation ->
                                    state.stateHolders
                                        .filterIsInstance<ListScreenStateHolders.Timeline>()
                                        .firstOrNull()
                                        ?.accept
                                        ?.invoke(
                                            TimelineState.Action.UpdatePreferredPresentation(
                                                timeline = timeline,
                                                presentation = presentation,
                                            ),
                                        )
                                },
                            )
                        },
                        onBackPressed = { viewModel.accept(Action.Navigate.Pop) },
                        actions = {
                            state.timelineState
                                ?.timeline
                                ?.withListTimelineOrNull { listTimeline ->
                                    FeedListStatus(
                                        status = state.listStatus,
                                        uri = listTimeline.feedList.uri,
                                        onListStatusUpdated = {
                                            viewModel.accept(Action.UpdateFeedListStatus(it))
                                        },
                                    )
                                }
                            ShareRecordButton(
                                onShareClicked = {
                                    state.timelineState?.timeline?.uri
                                        ?.asEmbeddableRecordUriOrNull()
                                        ?.let { recordUri ->
                                            recordOptionsSheetState.showOptions(recordUri)
                                        }
                                },
                            )
                        },
                    )
                },
                content = { paddingValues ->
                    ListScreen(
                        paneScaffoldState = this,
                        modifier = Modifier
                            .padding(
                                top = paddingValues.calculateTopPadding(),
                            ),
                        state = state,
                        actions = viewModel.accept,
                    )
                    SecondaryPaneCloseBackHandler()
                },
            )
        },
    )
}

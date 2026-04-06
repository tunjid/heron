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

package com.tunjid.heron.standard.publication.di

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.data.core.types.ProfileHandleOrId
import com.tunjid.heron.data.core.types.StandardPublicationUri
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.data.utilities.getAsRawUri
import com.tunjid.heron.scaffold.di.ScaffoldBindings
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.decodeReferringRoute
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.hydrate
import com.tunjid.heron.scaffold.scaffold.AppBarTitle
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.PoppableDestinationTopAppBar
import com.tunjid.heron.scaffold.scaffold.SecondaryPaneCloseBackHandler
import com.tunjid.heron.scaffold.scaffold.predictiveBackContentTransformProvider
import com.tunjid.heron.scaffold.scaffold.predictiveBackPlacement
import com.tunjid.heron.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.viewModelCoroutineScope
import com.tunjid.heron.standard.publication.Action
import com.tunjid.heron.standard.publication.ActualStandardPublicationViewModel
import com.tunjid.heron.standard.publication.RouteViewModelInitializer
import com.tunjid.heron.standard.publication.StandardPublicationScreen
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
import com.tunjid.treenav.strings.routePath
import com.tunjid.treenav.strings.toRouteTrie
import com.tunjid.treenav.strings.urlRouteMatcher
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.StringKey
import heron.feature.standard_publication.generated.resources.Res
import heron.feature.standard_publication.generated.resources.publication
import org.jetbrains.compose.resources.stringResource

private const val StandardPublicationRoutePattern =
    "/profile/{profileId}/standard/publication/{publicationUriSuffix}"
private const val StandardPublicationUriRoutePattern =
    "/{did}/site.standard.publication/{publicationUriSuffix}"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
    children = listOfNotNull(
        routeParams.decodeReferringRoute(),
    ),
)

internal sealed class PublicationRequest {
    data class WithProfile(
        val profileHandleOrId: ProfileHandleOrId,
        val publicationUriSuffix: String,
    ) : PublicationRequest()

    data class WithUri(
        val uri: StandardPublicationUri,
    ) : PublicationRequest()
}

private val Route.profileId by mappedRoutePath(
    mapper = ::ProfileHandleOrId,
)

private val Route.publicationUriSuffix by routePath()

private val RequestTrie = mapOf(
    PathPattern(StandardPublicationRoutePattern) to { route: Route ->
        PublicationRequest.WithProfile(
            profileHandleOrId = route.profileId,
            publicationUriSuffix = route.publicationUriSuffix,
        )
    },
    PathPattern(StandardPublicationUriRoutePattern) to { route: Route ->
        PublicationRequest.WithUri(
            uri = route.routeParams.pathAndQueries
                .getAsRawUri(Uri.Host.AtProto)
                .let(::StandardPublicationUri),
        )
    },
).toRouteTrie()

internal val Route.publicationRequest: PublicationRequest
    get() = checkNotNull(RequestTrie[this]).invoke(this)

@BindingContainer
object StandardPublicationNavigationBindings {

    @Provides
    @IntoMap
    @StringKey(StandardPublicationRoutePattern)
    fun provideRouteMatcher(): RouteMatcher =
        urlRouteMatcher(
            routePattern = StandardPublicationRoutePattern,
            routeMapper = ::createRoute,
        )

    @Provides
    @IntoMap
    @StringKey(StandardPublicationUriRoutePattern)
    fun provideUriRouteMatcher(): RouteMatcher =
        urlRouteMatcher(
            routePattern = StandardPublicationUriRoutePattern,
            routeMapper = ::createRoute,
        )
}

@BindingContainer
class StandardPublicationBindings(
    @Includes dataBindings: DataBindings,
    @Includes scaffoldBindings: ScaffoldBindings,
) {

    @Provides
    @IntoMap
    @StringKey(StandardPublicationRoutePattern)
    fun providePaneEntry(
        routeParser: RouteParser,
        viewModelInitializer: RouteViewModelInitializer,
    ): PaneEntry<ThreePane, Route> = routePaneEntry(
        routeParser = routeParser,
        viewModelInitializer = viewModelInitializer,
    )

    @Provides
    @IntoMap
    @StringKey(StandardPublicationUriRoutePattern)
    fun provideUriPaneEntry(
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
            val viewModel = viewModel<ActualStandardPublicationViewModel> {
                viewModelInitializer.invoke(
                    scope = viewModelCoroutineScope(),
                    route = routeParser.hydrate(route),
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()
            val paneScaffoldState = rememberPaneScaffoldState()

            val topAppBarNestedScrollConnection =
                topAppBarNestedScrollConnection()

            paneScaffoldState.PaneScaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .predictiveBackPlacement(paneScaffoldState = paneScaffoldState)
                    .nestedScroll(topAppBarNestedScrollConnection),
                showNavigation = true,
                snackBarMessages = state.messages,
                onSnackBarMessageConsumed = {
                    viewModel.accept(Action.SnackbarDismissed(it))
                },
                topBar = {
                    PoppableDestinationTopAppBar(
                        title = {
                            AppBarTitle(
                                title = state.publication?.name
                                    ?: stringResource(Res.string.publication),
                            )
                        },
                        transparencyFactor = topAppBarNestedScrollConnection::verticalOffsetProgress,
                        onBackPressed = { viewModel.accept(Action.Navigate.Pop) },
                    )
                },
                content = { paddingValues ->
                    StandardPublicationScreen(
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

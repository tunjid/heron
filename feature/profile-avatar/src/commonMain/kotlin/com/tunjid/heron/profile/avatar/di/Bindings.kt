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

package com.tunjid.heron.profile.avatar.di

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.fromBase64EncodedUrl
import com.tunjid.heron.data.core.types.ProfileHandleOrId
import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.profile.avatar.Action
import com.tunjid.heron.profile.avatar.ActualProfileAvatarViewModel
import com.tunjid.heron.profile.avatar.AvatarScreen
import com.tunjid.heron.profile.avatar.RouteViewModelInitializer
import com.tunjid.heron.scaffold.di.ScaffoldBindings
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.decodeReferringRoute
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.hydrate
import com.tunjid.heron.scaffold.scaffold.DragToPopState.Companion.dragToPop
import com.tunjid.heron.scaffold.scaffold.DragToPopState.Companion.rememberDragToPopState
import com.tunjid.heron.scaffold.scaffold.PaneExpandableFloatingToolbar
import com.tunjid.heron.scaffold.scaffold.PaneNavigationRail
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.PoppableDestinationTopAppBar
import com.tunjid.heron.scaffold.scaffold.SecondaryPaneCloseBackHandler
import com.tunjid.heron.scaffold.scaffold.floatingToolbarNestedScroll
import com.tunjid.heron.scaffold.scaffold.fullAppbarTransparency
import com.tunjid.heron.scaffold.scaffold.predictiveBackContentTransform
import com.tunjid.heron.scaffold.scaffold.predictiveBackPlacement
import com.tunjid.heron.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.viewModelCoroutineScope
import com.tunjid.treenav.compose.PaneEntry
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.RouteParser
import com.tunjid.treenav.strings.mappedRoutePath
import com.tunjid.treenav.strings.optionalMappedRouteQuery
import com.tunjid.treenav.strings.optionalRouteQuery
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.urlRouteMatcher
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.StringKey

private const val RoutePattern = "/profile/{profileHandleOrId}/avatar"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
    children = listOfNotNull(
        routeParams.decodeReferringRoute(),
    ),
)

internal val Route.profileHandleOrId by mappedRoutePath(
    mapper = ::ProfileHandleOrId,
)

internal val Route.avatarSharedElementKey by optionalRouteQuery()

internal val Route.profile: Profile? by optionalMappedRouteQuery(
    mapper = String::fromBase64EncodedUrl,
)

@BindingContainer
object ProfileAvatarNavigationBindings {

    @Provides
    @IntoMap
    @StringKey(RoutePattern)
    fun provideRouteMatcher(): RouteMatcher =
        urlRouteMatcher(
            routePattern = RoutePattern,
            routeMapper = ::createRoute,
        )
}

@BindingContainer
class ProfileAvatarBindings(
    @Includes dataBindings: DataBindings,
    @Includes scaffoldBindings: ScaffoldBindings,
) {

    @Provides
    @IntoMap
    @StringKey(RoutePattern)
    fun providePaneEntry(
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
            val viewModel = viewModel<ActualProfileAvatarViewModel> {
                viewModelInitializer.invoke(
                    scope = viewModelCoroutineScope(),
                    route = routeParser.hydrate(route),
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()

            var floatingToolbarExpanded by rememberSaveable { mutableStateOf(true) }

            rememberPaneScaffoldState().PaneScaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .predictiveBackPlacement(paneScope = this)
                    .floatingToolbarNestedScroll(
                        expanded = floatingToolbarExpanded,
                        onExpand = { floatingToolbarExpanded = true },
                        onCollapse = { floatingToolbarExpanded = false },
                    ),
                containerColor = Color.Transparent,
                showNavigation = true,
                topBar = {
                    PoppableDestinationTopAppBar(
                        transparencyFactor = ::fullAppbarTransparency,
                        onBackPressed = { viewModel.accept(Action.Navigate.Pop) },
                    )
                },
                useFloatingToolbar = true,
                floatingToolbar = {
                    PaneExpandableFloatingToolbar(
                        expanded = floatingToolbarExpanded,
                    )
                },
                navigationRail = {
                    PaneNavigationRail()
                },
                onSnackBarMessageConsumed = {
                    viewModel.accept(Action.SnackbarDismissed(it))
                },
                content = {
                    AvatarScreen(
                        paneScaffoldState = this,
                        state = state,
                        actions = viewModel.accept,
                        modifier = Modifier
                            .dragToPop(rememberDragToPopState()),
                    )
                    SecondaryPaneCloseBackHandler()
                },
            )
        },
    )
}

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.fromBase64EncodedUrl
import com.tunjid.heron.data.core.types.ProfileHandleOrId
import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.profile.avatar.Action
import com.tunjid.heron.profile.avatar.ActualProfileAvatarViewModel
import com.tunjid.heron.profile.avatar.AvatarScreen
import com.tunjid.heron.profile.avatar.ProfileAvatarViewModelInitializer
import com.tunjid.heron.profile.avatar.ProfileStateHolder
import com.tunjid.heron.ui.bottomNavigationNestedScrollConnection
import com.tunjid.heron.ui.coroutines.RouteViewModelInitializer
import com.tunjid.heron.ui.modifiers.ifTrue
import com.tunjid.heron.ui.scaffold.di.ScaffoldBindings
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.decodeReferringRoute
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.hydrate
import com.tunjid.heron.ui.scaffold.scaffold.NavigationContentTransformer
import com.tunjid.heron.ui.scaffold.scaffold.PaneNavigationRail
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.ui.scaffold.scaffold.PoppableDestinationTopAppBar
import com.tunjid.heron.ui.scaffold.scaffold.SecondaryPaneCloseBackHandler
import com.tunjid.heron.ui.scaffold.scaffold.fullAppbarTransparency
import com.tunjid.heron.ui.scaffold.scaffold.predictiveBackPlacement
import com.tunjid.heron.ui.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.rememberRouteViewModel
import com.tunjid.mutator.compose.produceStateWithLifecycle
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
import dev.zacsweers.metro.ClassKey
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
    @ClassKey(ActualProfileAvatarViewModel::class)
    fun provideRouteViewModelInitializer(
        initializer: ProfileAvatarViewModelInitializer,
    ): RouteViewModelInitializer = RouteViewModelInitializer(initializer::invoke)

    @Provides
    @IntoMap
    @StringKey(RoutePattern)
    fun providePaneEntry(
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
            val paneScaffoldState = rememberPaneScaffoldState()
            val stateHolder: ProfileStateHolder = paneScaffoldState.rememberRouteViewModel<ActualProfileAvatarViewModel>(
                route = routeParser.hydrate(route),
            )
            val state = stateHolder.produceStateWithLifecycle()

            val bottomNavigationNestedScrollConnection =
                bottomNavigationNestedScrollConnection(
                    isCompact = paneScaffoldState.prefersCompactBottomNav,
                )

            paneScaffoldState.PaneScaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .predictiveBackPlacement(paneScaffoldState = paneScaffoldState)
                    .ifTrue(paneScaffoldState.prefersAutoHidingBottomNav) {
                        nestedScroll(bottomNavigationNestedScrollConnection)
                    },
                containerColor = Color.Transparent,
                showNavigation = true,
                topBar = {
                    PoppableDestinationTopAppBar(
                        transparencyFactor = ::fullAppbarTransparency,
                        onBackPressed = { stateHolder.accept(Action.Navigate.Pop) },
                    )
                },
                navigationRail = {
                    PaneNavigationRail()
                },
                onSnackBarMessageConsumed = {
                    stateHolder.accept(Action.SnackbarDismissed(it))
                },
                content = {
                    AvatarScreen(
                        paneScaffoldState = this,
                        state = state,
                        actions = stateHolder.accept,
                    )
                    SecondaryPaneCloseBackHandler()
                },
            )
        },
    )
}

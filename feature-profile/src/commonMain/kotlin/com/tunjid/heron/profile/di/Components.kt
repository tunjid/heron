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

package com.tunjid.heron.profile.di

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.fromBase64EncodedUrl
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.di.DataComponent
import com.tunjid.heron.profile.Action
import com.tunjid.heron.profile.ActualProfileViewModel
import com.tunjid.heron.profile.ProfileScreen
import com.tunjid.heron.profile.ProfileViewModelCreator
import com.tunjid.heron.scaffold.di.ScaffoldComponent
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.decodeReferringRoute
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.hydrate
import com.tunjid.heron.scaffold.navigation.routePatternAndMatcher
import com.tunjid.heron.scaffold.scaffold.PaneFab
import com.tunjid.heron.scaffold.scaffold.PaneNavigationBar
import com.tunjid.heron.scaffold.scaffold.PaneNavigationRail
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.PoppableDestinationTopAppBar
import com.tunjid.heron.scaffold.scaffold.SecondaryPaneCloseBackHandler
import com.tunjid.heron.scaffold.scaffold.fabOffset
import com.tunjid.heron.scaffold.scaffold.isFabExpanded
import com.tunjid.heron.scaffold.scaffold.predictiveBackBackgroundModifier
import com.tunjid.heron.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.scaffold.ui.bottomNavigationNestedScrollConnection
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
import heron.feature_profile.generated.resources.Res
import heron.feature_profile.generated.resources.mention
import heron.feature_profile.generated.resources.post
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.KmpComponentCreate
import me.tatarka.inject.annotations.Provides
import org.jetbrains.compose.resources.stringResource

private const val RoutePattern = "/profile/{profileId}"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
    children = listOfNotNull(
        routeParams.decodeReferringRoute()
    )
)

internal val Route.profileId by mappedRoutePath(
    mapper = ::Id
)

internal val Route.avatarSharedElementKey by optionalRouteQuery()

internal val Route.profile: Profile? by optionalMappedRouteQuery(
    mapper = String::fromBase64EncodedUrl,
)

@KmpComponentCreate
expect fun ProfileNavigationComponent.Companion.create(): ProfileNavigationComponent

@KmpComponentCreate
expect fun ProfileComponent.Companion.create(
    dataComponent: DataComponent,
    scaffoldComponent: ScaffoldComponent,
): ProfileComponent

@Component
abstract class ProfileNavigationComponent {
    companion object

    @IntoMap
    @Provides
    fun profileRouteParser(): Pair<String, RouteMatcher> =
        routePatternAndMatcher(
            routePattern = RoutePattern,
            routeMapper = ::createRoute,
        )

}

@Component
abstract class ProfileComponent(
    @Component val dataComponent: DataComponent,
    @Component val scaffoldComponent: ScaffoldComponent,
) {
    companion object

    @IntoMap
    @Provides
    fun routeAdaptiveConfiguration(
        routeParser: RouteParser,
        creator: ProfileViewModelCreator,
    ) = RoutePattern to threePaneEntry<Route>(
        paneMapping = { route ->
            mapOf(
                ThreePane.Primary to route,
                ThreePane.Secondary to route.children.firstOrNull() as? Route
            )
        },
        render = { route ->
            val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
            val viewModel = viewModel<ActualProfileViewModel> {
                creator.invoke(
                    scope = lifecycleCoroutineScope,
                    route = routeParser.hydrate(route),
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()

            val bottomNavigationNestedScrollConnection =
                bottomNavigationNestedScrollConnection()

            rememberPaneScaffoldState().PaneScaffold(
                modifier = Modifier
                    .predictiveBackBackgroundModifier(paneScope = this)
                    .nestedScroll(bottomNavigationNestedScrollConnection),
                showNavigation = true,
                topBar = {
                    PoppableDestinationTopAppBar(
                        // Limit width so tabs may be tapped
                        modifier = Modifier.width(60.dp),
                        onBackPressed = { viewModel.accept(Action.Navigate.Pop) },
                    )
                },
                floatingActionButton = {
                    PaneFab(
                        modifier = Modifier
                            .offset {
                                fabOffset(bottomNavigationNestedScrollConnection.offset)
                            },
                        text = stringResource(
                            if (state.isSignedInProfile) Res.string.post
                            else Res.string.mention
                        ),
                        icon =
                            if (state.isSignedInProfile) Icons.Rounded.Edit
                            else Icons.Rounded.AlternateEmail,
                        expanded = isFabExpanded(bottomNavigationNestedScrollConnection.offset),
                        onClick = {
                            viewModel.accept(
                                Action.Navigate.DelegateTo(
                                    NavigationAction.Common.ComposePost(
                                        type =
                                            if (state.isSignedInProfile) Post.Create.Timeline
                                            else Post.Create.Mention(state.profile),
                                        sharedElementPrefix = null,
                                    )
                                )
                            )
                        }
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
                snackBarMessages = state.messages,
                onSnackBarMessageConsumed = {
                },
                content = {
                    ProfileScreen(
                        paneScaffoldState = this,
                        state = state,
                        actions = viewModel.accept,
                        modifier = Modifier,
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

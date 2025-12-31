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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.types.LabelerUri
import com.tunjid.heron.data.core.types.ProfileHandleOrId
import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.profile.Action
import com.tunjid.heron.profile.ActualProfileViewModel
import com.tunjid.heron.profile.ProfileScreen
import com.tunjid.heron.profile.RouteViewModelInitializer
import com.tunjid.heron.scaffold.di.ScaffoldBindings
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.decodeReferringRoute
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.hydrate
import com.tunjid.heron.scaffold.navigation.composePostDestination
import com.tunjid.heron.scaffold.navigation.signInDestination
import com.tunjid.heron.scaffold.scaffold.PaneExpandableFloatingToolbar
import com.tunjid.heron.scaffold.scaffold.PaneFloatingToolbarFab
import com.tunjid.heron.scaffold.scaffold.PaneNavigationRail
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.PaneSnackbarHost
import com.tunjid.heron.scaffold.scaffold.PoppableDestinationTopAppBar
import com.tunjid.heron.scaffold.scaffold.SecondaryPaneCloseBackHandler
import com.tunjid.heron.scaffold.scaffold.floatingToolbarNestedScroll
import com.tunjid.heron.scaffold.scaffold.fullAppbarTransparency
import com.tunjid.heron.scaffold.scaffold.predictiveBackContentTransform
import com.tunjid.heron.scaffold.scaffold.predictiveBackPlacement
import com.tunjid.heron.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.viewModelCoroutineScope
import com.tunjid.heron.ui.text.CommonStrings
import com.tunjid.treenav.compose.PaneEntry
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.RouteParser
import com.tunjid.treenav.strings.mappedRoutePath
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.urlRouteMatcher
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.StringKey
import heron.feature.profile.generated.resources.Res
import heron.feature.profile.generated.resources.mention
import heron.feature.profile.generated.resources.post
import heron.ui.core.generated.resources.sign_in
import org.jetbrains.compose.resources.stringResource

private const val RoutePattern = "/profile/{profileHandleOrId}"
private const val LabelerPattern = "/{profileHandleOrId}/${LabelerUri.NAMESPACE}/self"

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

@BindingContainer
object ProfileNavigationBindings {

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
    @StringKey(LabelerPattern)
    fun provideLabelerMatcher(): RouteMatcher =
        urlRouteMatcher(
            routePattern = LabelerPattern,
            routeMapper = ::createRoute,
        )
}

@BindingContainer
class ProfileBindings(
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

    @Provides
    @IntoMap
    @StringKey(LabelerPattern)
    fun provideLabelerPaneEntry(
        routeParser: RouteParser,
        viewModelInitializer: RouteViewModelInitializer,
    ): PaneEntry<ThreePane, Route> = routePaneEntry(
        routeParser = routeParser,
        viewModelInitializer = viewModelInitializer,
    )

    private fun routePaneEntry(
        routeParser: RouteParser,
        viewModelInitializer: RouteViewModelInitializer,
    ) = threePaneEntry(
        contentTransform = predictiveBackContentTransform,
        paneMapping = { route ->
            mapOf(
                ThreePane.Primary to route,
                ThreePane.Secondary to route.children.firstOrNull() as? Route,
            )
        },
        render = { route ->
            val viewModel = viewModel<ActualProfileViewModel> {
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
                showNavigation = true,
                topBar = {
                    PoppableDestinationTopAppBar(
                        // Limit width so tabs may be tapped
                        modifier = Modifier.width(72.dp),
                        transparencyFactor = ::fullAppbarTransparency,
                        onBackPressed = { viewModel.accept(Action.Navigate.Pop) },
                    )
                },
                snackBarHost = {
                    PaneSnackbarHost()
                },
                useFloatingToolbar = true,
                floatingToolbar = {
                    PaneExpandableFloatingToolbar(
                        expanded = floatingToolbarExpanded,
                        fab = {
                            val fabIcon = when {
                                isSignedOut -> Icons.AutoMirrored.Rounded.Login
                                state.isSignedInProfile -> Icons.Rounded.Edit
                                else -> Icons.Rounded.AlternateEmail
                            }
                            PaneFloatingToolbarFab(
                                icon = fabIcon,
                                onClick = {
                                    viewModel.accept(
                                        Action.Navigate.To(
                                            when {
                                                isSignedOut -> signInDestination()
                                                else -> composePostDestination(
                                                    type =
                                                    if (state.isSignedInProfile) Post.Create.Timeline
                                                    else Post.Create.Mention(state.profile),
                                                    sharedElementPrefix = null,
                                                )
                                            },
                                        ),
                                    )
                                },
                            )
                        },
                    )
                },
                navigationRail = {
                    PaneNavigationRail()
                },
                snackBarMessages = state.messages,
                onSnackBarMessageConsumed = {
                    viewModel.accept(Action.SnackbarDismissed(it))
                },
                content = {
                    ProfileScreen(
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

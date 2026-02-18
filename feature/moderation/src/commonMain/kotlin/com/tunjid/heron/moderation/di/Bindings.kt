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

package com.tunjid.heron.moderation.di

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.round
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.moderation.Action
import com.tunjid.heron.moderation.ActualModerationViewModel
import com.tunjid.heron.moderation.ModerationScreen
import com.tunjid.heron.moderation.RouteViewModelInitializer
import com.tunjid.heron.scaffold.di.ScaffoldBindings
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.decodeReferringRoute
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.hydrate
import com.tunjid.heron.scaffold.scaffold.AppBarTitle
import com.tunjid.heron.scaffold.scaffold.PaneNavigationBar
import com.tunjid.heron.scaffold.scaffold.PaneNavigationRail
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.PoppableDestinationTopAppBar
import com.tunjid.heron.scaffold.scaffold.SecondaryPaneCloseBackHandler
import com.tunjid.heron.scaffold.scaffold.predictiveBackContentTransformProvider
import com.tunjid.heron.scaffold.scaffold.predictiveBackPlacement
import com.tunjid.heron.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.viewModelCoroutineScope
import com.tunjid.heron.ui.bottomNavigationNestedScrollConnection
import com.tunjid.heron.ui.modifiers.ifTrue
import com.tunjid.treenav.compose.PaneEntry
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.RouteParser
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.urlRouteMatcher
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.StringKey
import heron.feature.moderation.generated.resources.Res
import heron.feature.moderation.generated.resources.moderation_settings
import org.jetbrains.compose.resources.stringResource

private const val RoutePattern = "/moderation"

private fun createRoute(routeParams: RouteParams) =
    routeOf(params = routeParams, children = listOfNotNull(routeParams.decodeReferringRoute()))

@BindingContainer
object ModerationNavigationBindings {

    @Provides
    @IntoMap
    @StringKey(RoutePattern)
    fun provideRouteMatcher(): RouteMatcher =
        urlRouteMatcher(routePattern = RoutePattern, routeMapper = ::createRoute)
}

@BindingContainer
class ModerationBindings(
    @Includes dataBindings: DataBindings,
    @Includes scaffoldBindings: ScaffoldBindings,
) {

    @Provides
    @IntoMap
    @StringKey(RoutePattern)
    fun providePaneEntry(
        routeParser: RouteParser,
        viewModelInitializer: RouteViewModelInitializer,
    ): PaneEntry<ThreePane, Route> =
        routePaneEntry(routeParser = routeParser, viewModelInitializer = viewModelInitializer)

    private fun routePaneEntry(
        routeParser: RouteParser,
        viewModelInitializer: RouteViewModelInitializer,
    ) =
        threePaneEntry<Route>(
            contentTransform = predictiveBackContentTransformProvider(),
            paneMapping = { route ->
                mapOf(
                    ThreePane.Primary to route,
                    ThreePane.Secondary to route.children.firstOrNull() as? Route,
                )
            },
            render = { route ->
                val viewModel =
                    viewModel<ActualModerationViewModel> {
                        viewModelInitializer.invoke(
                            scope = viewModelCoroutineScope(),
                            route = routeParser.hydrate(route),
                        )
                    }
                val state by viewModel.state.collectAsStateWithLifecycle()
                val paneScaffoldState = rememberPaneScaffoldState()

                val bottomNavigationNestedScrollConnection =
                    bottomNavigationNestedScrollConnection(
                        isCompact = paneScaffoldState.prefersCompactBottomNav
                    )

                paneScaffoldState.PaneScaffold(
                    modifier =
                        Modifier.fillMaxSize()
                            .predictiveBackPlacement(paneScaffoldState = paneScaffoldState)
                            .ifTrue(paneScaffoldState.prefersAutoHidingBottomNav) {
                                nestedScroll(bottomNavigationNestedScrollConnection)
                            },
                    showNavigation = true,
                    snackBarMessages = state.messages,
                    onSnackBarMessageConsumed = { viewModel.accept(Action.SnackbarDismissed(it)) },
                    topBar = {
                        PoppableDestinationTopAppBar(
                            title = {
                                AppBarTitle(title = stringResource(Res.string.moderation_settings))
                            },
                            onBackPressed = { viewModel.accept(Action.Navigate.Pop) },
                        )
                    },
                    navigationBar = {
                        PaneNavigationBar(
                            modifier =
                                Modifier.offset {
                                    bottomNavigationNestedScrollConnection.offset.round()
                                }
                        )
                    },
                    navigationRail = { PaneNavigationRail() },
                    content = { paddingValues ->
                        ModerationScreen(
                            paneScaffoldState = this,
                            state = state,
                            actions = viewModel.accept,
                            modifier = Modifier.padding(top = paddingValues.calculateTopPadding()),
                        )
                        SecondaryPaneCloseBackHandler()
                    },
                )
            },
        )
}

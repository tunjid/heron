/*
 *    Copyright 2026 Adetunji Dahunsi
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

package com.tunjid.heron.inference.di

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.round
import com.tunjid.heron.inference.Action
import com.tunjid.heron.inference.ActualInferenceViewModel
import com.tunjid.heron.inference.InferenceScreen
import com.tunjid.heron.inference.InferenceStateHolder
import com.tunjid.heron.inference.InferenceViewModelInitializer
import com.tunjid.heron.ui.modifiers.ifTrue
import com.tunjid.heron.ui.scaffold.di.NavigationScope
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
import com.tunjid.heron.ui.verticalOffsetProgress
import com.tunjid.mutator.compose.produceStateWithLifecycle
import com.tunjid.treenav.compose.PaneEntry
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.RouteParser
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.urlRouteMatcher
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ClassKey
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.StringKey
import heron.feature.inference.generated.resources.Res
import heron.feature.inference.generated.resources.inference
import org.jetbrains.compose.resources.stringResource

private const val InferenceRoutePattern = "/inference"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
    children = listOfNotNull(
        routeParams.decodeReferringRoute(),
    ),
)

@BindingContainer
@ContributesTo(NavigationScope::class)
object InferenceNavigationBindings {

    @Provides
    @IntoMap
    @StringKey(InferenceRoutePattern)
    fun provideRouteMatcher(): RouteMatcher =
        urlRouteMatcher(
            routePattern = InferenceRoutePattern,
            routeMapper = ::createRoute,
        )
}

@BindingContainer
@ContributesTo(AppScope::class)
object InferenceBindings {

    @Provides
    @IntoMap
    @ClassKey(InferenceStateHolder::class)
    fun provideRouteStateHolderInitializer(
        initializer: InferenceViewModelInitializer,
    ): RouteStateHolderInitializer = RouteStateHolderInitializer(initializer::invoke)

    @Provides
    @IntoMap
    @StringKey(InferenceRoutePattern)
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
    val stateHolder = paneScaffoldState.retainRouteStateHolder<InferenceStateHolder>(
        route = route,
    )
    val state = stateHolder.produceStateWithLifecycle()

    val topAppBarNestedScrollConnection =
        paneScaffoldState.topAppBarNestedScrollConnection

    val bottomNavigationNestedScrollConnection =
        paneScaffoldState.bottomNavigationNestedScrollConnection

    paneScaffoldState.PaneScaffold(
        modifier = Modifier
            .fillMaxSize()
            .predictiveBackPlacement(paneScaffoldState = paneScaffoldState)
            .nestedScroll(topAppBarNestedScrollConnection)
            .ifTrue(paneScaffoldState.prefersAutoHidingBottomNav) {
                nestedScroll(bottomNavigationNestedScrollConnection)
            },
        showNavigation = true,
        topBar = {
            PoppableDestinationTopAppBar(
                title = {
                    AppBarTitle(
                        title = stringResource(Res.string.inference),
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
            InferenceScreen(
                paneScaffoldState = this,
                state = state,
                actions = stateHolder.accept,
                modifier = Modifier,
            )
            SecondaryPaneCloseBackHandler()
        },
    )
}

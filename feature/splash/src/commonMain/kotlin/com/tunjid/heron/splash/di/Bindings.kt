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

package com.tunjid.heron.splash.di

import androidx.compose.ui.Modifier
import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.splash.ActualSplashViewModel
import com.tunjid.heron.splash.SplashScreen
import com.tunjid.heron.splash.SplashStateHolder
import com.tunjid.heron.splash.SplashViewModelInitializer
import com.tunjid.heron.ui.coroutines.RouteViewModelInitializer
import com.tunjid.heron.ui.scaffold.di.ScaffoldBindings
import com.tunjid.heron.ui.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.rememberRouteViewModel
import com.tunjid.mutator.compose.produceStateWithLifecycle
import com.tunjid.treenav.compose.PaneEntry
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.urlRouteMatcher
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ClassKey
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.StringKey

private const val RoutePattern = "/splash"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
)

@BindingContainer
object SplashNavigationBindings {

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
class SplashBindings(
    @Includes dataBindings: DataBindings,
    @Includes scaffoldBindings: ScaffoldBindings,
) {

    @Provides
    @IntoMap
    @ClassKey(ActualSplashViewModel::class)
    fun provideRouteViewModelInitializer(
        initializer: SplashViewModelInitializer,
    ): RouteViewModelInitializer = RouteViewModelInitializer(initializer::invoke)

    @Provides
    @IntoMap
    @StringKey(RoutePattern)
    fun providePaneEntry(): PaneEntry<ThreePane, Route> = routePaneEntry()

    private fun routePaneEntry() = threePaneEntry(
        render = { route ->
            val paneScaffoldState = rememberPaneScaffoldState()
            val stateHolder: SplashStateHolder = paneScaffoldState.rememberRouteViewModel<ActualSplashViewModel>(
                route = route,
            )
            stateHolder.produceStateWithLifecycle()

            SplashScreen(
                paneScaffoldState = paneScaffoldState,
                modifier = Modifier,
            )
        },
    )
}

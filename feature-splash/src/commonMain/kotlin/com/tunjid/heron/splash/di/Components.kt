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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.data.di.DataComponent
import com.tunjid.heron.scaffold.di.ScaffoldComponent
import com.tunjid.heron.scaffold.navigation.routeAndMatcher
import com.tunjid.heron.scaffold.navigation.routeOf
import com.tunjid.heron.splash.ActualSplashViewModel
import com.tunjid.heron.splash.SplashScreen
import com.tunjid.heron.splash.SplashViewModelCreator
import com.tunjid.heron.ui.requirePanedSharedElementScope
import com.tunjid.treenav.compose.threepane.threePaneListDetailStrategy
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.KmpComponentCreate
import me.tatarka.inject.annotations.Provides

private const val RoutePattern = "/splash"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
)

@KmpComponentCreate
expect fun SplashNavigationComponent.Companion.create(): SplashNavigationComponent

@KmpComponentCreate
expect fun SplashComponent.Companion.create(
    dataComponent: DataComponent,
    scaffoldComponent: ScaffoldComponent,
): SplashComponent

@Component
abstract class SplashNavigationComponent {
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
abstract class SplashComponent(
    @Component val dataComponent: DataComponent,
    @Component val scaffoldComponent: ScaffoldComponent,
) {
    companion object

    @IntoMap
    @Provides
    fun routeAdaptiveConfiguration(
        creator: SplashViewModelCreator,
    ) = RoutePattern to threePaneListDetailStrategy(
        render = { route ->
            val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
            val viewModel = viewModel<ActualSplashViewModel> {
                creator.invoke(
                    scope = lifecycleCoroutineScope,
                    route = route,
                )
            }
            viewModel.state.collectAsStateWithLifecycle()

            SplashScreen(
                panedSharedElementScope = requirePanedSharedElementScope(),
                modifier = Modifier,
            )
        }
    )
}

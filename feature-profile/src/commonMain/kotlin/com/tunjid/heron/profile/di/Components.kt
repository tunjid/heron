/*
 * Copyright 2024 Adetunji Dahunsi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.heron.profile.di

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.fromBase64EncodedUrl
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.di.DataComponent
import com.tunjid.heron.profile.Action
import com.tunjid.heron.profile.ActualProfileStateHolder
import com.tunjid.heron.profile.ProfileScreen
import com.tunjid.heron.profile.ProfileStateHolderCreator
import com.tunjid.heron.scaffold.di.ScaffoldComponent
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.decodeReferringRoute
import com.tunjid.heron.scaffold.navigation.routeAndMatcher
import com.tunjid.heron.scaffold.navigation.routeOf
import com.tunjid.heron.scaffold.scaffold.BottomAppBar
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.ui.SharedElementScope
import com.tunjid.heron.scaffold.scaffold.predictiveBackBackgroundModifier
import com.tunjid.heron.ui.requirePanedSharedElementScope
import com.tunjid.heron.scaffold.ui.bottomAppBarAccumulatedOffsetNestedScrollConnection
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneListDetailStrategy
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import heron.feature_profile.generated.resources.Res
import heron.feature_profile.generated.resources.back
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoMap
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

internal val Route.profileId
    get() = Id(routeParams.pathArgs.getValue("profileId"))

internal val Route.avatarSharedElementKey
    get() = routeParams.queryParams["avatarSharedElementKey"]?.firstOrNull()

internal val Route.profile
    get():Profile? = routeParams.queryParams["profile"]?.firstOrNull()?.fromBase64EncodedUrl()

@Component
abstract class ProfileNavigationComponent {

    @IntoMap
    @Provides
    fun profileRouteParser(): Pair<String, RouteMatcher> =
        routeAndMatcher(
            routePattern = RoutePattern,
            routeMapper = ::createRoute,
        )

}

@Component
abstract class ProfileComponent(
    @Component val dataComponent: DataComponent,
    @Component val scaffoldComponent: ScaffoldComponent,
) {

    @IntoMap
    @Provides
    fun routeAdaptiveConfiguration(
        creator: ProfileStateHolderCreator,
    ) = RoutePattern to threePaneListDetailStrategy(
        paneMapping = { route ->
            mapOf(
                ThreePane.Primary to route,
                ThreePane.Secondary to route.children.firstOrNull() as? Route
            )
        },
        render = { route ->
            val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
            val viewModel = viewModel<ActualProfileStateHolder> {
                creator.invoke(
                    scope = lifecycleCoroutineScope,
                    route = route,
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()

            val sharedElementScope = requirePanedSharedElementScope()
            val bottomNavAccumulatedOffsetNestedScrollConnection =
                bottomAppBarAccumulatedOffsetNestedScrollConnection()

            PaneScaffold(
                modifier = Modifier
                    .predictiveBackBackgroundModifier(paneScope = this)
                    .nestedScroll(bottomNavAccumulatedOffsetNestedScrollConnection),
                showNavigation = true,
                topBar = {
                    TopBar {
                        viewModel.accept(
                            Action.Navigate.DelegateTo(NavigationAction.Common.Pop)
                        )
                    }
                },
                bottomBar = {
                    BottomBar(
                        sharedElementScope = sharedElementScope,
                        modifier = Modifier.offset {
                            bottomNavAccumulatedOffsetNestedScrollConnection.offset.round()
                        }
                    )
                },
                snackBarMessages = state.messages,
                onSnackBarMessageConsumed = {
                },
                content = {
                    ProfileScreen(
                        sharedElementScope = sharedElementScope,
                        state = state,
                        actions = viewModel.accept,
                        modifier = Modifier,
                    )
                }
            )
        }
    )
}


@Composable
private fun TopBar(
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit,
) {
    TopAppBar(
        // Limit width so tabs may be tapped
        modifier = modifier.width(60.dp),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
        ),
        navigationIcon = {
            FilledTonalIconButton(
                modifier = Modifier,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                        alpha = 0.9f
                    )
                ),
                onClick = onBackPressed,
            ) {
                Image(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(Res.string.back),
                )
            }
        },
        title = {

        },
    )
}

@Composable
private fun BottomBar(
    modifier: Modifier = Modifier,
    sharedElementScope: SharedElementScope,
) {
    BottomAppBar(
        modifier = modifier,
        sharedElementScope = sharedElementScope,
    )
}
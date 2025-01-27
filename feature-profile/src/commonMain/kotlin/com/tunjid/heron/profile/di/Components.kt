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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.ui.unit.IntOffset
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
import com.tunjid.heron.scaffold.navigation.routeAndMatcher
import com.tunjid.heron.scaffold.navigation.routeOf
import com.tunjid.heron.scaffold.scaffold.BottomAppBar
import com.tunjid.heron.scaffold.scaffold.Fab
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.SecondaryPaneCloseBackHandler
import com.tunjid.heron.scaffold.scaffold.isFabExpanded
import com.tunjid.heron.scaffold.scaffold.predictiveBackBackgroundModifier
import com.tunjid.heron.scaffold.ui.bottomNavigationNestedScrollConnection
import com.tunjid.heron.ui.PanedSharedElementScope
import com.tunjid.heron.ui.requirePanedSharedElementScope
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneListDetailStrategy
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.RouteParser
import heron.feature_profile.generated.resources.Res
import heron.feature_profile.generated.resources.back
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

internal val Route.profileId
    get() = Id(routeParams.pathArgs.getValue("profileId"))

internal val Route.avatarSharedElementKey
    get() = routeParams.queryParams["avatarSharedElementKey"]?.firstOrNull()

internal val Route.profile
    get():Profile? = routeParams.queryParams["profile"]?.firstOrNull()?.fromBase64EncodedUrl()

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
    companion object

    @IntoMap
    @Provides
    fun routeAdaptiveConfiguration(
        routeParser: RouteParser,
        creator: ProfileViewModelCreator,
    ) = RoutePattern to threePaneListDetailStrategy<Route>(
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

            val sharedElementScope = requirePanedSharedElementScope()
            val bottomNavigationNestedScrollConnection =
                bottomNavigationNestedScrollConnection()

            PaneScaffold(
                modifier = Modifier
                    .predictiveBackBackgroundModifier(paneScope = this)
                    .nestedScroll(bottomNavigationNestedScrollConnection),
                showNavigation = true,
                topBar = {
                    TopBar {
                        viewModel.accept(
                            Action.Navigate.DelegateTo(NavigationAction.Common.Pop)
                        )
                    }
                },
                floatingActionButton = {
                    Fab(
                        modifier = Modifier
                            .offset {
                                if (isMediumScreenWidthOrWider) IntOffset.Zero
                                else bottomNavigationNestedScrollConnection.offset.round()
                            },
                        panedSharedElementScope = sharedElementScope,
                        expanded = isFabExpanded(
                            offset = bottomNavigationNestedScrollConnection.offset
                        ),
                        text = stringResource(
                            if (state.isSignedInProfile) Res.string.post
                            else Res.string.mention
                        ),
                        icon =
                        if (state.isSignedInProfile) Icons.Rounded.Edit
                        else Icons.Rounded.AlternateEmail,
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
                bottomBar = {
                    BottomBar(
                        panedSharedElementScope = sharedElementScope,
                        modifier = Modifier.offset {
                            bottomNavigationNestedScrollConnection.offset.round()
                        }
                    )
                },
                snackBarMessages = state.messages,
                onSnackBarMessageConsumed = {
                },
                content = {
                    ProfileScreen(
                        panedSharedElementScope = sharedElementScope,
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
    panedSharedElementScope: PanedSharedElementScope,
) {
    BottomAppBar(
        modifier = modifier,
        panedSharedElementScope = panedSharedElementScope,
    )
}
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

package com.tunjid.heron.profiles.di

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.data.core.types.ProfileHandleOrId
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.data.di.DataComponent
import com.tunjid.heron.profiles.Action
import com.tunjid.heron.profiles.ActualProfilesViewModel
import com.tunjid.heron.profiles.Load
import com.tunjid.heron.profiles.ProfilesScreen
import com.tunjid.heron.profiles.RouteViewModelInitializer
import com.tunjid.heron.scaffold.di.ScaffoldComponent
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.decodeReferringRoute
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.hydrate
import com.tunjid.heron.scaffold.navigation.routePatternAndMatcher
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.predictiveBackBackgroundModifier
import com.tunjid.heron.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.PathPattern
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.RouteParser
import com.tunjid.treenav.strings.mappedRoutePath
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.toRouteTrie
import heron.feature_profiles.generated.resources.Res
import heron.feature_profiles.generated.resources.back
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.KmpComponentCreate
import me.tatarka.inject.annotations.Provides
import org.jetbrains.compose.resources.stringResource

private const val PostLikesPattern = "/profile/{profileHandleOrId}/post/{postRecordKey}/liked-by"
private const val PostRepostsPattern = "/profile/{profileHandleOrId}/post/{postRecordKey}/reposted-by"
private const val ProfileFollowersPattern = "/profile/{profileHandleOrId}/followers"
private const val ProfileFollowingPattern = "/profile/{profileHandleOrId}/follows"

private val LoadTrie = mapOf(
    PathPattern(PostLikesPattern) to { route: Route ->
        Load.Post.Likes(
            route.postRecordKey,
            route.profileHandleOrId,
        )
    },
    PathPattern(PostRepostsPattern) to { route: Route ->
        Load.Post.Reposts(
            route.postRecordKey,
            route.profileHandleOrId,
        )
    },
    PathPattern(ProfileFollowersPattern) to { route: Route ->
        Load.Profile.Followers(
            route.profileHandleOrId,
        )
    },
    PathPattern(ProfileFollowingPattern) to { route: Route ->
        Load.Profile.Following(
            route.profileHandleOrId,
        )
    },
).toRouteTrie()

internal val Route.load
    get() = LoadTrie[this]?.invoke(this)!!

private val Route.profileHandleOrId by mappedRoutePath(
    mapper = ::ProfileHandleOrId
)

private val Route.postRecordKey by mappedRoutePath(
    mapper = ::RecordKey
)

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
    children = listOfNotNull(
        routeParams.decodeReferringRoute()
    )
)

@KmpComponentCreate
expect fun ProfilesNavigationComponent.Companion.create(): ProfilesNavigationComponent

@KmpComponentCreate
expect fun ProfilesComponent.Companion.create(
    dataComponent: DataComponent,
    scaffoldComponent: ScaffoldComponent,
): ProfilesComponent

@Component
abstract class ProfilesNavigationComponent {
    companion object

    @IntoMap
    @Provides
    fun postLikesRouteParser(): Pair<String, RouteMatcher> =
        routePatternAndMatcher(
            routePattern = PostLikesPattern,
            routeMapper = ::createRoute,
        )

    @IntoMap
    @Provides
    fun postRepostsRouteParser(): Pair<String, RouteMatcher> =
        routePatternAndMatcher(
            routePattern = PostRepostsPattern,
            routeMapper = ::createRoute,
        )

    @IntoMap
    @Provides
    fun profileFollowersRouteParser(): Pair<String, RouteMatcher> =
        routePatternAndMatcher(
            routePattern = ProfileFollowersPattern,
            routeMapper = ::createRoute,
        )

    @IntoMap
    @Provides
    fun profileFollowingRouteParser(): Pair<String, RouteMatcher> =
        routePatternAndMatcher(
            routePattern = ProfileFollowingPattern,
            routeMapper = ::createRoute,
        )
}

@Component
abstract class ProfilesComponent(
    @Component val dataComponent: DataComponent,
    @Component val scaffoldComponent: ScaffoldComponent,
) {
    companion object

    @IntoMap
    @Provides
    fun postLikesAdaptiveConfiguration(
        routeParser: RouteParser,
        viewModelInitializer: RouteViewModelInitializer,
    ) = PostLikesPattern to routePaneEntry(
        routeParser = routeParser,
        viewModelInitializer = viewModelInitializer,
    )

    @IntoMap
    @Provides
    fun postRepostsAdaptiveConfiguration(
        routeParser: RouteParser,
        viewModelInitializer: RouteViewModelInitializer,
    ) = PostRepostsPattern to routePaneEntry(
        routeParser = routeParser,
        viewModelInitializer = viewModelInitializer,
    )

    @IntoMap
    @Provides
    fun profileFollowersAdaptiveConfiguration(
        routeParser: RouteParser,
        viewModelInitializer: RouteViewModelInitializer,
    ) = ProfileFollowersPattern to routePaneEntry(
        routeParser = routeParser,
        viewModelInitializer = viewModelInitializer,
    )

    @IntoMap
    @Provides
    fun profileFollowingAdaptiveConfiguration(
        routeParser: RouteParser,
        viewModelInitializer: RouteViewModelInitializer,
    ) = ProfileFollowingPattern to routePaneEntry(
        routeParser = routeParser,
        viewModelInitializer = viewModelInitializer,
    )

    private fun routePaneEntry(
        routeParser: RouteParser,
        viewModelInitializer: RouteViewModelInitializer,
    ) = threePaneEntry(
        paneMapping = { route ->
            mapOf(
                ThreePane.Primary to route,
                ThreePane.Secondary to route.children.firstOrNull() as? Route
            )
        },
        render = { route ->
            val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
            val viewModel = viewModel<ActualProfilesViewModel> {
                viewModelInitializer.invoke(
                    scope = lifecycleCoroutineScope,
                    route = routeParser.hydrate(route),
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()

            rememberPaneScaffoldState().PaneScaffold(
                modifier = Modifier
                    .predictiveBackBackgroundModifier(paneScope = this),
                showNavigation = true,
                snackBarMessages = state.messages,
                onSnackBarMessageConsumed = {
                },
                topBar = {
                    TopBar { viewModel.accept(Action.Navigate.Pop) }
                },
                content = { paddingValues ->
                    ProfilesScreen(
                        paneScaffoldState = this,
                        modifier = Modifier
                            .padding(
                                paddingValues = PaddingValues(
                                    top = paddingValues.calculateTopPadding()
                                )
                            ),
                        state = state,
                        actions = viewModel.accept,
                    )
                }
            )
        }
    )
}

@Composable
private fun TopBar(
    onBackPressed: () -> Unit,
) {
    TopAppBar(
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
        title = {},
    )
}

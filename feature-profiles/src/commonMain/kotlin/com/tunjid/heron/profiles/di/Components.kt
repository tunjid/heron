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
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.di.DataComponent
import com.tunjid.heron.profiles.Action
import com.tunjid.heron.profiles.ActualProfilesStateHolder
import com.tunjid.heron.profiles.Load
import com.tunjid.heron.profiles.ProfilesScreen
import com.tunjid.heron.profiles.ProfilesStateHolderCreator
import com.tunjid.heron.scaffold.di.ScaffoldComponent
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.decodeReferringRoute
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.hydrate
import com.tunjid.heron.scaffold.navigation.routeAndMatcher
import com.tunjid.heron.scaffold.navigation.routeOf
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.predictiveBackBackgroundModifier
import com.tunjid.heron.ui.requirePanedSharedElementScope
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneListDetailStrategy
import com.tunjid.treenav.strings.PathPattern
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.RouteParser
import com.tunjid.treenav.strings.RouteTrie
import heron.feature_profiles.generated.resources.Res
import heron.feature_profiles.generated.resources.back
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.KmpComponentCreate
import me.tatarka.inject.annotations.Provides
import org.jetbrains.compose.resources.stringResource

private const val PostLikesPattern = "/post/{postId}/likes"
private const val PostRepostsPattern = "/post/{postId}/reposts"
private const val ProfileFollowersPattern = "/profile/{profileId}/followers"
private const val ProfileFollowingPattern = "/profile/{profileId}/follows"

private val LoadTrie = RouteTrie<(Route) -> Load>().apply {
    set(PathPattern(PostLikesPattern)) { Load.Post.Likes(it.postId) }
    set(PathPattern(PostRepostsPattern)) { Load.Post.Reposts(it.postId) }
    set(PathPattern(ProfileFollowersPattern)) { Load.Profile.Followers(it.profileId) }
    set(PathPattern(ProfileFollowingPattern)) { Load.Profile.Following(it.profileId) }
}

internal val Route.load
    get() = LoadTrie[this]?.invoke(this)!!

private val Route.profileId
    get() = Id(routeParams.pathArgs.getValue("profileId"))

private val Route.postId
    get() = Id(routeParams.pathArgs.getValue("postId"))

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
        routeAndMatcher(
            routePattern = PostLikesPattern,
            routeMapper = ::createRoute,
        )

    @IntoMap
    @Provides
    fun postRepostsRouteParser(): Pair<String, RouteMatcher> =
        routeAndMatcher(
            routePattern = PostRepostsPattern,
            routeMapper = ::createRoute,
        )

    @IntoMap
    @Provides
    fun profileFollowersRouteParser(): Pair<String, RouteMatcher> =
        routeAndMatcher(
            routePattern = ProfileFollowersPattern,
            routeMapper = ::createRoute,
        )

    @IntoMap
    @Provides
    fun profileFollowingRouteParser(): Pair<String, RouteMatcher> =
        routeAndMatcher(
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
        creator: ProfilesStateHolderCreator,
    ) = PostLikesPattern to profilesStrategy(
        routeParser = routeParser,
        creator = creator,
    )

    @IntoMap
    @Provides
    fun postRepostsAdaptiveConfiguration(
        routeParser: RouteParser,
        creator: ProfilesStateHolderCreator,
    ) = PostRepostsPattern to profilesStrategy(
        routeParser = routeParser,
        creator = creator,
    )

    @IntoMap
    @Provides
    fun profileFollowersAdaptiveConfiguration(
        routeParser: RouteParser,
        creator: ProfilesStateHolderCreator,
    ) = ProfileFollowersPattern to profilesStrategy(
        routeParser = routeParser,
        creator = creator,
    )

    @IntoMap
    @Provides
    fun profileFollowingAdaptiveConfiguration(
        routeParser: RouteParser,
        creator: ProfilesStateHolderCreator,
    ) = ProfileFollowingPattern to profilesStrategy(
        routeParser = routeParser,
        creator = creator,
    )

    private fun profilesStrategy(
        routeParser: RouteParser,
        creator: ProfilesStateHolderCreator,
    ) = threePaneListDetailStrategy(
        paneMapping = { route ->
            mapOf(
                ThreePane.Primary to route,
                ThreePane.Secondary to route.children.firstOrNull() as? Route
            )
        },
        render = { route ->
            val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
            val viewModel = viewModel<ActualProfilesStateHolder> {
                creator.invoke(
                    scope = lifecycleCoroutineScope,
                    route = routeParser.hydrate(route),
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()

            PaneScaffold(
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
                        panedSharedElementScope = requirePanedSharedElementScope(),
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

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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.data.core.types.ProfileHandleOrId
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.profiles.Action
import com.tunjid.heron.profiles.ActualProfilesViewModel
import com.tunjid.heron.profiles.Load
import com.tunjid.heron.profiles.ProfilesScreen
import com.tunjid.heron.profiles.RouteViewModelInitializer
import com.tunjid.heron.scaffold.di.ScaffoldBindings
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.decodeReferringRoute
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.hydrate
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.PoppableDestinationTopAppBar
import com.tunjid.heron.scaffold.scaffold.predictiveBackContentTransform
import com.tunjid.heron.scaffold.scaffold.predictiveBackPlacement
import com.tunjid.heron.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.viewModelCoroutineScope
import com.tunjid.treenav.compose.PaneEntry
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
import com.tunjid.treenav.strings.urlRouteMatcher
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.StringKey
import heron.feature.profiles.generated.resources.Res
import heron.feature.profiles.generated.resources.blocked_accounts
import heron.feature.profiles.generated.resources.followers
import heron.feature.profiles.generated.resources.following
import heron.feature.profiles.generated.resources.likes
import heron.feature.profiles.generated.resources.muted_accounts
import heron.feature.profiles.generated.resources.reposts
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val BlockedProfilesPattern = "/moderation/blocked-accounts"
private const val MutedProfilesPattern = "/moderation/muted-accounts"
private const val PostLikesPattern = "/profile/{profileHandleOrId}/post/{postRecordKey}/liked-by"
private const val PostRepostsPattern =
    "/profile/{profileHandleOrId}/post/{postRecordKey}/reposted-by"
private const val ProfileFollowersPattern = "/profile/{profileHandleOrId}/followers"
private const val ProfileFollowingPattern = "/profile/{profileHandleOrId}/follows"

private val LoadTrie = mapOf(
    PathPattern(BlockedProfilesPattern) to {
        Load.Moderation.Blocks
    },
    PathPattern(MutedProfilesPattern) to {
        Load.Moderation.Mutes
    },
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
    mapper = ::ProfileHandleOrId,
)

private val Route.postRecordKey by mappedRoutePath(
    mapper = ::RecordKey,
)

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
    children = listOfNotNull(
        routeParams.decodeReferringRoute(),
    ),
)

@BindingContainer
object ProfilesNavigationBindings {

    @Provides
    @IntoMap
    @StringKey(BlockedProfilesPattern)
    fun provideBlocksRouteMatcher(): RouteMatcher =
        urlRouteMatcher(
            routePattern = BlockedProfilesPattern,
            routeMapper = ::createRoute,
        )

    @Provides
    @IntoMap
    @StringKey(MutedProfilesPattern)
    fun provideMutesRouteMatcher(): RouteMatcher =
        urlRouteMatcher(
            routePattern = MutedProfilesPattern,
            routeMapper = ::createRoute,
        )

    @Provides
    @IntoMap
    @StringKey(PostLikesPattern)
    fun providePostLikesRouteMatcher(): RouteMatcher =
        urlRouteMatcher(
            routePattern = PostLikesPattern,
            routeMapper = ::createRoute,
        )

    @Provides
    @IntoMap
    @StringKey(PostRepostsPattern)
    fun providePostRepostsRouteMatcher(): RouteMatcher =
        urlRouteMatcher(
            routePattern = PostRepostsPattern,
            routeMapper = ::createRoute,
        )

    @Provides
    @IntoMap
    @StringKey(ProfileFollowersPattern)
    fun provideProfileFollowersRouteMatcher(): RouteMatcher =
        urlRouteMatcher(
            routePattern = ProfileFollowersPattern,
            routeMapper = ::createRoute,
        )

    @Provides
    @IntoMap
    @StringKey(ProfileFollowingPattern)
    fun provideProfileFollowingRouteMatcher(): RouteMatcher =
        urlRouteMatcher(
            routePattern = ProfileFollowingPattern,
            routeMapper = ::createRoute,
        )
}

@BindingContainer
class ProfilesBindings(
    @Includes dataBindings: DataBindings,
    @Includes scaffoldBindings: ScaffoldBindings,
) {

    @Provides
    @IntoMap
    @StringKey(BlockedProfilesPattern)
    fun provideBlocksPaneEntry(
        routeParser: RouteParser,
        viewModelInitializer: RouteViewModelInitializer,
    ): PaneEntry<ThreePane, Route> = routePaneEntry(
        routeParser = routeParser,
        viewModelInitializer = viewModelInitializer,
    )

    @Provides
    @IntoMap
    @StringKey(MutedProfilesPattern)
    fun provideMutesPaneEntry(
        routeParser: RouteParser,
        viewModelInitializer: RouteViewModelInitializer,
    ): PaneEntry<ThreePane, Route> = routePaneEntry(
        routeParser = routeParser,
        viewModelInitializer = viewModelInitializer,
    )

    @Provides
    @IntoMap
    @StringKey(PostLikesPattern)
    fun providePostLikesPaneEntry(
        routeParser: RouteParser,
        viewModelInitializer: RouteViewModelInitializer,
    ): PaneEntry<ThreePane, Route> = routePaneEntry(
        routeParser = routeParser,
        viewModelInitializer = viewModelInitializer,
    )

    @Provides
    @IntoMap
    @StringKey(PostRepostsPattern)
    fun providePostRepostsPaneEntry(
        routeParser: RouteParser,
        viewModelInitializer: RouteViewModelInitializer,
    ): PaneEntry<ThreePane, Route> = routePaneEntry(
        routeParser = routeParser,
        viewModelInitializer = viewModelInitializer,
    )

    @Provides
    @IntoMap
    @StringKey(ProfileFollowersPattern)
    fun provideProfileFollowersPaneEntry(
        routeParser: RouteParser,
        viewModelInitializer: RouteViewModelInitializer,
    ): PaneEntry<ThreePane, Route> = routePaneEntry(
        routeParser = routeParser,
        viewModelInitializer = viewModelInitializer,
    )

    @Provides
    @IntoMap
    @StringKey(ProfileFollowingPattern)
    fun provideProfileFollowingPaneEntry(
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
            val hydratedRoute = routeParser.hydrate(route)
            val load = hydratedRoute.load
            val titleRes = load.titleRes()

            val viewModel = viewModel<ActualProfilesViewModel> {
                viewModelInitializer.invoke(
                    scope = viewModelCoroutineScope(),
                    route = routeParser.hydrate(route),
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()

            rememberPaneScaffoldState().PaneScaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .predictiveBackPlacement(paneScope = this),
                showNavigation = true,
                snackBarMessages = state.messages,
                onSnackBarMessageConsumed = {
                    viewModel.accept(Action.SnackbarDismissed(it))
                },
                topBar = {
                    PoppableDestinationTopAppBar(
                        onBackPressed = { viewModel.accept(Action.Navigate.Pop) },
                        title = {
                            Text(
                                text = stringResource(titleRes),
                            )
                        },
                    )
                },
                content = { paddingValues ->
                    ProfilesScreen(
                        paneScaffoldState = this,
                        modifier = Modifier
                            .padding(
                                paddingValues = PaddingValues(
                                    top = paddingValues.calculateTopPadding(),
                                ),
                            ),
                        state = state,
                        actions = viewModel.accept,
                    )
                },
            )
        },
    )
}

fun Load.titleRes(): StringResource = when (this) {
    is Load.Post.Likes -> Res.string.likes
    is Load.Post.Reposts -> Res.string.reposts
    is Load.Profile.Followers -> Res.string.followers
    is Load.Profile.Following -> Res.string.following
    Load.Moderation.Blocks -> Res.string.blocked_accounts
    Load.Moderation.Mutes -> Res.string.muted_accounts
}

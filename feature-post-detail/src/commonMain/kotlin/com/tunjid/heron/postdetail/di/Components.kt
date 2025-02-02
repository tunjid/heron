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

package com.tunjid.heron.postdetail.di

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material3.Badge
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.fromBase64EncodedUrl
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.di.DataComponent
import com.tunjid.heron.postdetail.Action
import com.tunjid.heron.postdetail.ActualPostDetailViewModel
import com.tunjid.heron.postdetail.PostDetailScreen
import com.tunjid.heron.postdetail.PostDetailViewModelCreator
import com.tunjid.heron.scaffold.di.ScaffoldComponent
import com.tunjid.heron.scaffold.navigation.AppStack
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.decodeReferringRoute
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.hydrate
import com.tunjid.heron.scaffold.navigation.routeAndMatcher
import com.tunjid.heron.scaffold.navigation.routeOf
import com.tunjid.heron.scaffold.scaffold.PaneBottomAppBar
import com.tunjid.heron.scaffold.scaffold.PaneFab
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.PoppableDestinationTopAppBar
import com.tunjid.heron.scaffold.scaffold.SecondaryPaneCloseBackHandler
import com.tunjid.heron.scaffold.scaffold.isFabExpanded
import com.tunjid.heron.scaffold.scaffold.predictiveBackBackgroundModifier
import com.tunjid.heron.scaffold.ui.bottomNavigationNestedScrollConnection
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneListDetailStrategy
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.RouteParser
import heron.feature_post_detail.generated.resources.Res
import heron.feature_post_detail.generated.resources.reply
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.KmpComponentCreate
import me.tatarka.inject.annotations.Provides
import org.jetbrains.compose.resources.stringResource

private const val RoutePattern = "/profile/{profileId}/post/{postId}"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
    children = listOfNotNull(
        routeParams.decodeReferringRoute()
    )
)

internal val Route.post
    get(): Post? = routeParams.queryParams["post"]?.firstOrNull()?.fromBase64EncodedUrl()

internal val Route.postUri
    get() = Uri(routeParams.queryParams.getValue("postUri").first())

internal val Route.sharedElementPrefix
    get() = routeParams.queryParams.getValue("sharedElementPrefix").first()

@KmpComponentCreate
expect fun PostDetailNavigationComponent.Companion.create(): PostDetailNavigationComponent

@KmpComponentCreate
expect fun PostDetailComponent.Companion.create(
    dataComponent: DataComponent,
    scaffoldComponent: ScaffoldComponent,
): PostDetailComponent

@Component
abstract class PostDetailNavigationComponent {
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
abstract class PostDetailComponent(
    @Component val dataComponent: DataComponent,
    @Component val scaffoldComponent: ScaffoldComponent,
) {
    companion object

    @IntoMap
    @Provides
    fun routeAdaptiveConfiguration(
        routeParser: RouteParser,
        creator: PostDetailViewModelCreator,
    ) = RoutePattern to threePaneListDetailStrategy<Route>(
        paneMapping = { route ->
            mapOf(
                ThreePane.Primary to route,
                ThreePane.Secondary to route.children.firstOrNull() as? Route
            )
        },
        render = { route ->
            val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
            val viewModel = viewModel<ActualPostDetailViewModel> {
                creator.invoke(
                    scope = lifecycleCoroutineScope,
                    route = routeParser.hydrate(route),
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()

            val bottomNavigationNestedScrollConnection =
                bottomNavigationNestedScrollConnection()

            PaneScaffold(
                modifier = Modifier
                    .predictiveBackBackgroundModifier(paneScope = this)
                    .nestedScroll(bottomNavigationNestedScrollConnection),
                showNavigation = true,
                snackBarMessages = state.messages,
                onSnackBarMessageConsumed = {
                },
                topBar = {
                    PoppableDestinationTopAppBar(
                        onBackPressed = { viewModel.accept(Action.Navigate.Pop) },
                    )
                },
                floatingActionButton = {
                    PaneFab(
                        modifier = Modifier
                            .offset {
                                if (isMediumScreenWidthOrWider) IntOffset.Zero
                                else bottomNavigationNestedScrollConnection.offset.round()
                            },
                        expanded = isFabExpanded(
                            offset = bottomNavigationNestedScrollConnection.offset
                        ),
                        text = stringResource(Res.string.reply),
                        icon = Icons.AutoMirrored.Rounded.Reply,
                        onClick = onClick@{
                            val anchorPost = state.anchorPost ?: return@onClick
                            viewModel.accept(
                                Action.Navigate.DelegateTo(
                                    NavigationAction.Common.ComposePost(
                                        type = Post.Create.Reply(
                                            parent = anchorPost,
                                        ),
                                        sharedElementPrefix = state.sharedElementPrefix,
                                    )
                                )
                            )
                        }
                    )
                },
                bottomBar = {
                    PaneBottomAppBar(
                        modifier = Modifier.offset {
                            bottomNavigationNestedScrollConnection.offset.round()
                        },
                        badge = { stack ->
                            if (stack == AppStack.Notifications && state.unreadNotificationCount != 0L) {
                                Badge(Modifier.size(4.dp))
                            }
                        },
                    )
                },
                content = { paddingValues ->
                    PostDetailScreen(
                        paneScaffoldState = this,
                        state = state,
                        actions = viewModel.accept,
                        modifier = Modifier
                            .padding(
                                paddingValues = PaddingValues(
                                    top = paddingValues.calculateTopPadding()
                                )
                            ),
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

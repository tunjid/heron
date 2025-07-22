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

package com.tunjid.heron.conversation.di

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.round
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.composables.accumulatedoffsetnestedscrollconnection.rememberAccumulatedOffsetNestedScrollConnection
import com.tunjid.heron.conversation.Action
import com.tunjid.heron.conversation.ActualConversationViewModel
import com.tunjid.heron.conversation.ConversationScreen
import com.tunjid.heron.conversation.RouteViewModelInitializer
import com.tunjid.heron.conversation.ui.ConversationTitle
import com.tunjid.heron.conversation.ui.conversationSharedElementKey
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.fromBase64EncodedUrl
import com.tunjid.heron.data.core.types.ConversationId
import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.scaffold.di.ScaffoldBindings
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.scaffold.PaneNavigationBar
import com.tunjid.heron.scaffold.scaffold.PaneNavigationRail
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.PoppableDestinationTopAppBar
import com.tunjid.heron.scaffold.scaffold.predictiveBackContentTransform
import com.tunjid.heron.scaffold.scaffold.predictiveBackPlacement
import com.tunjid.heron.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.viewModelCoroutineScope
import com.tunjid.heron.scaffold.ui.bottomNavigationNestedScrollConnection
import com.tunjid.heron.ui.UiTokens
import com.tunjid.treenav.compose.PaneEntry
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.mappedRoutePath
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.urlRouteMatcher
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.StringKey

private const val RoutePattern = "/messages/{conversationId}"

private fun createRoute(
    routeParams: RouteParams,
) = routeOf(
    params = routeParams,
)

internal val Route.conversationId by mappedRoutePath(
    mapper = ::ConversationId,
)

internal val Route.members: List<Profile>
    get() = routeParams.queryParams["model"]
        ?.map { it.fromBase64EncodedUrl() }
        ?: emptyList()

@BindingContainer
object ConversationNavigationBindings {

    @Provides
    @IntoMap
    @StringKey(RoutePattern)
    fun provideRouteMatcher(): RouteMatcher =
        urlRouteMatcher(
            routePattern = RoutePattern,
            routeMapper = ::createRoute
        )
}

@BindingContainer
class ConversationBindings(
    @Includes dataBindings: DataBindings,
    @Includes scaffoldBindings: ScaffoldBindings,
) {

    @Provides
    @IntoMap
    @StringKey(RoutePattern)
    fun providePaneEntry(
        viewModelInitializer: RouteViewModelInitializer,
    ): PaneEntry<ThreePane, Route> = routePaneEntry(
        viewModelInitializer = viewModelInitializer,
    )

    private fun routePaneEntry(
        viewModelInitializer: RouteViewModelInitializer,
    ) = threePaneEntry(
        contentTransform = predictiveBackContentTransform,
        render = { route ->
            val viewModel = viewModel<ActualConversationViewModel> {
                viewModelInitializer.invoke(
                    scope = viewModelCoroutineScope(),
                    route = route,
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()

            val statusBarHeight = UiTokens.statusBarHeight
            val topAppBarOffsetNestedScrollConnection =
                rememberAccumulatedOffsetNestedScrollConnection(
                    maxOffset = { Offset.Zero },
                    minOffset = {
                        Offset(
                            x = 0f,
                            y = -(statusBarHeight + UiTokens.toolbarHeight).toPx()
                        )
                    },
                )
            val bottomNavigationNestedScrollConnection =
                bottomNavigationNestedScrollConnection()

            rememberPaneScaffoldState().PaneScaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .predictiveBackPlacement(paneScope = this)
                    .nestedScroll(bottomNavigationNestedScrollConnection),
                showNavigation = true,
                snackBarMessages = state.messages,
                onSnackBarMessageConsumed = {
                },
                topBar = {
                    PoppableDestinationTopAppBar(
                        modifier = Modifier.offset {
                            topAppBarOffsetNestedScrollConnection.offset.round()
                        },
                        title = {
                            ConversationTitle(
                                conversationId = state.id,
                                signedInProfileId = state.signedInProfile?.did,
                                participants = state.members,
                                paneScaffoldState = this,
                                onProfileClicked = { profile ->
                                    viewModel.accept(
                                        Action.Navigate.DelegateTo(
                                            NavigationAction.Common.ToProfile(
                                                referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                profile = profile,
                                                avatarSharedElementKey = profile.conversationSharedElementKey(
                                                    conversationId = state.id
                                                ),
                                            )
                                        )
                                    )
                                }
                            )
                        },
                        onBackPressed = { viewModel.accept(Action.Navigate.Pop) },
                    )
                },
                navigationBar = {
                    PaneNavigationBar(
                        modifier = Modifier
                            .offset {
                                bottomNavigationNestedScrollConnection.offset.round()
                            },
                    )
                },
                navigationRail = {
                    PaneNavigationRail()
                },
                content = { paddingValues ->
                    ConversationScreen(
                        paneScaffoldState = this,
                        state = state,
                        actions = viewModel.accept,
                        modifier = Modifier
                            .padding(top = paddingValues.calculateTopPadding()),
                    )
                }
            )
        }
    )
}

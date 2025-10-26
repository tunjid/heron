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

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.heron.conversation.Action
import com.tunjid.heron.conversation.ActualConversationViewModel
import com.tunjid.heron.conversation.ConversationScreen
import com.tunjid.heron.conversation.RouteViewModelInitializer
import com.tunjid.heron.conversation.pendingRecord
import com.tunjid.heron.conversation.ui.ConversationTitle
import com.tunjid.heron.conversation.ui.UserInput
import com.tunjid.heron.conversation.ui.conversationSharedElementKey
import com.tunjid.heron.data.core.models.Message
import com.tunjid.heron.data.core.types.ConversationId
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.scaffold.di.ScaffoldBindings
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.decodeReferringRoute
import com.tunjid.heron.scaffold.navigation.profileDestination
import com.tunjid.heron.scaffold.scaffold.PaneNavigationRail
import com.tunjid.heron.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.scaffold.scaffold.PoppableDestinationTopAppBar
import com.tunjid.heron.scaffold.scaffold.bottomNavigationSharedBounds
import com.tunjid.heron.scaffold.scaffold.predictiveBackContentTransform
import com.tunjid.heron.scaffold.scaffold.predictiveBackPlacement
import com.tunjid.heron.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.viewModelCoroutineScope
import com.tunjid.heron.ui.bottomNavigationNestedScrollConnection
import com.tunjid.heron.ui.text.links
import com.tunjid.treenav.compose.PaneEntry
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.mappedRoutePath
import com.tunjid.treenav.strings.optionalMappedRouteQuery
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
    children = listOfNotNull(
        routeParams.decodeReferringRoute(),
    ),
)

internal val Route.conversationId by mappedRoutePath(
    mapper = ::ConversationId,
)

internal val Route.sharedUri by optionalMappedRouteQuery(
    mapper = ::GenericUri,
)

@BindingContainer
object ConversationNavigationBindings {

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
        paneMapping = { route ->
            mapOf(
                ThreePane.Primary to route,
                ThreePane.Secondary to route.children.firstOrNull() as? Route,
            )
        },
        render = { route ->
            val viewModel = viewModel<ActualConversationViewModel> {
                viewModelInitializer.invoke(
                    scope = viewModelCoroutineScope(),
                    route = route,
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()

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
                    viewModel.accept(Action.SnackbarDismissed(it))
                },
                topBar = {
                    PoppableDestinationTopAppBar(
                        title = {
                            ConversationTitle(
                                conversationId = state.id,
                                signedInProfileId = state.signedInProfile?.did,
                                participants = state.members,
                                paneScaffoldState = this,
                                onProfileClicked = { profile ->
                                    viewModel.accept(
                                        Action.Navigate.To(
                                            profileDestination(
                                                referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                profile = profile,
                                                avatarSharedElementKey = profile.conversationSharedElementKey(
                                                    conversationId = state.id,
                                                ),
                                            ),
                                        ),
                                    )
                                },
                            )
                        },
                        onBackPressed = { viewModel.accept(Action.Navigate.Pop) },
                    )
                },
                navigationBar = {
                    UserInput(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .imePadding()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .bottomNavigationSharedBounds(this),
                        pendingRecord = state.sharedRecord.pendingRecord,
                        sendMessage = remember(viewModel, state.id, state.sharedRecord) {
                            { annotatedString: AnnotatedString ->
                                viewModel.accept(
                                    Action.SendMessage(
                                        Message.Create(
                                            conversationId = state.id,
                                            text = annotatedString.text,
                                            links = annotatedString.links(),
                                            recordReference = state.sharedRecord
                                                .pendingRecord
                                                ?.reference,
                                        ),
                                    ),
                                )
                            }
                        },
                        removePendingRecordClicked = {
                            viewModel.accept(Action.SharedRecord.Remove)
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
                            .padding(paddingValues),
                    )
                },
            )
        },
    )
}

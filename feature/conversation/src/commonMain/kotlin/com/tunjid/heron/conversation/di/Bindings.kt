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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.tunjid.heron.conversation.Action
import com.tunjid.heron.conversation.ActualConversationViewModel
import com.tunjid.heron.conversation.ConversationScreen
import com.tunjid.heron.conversation.ConversationStateHolder
import com.tunjid.heron.conversation.ConversationViewModelInitializer
import com.tunjid.heron.conversation.pendingRecord
import com.tunjid.heron.conversation.ui.ConversationOverflowMenu
import com.tunjid.heron.conversation.ui.ConversationTitle
import com.tunjid.heron.conversation.ui.UserInput
import com.tunjid.heron.data.core.models.Message
import com.tunjid.heron.data.core.types.ConversationId
import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey
import com.tunjid.heron.ui.bottomNavigationNestedScrollConnection
import com.tunjid.heron.ui.modifiers.ifTrue
import com.tunjid.heron.ui.scaffold.di.ScaffoldBindings
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.decodeReferringRoute
import com.tunjid.heron.ui.scaffold.navigation.profileDestination
import com.tunjid.heron.ui.scaffold.scaffold.NavigationContentTransformer
import com.tunjid.heron.ui.scaffold.scaffold.PaneNavigationRail
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffold
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.PoppableDestinationTopAppBar
import com.tunjid.heron.ui.scaffold.scaffold.bottomNavigationSharedBounds
import com.tunjid.heron.ui.scaffold.scaffold.predictiveBackPlacement
import com.tunjid.heron.ui.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.retainRouteStateHolder
import com.tunjid.heron.ui.stateproduction.RouteStateHolderInitializer
import com.tunjid.heron.ui.text.links
import com.tunjid.mutator.compose.produceStateWithLifecycle
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
import dev.zacsweers.metro.ClassKey
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
    @ClassKey(ConversationStateHolder::class)
    fun provideRouteStateHolderInitializer(
        initializer: ConversationViewModelInitializer,
    ): RouteStateHolderInitializer = RouteStateHolderInitializer(initializer::invoke)

    @Provides
    @IntoMap
    @StringKey(RoutePattern)
    fun providePaneEntry(
        navigationContentTransformer: NavigationContentTransformer,
    ): PaneEntry<ThreePane, Route> = routePaneEntry(
        navigationContentTransformer = navigationContentTransformer,
    )

    private fun routePaneEntry(
        navigationContentTransformer: NavigationContentTransformer,
    ) = threePaneEntry(
        contentTransform = navigationContentTransformer::contentTransform,
        paneMapping = { route ->
            mapOf(
                ThreePane.Primary to route,
                ThreePane.Secondary to route.children.firstOrNull() as? Route,
            )
        },
        render = { route ->
            Route(
                route = route,
                paneScaffoldState = rememberPaneScaffoldState(),
            )
        },
    )
}

@Composable
internal fun Route(
    route: Route,
    paneScaffoldState: PaneScaffoldState,
) {
    val stateHolder = paneScaffoldState.retainRouteStateHolder<ConversationStateHolder>(
        route = route,
    )
    val state = stateHolder.produceStateWithLifecycle()

    val bottomNavigationNestedScrollConnection =
        bottomNavigationNestedScrollConnection(
            isCompact = paneScaffoldState.prefersCompactBottomNav,
        )

    paneScaffoldState.PaneScaffold(
        modifier = Modifier
            .fillMaxSize()
            .predictiveBackPlacement(paneScaffoldState = paneScaffoldState)
            .ifTrue(paneScaffoldState.prefersAutoHidingBottomNav) {
                nestedScroll(bottomNavigationNestedScrollConnection)
            },
        showNavigation = true,
        snackBarMessages = state.messages,
        onSnackBarMessageConsumed = {
            stateHolder.accept(Action.SnackbarDismissed(it))
        },
        topBar = {
            PoppableDestinationTopAppBar(
                title = {
                    ConversationTitle(
                        sharedElementPrefix = state.sharedElementPrefix,
                        signedInProfileId = state.signedInProfile?.did,
                        participants = state.conversation?.members.orEmpty(),
                        conversationName = state.conversation?.group?.name,
                        paneScaffoldState = this,
                        onProfileClicked = { profile ->
                            stateHolder.accept(
                                Action.Navigate.To(
                                    profileDestination(
                                        referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                        profile = profile,
                                        avatarSharedElementKey = profile.avatarSharedElementKey(
                                            prefix = state.sharedElementPrefix,
                                        ),
                                    ),
                                ),
                            )
                        },
                    )
                },
                actions = {
                    ConversationOverflowMenu(
                        conversation = state.conversation,
                        onAccept = {
                            stateHolder.accept(Action.AcceptConversation)
                        },
                        onLeave = {
                            stateHolder.accept(Action.LeaveConversation)
                        },
                        onToggleMute = { muted ->
                            stateHolder.accept(Action.ToggleMute(muted))
                        },
                    )
                },
                onBackPressed = { stateHolder.accept(Action.Navigate.Pop) },
            )
        },
        navigationBar = {
            UserInput(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .imePadding()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .bottomNavigationSharedBounds(this),
                inputText = state.inputText,
                pendingRecord = state.sharedRecord.pendingRecord,
                sendMessage = remember(
                    stateHolder,
                    state.id,
                    state.sharedRecord,
                ) {
                    { annotatedString: AnnotatedString ->
                        stateHolder.accept(
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
                    stateHolder.accept(Action.SharedRecord.Remove)
                },
                onTextChanged = {
                    stateHolder.accept(Action.TextChanged(it))
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
                actions = stateHolder.accept,
                modifier = Modifier
                    .padding(paddingValues),
            )
        },
    )
}

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

package com.tunjid.heron.messages

import androidx.compose.runtime.Stable
import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.models.contentDescription
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.MessageRepository
import com.tunjid.heron.data.repository.SearchQuery
import com.tunjid.heron.data.repository.SearchRepository
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.tiling.launchTilingMutations
import com.tunjid.heron.tiling.reset
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction
import com.tunjid.heron.ui.scaffold.navigation.NavigationMutation
import com.tunjid.heron.ui.scaffold.navigation.conversationDestination
import com.tunjid.heron.ui.stateproduction.RouteViewModel
import com.tunjid.heron.ui.text.Memo
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.mutator.coroutines.launchedCollect
import com.tunjid.mutator.coroutines.launchedCollectLatest
import com.tunjid.tiler.distinctBy
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import heron.feature.messages.generated.resources.Res
import heron.feature.messages.generated.resources.error_conversation_not_found
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withTimeout

internal typealias MessagesStateHolder = ActionSuspendingStateMutator<Action, State>

@AssistedFactory
fun interface MessagesViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualMessagesViewModel
}

@Stable
class ActualMessagesViewModel(
    mutator: MessagesStateHolder,
    scope: CoroutineScope,
    route: Route,
) : RouteViewModel(scope, route),
    MessagesStateHolder by mutator {

    @AssistedInject
    constructor(
        authRepository: AuthRepository,
        messagesRepository: MessageRepository,
        searchRepository: SearchRepository,
        navActions: (NavigationMutation) -> Unit,
        @Assisted scope: CoroutineScope,
        @Assisted route: Route,
    ) : this(
        mutator = scope.actionSuspendingStateMutator(
            state = State().toSnapshotMutable(),
            started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
            producer = { state, actions ->
                launchLoadProfileMutations(
                    state = state,
                    authRepository = authRepository,
                )
                actions.launchMutationsIn(
                    productionScope = this,
                    keySelector = Action::key,
                ) {
                    when (val action = type()) {
                        is Action.SnackbarDismissed -> action.flow.launchSnackbarDismissalMutations(state)
                        is Action.Navigate -> action.flow.collect {
                            navActions(it.navigationMutation)
                        }
                        is Action.SetIsSearching -> action.flow.launchSetIsSearchingMutations(state)
                        is Action.SearchQueryChanged -> action.flow.launchSearchQueryChangeMutations(
                            state = state,
                            searchRepository = searchRepository,
                        )
                        is Action.ResolveConversation -> action.flow.launchResolveConversationMutations(
                            state = state,
                            navActions = navActions,
                            messagesRepository = messagesRepository,
                        )
                        is Action.Tile ->
                            action.flow
                                .map { it.tilingAction }
                                .launchTilingMutations(
                                    state = state,
                                    updateQueryData = { copy(data = it) },
                                    refreshQuery = { copy(data = data.reset()) },
                                    cursorListLoader = messagesRepository::conversations,
                                    onNewItems = { items ->
                                        items.distinctBy(Conversation::id)
                                    },
                                )
                    }
                }
            },
        ),
        scope = scope,
        route = route,
    )
}

context(productionScope: CoroutineScope)
private fun launchLoadProfileMutations(
    state: State.SnapshotMutable,
    authRepository: AuthRepository,
) = authRepository.signedInUser.launchedCollect {
    state.signedInProfile = it
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SnackbarDismissed>.launchSnackbarDismissalMutations(
    state: State.SnapshotMutable,
) = launchedCollect { event ->
    state.messages -= event.message
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SetIsSearching>.launchSetIsSearchingMutations(
    state: State.SnapshotMutable,
) = launchedCollect { event ->
    state.isSearching = event.isSearching
}

context(productionScope: CoroutineScope)
private fun Flow<Action.ResolveConversation>.launchResolveConversationMutations(
    state: State.SnapshotMutable,
    navActions: (NavigationMutation) -> Unit,
    messagesRepository: MessageRepository,
) = launchedCollectLatest { action ->
    try {
        withTimeout(2.seconds) {
            messagesRepository.resolveConversation(
                with = action.with.did,
            )
        }
    } catch (e: TimeoutCancellationException) {
        Result.failure(e)
    }
        .onSuccess { conversationId ->
            navActions(
                conversationDestination(
                    id = conversationId,
                    members = listOf(action.with),
                    sharedElementPrefix = ConversationSearchResult,
                    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                ).navigationMutation,
            )
        }
        .onFailure {
            state.messages += Memo.Resource(
                Res.string.error_conversation_not_found,
                listOf(action.with.contentDescription),
            )
        }
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SearchQueryChanged>.launchSearchQueryChangeMutations(
    state: State.SnapshotMutable,
    searchRepository: SearchRepository,
) {
    val sharedActions = shareIn(
        scope = productionScope,
        started = SharingStarted.WhileSubscribed(),
        replay = 1,
    )
    sharedActions.launchedCollectLatest {
        state.searchQuery = it.query
    }
    sharedActions
        .debounce {
            if (it.query.isBlank()) 0.milliseconds
            else 300.milliseconds
        }
        .flatMapLatest { action ->
            if (action.query.isBlank()) flowOf(emptyList())
            else searchRepository.autoCompleteProfileSearch(
                query = SearchQuery.OfProfiles(
                    query = action.query,
                    isLocalOnly = false,
                    data = chatSearchData(),
                ),
                cursor = Cursor.Initial,
            )
        }.launchedCollect {
            state.autoCompletedProfiles = it.sortedByDescending(
                ProfileWithViewerState::canBeMessaged,
            )
        }
}

fun ProfileWithViewerState.canBeMessaged() =
    when (profile.metadata.chat.allowed) {
        Profile.ChatInfo.Allowed.Everyone -> true
        Profile.ChatInfo.Allowed.Following -> viewerState?.followedBy != null
        Profile.ChatInfo.Allowed.NoOne -> false
    }

private fun chatSearchData() = CursorQuery.Data(
    page = 0,
    cursorAnchor = Clock.System.now(),
    limit = 30,
)

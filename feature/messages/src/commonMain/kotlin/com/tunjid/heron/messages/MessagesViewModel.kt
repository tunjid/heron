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

import androidx.lifecycle.ViewModel
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
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.heron.scaffold.navigation.conversationDestination
import com.tunjid.heron.tiling.reset
import com.tunjid.heron.tiling.tilingMutations
import com.tunjid.heron.ui.text.Memo
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapLatestToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

internal typealias MessagesStateHolder = ActionStateMutator<Action, StateFlow<State>>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualMessagesViewModel
}

@AssistedInject
class ActualMessagesViewModel(
    authRepository: AuthRepository,
    messagesRepository: MessageRepository,
    searchRepository: SearchRepository,
    navActions: (NavigationMutation) -> Unit,
    @Assisted
    scope: CoroutineScope,
    @Suppress("UNUSED_PARAMETER")
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope),
    MessagesStateHolder by scope.actionStateFlowMutator(
        initialState = State(),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        inputs = listOf(
            loadProfileMutations(
                authRepository,
            ),
        ),
        actionTransform = transform@{ actions ->
            actions.toMutationStream(
                keySelector = Action::key,
            ) {
                when (val action = type()) {
                    is Action.SnackbarDismissed -> action.flow.snackbarDismissalMutations()
                    is Action.Navigate -> action.flow.consumeNavigationActions(
                        navigationMutationConsumer = navActions,
                    )
                    is Action.SetIsSearching -> action.flow.setIsSearchingMutations()
                    is Action.SearchQueryChanged -> action.flow.searchQueryChangeMutations(
                        searchRepository = searchRepository,
                    )
                    is Action.ResolveConversation -> action.flow.resolveConversationMutations(
                        navActions = navActions,
                        messagesRepository = messagesRepository,
                    )

                    is Action.Tile ->
                        action.flow
                            .map { it.tilingAction }
                            .tilingMutations(
                                currentState = { state() },
                                updateQueryData = { copy(data = it) },
                                refreshQuery = { copy(data = data.reset()) },
                                cursorListLoader = messagesRepository::conversations,
                                onNewItems = { items ->
                                    items.distinctBy(Conversation::id)
                                },
                                onTilingDataUpdated = { copy(tilingData = it) },
                            )
                }
            }
        },
    )

private fun loadProfileMutations(
    authRepository: AuthRepository,
): Flow<Mutation<State>> =
    authRepository.signedInUser.mapToMutation {
        copy(signedInProfile = it)
    }

private fun Flow<Action.SnackbarDismissed>.snackbarDismissalMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(messages = messages - action.message)
    }

private fun Flow<Action.ResolveConversation>.resolveConversationMutations(
    navActions: (NavigationMutation) -> Unit,
    messagesRepository: MessageRepository,
): Flow<Mutation<State>> =
    mapLatestToManyMutations { action ->
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
                emit {
                    copy(
                        messages = messages + Memo.Resource(
                            Res.string.error_conversation_not_found,
                            listOf(action.with.contentDescription),
                        ),
                    )
                }
            }
    }

private fun Flow<Action.SetIsSearching>.setIsSearchingMutations(): Flow<Mutation<State>> =
    mapToMutation { copy(isSearching = it.isSearching) }

private fun Flow<Action.SearchQueryChanged>.searchQueryChangeMutations(
    searchRepository: SearchRepository,
): Flow<Mutation<State>> = channelFlow {
    val sharedActions = shareIn(
        scope = this,
        started = SharingStarted.WhileSubscribed(),
        replay = 1,
    )
    launch {
        sharedActions.collectLatest {
            send { copy(searchQuery = it.query) }
        }
    }
    launch {
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
            }.collect {
                send {
                    copy(
                        autoCompletedProfiles = it.sortedByDescending(
                            ProfileWithViewerState::canBeMessaged,
                        ),
                    )
                }
            }
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

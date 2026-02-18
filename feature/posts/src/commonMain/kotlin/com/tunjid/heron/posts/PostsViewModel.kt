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

package com.tunjid.heron.posts

import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.data.repository.MessageRepository
import com.tunjid.heron.data.repository.PostDataQuery
import com.tunjid.heron.data.repository.PostRepository
import com.tunjid.heron.data.repository.RecordRepository
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.data.repository.recentConversations
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.posts.di.PostsRequest
import com.tunjid.heron.posts.di.postsRequest
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.reset
import com.tunjid.heron.tiling.tilingMutations
import com.tunjid.heron.timeline.utilities.writeStatusMessage
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapLatestToManyMutations
import com.tunjid.mutator.coroutines.mapToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.distinctBy
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

internal typealias PostsStateHolder = ActionStateMutator<Action, StateFlow<State>>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(scope: CoroutineScope, route: Route): ActualPostsViewModel
}

@AssistedInject
class ActualPostsViewModel(
    navActions: (NavigationMutation) -> Unit,
    postsRepository: PostRepository,
    messageRepository: MessageRepository,
    recordRepository: RecordRepository,
    userDataRepository: UserDataRepository,
    writeQueue: WriteQueue,
    @Assisted scope: CoroutineScope,
    @Assisted route: Route,
) :
    ViewModel(viewModelScope = scope),
    PostsStateHolder by scope.actionStateFlowMutator(
        initialState =
            State(
                tilingData =
                    TilingState.Data(
                        currentQuery =
                            when (val request = route.postsRequest) {
                                is PostsRequest.Quotes ->
                                    PostDataQuery(
                                        profileId = request.profileHandleOrId,
                                        postRecordKey = request.postRecordKey,
                                        data =
                                            CursorQuery.Data(
                                                page = 0,
                                                cursorAnchor = Clock.System.now(),
                                            ),
                                    )
                                PostsRequest.Saved ->
                                    PostDataQuery(
                                        profileId = ProfileHandle(""),
                                        postRecordKey = RecordKey(""),
                                        data =
                                            CursorQuery.Data(
                                                page = 0,
                                                cursorAnchor = Clock.System.now(),
                                            ),
                                    )
                            }
                    )
            ),
        inputs =
            listOf(
                recentConversationMutations(messageRepository = messageRepository),
                loadPreferencesMutations(userDataRepository = userDataRepository),
            ),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        actionTransform = transform@{ actions ->
                actions.toMutationStream(keySelector = Action::key) {
                    when (val action = type()) {
                        is Action.SnackbarDismissed -> action.flow.snackbarDismissalMutations()
                        is Action.Navigate ->
                            action.flow.consumeNavigationActions(
                                navigationMutationConsumer = navActions
                            )
                        is Action.Tile ->
                            action.flow.postsLoadMutations(
                                request = route.postsRequest,
                                stateHolder = this@transform,
                                postsRepository = postsRepository,
                            )
                        is Action.SendPostInteraction ->
                            action.flow.postInteractionMutations(writeQueue = writeQueue)
                        is Action.UpdateMutedWord ->
                            action.flow.updateMutedWordMutations(writeQueue = writeQueue)
                        is Action.BlockAccount ->
                            action.flow.blockAccountMutations(writeQueue = writeQueue)
                        is Action.MuteAccount ->
                            action.flow.muteAccountMutations(writeQueue = writeQueue)
                        is Action.UpdateRecentLists ->
                            action.flow.recentListsMutations(recordRepository = recordRepository)
                    }
                }
            },
    )

private suspend fun Flow<Action.Tile>.postsLoadMutations(
    request: PostsRequest,
    stateHolder: SuspendingStateHolder<State>,
    postsRepository: PostRepository,
): Flow<Mutation<State>> =
    map { it.tilingAction }
        .tilingMutations(
            currentState = { stateHolder.state() },
            updateQueryData = PostDataQuery::updateData,
            refreshQuery = PostDataQuery::refresh,
            cursorListLoader = { query, cursor ->
                when (request) {
                    is PostsRequest.Quotes -> {
                        postsRepository.quotes(query, cursor)
                    }
                    PostsRequest.Saved -> {
                        postsRepository.saved(query, cursor)
                    }
                }
            },
            onNewItems = { items -> items.distinctBy(TimelineItem::id) },
            onTilingDataUpdated = { copy(tilingData = it) },
        )

private fun recentConversationMutations(
    messageRepository: MessageRepository
): Flow<Mutation<State>> =
    messageRepository.recentConversations().mapToMutation { conversations ->
        copy(recentConversations = conversations)
    }

fun Flow<Action.UpdateRecentLists>.recentListsMutations(
    recordRepository: RecordRepository
): Flow<Mutation<State>> = flatMapLatest {
    recordRepository.recentLists.mapToMutation { lists -> copy(recentLists = lists) }
}

private fun loadPreferencesMutations(
    userDataRepository: UserDataRepository
): Flow<Mutation<State>> = userDataRepository.preferences.mapToMutation { copy(preferences = it) }

private fun Flow<Action.SendPostInteraction>.postInteractionMutations(
    writeQueue: WriteQueue
): Flow<Mutation<State>> = mapToManyMutations { action ->
    val writable = Writable.Interaction(action.interaction)
    val status = writeQueue.enqueue(writable)
    writable.writeStatusMessage(status)?.let { emit { copy(messages = messages + it) } }
}

private fun Flow<Action.UpdateMutedWord>.updateMutedWordMutations(
    writeQueue: WriteQueue
): Flow<Mutation<State>> = mapToManyMutations {
    val writable =
        Writable.TimelineUpdate(
            Timeline.Update.OfMutedWord.ReplaceAll(mutedWordPreferences = it.mutedWordPreference)
        )
    val status = writeQueue.enqueue(writable)
    writable.writeStatusMessage(status)?.let { emit { copy(messages = messages + it) } }
}

private fun Flow<Action.BlockAccount>.blockAccountMutations(
    writeQueue: WriteQueue
): Flow<Mutation<State>> = mapLatestToManyMutations { action ->
    val writable =
        Writable.Restriction(
            Profile.Restriction.Block.Add(
                signedInProfileId = action.signedInProfileId,
                profileId = action.profileId,
            )
        )
    val status = writeQueue.enqueue(writable)
    writable.writeStatusMessage(status)?.let { emit { copy(messages = messages + it) } }
}

private fun Flow<Action.MuteAccount>.muteAccountMutations(
    writeQueue: WriteQueue
): Flow<Mutation<State>> = mapLatestToManyMutations { action ->
    val writable =
        Writable.Restriction(
            Profile.Restriction.Mute.Add(
                signedInProfileId = action.signedInProfileId,
                profileId = action.profileId,
            )
        )
    val status = writeQueue.enqueue(writable)
    writable.writeStatusMessage(status)?.let { emit { copy(messages = messages + it) } }
}

private fun Flow<Action.SnackbarDismissed>.snackbarDismissalMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(messages = messages - action.message)
    }

private fun PostDataQuery.updateData(newData: CursorQuery.Data): PostDataQuery =
    copy(data = newData)

private fun PostDataQuery.refresh(): PostDataQuery = copy(data = data.reset())

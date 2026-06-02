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

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
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
import com.tunjid.heron.tiling.launchTilingMutations
import com.tunjid.heron.tiling.reset
import com.tunjid.heron.timeline.utilities.launchAndCollectEnqueueMutations
import com.tunjid.heron.ui.coroutines.launchAndCollect
import com.tunjid.heron.ui.coroutines.launchAndCollectLatest
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.tiler.distinctBy
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map

internal typealias PostsStateHolder = ActionSuspendingStateMutator<Action, State>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualPostsViewModel
}

@Stable
@AssistedInject
class ActualPostsViewModel(
    navActions: (NavigationMutation) -> Unit,
    postsRepository: PostRepository,
    messageRepository: MessageRepository,
    recordRepository: RecordRepository,
    userDataRepository: UserDataRepository,
    writeQueue: WriteQueue,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope),
    PostsStateHolder by scope.actionSuspendingStateMutator(
        state = State(route.postsRequest).toSnapshotMutable(),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        producer = { state, actions ->
            launchLoadPreferencesMutations(
                state = state,
                userDataRepository = userDataRepository,
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
                    is Action.Tile -> action.flow.launchPostsLoadMutations(
                        state = state,
                        request = route.postsRequest,
                        postsRepository = postsRepository,
                    )
                    is Action.SendPostInteraction -> action.flow.launchPostInteractionMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is Action.BlockAccount -> action.flow.launchBlockAccountMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is Action.MuteAccount -> action.flow.launchMuteAccountMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is Action.UpdateRecentConversations -> action.flow.launchRecentConversationMutations(
                        state = state,
                        messageRepository = messageRepository,
                    )
                    is Action.UpdateRecentLists -> action.flow.launchRecentListsMutations(
                        state = state,
                        recordRepository = recordRepository,
                    )
                    is Action.DeleteRecord -> action.flow.launchDeleteRecordMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                }
            }
        },
    )

context(productionScope: CoroutineScope)
private fun Flow<Action.Tile>.launchPostsLoadMutations(
    state: State.SnapshotMutable,
    request: PostsRequest,
    postsRepository: PostRepository,
) = map { it.tilingAction }
    .launchTilingMutations(
        state = state,
        updateQueryData = PostDataQuery::updateData,
        refreshQuery = PostDataQuery::refresh,
        cursorListLoader = { query, cursor ->
            when (request) {
                is PostsRequest.Quotes -> postsRepository.quotes(query, cursor)
                PostsRequest.Saved -> postsRepository.saved(query, cursor)
            }
        },
        onNewItems = { items -> items.distinctBy(TimelineItem::id) },
    )

context(productionScope: CoroutineScope)
private fun Flow<Action.UpdateRecentConversations>.launchRecentConversationMutations(
    state: State.SnapshotMutable,
    messageRepository: MessageRepository,
) = launchAndCollectLatest {
    messageRepository.recentConversations().collect { conversations ->
        state.recentConversations = conversations
    }
}

context(productionScope: CoroutineScope)
private fun Flow<Action.UpdateRecentLists>.launchRecentListsMutations(
    state: State.SnapshotMutable,
    recordRepository: RecordRepository,
) = launchAndCollectLatest {
    recordRepository.recentLists.collect { lists ->
        state.recentLists = lists
    }
}

context(productionScope: CoroutineScope)
private fun launchLoadPreferencesMutations(
    state: State.SnapshotMutable,
    userDataRepository: UserDataRepository,
) = userDataRepository.preferences.launchAndCollect {
    state.preferences = it
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SendPostInteraction>.launchPostInteractionMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { Writable.Interaction(it.interaction) },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.BlockAccount>.launchBlockAccountMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { action ->
        Writable.Restriction(
            Profile.Restriction.Block.Add(
                signedInProfileId = action.signedInProfileId,
                profileId = action.profileId,
            ),
        )
    },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.MuteAccount>.launchMuteAccountMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { action ->
        Writable.Restriction(
            Profile.Restriction.Mute.Add(
                signedInProfileId = action.signedInProfileId,
                profileId = action.profileId,
            ),
        )
    },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.DeleteRecord>.launchDeleteRecordMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { Writable.RecordDeletion(recordUri = it.recordUri) },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.SnackbarDismissed>.launchSnackbarDismissalMutations(
    state: State.SnapshotMutable,
) = launchAndCollect { event ->
    state.messages -= event.message
}

private fun PostDataQuery.updateData(newData: CursorQuery.Data): PostDataQuery =
    copy(data = newData)

private fun PostDataQuery.refresh(): PostDataQuery =
    copy(data = data.reset())

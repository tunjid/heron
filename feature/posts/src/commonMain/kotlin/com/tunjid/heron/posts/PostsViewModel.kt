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
import com.tunjid.heron.data.core.models.DataQuery
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.repository.PostQuotesQuery
import com.tunjid.heron.data.repository.PostRepository
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.data.utilities.writequeue.toSubscriptionWritable
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.posts.di.PostsRequest
import com.tunjid.heron.posts.di.postsRequest
import com.tunjid.heron.tiling.launchTilingMutations
import com.tunjid.heron.tiling.reset
import com.tunjid.heron.timeline.utilities.launchAndCollectEnqueueMutations
import com.tunjid.heron.ui.scaffold.navigation.NavigationMutation
import com.tunjid.heron.ui.stateproduction.RouteStateHolder
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.isNoOp
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.mutator.coroutines.launchedCollect
import com.tunjid.tiler.distinctBy
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted

@Stable
internal interface PostsStateHolder :
    RouteStateHolder,
    ActionSuspendingStateMutator<Action, State>

@AssistedFactory
fun interface PostsViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualPostsViewModel
}

@Stable
class ActualPostsViewModel(
    mutator: ActionSuspendingStateMutator<Action, State>,
    scope: CoroutineScope,
) : ViewModel(viewModelScope = scope),
    PostsStateHolder,
    ActionSuspendingStateMutator<Action, State> by mutator {

    @AssistedInject
    constructor(
        navActions: (NavigationMutation) -> Unit,
        postsRepository: PostRepository,
        userDataRepository: UserDataRepository,
        writeQueue: WriteQueue,
        @Assisted scope: CoroutineScope,
        @Assisted route: Route,
    ) : this(
        mutator = scope.actionSuspendingStateMutator(
            state = State(route.postsRequest).toSnapshotMutable(),
            started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
            producer = { state, actions ->
                launchStateHolderCreationMutations(
                    state = state,
                    viewModelScope = scope,
                    postsRepository = postsRepository,
                )
                launchLoadPreferencesMutations(
                    state = state,
                    userDataRepository = userDataRepository,
                )
                actions.launchMutationsIn(
                    productionScope = this,
                    keySelector = Action::key,
                ) {
                    when (val action = type()) {
                        is Action.SnackbarDismissed -> action.flow.launchSnackbarDismissalMutations(
                            state,
                        )
                        is Action.Navigate -> action.flow.collect {
                            navActions(it.navigationMutation)
                        }
                        is Action.TogglePublicationSubscription -> action.flow.launchTogglePublicationSubscriptionMutations(
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
                        is Action.DeleteRecord -> action.flow.launchDeleteRecordMutations(
                            state = state,
                            writeQueue = writeQueue,
                        )
                    }
                }
            },
        ),
        scope = scope,
    )
}

private fun launchStateHolderCreationMutations(
    state: State.Mutable,
    viewModelScope: CoroutineScope,
    postsRepository: PostRepository,
) {
    state.stateHolders = state.stateHolders.map { holder ->
        val isNoOp = when (holder) {
            is PostsScreenStateHolders.Bookmarks -> holder.mutator.isNoOp()
            is PostsScreenStateHolders.QuotedPosts -> holder.mutator.isNoOp()
        }
        if (isNoOp) viewModelScope.postsScreenStateHolder(
            seed = holder,
            postsRepository = postsRepository,
        ) else holder
    }
}

context(productionScope: CoroutineScope)
private fun launchLoadPreferencesMutations(
    state: State.Mutable,
    userDataRepository: UserDataRepository,
) = userDataRepository.preferences.launchedCollect {
    state.preferences = it
}

context(productionScope: CoroutineScope)
private fun Flow<Action.TogglePublicationSubscription>.launchTogglePublicationSubscriptionMutations(
    state: State.Mutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { it.publication.toSubscriptionWritable() },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.BlockAccount>.launchBlockAccountMutations(
    state: State.Mutable,
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
    state: State.Mutable,
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
    state: State.Mutable,
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
    state: State.Mutable,
) = launchedCollect { event ->
    state.messages -= event.message
}

private fun CoroutineScope.postsScreenStateHolder(
    seed: PostsScreenStateHolders,
    postsRepository: PostRepository,
): PostsScreenStateHolders {
    val mutator = actionSuspendingStateMutator(
        state = seed.state,
        producer = { state, actions ->
            actions
                .launchTilingMutations(
                    state = state,
                    updateQueryData = CursorQuery::updateData,
                    refreshQuery = CursorQuery::refresh,
                    cursorListLoader = { query, cursor ->
                        when (seed.state.request) {
                            is PostsRequest.Quotes -> postsRepository.quotes(
                                query = query as PostQuotesQuery,
                                cursor = cursor,
                            )
                            PostsRequest.Saved -> postsRepository.saved(
                                query = query,
                                cursor = cursor,
                            )
                        }
                    },
                    onNewItems = { items -> items.distinctBy(TimelineItem::id) },
                )
        },
    )
    return when (seed) {
        is PostsScreenStateHolders.QuotedPosts -> PostsScreenStateHolders.QuotedPosts(
            sort = seed.sort,
            mutator = mutator,
        )
        is PostsScreenStateHolders.Bookmarks -> PostsScreenStateHolders.Bookmarks(
            mutator = mutator,
        )
    }
}

private fun CursorQuery.updateData(newData: CursorQuery.Data): CursorQuery =
    when (this) {
        is DataQuery -> copy(data = newData)
        is PostQuotesQuery -> copy(data = newData)
        else -> throw IllegalArgumentException("Unsupported query type")
    }

private fun CursorQuery.refresh(): CursorQuery =
    when (this) {
        is DataQuery -> copy(data = data.reset())
        is PostQuotesQuery -> copy(data = data.reset())
        else -> throw IllegalArgumentException("Unsupported query type")
    }

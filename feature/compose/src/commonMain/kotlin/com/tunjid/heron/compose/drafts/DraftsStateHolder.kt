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

package com.tunjid.heron.compose.drafts

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.repository.PostRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.tiling.launchTilingMutations
import com.tunjid.heron.ui.stateproduction.SheetStateHolder
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.mutator.coroutines.launchedCollect
import com.tunjid.tiler.distinctBy
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map

@Stable
interface DraftsStateHolder :
    SheetStateHolder,
    ActionSuspendingStateMutator<DraftsAction, DraftsState>

@AssistedFactory
fun interface DraftsViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
    ): DraftsViewModel
}

class DraftsViewModel(
    mutator: ActionSuspendingStateMutator<DraftsAction, DraftsState>,
    scope: CoroutineScope,
) : ViewModel(viewModelScope = scope),
    DraftsStateHolder,
    ActionSuspendingStateMutator<DraftsAction, DraftsState> by mutator {

    @AssistedInject
    constructor(
        postRepository: PostRepository,
        writeQueue: WriteQueue,
        @Assisted scope: CoroutineScope,
    ) : this(
        mutator = scope.actionSuspendingStateMutator(
            state = DraftsState.Immutable().toSnapshotMutable(),
            started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
            producer = { state, actions ->
                actions.launchMutationsIn(
                    productionScope = this,
                    keySelector = DraftsAction::key,
                ) {
                    when (val action = type()) {
                        is DraftsAction.Tile -> action.flow.launchDraftsLoadMutations(
                            state = state,
                            postRepository = postRepository,
                        )
                        is DraftsAction.Delete -> action.flow.launchDeleteDraftMutations(
                            writeQueue = writeQueue,
                        )
                    }
                }
            },
        ),
        scope = scope,
    )
}

context(productionScope: CoroutineScope)
private fun Flow<DraftsAction.Tile>.launchDraftsLoadMutations(
    state: DraftsState.SnapshotMutable,
    postRepository: PostRepository,
) = map { it.tilingAction }
    .launchTilingMutations(
        state = state,
        updateQueryData = DraftQuery::updateData,
        refreshQuery = DraftQuery::refresh,
        cursorListLoader = { query, cursor ->
            postRepository.drafts(query, cursor)
        },
        onNewItems = { items -> items.distinctBy(Post.Draft::id) },
    )

context(productionScope: CoroutineScope)
private fun Flow<DraftsAction.Delete>.launchDeleteDraftMutations(
    writeQueue: WriteQueue,
) = launchedCollect { action ->
    writeQueue.enqueue(
        Writable.PostDraft.Delete(draftId = action.draftId),
    )
}

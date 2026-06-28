package com.tunjid.heron.sheets.threadgate

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.ThreadGate
import com.tunjid.heron.data.repository.RecordRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.sheets.utilities.SheetWhileSubscribed
import com.tunjid.heron.timeline.utilities.launchAndCollectEnqueueMutations
import com.tunjid.heron.ui.stateproduction.SheetStateHolder
import com.tunjid.heron.ui.text.Memo
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.mutator.coroutines.launchedCollect
import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted

interface ThreadGateStateHolder :
    SheetStateHolder,
    ActionSuspendingStateMutator<ThreadGateAction, ThreadGateState>

@AssistedFactory
fun interface ThreadGateViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
    ): ThreadGateViewModel
}

class ThreadGateViewModel(
    mutator: ActionSuspendingStateMutator<ThreadGateAction, ThreadGateState>,
    scope: CoroutineScope,
) : ViewModel(viewModelScope = scope),
    ThreadGateStateHolder,
    ActionSuspendingStateMutator<ThreadGateAction, ThreadGateState> by mutator {

    @AssistedInject
    constructor(
        recordRepository: RecordRepository,
        writeQueue: WriteQueue,
        @Assisted scope: CoroutineScope,
    ) : this(
        mutator = scope.actionSuspendingStateMutator(
            state = ThreadGateState.Immutable().toSnapshotMutable(),
            started = SharingStarted.WhileSubscribed(SheetWhileSubscribed),
            producer = { state, actions ->
                launchLoadRecentListsMutations(
                    state = state,
                    recordRepository = recordRepository,
                )
                actions.launchMutationsIn(
                    productionScope = this,
                    keySelector = ThreadGateAction::key,
                ) {
                    when (val action = type()) {
                        is ThreadGateAction.Initialize -> action.flow.launchInitializeMutations(
                            state = state,
                        )
                        is ThreadGateAction.UpdateAllowed -> action.flow.launchUpdateAllowedMutations(
                            state = state,
                        )
                        is ThreadGateAction.Reset -> action.flow.launchResetMutations(
                            state = state,
                        )
                        is ThreadGateAction.SendInteraction -> action.flow.launchSendInteractionMutations(
                            state = state,
                            writeQueue = writeQueue,
                        )
                        is ThreadGateAction.SnackbarDismissed -> action.flow.launchSnackbarDismissalMutations(
                            state = state,
                        )
                    }
                }
            },
        ),
        scope = scope,
    )
}

context(productionScope: CoroutineScope)
private fun launchLoadRecentListsMutations(
    state: ThreadGateState.SnapshotMutable,
    recordRepository: RecordRepository,
) = recordRepository.recentLists
    .launchedCollect { state.recentLists = it }

context(productionScope: CoroutineScope)
private fun Flow<ThreadGateAction.Initialize>.launchInitializeMutations(
    state: ThreadGateState.SnapshotMutable,
) = launchedCollect {
    state.mode = it.mode
    state.allowed = it.allowed
}

context(productionScope: CoroutineScope)
private fun Flow<ThreadGateAction.UpdateAllowed>.launchUpdateAllowedMutations(
    state: ThreadGateState.SnapshotMutable,
) = launchedCollect { action ->
    state.allowed = action.allowed
}

context(productionScope: CoroutineScope)
private fun Flow<ThreadGateAction.Reset>.launchResetMutations(
    state: ThreadGateState.SnapshotMutable,
) = launchedCollect {
    state.mode = null
    state.allowed = null
    state.dismiss = false
}

context(productionScope: CoroutineScope)
private fun Flow<ThreadGateAction.SendInteraction>.launchSendInteractionMutations(
    state: ThreadGateState.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { Writable.Interaction(it.interaction) },
) { _, memo ->
    // Only request dismissal once the write is enqueued; until then the sheet stays shown so state
    // production (WhileSubscribed) keeps the enqueue from being cancelled.
    if (memo != null) state.messages += memo
    state.dismiss = true
}

context(productionScope: CoroutineScope)
private fun Flow<ThreadGateAction.SnackbarDismissed>.launchSnackbarDismissalMutations(
    state: ThreadGateState.SnapshotMutable,
) = launchedCollect {
    state.messages -= it.message
}

@Stable
@Snapshottable
interface ThreadGateState {
    @SnapshotSpec
    data class Immutable(
        val recentLists: List<FeedList> = emptyList(),
        val mode: Mode? = null,
        val allowed: ThreadGate.Allowed? = null,
        val messages: List<Memo> = emptyList(),
        val dismiss: Boolean = false,
    ) : ThreadGateState
}

sealed class ThreadGateAction(val key: String) {
    data class Initialize(
        val mode: Mode,
        val allowed: ThreadGate.Allowed?,
    ) : ThreadGateAction("Initialize")

    data class UpdateAllowed(
        val allowed: ThreadGate.Allowed?,
    ) : ThreadGateAction("UpdateAllowed")

    data object Reset : ThreadGateAction("Reset")

    data class SendInteraction(
        val interaction: Post.Interaction,
    ) : ThreadGateAction("SendInteraction")

    data class SnackbarDismissed(
        val message: Memo,
    ) : ThreadGateAction("SnackbarDismissed")
}

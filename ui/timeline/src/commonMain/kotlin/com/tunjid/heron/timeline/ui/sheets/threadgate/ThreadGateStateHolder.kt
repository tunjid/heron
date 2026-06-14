package com.tunjid.heron.timeline.ui.sheets.threadgate

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.ThreadGate
import com.tunjid.heron.data.repository.RecordRepository
import com.tunjid.heron.timeline.utilities.SheetWhileSubscribed
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

typealias ThreadGateStateHolder = ActionSuspendingStateMutator<ThreadGateAction, ThreadGateState>

@AssistedFactory
fun interface ThreadGateViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
    ): ThreadGateViewModel
}

@AssistedInject
class ThreadGateViewModel(
    recordRepository: RecordRepository,
    @Assisted scope: CoroutineScope,
) : ViewModel(viewModelScope = scope),
    ThreadGateStateHolder by scope.actionSuspendingStateMutator(
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
                }
            }
        },
    )

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
}

@Stable
@Snapshottable
interface ThreadGateState {
    @SnapshotSpec
    data class Immutable(
        val recentLists: List<FeedList> = emptyList(),
        val mode: Mode? = null,
        val allowed: ThreadGate.Allowed? = null,
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
}

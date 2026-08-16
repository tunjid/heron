package com.tunjid.heron.ui.scaffold.ui

import androidx.compose.runtime.Stable
import com.tunjid.heron.data.di.AppMainScope
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.mutator.coroutines.launchedCollectLatest
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted

@Stable
interface UiStateHolder : ActionSuspendingStateMutator<UiAction, UiState>

@Inject
class AppUiStateHolder(
    @AppMainScope
    appMainScope: CoroutineScope,
) : UiStateHolder,
    ActionSuspendingStateMutator<UiAction, UiState> by appMainScope.actionSuspendingStateMutator<UiAction, UiState.SnapshotMutable>(
        state = UiState.Immutable().toSnapshotMutable(),
        started = SharingStarted.Eagerly,
        producer = { state, actions ->
            actions.launchMutationsIn(
                productionScope = appMainScope,
                keySelector = UiAction::key,
            ) {
                when (val action = type()) {
                    is UiAction.UpdateDismissBehavior -> action.flow.launchDismissBehaviorMutations(
                        state = state,
                    )
                    is UiAction.UpdatePaneAnchor -> action.flow.launchPaneAnchorMutations(
                        state = state,
                    )
                    is UiAction.UpdateShowNavigation -> action.flow.launchShowNavigationMutations(
                        state = state,
                    )
                }
            }
        },
    )

context(productionScope: CoroutineScope)
private fun Flow<UiAction.UpdateShowNavigation>.launchShowNavigationMutations(
    state: UiState.SnapshotMutable,
) = launchedCollectLatest {
    state.showNavigation = it.showNavigation
}

context(productionScope: CoroutineScope)
private fun Flow<UiAction.UpdateDismissBehavior>.launchDismissBehaviorMutations(
    state: UiState.SnapshotMutable,
) = launchedCollectLatest {
    state.dismissBehavior = it.dismissBehavior
}

context(productionScope: CoroutineScope)
private fun Flow<UiAction.UpdatePaneAnchor>.launchPaneAnchorMutations(
    state: UiState.SnapshotMutable,
) = launchedCollectLatest {
    state.currentPaneAnchor = it.paneAnchor
}

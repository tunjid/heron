package com.tunjid.heron.ui.scaffold.ui

import androidx.compose.runtime.Stable
import com.tunjid.heron.data.di.AppMainScope
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.ui.scaffold.scaffold.BottomNavStatus
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.mutator.coroutines.launchedCollectLatest
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Stable
internal interface UiStateHolder : ActionSuspendingStateMutator<UiAction, UiState>

@Inject
internal class AppUiStateHolder(
    @AppMainScope
    appMainScope: CoroutineScope,
    userDataRepository: UserDataRepository,
) : UiStateHolder,
    ActionSuspendingStateMutator<UiAction, UiState> by appMainScope.actionSuspendingStateMutator(
        state = UiState.Immutable().toSnapshotMutable(),
        started = SharingStarted.Eagerly,
        producer = { state, actions ->
            launchResetTransientBottomNavMutations(
                state = state,
                userDataRepository = userDataRepository,
            )
            actions.launchMutationsIn(
                productionScope = this,
                keySelector = UiAction::key,
            ) {
                when (val action = type()) {
                    is UiAction.UpdateDismissBehavior -> action.flow.launchDismissBehaviorMutations(
                        state = state,
                    )
                    is UiAction.UpdatePaneAnchor -> action.flow.launchPaneAnchorMutations(
                        state = state,
                    )
                    is UiAction.UpdateRouteImmersion -> action.flow.launchRouteImmersionMutations(
                        state = state,
                    )
                    is UiAction.UpdateTransientBottomNav -> action.flow.launchToggleTransientBottomNavMutations(
                        state = state,
                    )
                }
            }
        },
    )

context(productionScope: CoroutineScope)
private fun launchResetTransientBottomNavMutations(
    state: UiState.Mutable,
    userDataRepository: UserDataRepository,
) = userDataRepository.navigation
    .map {
        it.backStacks.getOrNull(it.activeNav)?.lastOrNull()
    }
    .distinctUntilChanged()
    .launchedCollectLatest {
        state.transientBottomNavStatus = null
    }

context(productionScope: CoroutineScope)
private fun Flow<UiAction.UpdateDismissBehavior>.launchDismissBehaviorMutations(
    state: UiState.Mutable,
) = launchedCollectLatest {
    state.dismissBehavior = it.dismissBehavior
}

context(productionScope: CoroutineScope)
private fun Flow<UiAction.UpdatePaneAnchor>.launchPaneAnchorMutations(
    state: UiState.Mutable,
) = launchedCollectLatest {
    state.currentPaneAnchor = it.paneAnchor
}

context(productionScope: CoroutineScope)
private fun Flow<UiAction.UpdateRouteImmersion>.launchRouteImmersionMutations(
    state: UiState.Mutable,
) = launchedCollectLatest {
    when (it) {
        is UiAction.UpdateRouteImmersion.Immersive -> state.immersiveRouteIds += it.route.id
        is UiAction.UpdateRouteImmersion.Standard -> state.immersiveRouteIds -= it.route.id
    }
}

context(productionScope: CoroutineScope)
private fun Flow<UiAction.UpdateTransientBottomNav>.launchToggleTransientBottomNavMutations(
    state: UiState.Mutable,
) = launchedCollectLatest {
    state.transientBottomNavStatus = when (it) {
        UiAction.UpdateTransientBottomNav.ClearTransient -> null
        UiAction.UpdateTransientBottomNav.SetTransient -> BottomNavStatus.Collapsed.Partially
    }
}

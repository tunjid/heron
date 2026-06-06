package com.tunjid.heron.scaffold.identity

import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.di.AppMainScope
import com.tunjid.heron.data.network.NetworkMonitor
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.data.utilities.DatabaseCleanup
import com.tunjid.heron.data.utilities.writequeue.FailedWrite
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.ui.coroutines.launchAndCollect
import com.tunjid.heron.ui.coroutines.launchAndCollectLatest
import com.tunjid.heron.ui.text.CommonStrings
import com.tunjid.heron.ui.text.Memo
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import dev.zacsweers.metro.Inject
import heron.ui.core.generated.resources.error_session_switch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

interface IdentityStateHolder : ActionSuspendingStateMutator<IdentityAction, IdentityState>

@Inject
class AppIdentityStateHolder(
    @AppMainScope
    appMainScope: CoroutineScope,
    authRepository: AuthRepository,
    userDataRepository: UserDataRepository,
    networkMonitor: NetworkMonitor,
    writeQueue: WriteQueue,
    databaseCleanup: DatabaseCleanup,
) : IdentityStateHolder,
    ActionSuspendingStateMutator<IdentityAction, IdentityState> by appMainScope.actionSuspendingStateMutator(
        state = IdentityState.Immutable().toSnapshotMutable(),
        started = SharingStarted.Eagerly,
        producer = { state, actions ->
            authRepository.signedInUser
                .launchAndCollect(state::signedInProfile::set)
            authRepository.pastSessions
                .launchAndCollect(state::pastSessions::set)
            userDataRepository.preferences
                .launchAndCollect(state::preferences::set)
            networkMonitor.isConnected
                .launchAndCollect(state::isConnected::set)
            writeQueue.failedWrites
                .map(List<FailedWrite>::lastOrNull)
                .distinctUntilChanged()
                .drop(1)
                .launchAndCollect(state::lastFailedWrite::set)
            launch {
                writeQueue.drain()
            }
            launch {
                databaseCleanup.cleanup()
            }
            actions.launchMutationsIn(
                productionScope = this,
            ) {
                when (val action = type()) {
                    is IdentityAction.Switch -> action.flow.launchSwitchSessionMutations(
                        state = state,
                        authRepository = authRepository,
                    )
                }
            }
        },
    )

context(productionScope: CoroutineScope)
private fun Flow<IdentityAction.Switch>.launchSwitchSessionMutations(
    state: IdentityState.SnapshotMutable,
    authRepository: AuthRepository,
) = debounce { action ->
    when (action) {
        IdentityAction.Switch.Cancel -> 0.seconds
        IdentityAction.Switch.Choose -> 0.seconds
        is IdentityAction.Switch.Transition -> 300.milliseconds
    }
}
    .launchAndCollectLatest { action ->
        when (action) {
            IdentityAction.Switch.Cancel ->
                state.switchStatus = IdentityState.SwitchStatus.Stable.Idle
            IdentityAction.Switch.Choose ->
                state.switchStatus = IdentityState.SwitchStatus.Choosing
            is IdentityAction.Switch.Transition ->
                when (action.summary.profileId) {
                    state.signedInProfile?.did -> {
                        state.switchStatus = IdentityState.SwitchStatus.Stable.Idle
                    }
                    else -> {
                        state.switchStatus = IdentityState.SwitchStatus.Switching(
                            session = action.summary,
                        )

                        when (val outcome = authRepository.switchSession(action.summary)) {
                            is Outcome.Success -> {
                                state.switchStatus = IdentityState.SwitchStatus.Stable.Idle
                            }

                            is Outcome.Failure -> {
                                state.switchStatus = IdentityState.SwitchStatus.Stable.Error(
                                    outcome.exception.message?.let(Memo::Text)
                                        ?: Memo.Resource(CommonStrings.error_session_switch),
                                )
                                // Give the user 3 seconds to act, else revert
                                delay(3.seconds)
                                state.switchStatus = IdentityState.SwitchStatus.Stable.Idle
                            }
                        }
                    }
                }
        }
    }

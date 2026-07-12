package com.tunjid.heron.ui.scaffold.identity

import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.di.AppMainScope
import com.tunjid.heron.data.ml.engine.EngineState
import com.tunjid.heron.data.ml.engine.InferenceEngine
import com.tunjid.heron.data.ml.model.InferenceModelManager
import com.tunjid.heron.data.ml.model.ModelStatus
import com.tunjid.heron.data.network.NetworkMonitor
import com.tunjid.heron.data.platform.MemoryMonitor
import com.tunjid.heron.data.platform.MemoryPressure
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.data.utilities.DatabaseCleanup
import com.tunjid.heron.data.utilities.writequeue.FailedWrite
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.media.video.PlayerStatus
import com.tunjid.heron.media.video.VideoPlayerController
import com.tunjid.heron.ui.text.CommonStrings
import com.tunjid.heron.ui.text.Memo
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.mutator.coroutines.launchedCollect
import com.tunjid.mutator.coroutines.launchedCollectLatest
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

@Stable
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
    inferenceEngine: InferenceEngine,
    inferenceModelManager: InferenceModelManager,
    memoryMonitor: MemoryMonitor,
    videoPlayerController: VideoPlayerController,
) : IdentityStateHolder,
    ActionSuspendingStateMutator<IdentityAction, IdentityState> by appMainScope.actionSuspendingStateMutator(
        state = IdentityState.Immutable().toSnapshotMutable(),
        started = SharingStarted.Eagerly,
        producer = { state, actions ->
            authRepository.signedInUser.launchedCollect {
                state.signedInProfile = it
            }
            authRepository.pastSessions.launchedCollect {
                state.pastSessions = it
            }
            userDataRepository.preferences.launchedCollect {
                state.preferences = it
            }
            networkMonitor.isConnected.launchedCollect {
                state.isConnected = it
            }
            writeQueue.failedWrites
                .map(List<FailedWrite>::lastOrNull)
                .distinctUntilChanged()
                .drop(1)
                .launchedCollect {
                    state.lastFailedWrite = it
                }

            launch {
                writeQueue.drain()
            }
            launch {
                databaseCleanup.cleanup()
            }
            launchDefaultModelMutations(
                userDataRepository = userDataRepository,
                inferenceEngine = inferenceEngine,
                inferenceModelManager = inferenceModelManager,
            )
            launchMemoryPressureResetMutations(
                memoryMonitor = memoryMonitor,
                videoPlayerController = videoPlayerController,
                inferenceEngine = inferenceEngine,
            )
            actions.launchMutationsIn(
                productionScope = this,
            ) {
                when (val action = type()) {
                    is IdentityAction.Switch -> action.flow.launchSwitchSessionMutations(
                        state = state,
                        authRepository = authRepository,
                    )
                    is IdentityAction.ClearFailedWrite -> action.flow.launchClearFailedWriteMutations(
                        state = state,
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
    .launchedCollectLatest { action ->
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

context(productionScope: CoroutineScope)
private fun Flow<IdentityAction.ClearFailedWrite>.launchClearFailedWriteMutations(
    state: IdentityState.SnapshotMutable,
) = launchedCollect {
    state.lastFailedWrite = null
}

context(productionScope: CoroutineScope)
private fun launchDefaultModelMutations(
    userDataRepository: UserDataRepository,
    inferenceEngine: InferenceEngine,
    inferenceModelManager: InferenceModelManager,
) {
    productionScope.launch {
        val local = userDataRepository.preferences
            .map { it.local }
            .first { it.loadDefaultModelOnLaunch && it.defaultModelName != null }
        val modelName = local.defaultModelName ?: return@launch
        val model = inferenceModelManager.models
            .firstOrNull { it.name == modelName }
            ?: return@launch
        when (val status = inferenceModelManager.status(model).first()) {
            is ModelStatus.Downloaded ->
                if (inferenceEngine.state.first() is EngineState.Uninitialized) {
                    inferenceEngine.load(status.loadedModel)
                }
            is ModelStatus.Pending -> Unit
        }
    }
}

context(productionScope: CoroutineScope)
private fun launchMemoryPressureResetMutations(
    memoryMonitor: MemoryMonitor,
    inferenceEngine: InferenceEngine,
    videoPlayerController: VideoPlayerController,
) {
    merge(
        memoryMonitor.pressure
            .filter { it != MemoryPressure.Normal },
        snapshotFlow flow@{
            val playerState = videoPlayerController.activePlayerState ?: return@flow null
            val status = playerState.status
            if (status !is PlayerStatus.Play) return@flow null
            playerState.videoId
        }
            .filterNotNull(),
    )
        .launchedCollect {
            // Under real memory pressure, shed a loaded-but-idle model to reclaim its footprint;
            // never interrupt an in-flight generation. It lazily reloads on the next request.
            if (inferenceEngine.state.first() is EngineState.Ready.Idle) {
                inferenceEngine.reset()
            }
        }
}

/*
 *    Copyright 2026 Adetunji Dahunsi
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

package com.tunjid.heron.inference

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.ml.engine.InferenceEngine
import com.tunjid.heron.data.ml.model.InferenceModelManager
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.ui.scaffold.navigation.NavigationMutation
import com.tunjid.heron.ui.stateproduction.RouteStateHolder
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.mutator.coroutines.launchedCollect
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Stable
internal interface InferenceStateHolder :
    RouteStateHolder,
    ActionSuspendingStateMutator<Action, State>

@AssistedFactory
fun interface InferenceViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualInferenceViewModel
}

@Stable
class ActualInferenceViewModel(
    mutator: ActionSuspendingStateMutator<Action, State>,
    scope: CoroutineScope,
) : ViewModel(viewModelScope = scope),
    InferenceStateHolder,
    ActionSuspendingStateMutator<Action, State> by mutator {

    @AssistedInject
    constructor(
        navActions: (NavigationMutation) -> Unit,
        inferenceEngine: InferenceEngine,
        inferenceModelManager: InferenceModelManager,
        @Assisted scope: CoroutineScope,
        @Assisted route: Route,
    ) : this(
        mutator = scope.actionSuspendingStateMutator(
            state = State(inferenceModelManager.models).toSnapshotMutable(),
            started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
            producer = { state, actions ->
                launchModelStatusMutations(
                    state = state,
                    inferenceModelManager = inferenceModelManager,
                )
                actions.launchMutationsIn(
                    productionScope = this,
                    keySelector = Action::key,
                ) {
                    when (val action = type()) {
                        is Action.Load -> action.flow.launchLoadModelMutations(
                            inferenceEngine = inferenceEngine,
                        )
                        is Action.Download -> action.flow.launchDownloadMutations(
                            inferenceModelManager = inferenceModelManager,
                        )
                        is Action.Cancel -> action.flow.launchCancelMutations(
                            inferenceModelManager = inferenceModelManager,
                        )
                        is Action.Navigate -> action.flow.launchedCollect {
                            navActions(it.navigationMutation)
                        }
                    }
                }
            },
        ),
        scope = scope,
    )
}

context(productionScope: CoroutineScope)
private fun launchEngineStatesMutations(
    state: State.SnapshotMutable,
    inferenceEngine: InferenceEngine,
) {
    inferenceEngine.state
        .launchedCollect {
            state.engineState = it
        }
}

context(productionScope: CoroutineScope)
private fun launchModelStatusMutations(
    state: State.SnapshotMutable,
    inferenceModelManager: InferenceModelManager,
) {
    val models = inferenceModelManager.models
    if (models.isEmpty()) return
    combine(
        models.map { model ->
            inferenceModelManager.status(model).map { status ->
                ModelItem(
                    model = model,
                    status = status,
                )
            }
        },
    ) { items ->
        items.toList()
    }
        .launchedCollect(state::models::set)
}

context(productionScope: CoroutineScope)
private fun Flow<Action.Download>.launchDownloadMutations(
    inferenceModelManager: InferenceModelManager,
) = launchedCollect { action ->
    inferenceModelManager.enqueueDownload(action.model)
}

context(productionScope: CoroutineScope)
private fun Flow<Action.Cancel>.launchCancelMutations(
    inferenceModelManager: InferenceModelManager,
) = launchedCollect { action ->
    inferenceModelManager.cancelDownload(action.model)
}

context(productionScope: CoroutineScope)
private fun Flow<Action.Load>.launchLoadModelMutations(
    inferenceEngine: InferenceEngine,
) = launchedCollect { action ->
    inferenceEngine.load(action.model)
}

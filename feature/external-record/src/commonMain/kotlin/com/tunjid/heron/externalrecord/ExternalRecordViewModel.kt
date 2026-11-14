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

package com.tunjid.heron.externalrecord

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.StandardDocument
import com.tunjid.heron.data.core.types.EmbeddableRecordUri
import com.tunjid.heron.data.core.types.StandardPublicationUri
import com.tunjid.heron.data.core.types.recordUriOrNull
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.RecordRepository
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.externalrecord.di.ExternalRecordRequest
import com.tunjid.heron.externalrecord.di.externalRecordRequest
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.tiling.launchTilingMutations
import com.tunjid.heron.tiling.reset
import com.tunjid.heron.timeline.ui.toWritable
import com.tunjid.heron.timeline.utilities.launchAndCollectEnqueueMutations
import com.tunjid.heron.ui.scaffold.navigation.NavigationMutation
import com.tunjid.heron.ui.stateproduction.RouteStateHolder
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
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
import kotlinx.coroutines.flow.first

@Stable
internal interface ExternalRecordStateHolder :
    RouteStateHolder,
    ActionSuspendingStateMutator<Action, State>

@AssistedFactory
fun interface ExternalRecordViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualExternalRecordViewModel
}

@Stable
class ActualExternalRecordViewModel(
    mutator: ActionSuspendingStateMutator<Action, State>,
    scope: CoroutineScope,
) : ViewModel(viewModelScope = scope),
    ExternalRecordStateHolder,
    ActionSuspendingStateMutator<Action, State> by mutator {

    @AssistedInject
    constructor(
        navActions: (NavigationMutation) -> Unit,
        profileRepository: ProfileRepository,
        recordRepository: RecordRepository,
        writeQueue: WriteQueue,
        @Assisted scope: CoroutineScope,
        @Assisted route: Route,
    ) : this(
        mutator = scope.actionSuspendingStateMutator(
            state = State(route).toSnapshotMutable(),
            started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
            producer = { state, actions ->
                launchRecordMutations(
                    state = state,
                    route = route,
                    viewModelScope = scope,
                    profileRepository = profileRepository,
                    recordRepository = recordRepository,
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
                        is Action.RecordInteraction -> action.flow.launchRecordInteractionMutations(
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

context(productionScope: CoroutineScope)
private suspend fun launchRecordMutations(
    state: State.Mutable,
    route: Route,
    viewModelScope: CoroutineScope,
    profileRepository: ProfileRepository,
    recordRepository: RecordRepository,
) {
    val recordUri = state.record?.embeddableRecordUri ?: when (val request = route.externalRecordRequest) {
        is ExternalRecordRequest.WithUri -> request.uri
        is ExternalRecordRequest.WithProfile ->
            recordUriOrNull(
                profileId = profileRepository.profile(request.profileHandleOrId)
                    .first()
                    .did,
                namespace = request.namespace,
                recordKey = request.recordKey,
            ) as? EmbeddableRecordUri
    } ?: return

    when (recordUri) {
        is StandardPublicationUri -> {
            if (state.stateHolders.isEmpty()) {
                state.stateHolders = listOf(
                    ExternalRecordStateHolders.Documents(
                        mutator = viewModelScope.documentsStateHolder(
                            publicationUri = recordUri,
                            recordRepository = recordRepository,
                        ),
                    ),
                )
            }
            recordRepository.publication(recordUri).launchedCollect {
                state.record = it
            }
        }
        else -> Unit
    }
}

context(productionScope: CoroutineScope)
private fun Flow<Action.RecordInteraction>.launchRecordInteractionMutations(
    state: State.Mutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { it.interaction.toWritable() },
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

private fun CoroutineScope.documentsStateHolder(
    publicationUri: StandardPublicationUri,
    recordRepository: RecordRepository,
): DocumentsStateHolder = actionSuspendingStateMutator(
    state = DocumentsTilingState(publicationUri = publicationUri).toSnapshotMutable(),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    producer = { state, actions ->
        actions.launchTilingMutations(
            state = state,
            updateQueryData = { copy(data = it) },
            refreshQuery = { copy(data = data.reset()) },
            cursorListLoader = recordRepository::publicationDocuments,
            onNewItems = { items -> items.distinctBy(StandardDocument::uri) },
        )
    },
)

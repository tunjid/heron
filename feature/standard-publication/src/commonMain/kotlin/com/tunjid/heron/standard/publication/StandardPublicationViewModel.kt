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

package com.tunjid.heron.standard.publication

import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.StandardDocument
import com.tunjid.heron.data.core.models.StandardSubscription
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.data.core.types.StandardPublicationUri
import com.tunjid.heron.data.core.types.recordUriOrNull
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.RecordRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.heron.standard.publication.di.PublicationRequest
import com.tunjid.heron.standard.publication.di.publicationRequest
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.reset
import com.tunjid.heron.tiling.tilingMutations
import com.tunjid.heron.timeline.utilities.enqueueMutations
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.distinctBy
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge

internal typealias StandardPublicationStateHolder = ActionStateMutator<Action, StateFlow<State>>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualStandardPublicationViewModel
}

@AssistedInject
class ActualStandardPublicationViewModel(
    navActions: (NavigationMutation) -> Unit,
    profileRepository: ProfileRepository,
    recordRepository: RecordRepository,
    writeQueue: WriteQueue,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope),
    StandardPublicationStateHolder by scope.actionStateFlowMutator(
        initialState = State(route),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        actionTransform = transform@{ actions ->
            merge(
                publicationMutations(
                    route = route,
                    scope = scope,
                    stateHolder = this,
                    profileRepository = profileRepository,
                    recordRepository = recordRepository,
                ),
                actions.toMutationStream(
                    keySelector = Action::key,
                ) {
                    when (val action = type()) {
                        is Action.SnackbarDismissed -> action.flow.snackbarDismissalMutations()
                        is Action.Navigate -> action.flow.consumeNavigationActions(
                            navigationMutationConsumer = navActions,
                        )
                        is Action.TogglePublicationSubscription -> action.flow.togglePublicationSubscriptionMutations(
                            writeQueue = writeQueue,
                        )
                    }
                },
            )
        },
    )

private fun publicationMutations(
    route: Route,
    scope: CoroutineScope,
    stateHolder: SuspendingStateHolder<State>,
    profileRepository: ProfileRepository,
    recordRepository: RecordRepository,
): Flow<Mutation<State>> = flow {
    val state = stateHolder.state()
    val publicationUri = state.publication?.uri ?: when (val request = route.publicationRequest) {
        is PublicationRequest.WithUri -> request.uri
        is PublicationRequest.WithProfile ->
            recordUriOrNull(
                profileId = profileRepository.profile(request.profileHandleOrId)
                    .first()
                    .did,
                namespace = StandardPublicationUri.NAMESPACE,
                recordKey = RecordKey(request.publicationUriSuffix),
            ) as? StandardPublicationUri
    } ?: return@flow

    if (state.documentsTilingStateHolder == null) {
        emit {
            copy(
                documentsTilingStateHolder = scope.documentsStateHolder(
                    publicationUri = publicationUri,
                    recordRepository = recordRepository,
                ),
            )
        }
    }

    emitAll(
        recordRepository.publication(publicationUri)
            .mapToMutation { copy(publication = it) },
    )
}

private fun Flow<Action.TogglePublicationSubscription>.togglePublicationSubscriptionMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> = this.enqueueMutations(
    writeQueue,
    toWritable = { action ->
        when (action) {
            is Action.TogglePublicationSubscription.Subscribe -> Writable.StandardSite.Subscribe(
                create = StandardSubscription.Create(publicationUri = action.publicationUri),
            )
            is Action.TogglePublicationSubscription.Unsubscribe -> Writable.RecordDeletion(
                recordUri = action.subscriptionUri,
            )
        }
    },
) { _, memo ->
    if (memo != null) emit { copy(messages = messages + memo) }
}

private fun Flow<Action.SnackbarDismissed>.snackbarDismissalMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(messages = messages - action.message)
    }

private fun CoroutineScope.documentsStateHolder(
    publicationUri: StandardPublicationUri,
    recordRepository: RecordRepository,
) = actionStateFlowMutator<TilingState.Action, DocumentsTilingState>(
    initialState = DocumentsTilingState(
        publicationUri = publicationUri,
    ),
    actionTransform = transform@{ actions ->
        actions.toMutationStream {
            type().flow
                .tilingMutations(
                    currentState = { state() },
                    updateQueryData = { copy(data = it) },
                    refreshQuery = { copy(data = data.reset()) },
                    cursorListLoader = recordRepository::publicationDocuments,
                    onNewItems = { items ->
                        items.distinctBy(StandardDocument::uri)
                    },
                    onTilingDataUpdated = { copy(tilingData = it) },
                )
        }
    },
)

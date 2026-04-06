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
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.data.core.types.StandardPublicationUri
import com.tunjid.heron.data.core.types.recordUriOrNull
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.RecordRepository
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.heron.standard.publication.di.PublicationRequest
import com.tunjid.heron.standard.publication.di.publicationRequest
import com.tunjid.heron.tiling.reset
import com.tunjid.heron.tiling.tilingMutations
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
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

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
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope),
    StandardPublicationStateHolder by scope.actionStateFlowMutator(
        initialState = State(route),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        actionTransform = transform@{ actions ->
            actions.toMutationStream(
                keySelector = Action::key,
            ) {
                when (val action = type()) {
                    is Action.SnackbarDismissed -> action.flow.snackbarDismissalMutations()
                    is Action.Navigate -> action.flow.consumeNavigationActions(
                        navigationMutationConsumer = navActions,
                    )
                    is Action.Tile -> action.flow.publicationDocumentLoadMutations(
                        request = route.publicationRequest,
                        stateHolder = this@transform,
                        profileRepository = profileRepository,
                        recordRepository = recordRepository,
                    )
                }
            }
        },
    )

private suspend fun Flow<Action.Tile>.publicationDocumentLoadMutations(
    request: PublicationRequest,
    stateHolder: SuspendingStateHolder<State>,
    profileRepository: ProfileRepository,
    recordRepository: RecordRepository,
): Flow<Mutation<State>> {
    val publicationUri = when (request) {
        is PublicationRequest.WithUri -> request.uri
        is PublicationRequest.WithProfile ->
            recordUriOrNull(
                profileId = profileRepository.profile(request.profileHandleOrId)
                    .first()
                    .did,
                namespace = StandardPublicationUri.NAMESPACE,
                recordKey = RecordKey(request.publicationUriSuffix),
            ) as? StandardPublicationUri
    } ?: return emptyFlow()

    return map { it.tilingAction }
        .tilingMutations(
            currentState = { stateHolder.state() },
            updateQueryData = { copy(data = it) },
            refreshQuery = { copy(data = data.reset()) },
            cursorListLoader = { query, cursor ->
                recordRepository.publicationDocuments(
                    query = query.copy(publicationUri = publicationUri),
                    cursor = cursor,
                )
            },
            onNewItems = { items -> items.distinctBy(StandardDocument::uri) },
            onTilingDataUpdated = {
                val publication = it.items.firstNotNullOfOrNull(StandardDocument::publication)
                copy(
                    publication = publication ?: this.publication,
                    tilingData = it.copy(
                        currentQuery = it.currentQuery.copy(publicationUri = publicationUri),
                    ),
                )
            },
        )
}

private fun Flow<Action.SnackbarDismissed>.snackbarDismissalMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(messages = messages - action.message)
    }

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

package com.tunjid.heron.standard.subscription

import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.StandardPublication
import com.tunjid.heron.data.repository.RecordRepository
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.heron.tiling.TilingState
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
import kotlinx.coroutines.flow.map

internal typealias StandardSubscriptionStateHolder = ActionStateMutator<Action, StateFlow<State>>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualStandardSubscriptionViewModel
}

@AssistedInject
class ActualStandardSubscriptionViewModel(
    navActions: (NavigationMutation) -> Unit,
    recordRepository: RecordRepository,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope),
    StandardSubscriptionStateHolder by scope.actionStateFlowMutator(
        initialState = State(),
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
                    is Action.Tile -> action.flow.subscriptionLoadMutations(
                        stateHolder = this@transform,
                        recordRepository = recordRepository,
                    )
                }
            }
        },
    )

private suspend fun Flow<Action.Tile>.subscriptionLoadMutations(
    stateHolder: SuspendingStateHolder<State>,
    recordRepository: RecordRepository,
): Flow<Mutation<State>> =
    map { it.tilingAction }
        .tilingMutations(
            currentState = { stateHolder.state() },
            updateQueryData = { copy(data = it) },
            refreshQuery = { copy(data = data.reset()) },
            cursorListLoader = { query, cursor ->
                recordRepository.subscribedPublications(query, cursor)
            },
            onNewItems = { items -> items.distinctBy(StandardPublication::uri) },
            onTilingDataUpdated = { copy(tilingData = it) },
        )

private fun Flow<Action.SnackbarDismissed>.snackbarDismissalMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(messages = messages - action.message)
    }

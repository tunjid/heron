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

package com.tunjid.heron.notifications


import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.NotificationsRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.tilingMutations
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.distinctBy
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

internal typealias NotificationsStateHolder = ActionStateMutator<Action, StateFlow<State>>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualNotificationsViewModel
}

@Inject
class ActualNotificationsViewModel(
    navActions: (NavigationMutation) -> Unit,
    writeQueue: WriteQueue,
    authRepository: AuthRepository,
    notificationsRepository: NotificationsRepository,
    @Assisted
    scope: CoroutineScope,
    @Suppress("UNUSED_PARAMETER")
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope), NotificationsStateHolder by scope.actionStateFlowMutator(
    initialState = State(
    ),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    inputs = listOf(
        lastRefreshedMutations(
            notificationsRepository
        ),
        loadProfileMutations(
            authRepository
        ),
    ),
    actionTransform = transform@{ actions ->
        actions.toMutationStream(
            keySelector = Action::key
        ) {
            when (val action = type()) {
                is Action.Tile -> action.flow.notificationsMutations(
                    stateHolder = this@transform,
                    notificationsRepository = notificationsRepository,
                )

                is Action.SendPostInteraction -> action.flow.postInteractionMutations(
                    writeQueue = writeQueue,
                )

                is Action.MarkNotificationsRead -> action.flow.markNotificationsReadMutations(
                    notificationsRepository = notificationsRepository,
                )

                is Action.Navigate -> action.flow.consumeNavigationActions(
                    navigationMutationConsumer = navActions
                )
            }
        }
    }
)

private fun loadProfileMutations(
    authRepository: AuthRepository,
): Flow<Mutation<State>> =
    authRepository.signedInUser.mapToMutation {
        copy(signedInProfile = it)
    }

fun lastRefreshedMutations(
    notificationsRepository: NotificationsRepository,
): Flow<Mutation<State>> =
    notificationsRepository.lastRefreshed.mapToMutation { refreshedAt ->
        copy(
            lastRefreshed = refreshedAt,
            tilingData = tilingData.copy(
                status = when (val currentStatus = tilingData.status) {
                    is TilingState.Status.Initial -> currentStatus
                    is TilingState.Status.Refreshed -> currentStatus
                    is TilingState.Status.Refreshing -> {
                        if (refreshedAt == null || refreshedAt < tilingData.currentQuery.data.cursorAnchor) currentStatus
                        else TilingState.Status.Refreshed(
                            cursorAnchor = tilingData.currentQuery.data.cursorAnchor
                        )
                    }
                }
            )
        )
    }

private fun Flow<Action.SendPostInteraction>.postInteractionMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> =
    mapToManyMutations { action ->
        writeQueue.enqueue(Writable.Interaction(action.interaction))
    }

private fun Flow<Action.MarkNotificationsRead>.markNotificationsReadMutations(
    notificationsRepository: NotificationsRepository,
): Flow<Mutation<State>> =
    mapToManyMutations {
        notificationsRepository.markRead(it.at)
    }

suspend fun Flow<Action.Tile>.notificationsMutations(
    stateHolder: SuspendingStateHolder<State>,
    notificationsRepository: NotificationsRepository,
): Flow<Mutation<State>> =
    map { it.tilingAction }
        .tilingMutations(
            currentState = { stateHolder.state() },
            onRefreshQuery = { query ->
                query.copy(data = query.data.copy(page = 0, cursorAnchor = Clock.System.now()))
            },
            onNewItems = { notifications ->
                notifications.distinctBy(Notification::cid)
            },
            onTilingDataUpdated = {
                copy(tilingData = it)
            },
            updatePage = {
                copy(data = it)
            },
            cursorListLoader = notificationsRepository::notifications,
        )

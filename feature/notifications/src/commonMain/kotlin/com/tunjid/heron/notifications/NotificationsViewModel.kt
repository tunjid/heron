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
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.MessageRepository
import com.tunjid.heron.data.repository.NotificationsRepository
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.data.repository.recentConversations
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.heron.scaffold.scaffold.duplicateWriteMessage
import com.tunjid.heron.scaffold.scaffold.failedWriteMessage
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.refreshedStatus
import com.tunjid.heron.tiling.reset
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
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

internal typealias NotificationsStateHolder = ActionStateMutator<Action, StateFlow<State>>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualNotificationsViewModel
}

@AssistedInject
class ActualNotificationsViewModel(
    navActions: (NavigationMutation) -> Unit,
    writeQueue: WriteQueue,
    authRepository: AuthRepository,
    messageRepository: MessageRepository,
    notificationsRepository: NotificationsRepository,
    userDataRepository: UserDataRepository,
    @Assisted
    scope: CoroutineScope,
    @Suppress("UNUSED_PARAMETER")
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope),
    NotificationsStateHolder by scope.actionStateFlowMutator(
        initialState = State(),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        inputs = listOf(
            lastRefreshedMutations(
                notificationsRepository,
            ),
            loadProfileMutations(
                authRepository,
            ),
            recentConversationMutations(
                messageRepository = messageRepository,
            ),
            canShowRequestPermissionsButtonMutations(
                notificationsRepository,
            ),
            loadPreferencesMutations(
                userDataRepository = userDataRepository,
            ),
        ),
        actionTransform = transform@{ actions ->
            actions.toMutationStream(
                keySelector = Action::key,
            ) {
                when (val action = type()) {
                    is Action.Tile -> action.flow.notificationsMutations(
                        stateHolder = this@transform,
                        notificationsRepository = notificationsRepository,
                    )

                    is Action.SendPostInteraction -> action.flow.postInteractionMutations(
                        writeQueue = writeQueue,
                    )
                    is Action.SnackbarDismissed -> action.flow.snackbarDismissalMutations()

                    is Action.MarkNotificationsRead -> action.flow.markNotificationsReadMutations(
                        notificationsRepository = notificationsRepository,
                    )

                    is Action.Navigate -> action.flow.consumeNavigationActions(
                        navigationMutationConsumer = navActions,
                    )
                    is Action.UpdateMutedWord -> action.flow.updateMutedWordMutations(
                        writeQueue = writeQueue,
                    )
                }
            }
        },
    )

private fun loadProfileMutations(
    authRepository: AuthRepository,
): Flow<Mutation<State>> =
    authRepository.signedInUser.mapToMutation {
        copy(signedInProfile = it)
    }

fun recentConversationMutations(
    messageRepository: MessageRepository,
): Flow<Mutation<State>> =
    messageRepository.recentConversations()
        .mapToMutation { conversations ->
            copy(recentConversations = conversations)
        }

fun loadPreferencesMutations(
    userDataRepository: UserDataRepository,
): Flow<Mutation<State>> =
    userDataRepository.preferences
        .mapToMutation {
            copy(preferences = it)
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
                        else tilingData.refreshedStatus()
                    }
                },
            ),
        )
    }

fun canShowRequestPermissionsButtonMutations(
    notificationsRepository: NotificationsRepository,
): Flow<Mutation<State>> =
    notificationsRepository.hasPreviouslyRequestedNotificationPermissions
        .mapToMutation { hasPreviouslyRequestedNotificationPermissions ->
            copy(canAnimateRequestPermissionsButton = !hasPreviouslyRequestedNotificationPermissions)
        }

private fun Flow<Action.SendPostInteraction>.postInteractionMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> =
    mapToManyMutations { action ->
        when (writeQueue.enqueue(Writable.Interaction(action.interaction))) {
            WriteQueue.Status.Dropped -> emit {
                copy(messages = messages + action.interaction.failedWriteMessage())
            }
            WriteQueue.Status.Duplicate -> emit {
                copy(messages = messages + action.interaction.duplicateWriteMessage())
            }
            WriteQueue.Status.Enqueued -> Unit
        }
    }

private fun Flow<Action.UpdateMutedWord>.updateMutedWordMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> =
    mapToManyMutations { action ->
        writeQueue.enqueue(
            Writable.TimelineUpdate(
                Timeline.Update.OfMutedWord.ReplaceAll(
                    mutedWordPreferences = action.mutedWordPreferences,
                ),
            ),
        )
    }

private fun Flow<Action.SnackbarDismissed>.snackbarDismissalMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(messages = messages - action.message)
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
            // This is determined by State.lastRefreshed
            isRefreshedOnNewItems = false,
            currentState = { stateHolder.state() },
            updateQueryData = { copy(data = it) },
            refreshQuery = { copy(data = data.reset()) },
            cursorListLoader = notificationsRepository::notifications,
            onNewItems = { notifications ->
                notifications.distinctBy(Notification::cid)
            },
            onTilingDataUpdated = {
                copy(tilingData = it)
            },
        )

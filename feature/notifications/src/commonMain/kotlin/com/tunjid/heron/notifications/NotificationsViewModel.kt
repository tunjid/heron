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
import com.tunjid.heron.data.core.models.Profile
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
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.reset
import com.tunjid.heron.tiling.tilingMutations
import com.tunjid.heron.tiling.withRefreshedStatus
import com.tunjid.heron.timeline.utilities.launchAndCollectEnqueueMutations
import com.tunjid.heron.ui.coroutines.launchAndCollect
import com.tunjid.heron.ui.coroutines.launchAndCollectLatest
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.tiler.distinctBy
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map

internal typealias NotificationsStateHolder = ActionSuspendingStateMutator<Action, State>

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
    NotificationsStateHolder by scope.actionSuspendingStateMutator(
        initialState = State().toSnapshotMutable(),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        producer = { state, actions ->
            lastRefreshedMutations(
                state = state,
                notificationsRepository = notificationsRepository,
            )
            loadProfileMutations(
                state = state,
                authRepository = authRepository,
            )
            canShowRequestPermissionsButtonMutations(
                state = state,
                notificationsRepository = notificationsRepository,
            )
            loadPreferencesMutations(
                state = state,
                userDataRepository = userDataRepository,
            )
            actions.launchMutationsIn(
                productionScope = this,
                keySelector = Action::key,
            ) {
                when (val action = type()) {
                    is Action.Tile -> action.flow.notificationsMutations(
                        state = state,
                        notificationsRepository = notificationsRepository,
                    )
                    is Action.SendPostInteraction -> action.flow.postInteractionMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is Action.SnackbarDismissed -> action.flow.snackbarDismissalMutations(state)
                    is Action.MarkNotificationsRead -> action.flow.markNotificationsReadMutations(
                        notificationsRepository = notificationsRepository,
                    )
                    is Action.Navigate -> action.flow.collect {
                        navActions(it.navigationMutation)
                    }
                    is Action.UpdateMutedWord -> action.flow.updateMutedWordMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is Action.BlockAccount -> action.flow.blockAccountMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is Action.MuteAccount -> action.flow.muteAccountMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is Action.UpdateRecentConversations -> action.flow.recentConversationMutations(
                        state = state,
                        messageRepository = messageRepository,
                    )
                    is Action.DeleteRecord -> action.flow.deleteRecordMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                }
            }
        },
    )

context(productionScope: CoroutineScope)
private fun loadProfileMutations(
    state: State.SnapshotMutable,
    authRepository: AuthRepository,
) = authRepository.signedInUser.launchAndCollect {
    state.signedInProfile = it
}

context(productionScope: CoroutineScope)
private fun Flow<Action.UpdateRecentConversations>.recentConversationMutations(
    state: State.SnapshotMutable,
    messageRepository: MessageRepository,
) = launchAndCollectLatest {
    messageRepository.recentConversations().collect { conversations ->
        state.recentConversations = conversations
    }
}

context(productionScope: CoroutineScope)
private fun loadPreferencesMutations(
    state: State.SnapshotMutable,
    userDataRepository: UserDataRepository,
) = userDataRepository.preferences.launchAndCollect {
    state.preferences = it
}

context(productionScope: CoroutineScope)
private fun lastRefreshedMutations(
    state: State.SnapshotMutable,
    notificationsRepository: NotificationsRepository,
) = notificationsRepository.lastRefreshed.launchAndCollect { refreshedAt ->
    state.lastRefreshed = refreshedAt
    val currentStatus = state.tilingData.status
    if (currentStatus is TilingState.Status.Refreshing &&
        refreshedAt != null &&
        refreshedAt >= state.tilingData.currentQuery.data.cursorAnchor
    ) {
        state.tilingData.withRefreshedStatus()
    }
}

context(productionScope: CoroutineScope)
private fun canShowRequestPermissionsButtonMutations(
    state: State.SnapshotMutable,
    notificationsRepository: NotificationsRepository,
) = notificationsRepository.hasPreviouslyRequestedNotificationPermissions
    .launchAndCollect { hasPreviouslyRequestedNotificationPermissions ->
        state.canAnimateRequestPermissionsButton = !hasPreviouslyRequestedNotificationPermissions
    }

context(productionScope: CoroutineScope)
private fun Flow<Action.UpdateMutedWord>.updateMutedWordMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = {
        Writable.TimelineUpdate(
            Timeline.Update.OfMutedWord.ReplaceAll(
                mutedWordPreferences = it.mutedWordPreference,
            ),
        )
    },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.BlockAccount>.blockAccountMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { action ->
        Writable.Restriction(
            Profile.Restriction.Block.Add(
                signedInProfileId = action.signedInProfileId,
                profileId = action.profileId,
            ),
        )
    },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.MuteAccount>.muteAccountMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { action ->
        Writable.Restriction(
            Profile.Restriction.Mute.Add(
                signedInProfileId = action.signedInProfileId,
                profileId = action.profileId,
            ),
        )
    },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.DeleteRecord>.deleteRecordMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { action ->
        Writable.RecordDeletion(
            recordUri = action.recordUri,
        )
    },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.SendPostInteraction>.postInteractionMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { action -> Writable.Interaction(action.interaction) },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.SnackbarDismissed>.snackbarDismissalMutations(
    state: State.SnapshotMutable,
) = launchAndCollect { event ->
    state.messages -= event.message
}

context(productionScope: CoroutineScope)
private fun Flow<Action.MarkNotificationsRead>.markNotificationsReadMutations(
    notificationsRepository: NotificationsRepository,
) = launchAndCollect { action ->
    notificationsRepository.markRead(action.at)
}

context(productionScope: CoroutineScope)
private fun Flow<Action.Tile>.notificationsMutations(
    state: State.SnapshotMutable,
    notificationsRepository: NotificationsRepository,
) = map { it.tilingAction }
    .tilingMutations(
        // This is determined by State.lastRefreshed
        isRefreshedOnNewItems = false,
        currentState = { state },
        updateQueryData = { copy(data = it) },
        refreshQuery = { copy(data = data.reset()) },
        cursorListLoader = notificationsRepository::notifications,
        onNewItems = { notifications ->
            notifications.distinctBy(Notification::cid)
        },
    )

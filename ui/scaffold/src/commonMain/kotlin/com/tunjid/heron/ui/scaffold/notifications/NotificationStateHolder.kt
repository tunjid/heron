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

package com.tunjid.heron.ui.scaffold.notifications

import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.di.AppMainScope
import com.tunjid.heron.data.logging.LogPriority
import com.tunjid.heron.data.logging.logcat
import com.tunjid.heron.data.repository.NotificationsQuery
import com.tunjid.heron.data.repository.NotificationsRepository
import com.tunjid.heron.ui.scaffold.scaffold.AppState
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.mutator.coroutines.launchedCollect
import com.tunjid.mutator.coroutines.launchedCollectLatest
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull

interface NotificationStateHolder : ActionSuspendingStateMutator<NotificationAction, NotificationState>

@Inject
class AppNotificationStateHolder(
    @AppMainScope
    appMainScope: CoroutineScope,
    notifier: Notifier,
    notificationsRepository: NotificationsRepository,
) : NotificationStateHolder,
    ActionSuspendingStateMutator<NotificationAction, NotificationState> by appMainScope.actionSuspendingStateMutator(
        state = NotificationState.Immutable().toSnapshotMutable(),
        started = SharingStarted.Eagerly,
        producer = { state, actions ->
            actions.launchMutationsIn(
                productionScope = this,
                keySelector = NotificationAction::key,
            ) {
                when (val action = type()) {
                    is NotificationAction.UpdatePermissions -> action.flow.launchUpdateNotificationPermissions(
                        state = state,
                    )
                    is NotificationAction.HandleNotification -> action.flow.launchHandleNotificationMutations(
                        state = state,
                        notifier = notifier,
                        notificationsRepository = notificationsRepository,
                    )
                    is NotificationAction.RegisterToken -> action.flow.launchRegisterTokenMutations(
                        state = state,
                        notificationsRepository = notificationsRepository,
                    )
                    is NotificationAction.NotificationDismissed -> action.flow.launchNotificationDismissalMutations(
                        notificationsRepository = notificationsRepository,
                    )
                    is NotificationAction.NotificationProcessedOrDropped -> action.flow.launchNotificationProcessedOrDroppedMutations(
                        state = state,
                    )
                    is NotificationAction.RequestedNotificationPermission -> action.flow.launchMarkNotificationPermissionRequestedMutations(
                        notificationsRepository = notificationsRepository,
                    )
                    is NotificationAction.ToggleUnreadNotificationsMonitor -> action.flow.launchMonitorUnreadCountMutations(
                        state = state,
                        notificationsRepository = notificationsRepository,
                    )
                }
            }
        },
    )

context(productionScope: CoroutineScope)
private fun Flow<NotificationAction.ToggleUnreadNotificationsMonitor>.launchMonitorUnreadCountMutations(
    state: NotificationState.SnapshotMutable,
    notificationsRepository: NotificationsRepository,
) = distinctUntilChanged()
    .launchedCollectLatest { action ->
        if (action.monitor) notificationsRepository.unreadCount
            .collect { state.unreadCount = it }
    }

context(productionScope: CoroutineScope)
private fun Flow<NotificationAction.UpdatePermissions>.launchUpdateNotificationPermissions(
    state: NotificationState.SnapshotMutable,
) = launchedCollect {
    state.hasNotificationPermissions = it.hasNotificationPermissions
}

context(productionScope: CoroutineScope)
private fun Flow<NotificationAction.RegisterToken>.launchRegisterTokenMutations(
    state: NotificationState.SnapshotMutable,
    notificationsRepository: NotificationsRepository,
) = launchedCollectLatest { action ->
    // TODO: This should be enqueued, but the write queue does not currently
    //  support updating a queued write to something else. For now, just write
    //  using the app scope and fix in a follow up PR.
    if (state.hasNotificationPermissions) {
        val tokenRegistrationOutcome = notificationsRepository.registerPushNotificationToken(
            action.token,
        )
        when (tokenRegistrationOutcome) {
            is Outcome.Failure -> Unit
            Outcome.Success -> state.notificationToken = action.token
        }
        state.logcat(LogPriority.INFO) {
            "Push notification token registration outcome: $tokenRegistrationOutcome"
        }
    }
}

context(productionScope: CoroutineScope)
private fun Flow<NotificationAction.HandleNotification>.launchHandleNotificationMutations(
    state: NotificationState.SnapshotMutable,
    notifier: Notifier,
    notificationsRepository: NotificationsRepository,
) = buffer(NotificationProcessingBufferSize)
    .flatMapMerge(NotificationProcessingMaxConcurrencyLimit) { action ->

        val senderId = action.senderDid ?: return@flatMapMerge emptyFlow()
        val targetDid = action.targetDid ?: return@flatMapMerge emptyFlow()
        val recordUri = action.recordUri ?: return@flatMapMerge emptyFlow()
        val reason = action.reason ?: return@flatMapMerge emptyFlow()

        flow {
            val result = withTimeoutOrNull(
                AppState.NOTIFICATION_PROCESSING_TIMEOUT_SECONDS,
            ) {
                notificationsRepository.resolvePushNotification(
                    NotificationsQuery.Push(
                        senderId = senderId,
                        targetDid = targetDid,
                        recordUri = recordUri,
                        reason = reason,
                    ),
                )
            }

            when {
                result == null ->
                    logcat(LogPriority.WARN) {
                        "Notification processing timed out for $recordUri"
                    }

                result.isFailure ->
                    logcat(LogPriority.DEBUG) {
                        "Notification dropped: ${result.exceptionOrNull()?.message}"
                    }

                result.isSuccess ->
                    notifier.displayNotifications(
                        notifications = listOf(result.getOrThrow()),
                    )
            }
            emit(recordUri)
        }
    }
    .launchedCollect { recordUri ->
        state.processedNotificationRecordUris += recordUri
    }

context(productionScope: CoroutineScope)
private fun Flow<NotificationAction.NotificationDismissed>.launchNotificationDismissalMutations(
    notificationsRepository: NotificationsRepository,
) = launchedCollectLatest {
    notificationsRepository.markRead(it.dismissedAt)
}

context(productionScope: CoroutineScope)
private fun Flow<NotificationAction.NotificationProcessedOrDropped>.launchNotificationProcessedOrDroppedMutations(
    state: NotificationState.SnapshotMutable,
) = launchedCollect { action ->
    state.processedNotificationRecordUris -= action.recordUri
}

context(productionScope: CoroutineScope)
private fun Flow<NotificationAction.RequestedNotificationPermission>.launchMarkNotificationPermissionRequestedMutations(
    notificationsRepository: NotificationsRepository,
) = launchedCollectLatest {
    notificationsRepository.markNotificationPermissionsRequested()
}

private const val NotificationProcessingMaxConcurrencyLimit = 4
private const val NotificationProcessingBufferSize = 64

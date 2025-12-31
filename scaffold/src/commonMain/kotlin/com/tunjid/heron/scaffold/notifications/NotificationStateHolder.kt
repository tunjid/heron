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

package com.tunjid.heron.scaffold.notifications

import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.core.types.asRecordUriOrNull
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.logging.LogPriority
import com.tunjid.heron.data.logging.logcat
import com.tunjid.heron.data.repository.NotificationsQuery
import com.tunjid.heron.data.repository.NotificationsRepository
import com.tunjid.heron.scaffold.scaffold.AppState
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapLatestToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

interface NotificationStateHolder : ActionStateMutator<NotificationAction, StateFlow<NotificationState>>

data class NotificationState(
    val unreadCount: Long = 0L,
    val hasNotificationPermissions: Boolean = false,
    val notificationToken: String? = null,
    val processedNotificationRecordUris: Set<RecordUri> = emptySet(),
)

sealed class NotificationAction(
    val key: String,
) {
    data class UpdatePermissions(
        val hasNotificationPermissions: Boolean,
    ) : NotificationAction(key = "UpdatePermissions")

    data class RegisterToken(
        val token: String,
    ) : NotificationAction(key = "RegisterToken")

    data class HandleNotification(
        val payload: Map<String, String>,
    ) : NotificationAction(key = "HandleNotification") {
        val senderDid = payload[NotificationAtProtoSenderDid]
            ?.let(::ProfileId)

        val recordUri: RecordUri? = payload[NotificationAtProtoRecordUri]
            ?.let { "${Uri.Host.AtProto.prefix}$it" }
            ?.asRecordUriOrNull()
    }

    data class NotificationProcessedOrDropped(
        val recordUri: RecordUri,
    ) : NotificationAction(key = "NotificationProcessedOrDropped")

    data class NotificationDismissed(
        val dismissedAt: Instant,
    ) : NotificationAction(key = "NotificationDismissed")

    data class ToggleUnreadNotificationsMonitor(
        val monitor: Boolean,
    ) : NotificationAction(key = "ToggleUnreadNotificationsMonitor")

    data object RequestedNotificationPermission :
        NotificationAction(key = "RequestedNotificationPermission")
}

@Inject
class AppNotificationStateHolder(
    @Named("AppScope") appScope: CoroutineScope,
    notifier: Notifier,
    notificationsRepository: NotificationsRepository,
) : NotificationStateHolder,
    ActionStateMutator<NotificationAction, StateFlow<NotificationState>> by appScope.actionStateFlowMutator(
        initialState = NotificationState(),
        started = SharingStarted.Eagerly,
        actionTransform = { actions ->
            actions.toMutationStream(
                keySelector = NotificationAction::key,
            ) {
                when (val action = type()) {
                    is NotificationAction.UpdatePermissions -> action.flow.updateNotificationPermissions()
                    is NotificationAction.HandleNotification -> action.flow.handleNotificationMutations(
                        notifier = notifier,
                        notificationsRepository = notificationsRepository,
                    )
                    is NotificationAction.RegisterToken -> action.flow.registerTokenMutations(
                        currentState = { state() },
                        notificationsRepository = notificationsRepository,
                    )
                    is NotificationAction.NotificationDismissed -> action.flow.notificationDismissalMutations(
                        notificationsRepository = notificationsRepository,
                    )
                    is NotificationAction.NotificationProcessedOrDropped -> action.flow.notificationProcessedOrDroppedMutations()
                    is NotificationAction.RequestedNotificationPermission -> action.flow.markNotificationPermissionRequestedMutations(
                        notificationsRepository = notificationsRepository,
                    )
                    is NotificationAction.ToggleUnreadNotificationsMonitor -> action.flow.monitorUnreadCountMutations(
                        notificationsRepository = notificationsRepository,
                    )
                }
            }
        },
    )

private fun Flow<NotificationAction.ToggleUnreadNotificationsMonitor>.monitorUnreadCountMutations(
    notificationsRepository: NotificationsRepository,
): Flow<Mutation<NotificationState>> =
    distinctUntilChanged()
        .mapLatestToManyMutations { action ->
            if (action.monitor) emitAll(
                notificationsRepository.unreadCount
                    .mapToMutation { copy(unreadCount = it) },
            )
        }

private fun Flow<NotificationAction.UpdatePermissions>.updateNotificationPermissions(): Flow<Mutation<NotificationState>> =
    mapToMutation {
        copy(hasNotificationPermissions = it.hasNotificationPermissions)
    }

private fun Flow<NotificationAction.RegisterToken>.registerTokenMutations(
    currentState: suspend () -> NotificationState,
    notificationsRepository: NotificationsRepository,
): Flow<Mutation<NotificationState>> =
    mapLatestToManyMutations { action ->
        // TODO: This should be enqueued, but the write queue does not currently
        //  support updating a queued write to something else. For now, just write
        //  using the app scope and fix in a follow up PR.
        val state = currentState()
        if (state.hasNotificationPermissions && action.token != state.notificationToken) {
            val tokenRegistrationOutcome = notificationsRepository.registerPushNotificationToken(
                action.token,
            )
            when (tokenRegistrationOutcome) {
                is Outcome.Failure -> Unit
                Outcome.Success -> emit { copy(notificationToken = action.token) }
            }
            logcat(LogPriority.INFO) {
                "Push notification token registration outcome: $tokenRegistrationOutcome"
            }
        }
    }

private fun Flow<NotificationAction.HandleNotification>.handleNotificationMutations(
    notifier: Notifier,
    notificationsRepository: NotificationsRepository,
): Flow<Mutation<NotificationState>> =
    buffer(NotificationProcessingBufferSize)
        // Process up NotificationProcessingMaxConcurrencyLimit in parallel
        .flatMapMerge(
            concurrency = NotificationProcessingMaxConcurrencyLimit,
        ) { action ->
            val senderId = action.senderDid ?: return@flatMapMerge emptyFlow()
            val recordUri = action.recordUri ?: return@flatMapMerge emptyFlow()

            flow {
                val notification = withTimeoutOrNull(AppState.NOTIFICATION_PROCESSING_TIMEOUT_SECONDS) {
                    notificationsRepository.resolvePushNotification(
                        NotificationsQuery.Push(
                            senderId = senderId,
                            recordUri = recordUri,
                        ),
                    )
                        .getOrNull()
                }
                if (notification != null) notifier.displayNotifications(
                    notifications = listOf(notification),
                )
                else logcat(LogPriority.WARN) {
                    "Failed to resolve notification for $recordUri"
                }
                emit {
                    copy(
                        processedNotificationRecordUris = processedNotificationRecordUris + recordUri,
                    )
                }
            }
        }

private fun Flow<NotificationAction.NotificationDismissed>.notificationDismissalMutations(
    notificationsRepository: NotificationsRepository,
): Flow<Mutation<NotificationState>> =
    mapLatestToManyMutations {
        notificationsRepository.markRead(it.dismissedAt)
    }

private fun Flow<NotificationAction.NotificationProcessedOrDropped>.notificationProcessedOrDroppedMutations(): Flow<Mutation<NotificationState>> =
    mapToMutation { action ->
        copy(
            processedNotificationRecordUris = processedNotificationRecordUris - action.recordUri,
        )
    }

private fun Flow<NotificationAction.RequestedNotificationPermission>.markNotificationPermissionRequestedMutations(
    notificationsRepository: NotificationsRepository,
): Flow<Mutation<NotificationState>> =
    mapLatestToManyMutations {
        notificationsRepository.markNotificationPermissionsRequested()
    }

private const val NotificationAtProtoSenderDid = "senderDid"
private const val NotificationAtProtoRecordUri = "recordUri"
private const val NotificationProcessingMaxConcurrencyLimit = 4
private const val NotificationProcessingBufferSize = 64

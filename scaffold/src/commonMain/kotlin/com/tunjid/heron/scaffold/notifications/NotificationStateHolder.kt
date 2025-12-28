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

import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.data.repository.NotificationsRepository
import com.tunjid.heron.data.utilities.tidInstant
import com.tunjid.heron.scaffold.scaffold.AppState
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapLatestToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

interface NotificationStateHolder : ActionStateMutator<NotificationAction, StateFlow<NotificationState>>

data class NotificationState(
    val unreadCount: Long = 0L,
    val hasNotificationPermissions: Boolean = false,
    val processedNotificationRecordKeys: Set<RecordKey> = emptySet(),
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

        val recordKey: RecordKey? = payload[NotificationAtProtoRecordKey]
            ?.let(::RecordKey)

        internal val commitedAt = recordKey?.tidInstant

        internal val recordUri: GenericUri?

        val isProcessable
            get() =
                commitedAt != null && recordUri != null

        init {
            val senderDid = payload[NotificationAtProtoSenderDid]
            val collection = payload[NotificationAtProtoCollection]

            recordUri = if (senderDid == null || collection == null) null
            else GenericUri("$senderDid/$collection/${recordKey?.value}")
        }
    }

    data class NotificationProcessedOrDropped(
        val recordKey: RecordKey,
    ) : NotificationAction(key = "NotificationProcessedOrDropped")

    data class NotificationDismissed(
        val dismissedAt: Instant,
    ) : NotificationAction(key = "NotificationDismissed")
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
        inputs = listOf(
            unreadCountMutations(
                notificationsRepository = notificationsRepository,
            ),
        ),
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
                }
            }
        },
    )

private fun unreadCountMutations(
    notificationsRepository: NotificationsRepository,
): Flow<Mutation<NotificationState>> =
    notificationsRepository.unreadCount
        .mapToMutation { copy(unreadCount = it) }

private fun Flow<NotificationAction.UpdatePermissions>.updateNotificationPermissions(): Flow<Mutation<NotificationState>> =
    mapToMutation {
        copy(hasNotificationPermissions = it.hasNotificationPermissions)
    }

private fun Flow<NotificationAction.RegisterToken>.registerTokenMutations(
    currentState: suspend () -> NotificationState,
    notificationsRepository: NotificationsRepository,
): Flow<Mutation<NotificationState>> =
    mapLatestToManyMutations {
        // TODO: This should be enqueued, but the write queue does not currently
        //  support updating a queued write to something else. For now, just write
        //  using the app scope and fix in a follow up PR.
        if (currentState().hasNotificationPermissions) {
            notificationsRepository.registerPushNotificationToken(it.token)
        }
    }

private fun Flow<NotificationAction.HandleNotification>.handleNotificationMutations(
    notifier: Notifier,
    notificationsRepository: NotificationsRepository,
): Flow<Mutation<NotificationState>> = channelFlow {
    val sharedActions = MutableSharedFlow<NotificationAction.HandleNotification>(
        replay = NotificationProcessingReplaySize,
        extraBufferCapacity = NotificationProcessingBufferSize,
    )

    // Pipe emissions into the shared flow
    launch {
        collect { action ->
            if (action.commitedAt == null || action.recordUri == null) return@collect
            sharedActions.emit(action)
        }
    }

    // This coroutine refreshes the notifications database so new
    // notification are picked up by the display coroutine below.
    // It debounces network calls if emissions are close to each other in case a
    // user goes viral.
    // It needs a separate coroutine to allow for slow collectors when displaying
    launch {
        sharedActions
            .scan(
                initial = emptyList<NotificationAction.HandleNotification>(),
                operation = { consecutiveActions, action ->
                    consecutiveActions.plus(action).takeLast(2)
                },
            )
            .debounce { actions ->
                // throttle API calls if a user is viral
                if (actions.size < 2) return@debounce 0.seconds
                val (previousAction, currentAction) = actions
                when (currentAction.requireCommitedAt - previousAction.requireCommitedAt) {
                    in 0.seconds..1.seconds -> NotificationsRefreshDebounceSeconds
                    else -> 0.seconds
                }
            }
            .filter(List<NotificationAction.HandleNotification>::isNotEmpty)
            .collect { actions ->
                // Refresh the database to pick up new notifications
                notificationsRepository.fetchNotificationsFor(
                    requireNotNull(actions.last().recordUri),
                )
            }
    }

    // This coroutine strictly checks for unread notifications for a given commit.
    // It's collector is potentially slow.
    launch {
        sharedActions.collect { action ->
            withTimeout(AppState.NOTIFICATION_PROCESSING_TIMEOUT_SECONDS) {
                val recordKey = requireNotNull(action.recordKey)
                val after = requireNotNull(action.commitedAt) - NotificationQueryTimeWindow

                notifier.displayNotifications(
                    notifications = notificationsRepository.unreadNotifications(after)
                        .first(List<Notification>::isNotEmpty),
                )

                // Perform book keeping as necessary.
                send {
                    copy(
                        processedNotificationRecordKeys = processedNotificationRecordKeys + recordKey,
                    )
                }
            }
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
            processedNotificationRecordKeys = processedNotificationRecordKeys - action.recordKey,
        )
    }

private val NotificationAction.HandleNotification.requireCommitedAt
    get() = requireNotNull(commitedAt)

private val NotificationsRefreshDebounceSeconds = 3.seconds
private val NotificationQueryTimeWindow = 2.seconds
private const val NotificationAtProtoSenderDid = "senderDid"
private const val NotificationAtProtoCollection = "collection"
private const val NotificationAtProtoRecordKey = "recordKey"
private const val NotificationProcessingReplaySize = 4
private const val NotificationProcessingBufferSize = 64

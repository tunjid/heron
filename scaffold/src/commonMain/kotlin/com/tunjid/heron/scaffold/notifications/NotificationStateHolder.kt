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
import com.tunjid.heron.data.repository.NotificationsRepository
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.withTimeoutOrNull

interface NotificationStateHolder : ActionStateMutator<NotificationAction, StateFlow<NotificationState>>

data class NotificationState(
    val unreadCount: Long = 0L,
    val hasNotificationPermissions: Boolean = false,
    val latestPushNotifications: List<Notification> = emptyList(),
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
    ) : NotificationAction(key = "HandleNotification")

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
): Flow<Mutation<NotificationState>> =
// Each emission does the same thing. Simply conflate if a user went viral
    // and has lots of notifications
    conflate()
        .mapLatestToManyMutations { action ->
            // This is potentially expensive, collect it for a maximum of 3 seconds.
            withTimeoutOrNull(3.seconds) {
                emitAll(
                    notificationsRepository.getUnreadNotifications(action.payload)
                        .mapLatestToManyMutations {
                            emit { copy(latestPushNotifications = it) }
                            notifier.displayNotifications(it)
                        },
                )
            }
        }

private fun Flow<NotificationAction.NotificationDismissed>.notificationDismissalMutations(
    notificationsRepository: NotificationsRepository,
): Flow<Mutation<NotificationState>> =
    mapLatestToManyMutations {
        notificationsRepository.markRead(it.dismissedAt)
    }

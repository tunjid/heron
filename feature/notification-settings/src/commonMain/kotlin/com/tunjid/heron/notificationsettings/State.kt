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

package com.tunjid.heron.notificationsettings

import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.NotificationPreferences
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.ui.text.Memo
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class State(
    val notificationPreferences: NotificationPreferences? = null,
    val pendingUpdates: Map<Notification.Reason, NotificationPreferences.Update> = emptyMap(),
    @Transient
    val messages: List<Memo> = emptyList(),
)

fun State.updates() = pendingUpdates.values.toList()

sealed class Action(val key: String) {

    data class UpdateNotificationPreferences(
        val updates: List<NotificationPreferences.Update>,
    ) : Action(key = "UpdateNotificationPreferences")

    data class CacheNotificationPreferenceUpdate(
        val update: NotificationPreferences.Update,
    ) : Action(key = "CacheNotificationPreferenceUpdate")

    data class SnackbarDismissed(
        val message: Memo,
    ) : Action(key = "SnackbarDismissed")

    sealed class Navigate :
        Action(key = "Navigate"),
        NavigationAction {
        data object Pop : Navigate(), NavigationAction by NavigationAction.Pop

        data class To(
            val delegate: NavigationAction.Destination,
        ) : Navigate(),
            NavigationAction by delegate
    }
}

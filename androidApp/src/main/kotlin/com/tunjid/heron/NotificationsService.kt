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

package com.tunjid.heron

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tunjid.heron.data.logging.LogPriority
import com.tunjid.heron.data.logging.logcat
import com.tunjid.heron.data.logging.loggableText
import com.tunjid.heron.scaffold.notifications.AndroidNotifier.Companion.DISMISSAL_ACTION
import com.tunjid.heron.scaffold.notifications.AndroidNotifier.Companion.DISMISSAL_INSTANT_EXTRA
import com.tunjid.heron.scaffold.notifications.NotificationAction
import com.tunjid.heron.scaffold.scaffold.AppState
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class NotificationsService : FirebaseMessagingService() {

    override fun onNewToken(token: String) =
        appState.onNotificationAction(NotificationAction.RegisterToken(token = token))

    override fun onMessageReceived(message: RemoteMessage) {
        val action = NotificationAction.HandleNotification(payload = message.data)
        action.senderDid ?: return
        val recordUri = action.recordUri ?: return

        logcat(LogPriority.DEBUG) {
            "Received notification for $recordUri. Payload: ${message.data}"
        }
        appState.onNotificationAction(action)

        // await processing completion or timeout to prevent the app from being
        // killed due to background execution limits.
        try {
            runBlocking {
                withTimeout(AppState.NOTIFICATION_PROCESSING_TIMEOUT_SECONDS) {
                    appState.awaitNotificationProcessing(recordUri)
                }
            }
        } catch (e: Exception) {
            logcat(LogPriority.WARN) {
                "Notification processing timed out or failed for $recordUri. Cause: ${e.loggableText()}"
            }
        } finally {
            appState.onNotificationAction(
                NotificationAction.NotificationProcessedOrDropped(recordUri),
            )
        }
    }
}

class NotificationDismissReceiver : BroadcastReceiver() {
    @OptIn(ExperimentalTime::class)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DISMISSAL_ACTION) return

        val dismissedAtEpoch = intent.getLongExtra(
            /* name = */
            DISMISSAL_INSTANT_EXTRA,
            /* defaultValue = */
            0,
        )
        if (dismissedAtEpoch > 0) context.appState.onNotificationAction(
            NotificationAction.NotificationDismissed(
                dismissedAt = Instant.fromEpochMilliseconds(dismissedAtEpoch),
            ),
        )
    }
}

private val Context.appState
    get() = (applicationContext as HeronApplication).appState

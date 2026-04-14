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

import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.database.getDatabaseBuilder
import com.tunjid.heron.data.di.DataBindingArgs
import com.tunjid.heron.data.logging.IOSLogger
import com.tunjid.heron.data.logging.LogPriority
import com.tunjid.heron.data.logging.logcat
import com.tunjid.heron.data.logging.loggableText
import com.tunjid.heron.data.repository.SavedStateEncryption
import com.tunjid.heron.images.imageLoader
import com.tunjid.heron.media.video.AVFoundationPlayerController
import com.tunjid.heron.scaffold.notifications.IosNotifier
import com.tunjid.heron.scaffold.notifications.NotificationAction
import com.tunjid.heron.scaffold.scaffold.AppState
import dev.jordond.connectivity.Connectivity
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

fun createAppState(): AppState =
    createAppState(
        imageLoader = ::imageLoader,
        notifier = {
            IosNotifier()
        },
        logger = {
            IOSLogger()
        },
        videoPlayerController = { appMainScope ->
            AVFoundationPlayerController(
                appMainScope = appMainScope,
            )
        },
        args = { appMainScope ->
            DataBindingArgs(
                appMainScope = appMainScope,
                connectivity = Connectivity(),
                savedStatePath = savedStatePath(),
                savedStateFileSystem = FileSystem.SYSTEM,
                savedStateEncryption = SavedStateEncryption.None,
                databaseBuilder = getDatabaseBuilder(),
            )
        },
    )

/**
 * Called from Swift when Firebase provides a new FCM token.
 */
fun onNewFcmToken(appState: AppState, token: String) {
    appState.onNotificationAction(NotificationAction.RegisterToken(token = token))
}

/**
 * Called from Swift when a notification is tapped to deep link into the app.
 */
fun onNotificationTapped(appState: AppState, scheme: String, path: String) {
    appState.onDeepLink(GenericUri("$scheme$path"))
}

/**
 * Called from Swift when a data push notification arrives.
 * Processing runs on a background thread to avoid blocking the main thread.
 * The [onComplete] callback is invoked when processing finishes, and should be
 * used to call the iOS background fetch completion handler.
 */
fun onPushNotificationReceived(
    appState: AppState,
    payload: Map<String, String>,
    onComplete: () -> Unit,
) {
    IosNotificationBridge.handlePushNotification(appState, payload, onComplete)
}

private object IosNotificationBridge {
    fun handlePushNotification(
        appState: AppState,
        payload: Map<String, String>,
        onComplete: () -> Unit,
    ) {
        val action = NotificationAction.HandleNotification(payload = payload)
        action.senderDid ?: return onComplete()
        val recordUri = action.recordUri ?: return onComplete()

        logcat(LogPriority.DEBUG) {
            "Received push notification for $recordUri. Payload: $payload"
        }
        appState.onNotificationAction(action)

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                withTimeout(AppState.NOTIFICATION_PROCESSING_TIMEOUT_SECONDS) {
                    appState.awaitNotificationProcessing(recordUri)
                }
            } catch (e: Exception) {
                logcat(LogPriority.WARN) {
                    "Notification processing timed out or failed for $recordUri. Cause: ${e.loggableText()}"
                }
            } finally {
                appState.onNotificationAction(
                    NotificationAction.NotificationProcessedOrDropped(recordUri),
                )
                onComplete()
                scope.cancel()
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun savedStatePath(): Path {
    val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return (requireNotNull(documentDirectory).path + "/heron").toPath()
}

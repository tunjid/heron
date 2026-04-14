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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.tunjid.heron.scaffold.scaffold.LocalAppState
import kotlin.coroutines.resume
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNUserNotificationCenter

@Composable
actual fun hasNotificationPermissions(): Boolean {
    var hasPermissions by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    LifecycleResumeEffect(scope) {
        val job = scope.launch {
            hasPermissions = getNotificationAuthorizationStatus() == UNAuthorizationStatusAuthorized
        }
        onPauseOrDispose { job.cancel() }
    }

    return hasPermissions
}

@Composable
actual fun notificationPermissionsLauncher(
    onPermissionResult: (Boolean) -> Unit,
): () -> Unit {
    val appState = LocalAppState.current

    var rationale by remember { mutableStateOf<NotificationDialogRationale?>(null) }

    rationale?.let {
        NotificationsRationaleDialog(it) { callingRationale, shouldRequestPermissions ->
            if (shouldRequestPermissions) {
                appState.onNotificationAction(NotificationAction.RequestedNotificationPermission)
                when (callingRationale) {
                    NotificationDialogRationale.GoToSettings -> openAppSettings()
                    NotificationDialogRationale.RequestPermissions -> {
                        requestNotificationPermission { granted ->
                            onPermissionResult(granted)
                        }
                    }
                }
            }
            rationale = null
        }
    }

    val scope = rememberCoroutineScope()

    return remember(appState, scope) {
        {
            scope.launch {
                val status = getNotificationAuthorizationStatus()
                when (status) {
                    UNAuthorizationStatusDenied -> {
                        rationale = NotificationDialogRationale.GoToSettings
                    }
                    UNAuthorizationStatusAuthorized -> {
                        onPermissionResult(true)
                    }
                    else -> {
                        appState.onNotificationAction(NotificationAction.RequestedNotificationPermission)
                        requestNotificationPermission { granted ->
                            onPermissionResult(granted)
                            if (!granted) {
                                rationale = NotificationDialogRationale.GoToSettings
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun getNotificationAuthorizationStatus(): Long =
    suspendCancellableCoroutine { continuation ->
        UNUserNotificationCenter.currentNotificationCenter()
            .getNotificationSettingsWithCompletionHandler { settings ->
                if (!continuation.isActive) return@getNotificationSettingsWithCompletionHandler
                continuation.resume(settings?.authorizationStatus ?: 0L)
            }
    }

@OptIn(ExperimentalForeignApi::class)
private fun requestNotificationPermission(onResult: (Boolean) -> Unit) {
    UNUserNotificationCenter.currentNotificationCenter()
        .requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
        ) { granted, _ ->
            onResult(granted)
        }
}

private fun openAppSettings() {
    val url = NSURL(string = UIApplicationOpenSettingsURLString)
    UIApplication.sharedApplication.openURL(
        url,
        options = emptyMap<Any?, Any>(),
        completionHandler = null,
    )
}

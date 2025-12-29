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

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri as AndroidUri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.tunjid.heron.scaffold.scaffold.LocalAppState

@Composable
actual fun notificationPermissionsLauncher(
    onPermissionResult: (Boolean) -> Unit,
): () -> Unit {
    val activity = LocalActivity.current
    val appState = LocalAppState.current

    var shouldShowRationaleDialog by remember { mutableStateOf(false) }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionRequestLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { hasPermissions ->
                onPermissionResult(hasPermissions)
                if (!activity.shouldShowRationale() && !hasPermissions) {
                    activity.maybeOpenAppSettings()
                }
            },
        )
        if (shouldShowRationaleDialog) NotificationsRationaleDialog { shouldRequestPermissions ->
            if (shouldRequestPermissions) {
                appState.onNotificationAction(NotificationAction.RequestedNotificationPermission)
                permissionRequestLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            shouldShowRationaleDialog = false
        }
        return remember(permissionRequestLauncher, appState) {
            {
                if (activity.shouldShowRationale()) {
                    shouldShowRationaleDialog = true
                } else {
                    appState.onNotificationAction(NotificationAction.RequestedNotificationPermission)
                    permissionRequestLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    } else return EmptyLambda
}

@Composable
actual fun hasNotificationPermissions(): Boolean {
    val context = LocalContext.current
    var hasPermissions by remember { mutableStateOf(false) }

    LifecycleResumeEffect(context) {
        hasPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        onPauseOrDispose { }
    }

    return hasPermissions
}


private fun Activity?.shouldShowRationale(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
    if (this == null) return false
    return ActivityCompat.shouldShowRequestPermissionRationale(
        this,
        Manifest.permission.POST_NOTIFICATIONS,
    )
}

private fun Activity?.maybeOpenAppSettings() {
    this ?: return
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = AndroidUri.fromParts("package", packageName, null)
    }
    startActivity(intent)
}

private val EmptyLambda: () -> Unit = {}

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

package com.tunjid.heron.data.tasks

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.app.NotificationCompat

/** The progress notification shown while a transfer runs (a FGS notification, or a UIDT job notification). */
internal object TransferNotifications {

    const val ChannelId = "heron.transfers"

    // Mirrors the in-app deep links AndroidNotifier builds in :ui:scaffold.
    private const val DeepLinkActivity = "com.tunjid.heron.MainActivity"
    private const val DeepLinkScheme = "at"

    suspend fun Context.ensureChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                ChannelId,
                backgroundTaskDescriptor.channelName(),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    fun Context.progressNotification(
        id: TaskId,
        description: TaskDescription,
        progress: Progress?,
    ): Notification =
        NotificationCompat.Builder(this, ChannelId)
            .setContentTitle(description.title)
            .setContentText(description.subtitle)
            .setSmallIcon(backgroundTaskNotificationIcon)
            .setContentIntent(
                contentIntent(
                    id = id,
                    destination = description.destination,
                ),
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .apply {
                if (progress != null && progress.totalBytes > 0L) {
                    setProgress(100, (progress.fraction * 100).toInt(), false)
                    // The visible bar is percent-scaled (an Int, which a multi-GB total overflows), so
                    // also stash the raw byte counts in the extras for [progress] to read back exactly.
                    addExtras(
                        Bundle().apply {
                            putLong(KeyCompletedBytes, progress.completedBytes)
                            putLong(KeyTotalBytes, progress.totalBytes)
                        },
                    )
                } else {
                    setProgress(0, 0, true)
                }
            }
            .build()

    private fun Context.contentIntent(
        id: TaskId,
        destination: String?,
    ): PendingIntent? {
        val intent = when (destination) {
            null -> packageManager.getLaunchIntentForPackage(packageName)
            else -> Intent().apply {
                component = ComponentName(packageName, DeepLinkActivity)
                data = Uri.Builder()
                    .scheme(DeepLinkScheme)
                    .path(destination)
                    .build()
            }
        } ?: return null
        return PendingIntent.getActivity(
            /* context = */
            this,
            /* requestCode = */
            notificationId(id),
            /* intent = */
            intent,
            /* flags = */
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun notificationId(
        id: TaskId,
    ): Int = id.value.hashCode()

    fun Context.notificationProgress(
        id: TaskId,
    ): Progress? {
        val manager = getSystemService(NotificationManager::class.java)
        val extras = manager.activeNotifications
            .firstOrNull { it.id == notificationId(id) }
            ?.notification
            ?.extras
            ?: return null
        val total = extras.getLong(KeyTotalBytes, 0L)
        return if (total <= 0L) null
        else Progress(
            completedBytes = extras.getLong(KeyCompletedBytes, 0L),
            totalBytes = total,
        )
    }
}

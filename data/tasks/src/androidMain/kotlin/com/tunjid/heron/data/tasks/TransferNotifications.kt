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
import android.content.Context
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.tunjid.heron.data.tasks.TransferNotifications.build

/** The progress notification shown while a transfer runs (a FGS notification, or a UIDT job notification). */
internal object TransferNotifications {

    const val ChannelId = "heron.transfers"

    fun ensureChannel(
        context: Context,
    ) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(ChannelId) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    ChannelId,
                    "Downloads",
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
    }

    fun build(
        context: Context,
        title: String,
        progress: Progress?,
    ): Notification {
        ensureChannel(context)
        return NotificationCompat.Builder(context, ChannelId)
            .setContentTitle(title)
            .setSmallIcon(android.R.drawable.stat_sys_download)
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
    }

    /** A stable notification id per task, so progress updates replace rather than stack. */
    fun notificationId(
        id: TaskId,
    ): Int = id.value.hashCode()

    /**
     * Reads a running transfer's [Progress] back from its own active notification — the byte counts
     * [build] stashed in the notification extras — or `null` when there is no live notification or its
     * total is unknown. This is how the UIDT delegate observes progress: a `JobService` job has no
     * progress channel of its own, but its notification is queryable in-process.
     */
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

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
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.domain.navigation.R
import heron.scaffold.generated.resources.Res
import heron.scaffold.generated.resources.notification_channel_likes
import heron.scaffold.generated.resources.notification_channel_likes_repost
import heron.scaffold.generated.resources.notification_channel_mentions
import heron.scaffold.generated.resources.notification_channel_misc
import heron.scaffold.generated.resources.notification_channel_new_followers
import heron.scaffold.generated.resources.notification_channel_quotes
import heron.scaffold.generated.resources.notification_channel_replies
import heron.scaffold.generated.resources.notification_channel_repost_reposts
import heron.scaffold.generated.resources.notification_channel_reposts
import org.jetbrains.compose.resources.getString

class AndroidNotifier(
    private val context: Context,
) : Notifier {

    private val notificationManager = NotificationManagerCompat.from(context)

    override suspend fun displayNotifications(notifications: List<Notification>) {
        val currentLifecycleState = ProcessLifecycleOwner.get().lifecycle.currentStateFlow.value

        // Show notifications in the background only
        if (currentLifecycleState.isAtLeast(Lifecycle.State.RESUMED)) return

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notifications.forEach { notification ->
                notification.createChannelIfNeeded()

                val builder = NotificationCompat.Builder(
                    context,
                    notification.channelId,
                ).apply {
                    setSmallIcon(R.drawable.ic_heron_notification)
                    setContentTitle(notification.title())
                    notification.body()?.let(::setContentText)
                    setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    setAutoCancel(true)
                }

                notificationManager.notify(
                    /* tag = */
                    notification.uri.toString(),
                    /* id = */
                    notification.uri.uri.hashCode(),
                    /* notification = */
                    builder.build(),
                )
            }
        }
    }

    private suspend fun Notification.createChannelIfNeeded() {
        if (notificationManager.getNotificationChannel(channelId) == null) {
            val name = channelName()
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance)
            notificationManager.createNotificationChannel(channel)
        }
    }
}

private val Notification.channelId: String
    get() = when (this) {
        is Notification.Liked.Post -> "channel-liked-post"
        is Notification.Liked.Repost -> "channel-liked-repost"
        is Notification.Reposted.Post -> "channel-reposted-post"
        is Notification.Reposted.Repost -> "channel-reposted-repost"
        is Notification.Followed -> "channel-followed"
        is Notification.Mentioned -> "channel-mentioned"
        is Notification.RepliedTo -> "channel-replied"
        is Notification.Quoted -> "channel-quoted"
        is Notification.JoinedStarterPack,
        is Notification.SubscribedPost,
        is Notification.Verified,
        is Notification.Unverified,
        is Notification.Unknown,
        -> "channel-misc"
    }

private suspend fun Notification.channelName(): String =
    when (this) {
        is Notification.Liked.Post -> getString(Res.string.notification_channel_likes)
        is Notification.Liked.Repost -> getString(Res.string.notification_channel_likes_repost)
        is Notification.Reposted.Post -> getString(Res.string.notification_channel_reposts)
        is Notification.Reposted.Repost -> getString(Res.string.notification_channel_repost_reposts)
        is Notification.Followed -> getString(Res.string.notification_channel_new_followers)
        is Notification.Mentioned -> getString(Res.string.notification_channel_mentions)
        is Notification.RepliedTo -> getString(Res.string.notification_channel_replies)
        is Notification.Quoted -> getString(Res.string.notification_channel_quotes)
        is Notification.JoinedStarterPack,
        is Notification.SubscribedPost,
        is Notification.Verified,
        is Notification.Unverified,
        is Notification.Unknown,
        -> getString(Res.string.notification_channel_misc)
    }

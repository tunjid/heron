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
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.ui.text.CommonStrings
import heron.ui.core.generated.resources.notifications_account_unverified
import heron.ui.core.generated.resources.notifications_account_verified
import heron.ui.core.generated.resources.notifications_followed_you
import heron.ui.core.generated.resources.notifications_joined_from_your_starter_pack
import heron.ui.core.generated.resources.notifications_liked_your_post
import heron.ui.core.generated.resources.notifications_liked_your_repost
import heron.ui.core.generated.resources.notifications_mentioned_you
import heron.ui.core.generated.resources.notifications_post_subscription_description
import heron.ui.core.generated.resources.notifications_quoted_you
import heron.ui.core.generated.resources.notifications_replied_to_you
import heron.ui.core.generated.resources.notifications_reposted_your_post
import heron.ui.core.generated.resources.notifications_reposted_your_repost
import org.jetbrains.compose.resources.getString

interface Notifier {
    suspend fun displayNotifications(
        notifications: List<Notification>,
    )
}

object NoOpNotifier : Notifier {
    override suspend fun displayNotifications(
        notifications: List<Notification>,
    ) = Unit
}

@Composable
expect fun hasNotificationPermissions(): Boolean

@Composable
expect fun requestNotificationPermissions(
    onPermissionResult: (Boolean) -> Unit,
): () -> Unit

internal suspend fun Notification.title(): String = when (this) {
    is Notification.Liked.Post -> getString(
        CommonStrings.notifications_liked_your_post,
        author.nameOrHandle,
    )
    is Notification.Liked.Repost -> getString(
        CommonStrings.notifications_liked_your_repost,
        author.nameOrHandle,
    )
    is Notification.Reposted.Post -> getString(
        CommonStrings.notifications_reposted_your_post,
        author.nameOrHandle,
    )
    is Notification.Reposted.Repost -> getString(
        CommonStrings.notifications_reposted_your_repost,
        author.nameOrHandle,
    )
    is Notification.Followed -> getString(
        CommonStrings.notifications_followed_you,
        author.nameOrHandle,
    )
    is Notification.Mentioned -> getString(
        CommonStrings.notifications_mentioned_you,
        author.nameOrHandle,
    )
    is Notification.RepliedTo -> getString(
        CommonStrings.notifications_replied_to_you,
        author.nameOrHandle,
    )
    is Notification.Quoted -> getString(
        CommonStrings.notifications_quoted_you,
        author.nameOrHandle,
    )
    is Notification.JoinedStarterPack -> getString(
        CommonStrings.notifications_joined_from_your_starter_pack,
        author.nameOrHandle,
    )
    is Notification.SubscribedPost -> getString(CommonStrings.notifications_post_subscription_description)
    is Notification.Verified -> getString(CommonStrings.notifications_account_verified)
    is Notification.Unverified -> getString(CommonStrings.notifications_account_unverified)
    is Notification.Unknown -> "You have a new notification"
}

internal fun Notification.body(): String? = when (this) {
    is Notification.Followed -> null
    is Notification.JoinedStarterPack -> null
    is Notification.PostAssociated -> associatedPost.record?.text
    is Notification.Unknown -> null
    is Notification.Verified -> null
    is Notification.Unverified -> null
}

private val Profile.nameOrHandle
    get() = displayName ?: handle.id

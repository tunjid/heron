package com.tunjid.heron.notificationsettings.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Interests
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.NotificationPreferences
import heron.feature.notification_settings.generated.resources.Res
import heron.feature.notification_settings.generated.resources.notif_activity_from_others_desc
import heron.feature.notification_settings.generated.resources.notif_activity_from_others_title
import heron.feature.notification_settings.generated.resources.notif_everything_else_desc
import heron.feature.notification_settings.generated.resources.notif_everything_else_title
import heron.feature.notification_settings.generated.resources.notif_likes_desc
import heron.feature.notification_settings.generated.resources.notif_likes_title
import heron.feature.notification_settings.generated.resources.notif_likes_via_repost_desc
import heron.feature.notification_settings.generated.resources.notif_likes_via_repost_title
import heron.feature.notification_settings.generated.resources.notif_mentions_desc
import heron.feature.notification_settings.generated.resources.notif_mentions_title
import heron.feature.notification_settings.generated.resources.notif_new_followers_desc
import heron.feature.notification_settings.generated.resources.notif_new_followers_title
import heron.feature.notification_settings.generated.resources.notif_quotes_desc
import heron.feature.notification_settings.generated.resources.notif_quotes_title
import heron.feature.notification_settings.generated.resources.notif_replies_desc
import heron.feature.notification_settings.generated.resources.notif_replies_title
import heron.feature.notification_settings.generated.resources.notif_reposts_desc
import heron.feature.notification_settings.generated.resources.notif_reposts_title
import heron.feature.notification_settings.generated.resources.notif_reposts_via_repost_desc
import heron.feature.notification_settings.generated.resources.notif_reposts_via_repost_title
import org.jetbrains.compose.resources.StringResource

fun NotificationPreferences?.toNotificationSettingItems(): List<NotificationSettingItem> =
    this?.let { prefs ->
        listOf(
            NotificationSettingItem.Filterable(
                title = Res.string.notif_likes_title,
                icon = Icons.Rounded.Favorite,
                description = Res.string.notif_likes_desc,
                preference = prefs.like,
                reason = Notification.Reason.Like,
            ),
            NotificationSettingItem.Filterable(
                title = Res.string.notif_new_followers_title,
                icon = Icons.Rounded.PersonAdd,
                description = Res.string.notif_new_followers_desc,
                preference = prefs.follow,
                reason = Notification.Reason.Follow,
            ),
            NotificationSettingItem.Filterable(
                title = Res.string.notif_replies_title,
                icon = Icons.Rounded.ChatBubble,
                description = Res.string.notif_replies_desc,
                preference = prefs.reply,
                reason = Notification.Reason.Reply,
            ),
            NotificationSettingItem.Filterable(
                title = Res.string.notif_mentions_title,
                icon = Icons.Rounded.AlternateEmail,
                description = Res.string.notif_mentions_desc,
                preference = prefs.mention,
                reason = Notification.Reason.Mention,
            ),
            NotificationSettingItem.Filterable(
                title = Res.string.notif_quotes_title,
                icon = Icons.Rounded.FormatQuote,
                description = Res.string.notif_quotes_desc,
                preference = prefs.quote,
                reason = Notification.Reason.Quote,
            ),
            NotificationSettingItem.Filterable(
                title = Res.string.notif_reposts_title,
                icon = Icons.Rounded.Repeat,
                description = Res.string.notif_reposts_desc,
                preference = prefs.repost,
                reason = Notification.Reason.Repost,
            ),
            NotificationSettingItem.ActivityFromOthers(
                title = Res.string.notif_activity_from_others_title,
                icon = Icons.Rounded.NotificationsActive,
                description = Res.string.notif_activity_from_others_desc,
                preferences = listOf(
                    prefs.verified,
                    prefs.unverified,
                ),
            ),
            NotificationSettingItem.Filterable(
                title = Res.string.notif_likes_via_repost_title,
                icon = Icons.Rounded.Repeat,
                description = Res.string.notif_likes_via_repost_desc,
                preference = prefs.likeViaRepost,
                reason = Notification.Reason.LikeViaRepost,
            ),
            NotificationSettingItem.Filterable(
                title = Res.string.notif_reposts_via_repost_title,
                icon = Icons.Rounded.RepeatOne,
                description = Res.string.notif_reposts_via_repost_desc,
                preference = prefs.repostViaRepost,
                reason = Notification.Reason.RepostViaRepost,
            ),
            NotificationSettingItem.EverythingElse(
                title = Res.string.notif_everything_else_title,
                icon = Icons.Rounded.Interests,
                description = Res.string.notif_everything_else_desc,
                preferences = listOf(
                    prefs.joinedStarterPack,
                    prefs.subscribedPost,
                ),
            ),
        )
    } ?: emptyList()

@Stable
sealed class NotificationSettingItem {

    abstract val title: StringResource
    abstract val icon: ImageVector
    abstract val description: StringResource

    data class Filterable(
        override val title: StringResource,
        override val icon: ImageVector,
        override val description: StringResource,
        val preference: NotificationPreferences.Preference.Filterable,
        val reason: Notification.Reason,
    ) : NotificationSettingItem()

    data class EverythingElse(
        override val title: StringResource,
        override val icon: ImageVector,
        override val description: StringResource,
        val preferences: List<NotificationPreferences.Preference.Simple>,
    ) : NotificationSettingItem()

    data class ActivityFromOthers(
        override val title: StringResource,
        override val icon: ImageVector,
        override val description: StringResource,
        val preferences: List<NotificationPreferences.Preference.Simple>,
    ) : NotificationSettingItem()
}

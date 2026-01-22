package com.tunjid.heron.data.utilities.preferenceupdater

import app.bsky.notification.FilterablePreferenceInclude
import app.bsky.notification.Preferences
import com.tunjid.heron.data.core.models.NotificationPreferences
import com.tunjid.heron.data.repository.SavedState
import dev.zacsweers.metro.Inject

interface NotificationPreferenceUpdater {

    suspend fun update(
        notificationPreferences: Preferences,
        notifications: SavedState.Notifications,
    ): SavedState.Notifications
}

internal class ThingNotificationPreferenceUpdater @Inject constructor() : NotificationPreferenceUpdater {

    override suspend fun update(
        notificationPreferences: Preferences,
        notifications: SavedState.Notifications,
    ): SavedState.Notifications {
        val updatedNotificationPrefs = NotificationPreferences(
            follow = NotificationPreferences.Preference.Filterable(
                include = FilterablePreferenceInclude.safeValueOf(notificationPreferences.follow.include.value).asExternalModel(),
                list = notificationPreferences.follow.list,
                push = notificationPreferences.follow.push,
            ),
            like = NotificationPreferences.Preference.Filterable(
                include = FilterablePreferenceInclude.safeValueOf(notificationPreferences.like.include.value).asExternalModel(),
                list = notificationPreferences.like.list,
                push = notificationPreferences.like.push,
            ),
            likeViaRepost = NotificationPreferences.Preference.Filterable(
                include = FilterablePreferenceInclude.safeValueOf(notificationPreferences.likeViaRepost.include.value).asExternalModel(),
                list = notificationPreferences.likeViaRepost.list,
                push = notificationPreferences.likeViaRepost.push,
            ),
            mention = NotificationPreferences.Preference.Filterable(
                include = FilterablePreferenceInclude.safeValueOf(notificationPreferences.mention.include.value).asExternalModel(),
                list = notificationPreferences.mention.list,
                push = notificationPreferences.mention.push,
            ),
            quote = NotificationPreferences.Preference.Filterable(
                include = FilterablePreferenceInclude.safeValueOf(notificationPreferences.quote.include.value).asExternalModel(),
                list = notificationPreferences.quote.list,
                push = notificationPreferences.quote.push,
            ),
            reply = NotificationPreferences.Preference.Filterable(
                include = FilterablePreferenceInclude.safeValueOf(notificationPreferences.reply.include.value).asExternalModel(),
                list = notificationPreferences.reply.list,
                push = notificationPreferences.reply.push,
            ),
            repost = NotificationPreferences.Preference.Filterable(
                include = FilterablePreferenceInclude.safeValueOf(notificationPreferences.repost.include.value).asExternalModel(),
                list = notificationPreferences.repost.list,
                push = notificationPreferences.repost.push,
            ),
            repostViaRepost = NotificationPreferences.Preference.Filterable(
                include = FilterablePreferenceInclude.safeValueOf(notificationPreferences.repostViaRepost.include.value).asExternalModel(),
                list = notificationPreferences.repostViaRepost.list,
                push = notificationPreferences.repostViaRepost.push,
            ),
            joinedStarterPack = NotificationPreferences.Preference.Simple(
                list = notificationPreferences.starterpackJoined.list,
                push = notificationPreferences.starterpackJoined.push,
            ),
            subscribedPost = NotificationPreferences.Preference.Simple(
                list = notificationPreferences.subscribedPost.list,
                push = notificationPreferences.subscribedPost.push,
            ),
            unverified = NotificationPreferences.Preference.Simple(
                list = notificationPreferences.unverified.list,
                push = notificationPreferences.unverified.push,
            ),
            verified = NotificationPreferences.Preference.Simple(
                list = notificationPreferences.verified.list,
                push = notificationPreferences.verified.push,
            ),
        )
        return notifications.copy(preferences = updatedNotificationPrefs)
    }

    /** Map the lexicon Include to enum model Include */
    private fun FilterablePreferenceInclude.asExternalModel(): NotificationPreferences.Include =
        when (this) {
            FilterablePreferenceInclude.All -> NotificationPreferences.Include.All
            FilterablePreferenceInclude.Follows -> NotificationPreferences.Include.Follows
            is FilterablePreferenceInclude.Unknown -> NotificationPreferences.Include.Unknown
        }
}

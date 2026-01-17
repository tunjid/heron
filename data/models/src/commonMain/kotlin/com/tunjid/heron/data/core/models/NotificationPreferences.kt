package com.tunjid.heron.data.core.models

import kotlinx.serialization.Serializable

@Serializable
data class NotificationPreferences(
    val follow: Preference.Filterable,
    val like: Preference.Filterable,
    val likeViaRepost: Preference.Filterable,
    val mention: Preference.Filterable,
    val quote: Preference.Filterable,
    val reply: Preference.Filterable,
    val repost: Preference.Filterable,
    val repostViaRepost: Preference.Filterable,
    val joinedStarterPack: Preference.Simple,
    val subscribedPost: Preference.Simple,
    val unverified: Preference.Simple,
    val verified: Preference.Simple,
) {

    @Serializable
    sealed class Preference {

        abstract val list: Boolean
        abstract val push: Boolean

        fun allowsList() = list
        fun allowsPush() = push

        @Serializable
        data class Simple(
            override val list: Boolean,
            override val push: Boolean,
        ) : Preference()

        @Serializable
        data class Filterable(
            val include: Include,
            override val list: Boolean,
            override val push: Boolean,
        ) : Preference() {

            fun allowsAuthor(isFollowed: Boolean): Boolean =
                when (include) {
                    Include.All -> true
                    Include.Follows -> isFollowed
                    Include.Unknown -> false
                }

            @Serializable
            enum class Include(val value: String) {
                All("all"),
                Follows("follows"),
                Unknown("unknown"),
            }
        }
    }

    @Serializable
    sealed class Update {
        abstract val reason: Reason

        @Serializable
        data class Filterable(
            override val reason: Reason,
            val preference: Preference.Filterable,
        ) : Update()

        @Serializable
        data class Simple(
            override val reason: Reason,
            val preference: Preference.Simple,
        ) : Update()

        @Serializable
        sealed class Reason(val apiValue: String) {
            object Follow : Reason("follow")
            object Like : Reason("like")
            object LikeViaRepost : Reason("likeViaRepost")
            object Mention : Reason("mention")
            object Quote : Reason("quote")
            object Reply : Reason("reply")
            object Repost : Reason("repost")
            object RepostViaRepost : Reason("repostViaRepost")
            object StarterpackJoined : Reason("starterpackJoined")
            object SubscribedPost : Reason("subscribedPost")
            object Unverified : Reason("unverified")
            object Verified : Reason("verified")
        }
    }
}

// Helper to check if notification should be shown
fun NotificationPreferences.shouldShowNotification(
    reason: Notification.Reason,
    isAuthorFollowed: Boolean = false,
): Boolean = when (val pref = forReason(reason)) {
    is NotificationPreferences.Preference.Filterable ->
        pref.allowsPush() && pref.allowsAuthor(isAuthorFollowed)
    is NotificationPreferences.Preference.Simple ->
        pref.allowsPush()
    null -> false
}

fun NotificationPreferences.forReason(
    reason: Notification.Reason,
): NotificationPreferences.Preference? = when (reason) {
    Notification.Reason.Follow -> follow
    Notification.Reason.Like -> like
    Notification.Reason.LikeViaRepost -> likeViaRepost
    Notification.Reason.Mention -> mention
    Notification.Reason.Quote -> quote
    Notification.Reason.Reply -> reply
    Notification.Reason.Repost -> repost
    Notification.Reason.RepostViaRepost -> repostViaRepost
    Notification.Reason.JoinedStarterPack -> joinedStarterPack
    Notification.Reason.SubscribedPost -> subscribedPost
    Notification.Reason.Verified -> verified
    Notification.Reason.Unverified -> unverified
    Notification.Reason.Unknown -> null
}

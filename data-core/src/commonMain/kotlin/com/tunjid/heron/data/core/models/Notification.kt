package com.tunjid.heron.data.core.models

import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable


@Serializable
data class Notification(
    val uri: Uri,
    val cid: Id,
    val author: Profile,
    val reason: Reason,
    val reasonSubject: Uri?,
    val content: Content?,
    val isRead: Boolean,
    val indexedAt: Instant,
) {
    sealed interface Content {
        data class Liked(
            val post: Post,
        ) : Content

        data class Reposted(
            val post: Post,
        ) : Content

        data object Followed : Content

        data class Mentioned(
            val post: Post,
        ) : Content

        data class RepliedTo(
            val post: Post,
        ) : Content

        data class Quoted(
            val post: Post,
        ) : Content

        data object JoinedStarterPack : Content
    }

    enum class Reason {
        Unknown,
        Like,
        Repost,
        Follow,
        Mention,
        Reply,
        Quote,
        JoinedStarterPack,
    }
}
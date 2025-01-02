package com.tunjid.heron.data.core.models

import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable


@Serializable
sealed class Notification {

    abstract val uri: Uri
    abstract val cid: Id
    abstract val author: Profile
    abstract val reasonSubject: Uri?
    abstract val isRead: Boolean
    abstract val indexedAt: Instant

    data class Liked(
        override val uri: Uri,
        override val cid: Id,
        override val author: Profile,
        override val reasonSubject: Uri?,
        override val isRead: Boolean,
        override val indexedAt: Instant,
        val associatedPost: Post,
    ) : Notification()

    data class Reposted(
        override val uri: Uri,
        override val cid: Id,
        override val author: Profile,
        override val reasonSubject: Uri?,
        override val isRead: Boolean,
        override val indexedAt: Instant,
        val associatedPost: Post,
    ) : Notification()

    data class Followed(
        override val uri: Uri,
        override val cid: Id,
        override val author: Profile,
        override val reasonSubject: Uri?,
        override val isRead: Boolean,
        override val indexedAt: Instant,
    ) : Notification()

    data class Mentioned(
        override val uri: Uri,
        override val cid: Id,
        override val author: Profile,
        override val reasonSubject: Uri?,
        override val isRead: Boolean,
        override val indexedAt: Instant,
        val associatedPost: Post,
    ) : Notification()

    data class RepliedTo(
        override val uri: Uri,
        override val cid: Id,
        override val author: Profile,
        override val reasonSubject: Uri?,
        override val isRead: Boolean,
        override val indexedAt: Instant,
        val associatedPost: Post,
    ) : Notification()

    data class Quoted(
        override val uri: Uri,
        override val cid: Id,
        override val author: Profile,
        override val reasonSubject: Uri?,
        override val isRead: Boolean,
        override val indexedAt: Instant,
        val associatedPost: Post,
    ) : Notification()

    data class JoinedStarterPack(
        override val uri: Uri,
        override val cid: Id,
        override val author: Profile,
        override val reasonSubject: Uri?,
        override val isRead: Boolean,
        override val indexedAt: Instant,
    ) : Notification()

    data class Unknown(
        override val uri: Uri,
        override val cid: Id,
        override val author: Profile,
        override val reasonSubject: Uri?,
        override val isRead: Boolean,
        override val indexedAt: Instant,
    ) : Notification()


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
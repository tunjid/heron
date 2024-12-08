package com.tunjid.heron.data.network.models

import app.bsky.feed.FeedViewPost
import app.bsky.feed.FeedViewPostReasonUnion
import app.bsky.feed.ReplyRefParentUnion
import app.bsky.feed.ReplyRefRootUnion
import com.tunjid.heron.data.core.models.Constants
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.entities.FeedItemEntity
import com.tunjid.heron.data.database.entities.FeedReplyEntity
import com.tunjid.heron.data.database.entities.PostEntity
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.emptyPostEntity
import kotlinx.datetime.Instant

internal fun FeedViewPost.feedItemEntity(
    source: Uri,
) = FeedItemEntity(
    postId = Id(post.cid.cid),
    source = source,
    reply = reply?.let {
        FeedReplyEntity(
            rootPostId = it.root.postEntity().cid,
            parentPostId = it.parent.postEntity().cid,
        )
    },
    reposter = when (val reason = reason) {
        is FeedViewPostReasonUnion.ReasonRepost -> Id(reason.value.by.did.did)
        else -> null
    },
    isPinned = reason is FeedViewPostReasonUnion.ReasonPin,
    indexedAt = when (val reason = reason) {
        is FeedViewPostReasonUnion.ReasonPin -> Instant.DISTANT_PAST
        is FeedViewPostReasonUnion.ReasonRepost -> reason.value.indexedAt
        is FeedViewPostReasonUnion.Unknown,
        null -> post.indexedAt
    },
)

internal fun ReplyRefRootUnion.postEntity() = when (this) {
    is ReplyRefRootUnion.BlockedPost -> emptyPostEntity(
        id = Constants.blockedPostId,
    )

    is ReplyRefRootUnion.NotFoundPost -> emptyPostEntity(
        id = Constants.notFoundPostId,
    )

    is ReplyRefRootUnion.PostView -> PostEntity(
        cid = Id(value.cid.cid),
        uri = Uri(value.uri.atUri),
        authorId = Id(value.author.did.did),
        replyCount = value.replyCount,
        repostCount = value.repostCount,
        likeCount = value.likeCount,
        quoteCount = value.quoteCount,
        indexedAt = value.indexedAt,
    )

    is ReplyRefRootUnion.Unknown -> emptyPostEntity(
        id = Constants.unknownPostId,
    )
}

internal fun ReplyRefParentUnion.postEntity() = when (this) {
    is ReplyRefParentUnion.BlockedPost -> emptyPostEntity(
        id = Constants.blockedPostId,
    )

    is ReplyRefParentUnion.NotFoundPost -> emptyPostEntity(
        id = Constants.notFoundPostId,
    )

    is ReplyRefParentUnion.PostView -> PostEntity(
        cid = Id(value.cid.cid),
        uri = Uri(value.uri.atUri),
        authorId = Id(value.author.did.did),
        replyCount = value.replyCount,
        repostCount = value.repostCount,
        likeCount = value.likeCount,
        quoteCount = value.quoteCount,
        indexedAt = value.indexedAt,
    )

    is ReplyRefParentUnion.Unknown -> emptyPostEntity(
        id = Constants.unknownPostId,
    )
}

internal fun FeedViewPostReasonUnion.profileEntity() =
    when (this) {
        is FeedViewPostReasonUnion.ReasonRepost -> ProfileEntity(
            did = Id(value.by.did.did),
            handle = Id(value.by.handle.handle),
            displayName = value.by.displayName,
            description = null,
            avatar = value.by.avatar?.uri?.let(::Uri),
            banner = null,
            followersCount = 0,
            followsCount = 0,
            postsCount = 0,
            joinedViaStarterPack = null,
            indexedAt = null,
            createdAt = value.by.createdAt,
        )

        else -> null
    }

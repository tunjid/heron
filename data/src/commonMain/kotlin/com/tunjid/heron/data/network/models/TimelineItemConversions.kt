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

package com.tunjid.heron.data.network.models

import app.bsky.feed.FeedViewPost
import app.bsky.feed.FeedViewPostReasonUnion
import app.bsky.feed.PostViewEmbedUnion
import app.bsky.feed.ReplyRefParentUnion
import app.bsky.feed.ReplyRefRootUnion
import com.tunjid.heron.data.core.models.Constants
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.entities.FeedReplyEntity
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.TimelineItemEntity
import com.tunjid.heron.data.database.entities.emptyPostEntity
import kotlinx.datetime.Instant

internal fun FeedViewPost.feedItemEntity(
    sourceId: String,
) = TimelineItemEntity(
    postId = Id(post.cid.cid),
    sourceId = sourceId,
    reply = reply?.let {
        FeedReplyEntity(
            rootPostId = it.root.postEntity().cid,
            parentPostId = it.parent.postEntity().cid,
            grandParentPostAuthorId = it.grandparentAuthor?.did?.did?.let(::Id),
        )
    },
    reposter = when (val reason = reason) {
        is FeedViewPostReasonUnion.ReasonRepost -> Id(reason.value.by.did.did)
        else -> null
    },
    isPinned = reason is FeedViewPostReasonUnion.ReasonPin,
    hasMedia = when (post.embed) {
        is PostViewEmbedUnion.ExternalView -> false
        is PostViewEmbedUnion.ImagesView -> true
        is PostViewEmbedUnion.RecordView -> false
        is PostViewEmbedUnion.RecordWithMediaView -> true
        is PostViewEmbedUnion.Unknown -> false
        is PostViewEmbedUnion.VideoView -> true
        null -> false
    },
    indexedAt = when (val reason = reason) {
        is FeedViewPostReasonUnion.ReasonPin -> Instant.DISTANT_PAST
        is FeedViewPostReasonUnion.ReasonRepost -> reason.value.indexedAt
        is FeedViewPostReasonUnion.Unknown,
        null,
            -> post.indexedAt
    },
)

internal fun ReplyRefRootUnion.profileEntity() = when (this) {
    is ReplyRefRootUnion.PostView -> value.profileEntity()
    is ReplyRefRootUnion.BlockedPost,
    is ReplyRefRootUnion.NotFoundPost,
    is ReplyRefRootUnion.Unknown,
        -> null
}

internal fun ReplyRefParentUnion.profileEntity() = when (this) {
    is ReplyRefParentUnion.PostView -> value.profileEntity()
    is ReplyRefParentUnion.BlockedPost,
    is ReplyRefParentUnion.NotFoundPost,
    is ReplyRefParentUnion.Unknown,
        -> null
}

internal fun ReplyRefRootUnion.postEntity() = when (val ref = this) {
    is ReplyRefRootUnion.BlockedPost -> emptyPostEntity(
        id = Constants.blockedPostId,
        uri = ref.value.uri.atUri.let(::Uri),
        authorId = ref.value.author.did.did.let(::Id),
    )

    is ReplyRefRootUnion.NotFoundPost -> emptyPostEntity(
        id = Constants.notFoundPostId,
        uri = ref.value.uri.atUri.let(::Uri),
        authorId = Constants.unknownAuthorId,
    )

    is ReplyRefRootUnion.PostView -> ref.value.postEntity()

    is ReplyRefRootUnion.Unknown -> emptyPostEntity(
        id = Constants.unknownPostId,
        uri = Constants.unknownPostUri,
        authorId = Constants.unknownAuthorId,
    )
}

internal fun ReplyRefParentUnion.postEntity() = when (val ref = this) {
    is ReplyRefParentUnion.BlockedPost -> emptyPostEntity(
        id = Constants.blockedPostId,
        uri = ref.value.uri.atUri.let(::Uri),
        authorId = ref.value.author.did.did.let(::Id),
    )

    is ReplyRefParentUnion.NotFoundPost -> emptyPostEntity(
        id = Constants.notFoundPostId,
        uri = ref.value.uri.atUri.let(::Uri),
        authorId = Constants.unknownAuthorId,
    )

    is ReplyRefParentUnion.PostView -> ref.value.postEntity()

    is ReplyRefParentUnion.Unknown -> emptyPostEntity(
        id = Constants.unknownPostId,
        uri = Constants.unknownPostUri,
        authorId = Constants.unknownAuthorId,
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

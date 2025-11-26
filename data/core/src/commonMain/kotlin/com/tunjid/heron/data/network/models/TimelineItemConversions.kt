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

import app.bsky.embed.RecordViewRecordUnion
import app.bsky.feed.FeedViewPost
import app.bsky.feed.FeedViewPostReasonUnion
import app.bsky.feed.PostViewEmbedUnion
import app.bsky.feed.ReplyRefParentUnion
import app.bsky.feed.ReplyRefRootUnion
import com.tunjid.heron.data.core.models.Constants
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.StarterPackUri
import com.tunjid.heron.data.database.entities.FeedReplyEntity
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.TimelineItemEntity
import com.tunjid.heron.data.database.entities.stubPostEntity
import kotlinx.datetime.Instant

internal fun FeedViewPost.feedItemEntity(
    sourceId: String,
    viewingProfileId: ProfileId?,
) = TimelineItemEntity(
    postUri = PostUri(post.uri.atUri),
    viewingProfileId = viewingProfileId,
    sourceId = sourceId,
    reply = reply?.let {
        FeedReplyEntity(
            rootPostUri = it.root.postEntity().uri,
            parentPostUri = it.parent.postEntity().uri,
            grandParentPostAuthorId = it.grandparentAuthor?.did?.did?.let(::ProfileId),
        )
    },
    reposter = when (val reason = reason) {
        is FeedViewPostReasonUnion.ReasonRepost -> ProfileId(reason.value.by.did.did)
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
    embeddedRecordUri = when (val embed = post.embed) {
        is PostViewEmbedUnion.RecordView -> when (val recordUnion = embed.value.record) {
            is RecordViewRecordUnion.ViewRecord -> PostUri(recordUnion.value.uri.atUri)
            is RecordViewRecordUnion.FeedGeneratorView -> FeedGeneratorUri(recordUnion.value.uri.atUri)
            is RecordViewRecordUnion.GraphListView -> ListUri(recordUnion.value.uri.atUri)
            is RecordViewRecordUnion.GraphStarterPackViewBasic -> StarterPackUri(recordUnion.value.uri.atUri)
            is RecordViewRecordUnion.LabelerLabelerView -> null
            is RecordViewRecordUnion.Unknown -> null
            is RecordViewRecordUnion.ViewBlocked -> null // TODO: Handle blocked/muted posts later
            is RecordViewRecordUnion.ViewDetached -> null // TODO: Handle detached posts
            is RecordViewRecordUnion.ViewNotFound -> null // TODO: Handle deleted/missing posts
        }

        is PostViewEmbedUnion.RecordWithMediaView -> when (val recordUnion = embed.value.record.record) {
            is RecordViewRecordUnion.ViewRecord -> PostUri(recordUnion.value.uri.atUri)
            is RecordViewRecordUnion.FeedGeneratorView -> FeedGeneratorUri(recordUnion.value.uri.atUri)
            is RecordViewRecordUnion.GraphListView -> ListUri(recordUnion.value.uri.atUri)
            is RecordViewRecordUnion.GraphStarterPackViewBasic -> StarterPackUri(recordUnion.value.uri.atUri)
            is RecordViewRecordUnion.LabelerLabelerView -> null
            is RecordViewRecordUnion.Unknown -> null
            is RecordViewRecordUnion.ViewBlocked -> null // TODO
            is RecordViewRecordUnion.ViewDetached -> null // TODO
            is RecordViewRecordUnion.ViewNotFound -> null // TODO
        }
        is PostViewEmbedUnion.ExternalView,
        is PostViewEmbedUnion.ImagesView,
        is PostViewEmbedUnion.Unknown,
        is PostViewEmbedUnion.VideoView,
        null,
        -> null
    },

)

internal fun ReplyRefRootUnion.profileEntity() = when (this) {
    is ReplyRefRootUnion.PostView -> value.profileEntity()
    is ReplyRefRootUnion.BlockedPost -> value.author.profileEntity()
    is ReplyRefRootUnion.NotFoundPost,
    is ReplyRefRootUnion.Unknown,
    -> null
}

internal fun ReplyRefParentUnion.profileEntity() = when (this) {
    is ReplyRefParentUnion.PostView -> value.profileEntity()
    is ReplyRefParentUnion.BlockedPost -> value.author.profileEntity()
    is ReplyRefParentUnion.NotFoundPost,
    is ReplyRefParentUnion.Unknown,
    -> null
}

internal fun ReplyRefRootUnion.postEntity() = when (val ref = this) {
    is ReplyRefRootUnion.BlockedPost -> stubPostEntity(
        id = Constants.blockedPostId,
        uri = ref.value.uri.atUri.let(::PostUri),
        authorId = ref.value.author.did.did.let(::ProfileId),
    )

    is ReplyRefRootUnion.NotFoundPost -> stubPostEntity(
        id = Constants.notFoundPostId,
        uri = ref.value.uri.atUri.let(::PostUri),
        authorId = Constants.unknownAuthorId,
    )

    is ReplyRefRootUnion.PostView -> ref.value.postEntity()

    is ReplyRefRootUnion.Unknown -> stubPostEntity(
        id = Constants.unknownPostId,
        uri = Constants.unknownPostUri,
        authorId = Constants.unknownAuthorId,
    )
}

internal fun ReplyRefParentUnion.postEntity() = when (val ref = this) {
    is ReplyRefParentUnion.BlockedPost -> stubPostEntity(
        id = Constants.blockedPostId,
        uri = ref.value.uri.atUri.let(::PostUri),
        authorId = ref.value.author.did.did.let(::ProfileId),
    )

    is ReplyRefParentUnion.NotFoundPost -> stubPostEntity(
        id = Constants.notFoundPostId,
        uri = ref.value.uri.atUri.let(::PostUri),
        authorId = Constants.unknownAuthorId,
    )

    is ReplyRefParentUnion.PostView -> ref.value.postEntity()

    is ReplyRefParentUnion.Unknown -> stubPostEntity(
        id = Constants.unknownPostId,
        uri = Constants.unknownPostUri,
        authorId = Constants.unknownAuthorId,
    )
}

internal fun FeedViewPostReasonUnion.profileEntity() =
    when (this) {
        is FeedViewPostReasonUnion.ReasonRepost -> ProfileEntity(
            did = ProfileId(value.by.did.did),
            handle = ProfileHandle(value.by.handle.handle),
            displayName = value.by.displayName,
            description = null,
            avatar = value.by.avatar?.uri?.let(::ImageUri),
            banner = null,
            followersCount = null,
            followsCount = null,
            postsCount = null,
            joinedViaStarterPack = null,
            indexedAt = null,
            createdAt = value.by.createdAt,
            associated = ProfileEntity.Associated(
                createdListCount = value.by.associated?.lists,
                createdFeedGeneratorCount = value.by.associated?.feedgens,
                createdStarterPackCount = value.by.associated?.starterPacks,
                labeler = value.by.associated?.labeler,
                allowDms = value.by.associated?.chat?.allowIncoming?.value,
            ),
        )

        else -> null
    }

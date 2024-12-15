package com.tunjid.heron.data.network.models

import app.bsky.actor.GetProfileResponse
import app.bsky.actor.ProfileViewBasic
import app.bsky.feed.ReplyRefParentUnion
import app.bsky.feed.ReplyRefRootUnion
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.profile.ProfilePostStatisticsEntity
import com.tunjid.heron.data.database.entities.profile.ProfileProfileRelationshipsEntity
import app.bsky.feed.ViewerState as ProfileViewerState


internal fun ProfileViewerState.profilePostStatisticsEntity(
    viewingProfileId: Id,
    postId: Id,
) = ProfilePostStatisticsEntity(
    profileId = viewingProfileId,
    postId = postId,
    liked = like != null,
    reposted = repost != null,
    threadMuted = threadMuted == true,
    replyDisabled = replyDisabled == true,
    embeddingDisabled = embeddingDisabled == true,
    pinned = pinned == true,
)

//internal fun PostViewerState.profileProfileRelationshipsEntities(
//    viewingProfileId: Id,
//    otherProfileId: Id,
//): List<ProfileProfileRelationshipsEntity> =
//    listOf(
//        ProfileProfileRelationshipsEntity(
//            profileId = viewingProfileId,
//            otherProfileId = otherProfileId,
//            follows = following != null,
//            muted = muted == true,
//            blocking = blocking != null,
//        ),
//        ProfileProfileRelationshipsEntity(
//            profileId = otherProfileId,
//            otherProfileId = viewingProfileId,
//            follows = followedBy != null,
//            muted = mutedByList != null,
//            blocking = blockedBy == true,
//        ),
//    )

internal fun ProfileViewBasic.profileProfileRelationshipsEntities(
    viewingProfileId: Id,
): List<ProfileProfileRelationshipsEntity> =
    listOf(
        ProfileProfileRelationshipsEntity(
            profileId = viewingProfileId,
            otherProfileId = Id(did.did),
            follows = viewer?.following != null,
            muted = viewer?.muted == true,
            blocking = viewer?.blocking != null,
        ),
        ProfileProfileRelationshipsEntity(
            profileId = Id(did.did),
            otherProfileId = viewingProfileId,
            follows = viewer?.followedBy != null,
            muted = viewer?.mutedByList != null,
            blocking = viewer?.blockedBy == true,
        ),
    )

//internal fun PostViewerState.postAuthorKnownProfileRelationshipsEntities(
//    viewingProfileId: Id,
//    ): List<ProfileProfileRelationshipsEntity> =
//    (knownFollowers?.followers ?: emptyList()).map {
//        ProfileProfileRelationshipsEntity(
//            profileId = viewingProfileId,
//            otherProfileId = postEntity.cid,
//            follows = following != null,
//            muted = muted == true,
//            blocking = blocking != null,
//        )
//        }

internal fun ReplyRefRootUnion.profilePostStatisticsEntity(
    viewingProfileId: Id,
) = when (this) {
    is ReplyRefRootUnion.PostView -> value.viewer?.profilePostStatisticsEntity(
        viewingProfileId = viewingProfileId,
        postId = Id(value.cid.cid),
    )

    is ReplyRefRootUnion.BlockedPost,
    is ReplyRefRootUnion.NotFoundPost,
    is ReplyRefRootUnion.Unknown -> null
}

internal fun ReplyRefParentUnion.profilePostStatisticsEntity(
    viewingProfileId: Id,
) = when (this) {
    is ReplyRefParentUnion.PostView -> value.viewer?.profilePostStatisticsEntity(
        viewingProfileId = viewingProfileId,
        postId = Id(value.cid.cid),
    )

    is ReplyRefParentUnion.BlockedPost,
    is ReplyRefParentUnion.NotFoundPost,
    is ReplyRefParentUnion.Unknown -> null
}

internal fun GetProfileResponse.signedInUserProfileEntity() =
    ProfileEntity(
        did = Id(did.did),
        handle = Id(handle.handle),
        displayName = displayName,
        description = description,
        avatar = avatar?.uri?.let(::Uri),
        banner = banner?.uri?.let(::Uri),
        followersCount = followersCount,
        followsCount = followsCount,
        postsCount = followsCount,
        joinedViaStarterPack = joinedViaStarterPack?.cid?.cid?.let(::Id),
        indexedAt = indexedAt,
        createdAt = createdAt,
    )
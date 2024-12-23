package com.tunjid.heron.data.network.models

import app.bsky.actor.GetProfileResponse
import app.bsky.actor.ProfileView
import app.bsky.actor.ProfileViewBasic
import app.bsky.feed.PostView
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.profile.ProfileProfileRelationshipsEntity


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

internal fun ProfileView.profileEntity(): ProfileEntity =
    ProfileEntity(
        did = Id(did.did),
        handle = Id(handle.handle),
        displayName = displayName,
        description = null,
        avatar = avatar?.uri?.let(::Uri),
        banner = null,
        followersCount = 0,
        followsCount = 0,
        postsCount = 0,
        joinedViaStarterPack = null,
        indexedAt = null,
        createdAt = createdAt,
    )

internal fun ProfileViewBasic.profileEntity(): ProfileEntity =
    ProfileEntity(
        did = Id(did.did),
        handle = Id(handle.handle),
        displayName = displayName,
        description = null,
        avatar = avatar?.uri?.let(::Uri),
        banner = null,
        followersCount = 0,
        followsCount = 0,
        postsCount = 0,
        joinedViaStarterPack = null,
        indexedAt = null,
        createdAt = createdAt,
    )

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
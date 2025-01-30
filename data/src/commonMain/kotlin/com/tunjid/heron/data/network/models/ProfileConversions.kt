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

import app.bsky.actor.KnownFollowers
import app.bsky.actor.ProfileView
import app.bsky.actor.ProfileViewBasic
import app.bsky.actor.ProfileViewDetailed
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.profile.ProfileViewerStateEntity


//internal fun PostViewerState.profileViewerEntity(
//    viewingProfileId: Id,
//    otherProfileId: Id,
//): List<ProfileViewerEntity> =
//    listOf(
//        ProfileViewerEntity(
//            profileId = viewingProfileId,
//            otherProfileId = otherProfileId,
//            follows = following != null,
//            muted = muted == true,
//            blocking = blocking != null,
//        ),
//        ProfileViewerEntity(
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
        description = description,
        avatar = avatar?.uri?.let(::Uri),
        banner = null,
        followersCount = 0,
        followsCount = 0,
        postsCount = 0,
        joinedViaStarterPack = null,
        indexedAt = indexedAt,
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

internal fun ProfileViewDetailed.profileEntity(): ProfileEntity =
    ProfileEntity(
        did = Id(did.did),
        handle = Id(handle.handle),
        displayName = displayName,
        description = description,
        avatar = avatar?.uri?.let(::Uri),
        banner = banner?.uri?.let(::Uri),
        followersCount = followersCount,
        followsCount = followsCount,
        postsCount = postsCount,
        joinedViaStarterPack = joinedViaStarterPack?.cid?.cid?.let(::Id),
        indexedAt = indexedAt,
        createdAt = createdAt,
    )

internal fun ProfileViewBasic.profileViewerStateEntities(
    viewingProfileId: Id,
): List<ProfileViewerStateEntity> =
    listOf(
        ProfileViewerStateEntity(
            profileId = viewingProfileId,
            otherProfileId = Id(did.did),
            muted = viewer?.muted,
            mutedByList = viewer?.mutedByList?.cid?.cid?.let(::Id),
            blockedBy = viewer?.blockedBy,
            blockingByList = viewer?.blockingByList?.cid?.cid?.let(::Id),
            following = viewer?.following?.atUri?.let(::Uri),
            followedBy = viewer?.followedBy?.atUri?.let(::Uri),
            blocking = viewer?.blocking?.atUri?.let(::Uri),
        ),
    )

internal fun ProfileView.profileViewerStateEntities(
    viewingProfileId: Id,
): List<ProfileViewerStateEntity> =
    listOf(
        ProfileViewerStateEntity(
            profileId = viewingProfileId,
            otherProfileId = Id(did.did),
            muted = viewer?.muted,
            mutedByList = viewer?.mutedByList?.cid?.cid?.let(::Id),
            blockedBy = viewer?.blockedBy,
            blockingByList = viewer?.blockingByList?.cid?.cid?.let(::Id),
            following = viewer?.following?.atUri?.let(::Uri),
            followedBy = viewer?.followedBy?.atUri?.let(::Uri),
            blocking = viewer?.blocking?.atUri?.let(::Uri),
        ),
    )

internal fun ProfileViewDetailed.profileViewerStateEntities(
    viewingProfileId: Id,
): List<ProfileViewerStateEntity> =
    listOf(
        ProfileViewerStateEntity(
            profileId = viewingProfileId,
            otherProfileId = Id(did.did),
            muted = viewer?.muted,
            mutedByList = viewer?.mutedByList?.cid?.cid?.let(::Id),
            blockedBy = viewer?.blockedBy,
            blockingByList = viewer?.blockingByList?.cid?.cid?.let(::Id),
            following = viewer?.following?.atUri?.let(::Uri),
            followedBy = viewer?.followedBy?.atUri?.let(::Uri),
            blocking = viewer?.blocking?.atUri?.let(::Uri),
        ),
    )


internal fun ProfileViewBasic.profile() = Profile(
    did = did.did.let(::Id),
    handle = handle.handle.let(::Id),
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

internal fun ProfileView.profile() = Profile(
    did = did.did.let(::Id),
    handle = handle.handle.let(::Id),
    displayName = displayName,
    description = description,
    avatar = avatar?.uri?.let(::Uri),
    banner = null,
    followersCount = 0,
    followsCount = 0,
    postsCount = 0,
    joinedViaStarterPack = null,
    indexedAt = indexedAt,
    createdAt = createdAt
)

// TODO: Use this when known follower profiles are also saved
@Suppress("unused")
private fun KnownFollowers?.profileViewers(
    viewingProfileId: Id,
): List<ProfileViewerStateEntity> = when (this) {
    null -> emptyList()
    else -> followers.flatMap { profileViewBasic ->
        profileViewBasic.profileViewerStateEntities(
            viewingProfileId = viewingProfileId
        )
    }
}

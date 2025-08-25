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
import app.bsky.actor.ViewerState
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.types.GenericId
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ListId
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.profile.ProfileViewerStateEntity

internal fun ProfileView.profileEntity(): ProfileEntity = ProfileEntity(
    did = ProfileId(did.did),
    handle = ProfileHandle(handle.handle),
    displayName = displayName,
    description = description,
    avatar = avatar?.uri?.let(::ImageUri),
    banner = null,
    followersCount = null,
    followsCount = null,
    postsCount = null,
    joinedViaStarterPack = null,
    indexedAt = indexedAt,
    createdAt = createdAt,
    associated = ProfileEntity.Associated(
        createdListCount = associated?.lists,
        createdFeedGeneratorCount = associated?.feedgens,
        createdStarterPackCount = associated?.starterPacks,
        labeler = associated?.labeler,
        allowDms = associated?.chat?.allowIncoming?.value,
    ),
)

internal fun ProfileViewBasic.profileEntity(): ProfileEntity = ProfileEntity(
    did = ProfileId(did.did),
    handle = ProfileHandle(handle.handle),
    displayName = displayName,
    description = null,
    avatar = avatar?.uri?.let(::ImageUri),
    banner = null,
    followersCount = null,
    followsCount = null,
    postsCount = null,
    joinedViaStarterPack = null,
    indexedAt = null,
    createdAt = createdAt,
    associated = ProfileEntity.Associated(
        createdListCount = associated?.lists,
        createdFeedGeneratorCount = associated?.feedgens,
        createdStarterPackCount = associated?.starterPacks,
        labeler = associated?.labeler,
        allowDms = associated?.chat?.allowIncoming?.value,
    ),
)

internal fun ProfileViewDetailed.profileEntity(): ProfileEntity = ProfileEntity(
    did = ProfileId(did.did),
    handle = ProfileHandle(handle.handle),
    displayName = displayName,
    description = description,
    avatar = avatar?.uri?.let(::ImageUri),
    banner = banner?.uri?.let(::ImageUri),
    followersCount = followersCount,
    followsCount = followsCount,
    postsCount = postsCount,
    joinedViaStarterPack = joinedViaStarterPack?.cid?.cid?.let(::GenericId),
    indexedAt = indexedAt,
    createdAt = createdAt,
    associated = ProfileEntity.Associated(
        createdListCount = associated?.lists,
        createdFeedGeneratorCount = associated?.feedgens,
        createdStarterPackCount = associated?.starterPacks,
        labeler = associated?.labeler,
        allowDms = associated?.chat?.allowIncoming?.value,
    ),
)

internal fun ProfileViewBasic.profileViewerStateEntities(
    viewingProfileId: ProfileId,
): List<ProfileViewerStateEntity> = when (val viewer = viewer) {
    null -> emptyList()
    else -> listOf(
        profileViewerStateEntity(
            viewingProfileId = viewingProfileId,
            viewedProfileId = did.did.let(::ProfileId),
            viewer = viewer,
        ),
    )
}

internal fun ProfileView.profileViewerStateEntities(
    viewingProfileId: ProfileId,
): List<ProfileViewerStateEntity> = when (val viewer = viewer) {
    null -> emptyList()
    else -> listOf(
        profileViewerStateEntity(
            viewingProfileId = viewingProfileId,
            viewedProfileId = did.did.let(::ProfileId),
            viewer = viewer,
        ),
    )
}

internal fun ProfileViewDetailed.profileViewerStateEntities(
    viewingProfileId: ProfileId,
): List<ProfileViewerStateEntity> = when (val viewer = viewer) {
    null -> emptyList()
    else -> listOf(
        profileViewerStateEntity(
            viewingProfileId = viewingProfileId,
            viewedProfileId = did.did.let(::ProfileId),
            viewer = viewer,
        ),
    )
}

internal fun ProfileViewBasic.profile() = Profile(
    did = did.did.let(::ProfileId),
    handle = handle.handle.let(::ProfileHandle),
    displayName = displayName,
    description = null,
    avatar = avatar?.uri?.let(::ImageUri),
    banner = null,
    followersCount = 0,
    followsCount = 0,
    postsCount = 0,
    joinedViaStarterPack = null,
    indexedAt = null,
    createdAt = createdAt,
    metadata = Profile.Metadata(
        createdListCount = associated?.lists ?: 0,
        createdFeedGeneratorCount = associated?.feedgens ?: 0,
        createdStarterPackCount = associated?.starterPacks ?: 0,
    ),
)

internal fun ProfileView.profile() = Profile(
    did = did.did.let(::ProfileId),
    handle = handle.handle.let(::ProfileHandle),
    displayName = displayName,
    description = description,
    avatar = avatar?.uri?.let(::ImageUri),
    banner = null,
    followersCount = 0,
    followsCount = 0,
    postsCount = 0,
    joinedViaStarterPack = null,
    indexedAt = indexedAt,
    createdAt = createdAt,
    metadata = Profile.Metadata(
        createdListCount = associated?.lists ?: 0,
        createdFeedGeneratorCount = associated?.feedgens ?: 0,
        createdStarterPackCount = associated?.starterPacks ?: 0,
    ),
)

private fun profileViewerStateEntity(
    viewingProfileId: ProfileId,
    viewedProfileId: ProfileId,
    viewer: ViewerState,
) = ProfileViewerStateEntity(
    profileId = viewingProfileId,
    otherProfileId = viewedProfileId,
    muted = viewer.muted,
    mutedByList = viewer.mutedByList?.cid?.cid?.let(::ListId),
    blockedBy = viewer.blockedBy,
    blockingByList = viewer.blockingByList?.cid?.cid?.let(::ListId),
    following = viewer.following?.atUri?.let(::GenericUri),
    followedBy = viewer.followedBy?.atUri?.let(::GenericUri),
    blocking = viewer.blocking?.atUri?.let(::GenericUri),
    commonFollowersCount = viewer.knownFollowers?.count,
)

// TODO: Use this when known follower profiles are also saved
@Suppress("unused")
private fun KnownFollowers?.profileViewers(
    viewingProfileId: ProfileId,
): List<ProfileViewerStateEntity> = when (this) {
    null -> emptyList()
    else -> followers.flatMap { profileViewBasic ->
        profileViewBasic.profileViewerStateEntities(
            viewingProfileId = viewingProfileId,
        )
    }
}

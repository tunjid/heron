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

package com.tunjid.heron.data.utilities.multipleEntitysaver

import app.bsky.actor.ProfileView
import app.bsky.actor.ProfileViewBasic
import app.bsky.actor.ProfileViewDetailed
import com.tunjid.heron.data.core.models.Constants
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.profile.ProfileViewerStateEntity
import com.tunjid.heron.data.network.models.profileEntity
import com.tunjid.heron.data.network.models.profileViewerStateEntities
import sh.christian.ozone.api.Did

internal fun MultipleEntitySaver.add(
    viewingProfileId: ProfileId?,
    profileView: ProfileViewBasic,
) {
    add(profileView.profileEntity())
    if (viewingProfileId != null) {
        profileView.profileViewerStateEntities(
            viewingProfileId = viewingProfileId,
        ).forEach(::add)
    }

    profileView.viewer
        ?.knownFollowers
        ?.followers
        ?.forEach { knownFollowerProfile ->
            add(
                viewingProfileId = viewingProfileId,
                profileView = knownFollowerProfile,
            )

            // Save the common follower relationship with an unknown generic URI
            add(
                unknownFollower(
                    profileId = knownFollowerProfile.did.did.let(::ProfileId),
                    otherProfileId = profileView.did.did.let(::ProfileId),
                ),
            )
        }
}

internal fun MultipleEntitySaver.add(
    viewingProfileId: ProfileId?,
    profileView: ProfileView,
) {
    add(profileView.profileEntity())
    if (viewingProfileId != null) {
        profileView.profileViewerStateEntities(
            viewingProfileId = viewingProfileId,
        ).forEach(::add)
    }

    profileView.viewer
        ?.knownFollowers
        ?.followers
        ?.forEach { knownFollowerProfile ->
            add(
                viewingProfileId = viewingProfileId,
                profileView = knownFollowerProfile,
            )

            // Save the common follower relationship with an unknown generic URI
            add(
                unknownFollower(
                    profileId = knownFollowerProfile.did.did.let(::ProfileId),
                    otherProfileId = profileView.did.did.let(::ProfileId),
                ),
            )
        }
}

internal fun MultipleEntitySaver.add(
    viewingProfileId: ProfileId?,
    profileView: ProfileViewDetailed,
) {
    add(profileView.profileEntity())
    if (viewingProfileId != null) {
        profileView.profileViewerStateEntities(
            viewingProfileId = viewingProfileId,
        ).forEach(::add)
    }

    profileView.viewer
        ?.knownFollowers
        ?.followers
        ?.forEach { knownFollowerProfile ->
            add(
                viewingProfileId = viewingProfileId,
                profileView = knownFollowerProfile,
            )

            // Save the common follower relationship with an unknown generic URI
            add(
                unknownFollower(
                    profileId = knownFollowerProfile.did.did.let(::ProfileId),
                    otherProfileId = profileView.did.did.let(::ProfileId),
                ),
            )
        }
}

@Suppress("UnusedReceiverParameter")
internal fun MultipleEntitySaver.emptyProfileEntity(
    did: Did,
) = ProfileEntity(
    did = ProfileId(did.did),
    handle = Constants.unknownAuthorHandle,
    displayName = null,
    description = null,
    avatar = null,
    banner = null,
    followersCount = null,
    followsCount = null,
    postsCount = null,
    joinedViaStarterPack = null,
    indexedAt = null,
    createdAt = null,
    associated = ProfileEntity.Associated(
        createdListCount = 0,
        createdFeedGeneratorCount = 0,
        createdStarterPackCount = 0,
    ),
)

private fun unknownFollower(
    profileId: ProfileId,
    otherProfileId: ProfileId,
) = ProfileViewerStateEntity(
    profileId = profileId,
    otherProfileId = otherProfileId,
    muted = null,
    mutedByList = null,
    blockedBy = null,
    blockingByList = null,
    following = Constants.unknownGenericUri,
    followedBy = null,
    blocking = null,
    commonFollowersCount = null,
)

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

package com.tunjid.heron.data.core.models

import com.tunjid.heron.data.core.types.BlockUri
import com.tunjid.heron.data.core.types.FollowUri
import com.tunjid.heron.data.core.types.GenericId
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.utilities.File
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class Profile(
    val did: ProfileId,
    val handle: ProfileHandle,
    val displayName: String?,
    val description: String?,
    val avatar: ImageUri?,
    val banner: ImageUri?,
    val followersCount: Long?,
    val followsCount: Long?,
    val postsCount: Long?,
    val joinedViaStarterPack: GenericId?,
    val indexedAt: Instant?,
    val createdAt: Instant?,
    val metadata: Metadata,
    // Stubbed for migrations
    val labels: List<Label> = emptyList(),
    val isLabeler: Boolean = false,
) : UrlEncodableModel {

    @Serializable
    sealed class Connection {
        abstract val signedInProfileId: ProfileId
        abstract val profileId: ProfileId
        abstract val followedBy: FollowUri?

        @Serializable
        data class Follow(
            override val signedInProfileId: ProfileId,
            override val profileId: ProfileId,
            override val followedBy: FollowUri?,
        ) : Connection()

        @Serializable
        data class Unfollow(
            override val signedInProfileId: ProfileId,
            override val profileId: ProfileId,
            override val followedBy: FollowUri?,
            val followUri: FollowUri,
        ) : Connection()
    }

    @Serializable
    sealed class Restriction {
        abstract val signedInProfileId: ProfileId
        abstract val profileId: ProfileId

        @Serializable
        sealed class Block : Restriction() {
            @Serializable
            data class Add(
                override val signedInProfileId: ProfileId,
                override val profileId: ProfileId,
            ) : Block()

            @Serializable
            data class Remove(
                override val signedInProfileId: ProfileId,
                override val profileId: ProfileId,
                val blockUri: BlockUri,
            ) : Block()
        }

        @Serializable
        sealed class Mute : Restriction() {
            @Serializable
            data class Add(
                override val signedInProfileId: ProfileId,
                override val profileId: ProfileId,
            ) : Mute()

            @Serializable
            data class Remove(
                override val signedInProfileId: ProfileId,
                override val profileId: ProfileId,
            ) : Mute()
        }
    }

    @Serializable
    data class Metadata(
        val createdListCount: Long,
        val createdFeedGeneratorCount: Long,
        val createdStarterPackCount: Long,
        val chat: ChatInfo,
    )

    @Serializable
    data class Update(
        @ProtoNumber(1) val profileId: ProfileId,
        @ProtoNumber(2) val displayName: String,
        @ProtoNumber(3) val description: String,
        // Field's 4 & 5 were deprecated
        @ProtoNumber(6) val avatarFile: File.Media.Photo?,
        @ProtoNumber(7) val bannerFile: File.Media.Photo?,
    )

    @Serializable
    data class ChatInfo(val allowed: Allowed) {
        @Serializable
        sealed class Allowed {
            @Serializable data object Everyone : Allowed()

            @Serializable data object Following : Allowed()

            @Serializable data object NoOne : Allowed()
        }
    }
}

@Serializable
data class ProfileWithViewerState(val profile: Profile, val viewerState: ProfileViewerState?)

val Profile.contentDescription
    get() = displayName ?: handle.id

fun stubProfile(
    did: ProfileId,
    handle: ProfileHandle,
    displayName: String? = null,
    avatar: ImageUri? = null,
) =
    Profile(
        did = did,
        handle = handle,
        displayName = displayName,
        description = null,
        avatar = avatar,
        banner = null,
        followersCount = null,
        followsCount = null,
        postsCount = null,
        joinedViaStarterPack = null,
        indexedAt = null,
        createdAt = null,
        metadata =
            Profile.Metadata(
                createdListCount = 0,
                createdFeedGeneratorCount = 0,
                createdStarterPackCount = 0,
                chat = Profile.ChatInfo(allowed = Profile.ChatInfo.Allowed.NoOne),
            ),
        labels = emptyList(),
        isLabeler = false,
    )

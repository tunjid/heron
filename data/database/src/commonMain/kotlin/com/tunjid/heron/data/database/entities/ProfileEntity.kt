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

package com.tunjid.heron.data.database.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.stubProfile
import com.tunjid.heron.data.core.types.GenericId
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.profile.ProfileViewerStateEntity
import kotlin.time.Instant

@Entity(
    tableName = "profiles",
    indices = [
        Index(value = ["did"]),
        Index(value = ["handle"]),
    ],
)
data class ProfileEntity(
    @PrimaryKey
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
    @Embedded
    val associated: Associated,
) {
    data class Partial(
        val did: ProfileId,
        val handle: ProfileHandle,
        val displayName: String?,
        val avatar: ImageUri?,
    )

    // TODO Should this be in a separate table?
    data class Associated(
        val createdListCount: Long? = null,
        val createdFeedGeneratorCount: Long? = null,
        val createdStarterPackCount: Long? = null,
        val labeler: Boolean? = null,
        val allowDms: String? = null,
    )
}

fun ProfileEntity.partial() = ProfileEntity.Partial(
    did = did,
    handle = handle,
    displayName = displayName,
    avatar = avatar,
)

fun ProfileEntity?.asExternalModel(
    labels: List<Label> = emptyList(),
) =
    if (this == null) emptyProfile()
    else Profile(
        did = did,
        handle = handle,
        displayName = displayName,
        description = description,
        avatar = avatar,
        banner = banner,
        followersCount = followersCount,
        followsCount = followsCount,
        postsCount = postsCount,
        joinedViaStarterPack = joinedViaStarterPack,
        indexedAt = indexedAt,
        createdAt = createdAt,
        metadata = Profile.Metadata(
            createdListCount = associated.createdListCount ?: 0,
            createdFeedGeneratorCount = associated.createdFeedGeneratorCount ?: 0,
            createdStarterPackCount = associated.createdStarterPackCount ?: 0,
            chat = Profile.ChatInfo(
                allowed = associated.allowedChat(),
            ),
        ),
        labels = labels,
        isLabeler = associated.labeler ?: false,
    )

data class PopulatedProfileEntity(
    @Embedded
    val entity: ProfileEntity,
    @Embedded
    val relationship: ProfileViewerStateEntity?,
    @Relation(
        parentColumn = "did",
        entityColumn = "uri",
    )
    val labelEntities: List<LabelEntity>,
)

fun PopulatedProfileEntity.asExternalModel() = with(entity) {
    Profile(
        did = did,
        handle = handle,
        displayName = displayName,
        description = description,
        avatar = avatar,
        banner = banner,
        followersCount = followersCount,
        followsCount = followsCount,
        postsCount = postsCount,
        joinedViaStarterPack = joinedViaStarterPack,
        indexedAt = indexedAt,
        createdAt = createdAt,
        metadata = Profile.Metadata(
            createdListCount = associated.createdListCount ?: 0,
            createdFeedGeneratorCount = associated.createdFeedGeneratorCount ?: 0,
            createdStarterPackCount = associated.createdStarterPackCount ?: 0,
            chat = Profile.ChatInfo(
                allowed = associated.allowedChat(),
            ),
        ),
        labels = labelEntities.map(LabelEntity::asExternalModel),
        isLabeler = associated.labeler ?: false,
    )
}

private fun ProfileEntity.Associated?.allowedChat(): Profile.ChatInfo.Allowed =
    when (this?.allowDms) {
        ALLOW_DMS_ALL -> Profile.ChatInfo.Allowed.Everyone
        ALLOW_DMS_FOLLOWING -> Profile.ChatInfo.Allowed.Following
        else -> Profile.ChatInfo.Allowed.NoOne
    }

private fun emptyProfile() = stubProfile(
    did = ProfileId(""),
    handle = ProfileHandle(""),
)

private const val ALLOW_DMS_ALL = "all"
private const val ALLOW_DMS_FOLLOWING = "following"

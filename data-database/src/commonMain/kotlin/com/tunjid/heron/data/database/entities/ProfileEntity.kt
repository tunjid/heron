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
import androidx.room.PrimaryKey
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.entities.profile.ProfileViewerStateEntity
import kotlinx.datetime.Instant


@Entity(
    tableName = "profiles",
)
data class ProfileEntity(
    @PrimaryKey
    val did: Id,
    val handle: Id,
    val displayName: String?,
    val description: String?,
    val avatar: Uri?,
    val banner: Uri?,
    val followersCount: Long?,
    val followsCount: Long?,
    val postsCount: Long?,
    val joinedViaStarterPack: Id?,
    val indexedAt: Instant?,
    val createdAt: Instant?,
) {
    data class Partial(
        val did: Id,
        val handle: Id,
        val displayName: String?,
        val avatar: Uri?,
    )
}

fun ProfileEntity.partial() = ProfileEntity.Partial(
    did = did,
    handle = handle,
    displayName = displayName,
    avatar = avatar
)

fun ProfileEntity.asExternalModel() = Profile(
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
)

data class PopulatedProfileEntity(
    @Embedded
    val profileEntity: ProfileEntity,
    @Embedded
    val relationship: ProfileViewerStateEntity?,
)
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

import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable


@Serializable
data class Profile(
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
) : ByteSerializable

@Serializable
data class ProfileWithRelationship(
    val profile: Profile,
    val relationship: ProfileRelationship?,
)

val Profile.contentDescription get() = displayName ?: handle.id

fun stubProfile(
    did: Id,
    handle: Id,
    displayName: String? = null,
    avatar: Uri? = null,
) = Profile(
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
)
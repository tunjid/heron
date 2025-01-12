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
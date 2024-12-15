package com.tunjid.heron.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
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

fun emptyProfileEntity(
    did: Id
) = ProfileEntity(
    did = did,
    handle = did,
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
)

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
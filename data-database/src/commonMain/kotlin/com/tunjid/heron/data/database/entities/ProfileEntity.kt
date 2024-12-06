package com.tunjid.heron.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import kotlinx.datetime.Instant


@Entity(
    tableName = "profile",
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
package com.tunjid.heron.data.core.models

import com.tunjid.heron.data.core.types.Id
import kotlinx.datetime.Instant



data class Profile(
    val did: Id,
    val handle: Id,
    val displayName: String?,
    val description: String?,
    val avatar: String?,
    val banner: String?,
    val followersCount: Long?,
    val followsCount: Long?,
    val postsCount: Long?,
    val joinedViaStarterPack: Id?,
    val indexedAt: Instant?,
    val createdAt: Instant?,
)

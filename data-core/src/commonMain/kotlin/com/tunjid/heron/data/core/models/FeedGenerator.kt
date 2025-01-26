package com.tunjid.heron.data.core.models

import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class FeedGenerator(
    val cid: Id,
    val did: Id,
    val uri: Uri,
    val avatar: Uri?,
    val likeCount: Long?,
    val creatorId: Id,
    val displayName: String,
    val description: String?,
    val acceptsInteractions: Boolean?,
    val indexedAt: Instant,
)
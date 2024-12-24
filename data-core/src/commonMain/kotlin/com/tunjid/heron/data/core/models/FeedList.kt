package com.tunjid.heron.data.core.models

import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import kotlinx.datetime.Instant

data class FeedList(
    val cid: Id,
    val uri: Uri,
    val creatorId: Id,
    val name: String,
    val description: String?,
    val avatar: Uri?,
    val listItemCount: Long?,
    val purpose: String,
    val indexedAt: Instant,
)

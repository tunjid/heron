package com.tunjid.heron.data.core.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

data class CursorList<T>(
    val items: List<T>,
    val nextCursor: DoubleCursor,
) : List<T> by items {
    @Serializable
    data class DoubleCursor(
        val local: Instant?,
        val remote: String?,
    )
}

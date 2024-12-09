package com.tunjid.heron.data.core.models

import kotlinx.datetime.Instant

data class CursorList<T>(
    val items: List<T>,
    val nextCursor: DoubleCursor?,
) : List<T> by items {
    data class DoubleCursor(
        val local: Instant?,
        val remote: String?,
    )
}

val CursorList.DoubleCursor?.isInitialRequest
    get() = this != null
            && local == null
            && remote == null
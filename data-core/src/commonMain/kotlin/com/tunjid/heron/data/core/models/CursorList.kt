package com.tunjid.heron.data.core.models

import kotlinx.serialization.Serializable

data class CursorList<T>(
    val items: List<T>,
    val nextCursor: NetworkCursor,
) : List<T> by items

@Serializable
sealed class NetworkCursor {
    @Serializable
    data object Initial: NetworkCursor()
    @Serializable
    data object Pending: NetworkCursor()
    @Serializable
    data class Next(
        val remote: String,
    ): NetworkCursor()
}

val NetworkCursor.cursor get() = when(this) {
    NetworkCursor.Initial -> null
    NetworkCursor.Pending -> throw IllegalArgumentException(
        "Pending cursors cannot be used to fetch data"
    )
    is NetworkCursor.Next -> remote
}
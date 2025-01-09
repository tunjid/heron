package com.tunjid.heron.data.core.models

import kotlinx.serialization.Serializable

/**
 * A sublist [List] containing a cursor to fetch the consecutive list in its backing sequence.
 */
data class CursorList<T>(
    val items: List<T>,
    val nextCursor: Cursor,
) : List<T> by items

@Serializable
sealed class Cursor {
    @Serializable
    data object Initial : Cursor()

    @Serializable
    data object Pending : Cursor()

    @Serializable
    data class Next(
        val cursor: String,
    ) : Cursor()
}

val Cursor.value
    get() = when (this) {
        Cursor.Initial -> null
        Cursor.Pending -> throw IllegalArgumentException(
            "Pending cursors cannot be used to fetch data"
        )

        is Cursor.Next -> cursor
    }
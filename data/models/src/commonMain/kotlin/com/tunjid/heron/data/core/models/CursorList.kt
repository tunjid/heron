/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tunjid.heron.data.core.models

import kotlinx.serialization.Serializable

/**
 * A sublist [List] containing a cursor to fetch the consecutive list in its backing sequence.
 */
data class CursorList<out T>(
    val items: List<T>,
    val nextCursor: Cursor,
) : List<T> by items

fun <T> emptyCursorList(): CursorList<T> = EmptyCursorList

inline fun <T, R> CursorList<T>.mapCursorList(
    mapper: (T) -> R,
) = CursorList(
    items = items.map(mapper),
    nextCursor = nextCursor,
)

/**
 * Class for representing paginated queries from the network.
 */
@Serializable
sealed class Cursor {
    /**
     * Initial cursor, always has a null value
     */
    @Serializable
    data object Initial : Cursor()

    /**
     * Pending cursor. Used when paging from the local database with a network refresh.
     * A Pending cursor is used when data can be fetched from the database alone.
     * At some point it may be updated to a [Next] cursor after which a network request is made
     * and refreshes the local db.
     */
    @Serializable
    data object Pending : Cursor()

    /**
     * A Cursor that can be used to refresh data from the network. Can be used when paging from
     * both the database and network, or just the network alone.
     */
    @Serializable
    data class Next(
        val cursor: String,
    ) : Cursor()

    /**
     * Cursor signifying there is no more data to load.
     */
    @Serializable
    data object Final : Cursor()
}

val Cursor.value
    get() = when (this) {
        Cursor.Initial -> null
        Cursor.Pending -> throw IllegalArgumentException(
            "Pending cursors cannot be used to fetch data",
        )

        is Cursor.Next -> cursor

        Cursor.Final -> throw IllegalArgumentException(
            "Final cursors cannot be used to fetch data; there is nothing more to fetch",
        )
    }

private val EmptyCursorList = CursorList<Nothing>(
    items = emptyList(),
    nextCursor = Cursor.Pending,
)

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

package com.tunjid.heron.compose.gifs

import androidx.compose.runtime.Stable
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Gif
import com.tunjid.heron.data.repository.GifQuery
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.reset
import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Preset searches offered as pills in the GIF picker, mirroring Bluesky's picker.
 * [searchTerm] is null for trending GIFs.
 */
enum class GifCategory(
    val searchTerm: String?,
) {
    Trending(searchTerm = null),
    Love(searchTerm = "love"),
    Happy(searchTerm = "happy"),
    Sad(searchTerm = "cry"),
    Party(searchTerm = "congratulations"),
    Yes(searchTerm = "yes"),
}

@Stable
@Snapshottable
interface GifPickerState : TilingState<GifQuery, Gif> {

    @Serializable
    @SnapshotSpec
    data class Immutable(
        val searchText: String = "",
        /**
         * The selected category pill, or null while searching by [searchText].
         */
        val selectedCategory: GifCategory? = GifCategory.Trending,
        @Transient
        override val tilingData: TilingState.Data<GifQuery, Gif> = TilingState.Data(
            currentQuery = gifQuery(
                searchText = "",
                category = GifCategory.Trending,
            ),
        ),
    ) : GifPickerState
}

/**
 * A query for the first page of [searchText] if it is not blank, otherwise for [category].
 * Each call starts a new cursor anchor, so the tiling pipeline treats it as a fresh generation.
 */
internal fun gifQuery(
    searchText: String,
    category: GifCategory?,
): GifQuery {
    val data = CursorQuery.defaultStartData(
        limit = GifPageSize,
    )
    val term = searchText.trim().ifEmpty { category?.searchTerm }
    return when (term) {
        null -> GifQuery.Trending(
            data = data,
        )
        else -> GifQuery.Search(
            query = term,
            data = data,
        )
    }
}

internal fun GifQuery.updateData(
    newData: CursorQuery.Data,
): GifQuery = when (this) {
    is GifQuery.Search -> copy(data = newData)
    is GifQuery.Trending -> copy(data = newData)
}

internal fun GifQuery.refresh(): GifQuery =
    updateData(data.reset())

sealed class GifPickerAction(
    val key: String,
) {

    data class Tile(
        val tilingAction: TilingState.Action,
    ) : GifPickerAction("Tile")

    data class Search(
        val text: String,
    ) : GifPickerAction("Search")

    data class SelectCategory(
        val category: GifCategory,
    ) : GifPickerAction("SelectCategory")
}

// Divisible by 2 and 3, so full pages fill both 2 and 3 column grids.
private const val GifPageSize = 30L

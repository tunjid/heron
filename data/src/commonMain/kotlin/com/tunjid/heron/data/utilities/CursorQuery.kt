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

package com.tunjid.heron.data.utilities

import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.tiler.ListTiler
import com.tunjid.tiler.PivotRequest
import com.tunjid.tiler.QueryFetcher
import com.tunjid.tiler.Tile
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.listTiler
import com.tunjid.tiler.toPivotedTileInputs
import com.tunjid.tiler.utilities.NeighboredFetchResult
import com.tunjid.tiler.utilities.neighboredQueryFetcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.math.max

/**
 * Basic pagination query type, used for tiled requests.
 */
interface CursorQuery {
    val data: Data

    @Serializable
    data class Data(
        val page: Int,

        /**
         * The anchor point for the tiling pipeline.
         * Consecutive queries in a tiling pipeline mush have the same anchor unless
         * its being refreshed.
         */
        val cursorAnchor: Instant,

        /**
         * How many items to fetch for a query.
         */
        val limit: Long = 30L,
    )
}

val CursorQuery.Data.offset get() = page * limit

fun CursorQuery.hasDifferentAnchor(newQuery: CursorQuery) =
    data.cursorAnchor != newQuery.data.cursorAnchor

inline fun <Query : CursorQuery> Flow<Query>.ensureValidAnchors(
): Flow<Query> = scan<Query, Query?>(
    initial = null,
    operation = { lastQuery, currentQuery ->
        if (lastQuery == null) return@scan currentQuery

        // Everything is okay, proceed.
        if (!currentQuery.hasDifferentAnchor(lastQuery)) return@scan currentQuery

        // Favor the query that was requested with a more current anchor.
        // The query with the older anchor was most likely triggered by a scroll
        // at a boundary.
        if (currentQuery.data.cursorAnchor > lastQuery.data.cursorAnchor) currentQuery
        else lastQuery
    }
)
    .filterNotNull()

fun <Query : CursorQuery, Item> TiledList<Query, Item>.isValidFor(
    currentQuery: Query,
): Boolean { // Ignore results from stale queries
    var seenQuery = false
    val lastTileIndex = tileCount - 1
    for (index in 0..<tileCount) {
        if (!seenQuery) seenQuery = queryAtTile(index) == currentQuery
        if (index == lastTileIndex) continue
        if (queryAtTile(index).data.page + 1 != queryAtTile(index + 1).data.page) return false
    }
    return seenQuery
}

inline fun <Query : CursorQuery, Item> cursorTileInputs(
    numColumns: Flow<Int>,
    queries: Flow<Query>,
    crossinline updatePage: Query.(CursorQuery.Data) -> Query,
): Flow<Tile.Input<Query, Item>> = merge(
    numColumns.map { columns ->
        Tile.Limiter(
            maxQueries = 3 * max(1, columns),
            itemSizeHint = null,
        )
    },
    queries.toPivotedTileInputs(
        numColumns.map { columns ->
            val numColumns = max(1, columns)
            PivotRequest(
                onCount = numColumns * 3,
                offCount = numColumns * 2,
                comparator = cursorQueryComparator(),
                previousQuery = {
                    if ((data.page - 1) < 0) null
                    else updatePage(data.copy(page = data.page - 1))
                },
                nextQuery = {
                    updatePage(data.copy(page = data.page + 1))
                }
            )
        }
    )
)

fun <Query : CursorQuery, Item> cursorListTiler(
    startingQuery: Query,
    updatePage: Query.(CursorQuery.Data) -> Query,
    cursorListLoader: (Query, Cursor) -> Flow<CursorList<Item>>,
): ListTiler<Query, Item> = listTiler(
    order = Tile.Order.PivotSorted(
        query = startingQuery,
        comparator = cursorQueryComparator(),
    ),
    fetcher = cursorListQueryFetcher(
        startingQuery = startingQuery,
        nextPage = updatePage,
        cursorListLoader = cursorListLoader,
    )
)

fun <Query : CursorQuery> cursorQueryComparator() = compareBy { query: Query ->
    query.data.page
}


private inline fun <Query : CursorQuery, Item> cursorListQueryFetcher(
    startingQuery: Query,
    crossinline nextPage: Query.(CursorQuery.Data) -> Query,
    crossinline cursorListLoader: (Query, Cursor) -> Flow<CursorList<Item>>,
): QueryFetcher<Query, Item> =
    neighboredQueryFetcher<Query, Item, Cursor>(
        // Since the API doesn't allow for paging backwards, hold the tokens for a 50 pages
        // in memory
        maxTokens = 50,
        // Make sure the first page has an entry for its cursor/token
        seedQueryTokenMap = mapOf(
            startingQuery to Cursor.Initial
        ),
        fetcher = { query, cursor ->
            cursorListLoader(query, cursor)
                .map { networkCursorList ->
                    NeighboredFetchResult(
                        // Set the cursor for the next page and any other page with data available.
                        //
                        mapOf(
                            Pair(
                                first = query.nextPage(query.data.copy(page = query.data.page + 1)),
                                second = networkCursorList.nextCursor,
                            )
                        ),
                        items = networkCursorList
                    )
                }
        }
    )

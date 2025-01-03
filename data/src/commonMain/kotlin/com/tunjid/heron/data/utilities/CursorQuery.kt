package com.tunjid.heron.data.utilities

import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.tiler.ListTiler
import com.tunjid.tiler.PivotRequest
import com.tunjid.tiler.QueryFetcher
import com.tunjid.tiler.Tile
import com.tunjid.tiler.listTiler
import com.tunjid.tiler.toPivotedTileInputs
import com.tunjid.tiler.utilities.NeighboredFetchResult
import com.tunjid.tiler.utilities.neighboredQueryFetcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable


interface CursorQuery {
    val data: Data

    @Serializable
    data class Data(
        val page: Int,

        val firstRequestInstant: Instant,

        /**
         * How many items to fetch for a query.
         */
        val limit: Long = 50L,
    )
}

fun <Query : CursorQuery, Item> cursorTileInputs(
    numColumns: Flow<Int>,
    queries: Flow<Query>,
    updatePage: Query.(CursorQuery.Data) -> Query,
): Flow<Tile.Input<Query, Item>> = merge(
    numColumns.map { columns ->
        Tile.Limiter(
            maxQueries = 3 * columns,
            itemSizeHint = null,
        )
    },
    queries.toPivotedTileInputs(
        numColumns.map {
            cursorPivotRequest(
                numColumns = it,
                updatePage = updatePage,
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

private inline fun <Query : CursorQuery, Item> cursorPivotRequest(
    numColumns: Int,
    crossinline updatePage: Query.(CursorQuery.Data) -> Query,
) = PivotRequest<Query, Item>(
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

private inline fun <Query : CursorQuery> cursorQueryComparator() = compareBy { query: Query ->
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

package com.tunjid.heron.domain.timeline

import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.repository.TimelineQuery
import com.tunjid.mutator.Mutation
import com.tunjid.tiler.ListTiler
import com.tunjid.tiler.PivotRequest
import com.tunjid.tiler.QueryFetcher
import com.tunjid.tiler.Tile
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.listTiler
import com.tunjid.tiler.toPivotedTileInputs
import com.tunjid.tiler.toTiledList
import com.tunjid.tiler.utilities.NeighboredFetchResult
import com.tunjid.tiler.utilities.neighboredQueryFetcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan

sealed interface TimelineLoadAction<out Query : TimelineQuery> {
    data class GridSize<Query : TimelineQuery>(
        val numColumns: Int,
    ) : TimelineLoadAction<Query>

    data class LoadAround<Query : TimelineQuery>(
        val query: Query,
    ) : TimelineLoadAction<Query>
}

/**
 * Feed mutations as a function of the user's scroll position
 */
fun <Query : TimelineQuery, Action : TimelineLoadAction<Query>, State> Flow<Action>.timelineLoadMutations(
    startQuery: Query,
    startNumColumns: Int,
    cursorListLoader: (Query) -> Flow<CursorList<TimelineItem>>,
    mutations: (queries: Flow<Query>, numColumns: Flow<Int>, Flow<TiledList<Query, TimelineItem>>) -> Flow<Mutation<State>>
): Flow<Mutation<State>> = scan(
    initial = Pair(
        MutableStateFlow(startQuery),
        MutableStateFlow(startNumColumns),
    )
) { accumulator, action ->
    val (queries, numColumns) = accumulator
    // update backing states as a side effect
    @Suppress("UNCHECKED_CAST")
    when (action) {
        is TimelineLoadAction.GridSize<*> -> numColumns.value = action.numColumns
        is TimelineLoadAction.LoadAround<*> -> queries.value = action.query as Query
    }
    // Emit the same item with each action
    accumulator
}
    // Only emit once
    .distinctUntilChanged()
    .flatMapLatest { (queries, numColumns) ->
        val tiledList = timelineTileInputs(numColumns, queries).toTiledList(
            timelineTiler(
                startingQuery = queries.value,
                cursorListLoader = cursorListLoader,
            )
        )
        mutations(queries, numColumns, tiledList)
    }

private inline fun <Query : TimelineQuery> timelineTileInputs(
    numColumns: Flow<Int>,
    queries: Flow<Query>
): Flow<Tile.Input<Query, TimelineItem>> = merge(
    numColumns.map { columns ->
        Tile.Limiter(
            maxQueries = 3 * columns,
            itemSizeHint = null,
        )
    },
    queries.toPivotedTileInputs(
        numColumns.map(::timelinePivotRequest)
    )
)

private inline fun <Query : TimelineQuery> timelinePivotRequest(numColumns: Int) =
    PivotRequest<Query, TimelineItem>(
        onCount = numColumns * 3,
        offCount = numColumns * 2,
        comparator = timelineQueryComparator(),
        previousQuery = {
            if ((data.page - 1) < 0) null
            else updateData { copy(page = page - 1) }
        },
        nextQuery = {
            updateData { copy(page = page + 1) }
        }
    )

private inline fun <Query : TimelineQuery> timelineTiler(
    startingQuery: Query,
    crossinline cursorListLoader: (Query) -> Flow<CursorList<TimelineItem>>,
): ListTiler<Query, TimelineItem> = listTiler(
    order = Tile.Order.PivotSorted(
        query = startingQuery,
        comparator = timelineQueryComparator(),
    ),
    fetcher = timelineQueryFetcher(
        startingQuery = startingQuery,
        cursorListLoader = cursorListLoader,
    )
)

private inline fun <Query : TimelineQuery> timelineQueryFetcher(
    startingQuery: Query,
    crossinline cursorListLoader: (Query) -> Flow<CursorList<TimelineItem>>,
): QueryFetcher<Query, TimelineItem> = neighboredQueryFetcher(
    // Since the API doesn't allow for paging backwards, hold the tokens for a 50 pages
    // in memory
    maxTokens = 50,
    // Make sure the first page has an entry for its cursor/token
    seedQueryTokenMap = mapOf(
        startingQuery to CursorList.DoubleCursor(
            local = startingQuery.data.firstRequestInstant,
            remote = null,
        )
    ),
    fetcher = { query, cursor ->
        cursorListLoader(query.updateData { copy(nextItemCursor = cursor) })
            .map { feedItemCursorList ->
                NeighboredFetchResult(
                    // Set the cursor for the next page and any other page with data available.
                    //
                    mapOf(
                        query.updateData { copy(page = page + 1) } to feedItemCursorList.nextCursor
                    ),
                    items = feedItemCursorList
                )
            }
    }
)

@Suppress("UNCHECKED_CAST")
private inline fun <Query : TimelineQuery> Query.updateData(
    block: TimelineQuery.Data.() -> TimelineQuery.Data
): Query =
    when (this) {
        is TimelineQuery.Home -> copy(data = data.block())
        is TimelineQuery.Profile -> copy(data = data.block())
        else -> throw IllegalArgumentException("Unknown query type")
    } as Query

private fun <Query : TimelineQuery> timelineQueryComparator() = compareBy { query: Query ->
    query.data.page
}


/**
 * When returning from the backstack, the paging pipeline will be started
 * again, causing placeholders to be emitted.
 *
 * To keep preserve the existing state from being overwritten by
 * placeholders, the following algorithm iterates over each tile (chunk) of queries in the
 * [TiledList] to see if placeholders are displacing loaded items.
 *
 * If a displacement were to occur, favor the existing items over the displacing placeholders.
 *
 * Algorithm is O(2 * (3*NumOfColumns)).
 * See the project readme for details: https://github.com/tunjid/Tiler
 */
//private fun State.filterPlaceholdersFrom(
//    fetchedList: TiledList<ListingQuery, TimelineItem>
//) = buildTiledList {
//    val existingMap = 0.until(listings.tileCount).associateBy(
//        keySelector = listings::queryAtTile,
//        valueTransform = { tileIndex ->
//            val existingTile = listings.tileAt(tileIndex)
//            listings.subList(
//                fromIndex = existingTile.start,
//                toIndex = existingTile.end
//            )
//        }
//    )
//    for (tileIndex in 0 until fetchedList.tileCount) {
//        val fetchedTile = fetchedList.tileAt(tileIndex)
//        val fetchedQuery = fetchedList.queryAtTile(tileIndex)
//        when (fetchedList[fetchedTile.start]) {
//            // Items are already loaded, no swap necessary
//            is TimelineItem.Loaded -> addAll(
//                query = fetchedQuery,
//                items = fetchedList.subList(
//                    fromIndex = fetchedTile.start,
//                    toIndex = fetchedTile.end,
//                )
//            )
//            // Placeholder chunk in fetched list, check if loaded items are in the previous list
//            is TimelineItem.Preview,
//            is TimelineItem.Loading -> when (val existingChunk = existingMap[fetchedQuery]) {
//                // No existing items, reuse placeholders
//                null -> addAll(
//                    query = fetchedQuery,
//                    items = fetchedList.subList(
//                        fromIndex = fetchedTile.start,
//                        toIndex = fetchedTile.end,
//                    )
//                )
//
//                // Reuse existing items
//                else -> addAll(
//                    query = fetchedQuery,
//                    items = existingChunk
//                )
//            }
//        }
//    }
//}

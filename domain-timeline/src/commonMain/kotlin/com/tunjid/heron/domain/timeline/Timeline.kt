package com.tunjid.heron.domain.timeline

import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.NetworkCursor
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.repository.TimelineQuery
import com.tunjid.mutator.Mutation
import com.tunjid.tiler.ListTiler
import com.tunjid.tiler.PivotRequest
import com.tunjid.tiler.QueryFetcher
import com.tunjid.tiler.Tile
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.filter
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
    cursorListLoader: (Query, NetworkCursor) -> Flow<CursorList<TimelineItem>>,
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
        val tiledList = timelineTileInputs(numColumns, queries)
            .toTiledList(
                timelineTiler(
                    startingQuery = queries.value,
                    cursorListLoader = cursorListLoader,
                )
            )
            .map(TiledList<Query, TimelineItem>::filterThreadDuplicates)
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
    crossinline cursorListLoader: (Query, NetworkCursor) -> Flow<CursorList<TimelineItem>>,
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
    crossinline cursorListLoader: (Query, NetworkCursor) -> Flow<CursorList<TimelineItem>>,
): QueryFetcher<Query, TimelineItem> = neighboredQueryFetcher<Query, TimelineItem, NetworkCursor>(
    // Since the API doesn't allow for paging backwards, hold the tokens for a 50 pages
    // in memory
    maxTokens = 50,
    // Make sure the first page has an entry for its cursor/token
    seedQueryTokenMap = mapOf(
        startingQuery to NetworkCursor.Initial
    ),
    fetcher = { query, cursor ->
        cursorListLoader(query, cursor)
            .map { networkCursorList ->
                NeighboredFetchResult(
                    // Set the cursor for the next page and any other page with data available.
                    //
                    mapOf(
                        query.updateData { copy(page = page + 1) } to networkCursorList.nextCursor
                    ),
                    items = networkCursorList
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

private fun <Query : TimelineQuery> TiledList<Query, TimelineItem>.filterThreadDuplicates(): TiledList<Query, TimelineItem> {
    val threadRootIds = mutableSetOf<Id>()
    return filter { item ->
        when (item) {
            is TimelineItem.Pinned -> true
            is TimelineItem.Reply -> !threadRootIds.contains(item.rootPost.cid).also { contains ->
                if (!contains) threadRootIds.add(item.rootPost.cid)
            }

            is TimelineItem.Repost -> !threadRootIds.contains(item.post.cid).also { contains ->
                if (!contains) threadRootIds.add(item.post.cid)
            }

            is TimelineItem.Single -> !threadRootIds.contains(item.post.cid)
        }
    }
}

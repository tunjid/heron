package com.tunjid.heron.domain.timeline

import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.repository.TimelineQuery
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.ListTiler
import com.tunjid.tiler.PivotRequest
import com.tunjid.tiler.QueryFetcher
import com.tunjid.tiler.Tile
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.distinctBy
import com.tunjid.tiler.emptyTiledList
import com.tunjid.tiler.filter
import com.tunjid.tiler.listTiler
import com.tunjid.tiler.queries
import com.tunjid.tiler.toPivotedTileInputs
import com.tunjid.tiler.toTiledList
import com.tunjid.tiler.utilities.NeighboredFetchResult
import com.tunjid.tiler.utilities.neighboredQueryFetcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import kotlinx.datetime.Clock

data class TimelineState(
    val timeline: Timeline,
    val currentQuery: TimelineQuery,
    val numColumns: Int,
    val hasUpdates: Boolean,
    val items: TiledList<TimelineQuery, TimelineItem>,
)

typealias TimelineStateHolder = ActionStateMutator<TimelineLoadAction, StateFlow<TimelineState>>

fun timelineStateHolder(
    timeline: Timeline,
    startNumColumns: Int,
    scope: CoroutineScope,
    timelineRepository: TimelineRepository,
): TimelineStateHolder = scope.actionStateFlowMutator(
    initialState = TimelineState(
        timeline = timeline,
        currentQuery = TimelineQuery(
            timeline = timeline,
            data = TimelineQuery.Data(
                page = 0,
                firstRequestInstant = Clock.System.now(),
            ),
        ),
        numColumns = startNumColumns,
        hasUpdates = false,
        items = emptyTiledList(),
    ),
    inputs = listOf(
        hasUpdatesMutations(
            timeline = timeline,
            timelineRepository = timelineRepository,
        )
    ),
    actionTransform = transform@{ actions ->
        actions.toMutationStream(keySelector = { "" }) {
            type().flow.timelineMutations(
                stateHolder = this@transform,
                cursorListLoader = timelineRepository::timelineItems,
            )
        }
    }
)

sealed interface TimelineLoadAction {
    data class GridSize(
        val numColumns: Int,
    ) : TimelineLoadAction

    data class LoadAround(
        val query: TimelineQuery,
    ) : TimelineLoadAction
}

private fun hasUpdatesMutations(
    timeline: Timeline,
    timelineRepository: TimelineRepository,
): Flow<Mutation<TimelineState>> =
    timelineRepository.hasUpdates(timeline)
        .mapToMutation { copy(hasUpdates = it) }

/**
 * Feed mutations as a function of the user's scroll position
 */
private suspend fun Flow<TimelineLoadAction>.timelineMutations(
    stateHolder: SuspendingStateHolder<TimelineState>,
    cursorListLoader: (TimelineQuery, Cursor) -> Flow<CursorList<TimelineItem>>,
) = with(stateHolder) {
    with(this) {
        // Read the starting state at the time of subscription
        val startingState = state()
        scan(
            initial = Pair(
                MutableStateFlow(startingState.currentQuery),
                MutableStateFlow(startingState.numColumns),
            )
        ) { accumulator, action ->
            val (queries, numColumns) = accumulator
            // update backing states as a side effect
            when (action) {
                is TimelineLoadAction.GridSize -> numColumns.value = action.numColumns
                is TimelineLoadAction.LoadAround -> queries.value = action.query
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
                    .map(TiledList<TimelineQuery, TimelineItem>::filterThreadDuplicates)
                    .mapToMutation<TiledList<TimelineQuery, TimelineItem>, TimelineState> {
                        if (!it.queries().contains(currentQuery)) this
                        else copy(
                            items = it.distinctBy(TimelineItem::id)
                        )
                    }
                merge(
                    queries.mapToMutation { copy(currentQuery = it) },
                    numColumns.mapToMutation { copy(numColumns = it) },
                    tiledList,
                )
            }
    }
}

private inline fun timelineTileInputs(
    numColumns: Flow<Int>,
    queries: Flow<TimelineQuery>,
): Flow<Tile.Input<TimelineQuery, TimelineItem>> = merge(
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

private inline fun timelinePivotRequest(numColumns: Int) =
    PivotRequest<TimelineQuery, TimelineItem>(
        onCount = numColumns * 3,
        offCount = numColumns * 2,
        comparator = timelineQueryComparator(),
        previousQuery = {
            if ((data.page - 1) < 0) null
            else updatePage { copy(page = page - 1) }
        },
        nextQuery = {
            updatePage { copy(page = page + 1) }
        }
    )

private inline fun timelineTiler(
    startingQuery: TimelineQuery,
    crossinline cursorListLoader: (TimelineQuery, Cursor) -> Flow<CursorList<TimelineItem>>,
): ListTiler<TimelineQuery, TimelineItem> = listTiler(
    order = Tile.Order.PivotSorted(
        query = startingQuery,
        comparator = timelineQueryComparator(),
    ),
    fetcher = timelineQueryFetcher(
        startingQuery = startingQuery,
        cursorListLoader = cursorListLoader,
    )
)

private inline fun timelineQueryFetcher(
    startingQuery: TimelineQuery,
    crossinline cursorListLoader: (TimelineQuery, Cursor) -> Flow<CursorList<TimelineItem>>,
): QueryFetcher<TimelineQuery, TimelineItem> =
    neighboredQueryFetcher<TimelineQuery, TimelineItem, Cursor>(
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
                            query.updatePage { copy(page = page + 1) } to networkCursorList.nextCursor
                        ),
                        items = networkCursorList
                    )
                }
        }
    )

private inline fun TimelineQuery.updatePage(
    block: TimelineQuery.Data.() -> TimelineQuery.Data,
): TimelineQuery = TimelineQuery(
    timeline = timeline,
    data = data.block(),
)

private fun timelineQueryComparator() = compareBy { query: TimelineQuery ->
    query.data.page
}

private fun TiledList<TimelineQuery, TimelineItem>.filterThreadDuplicates(): TiledList<TimelineQuery, TimelineItem> {
    val threadRootIds = mutableSetOf<Id>()
    return filter { item ->
        when (item) {
            is TimelineItem.Pinned -> true
            is TimelineItem.Thread -> !threadRootIds.contains(item.posts.first().cid)
                .also { contains ->
                    if (!contains) threadRootIds.add(item.posts.first().cid)
                }

            is TimelineItem.Repost -> !threadRootIds.contains(item.post.cid).also { contains ->
                if (!contains) threadRootIds.add(item.post.cid)
            }

            is TimelineItem.Single -> !threadRootIds.contains(item.post.cid)
        }
    }
}

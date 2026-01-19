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

package com.tunjid.heron.tiling

import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.mapCursorList
import com.tunjid.heron.ui.coroutines.requireStateProducingBackgroundDispatcher
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.tiler.ListTiler
import com.tunjid.tiler.PivotRequest
import com.tunjid.tiler.QueryFetcher
import com.tunjid.tiler.Tile
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.emptyTiledList
import com.tunjid.tiler.listTiler
import com.tunjid.tiler.toPivotedTileInputs
import com.tunjid.tiler.toTiledList
import com.tunjid.tiler.utilities.NeighboredFetchResult
import com.tunjid.tiler.utilities.neighboredQueryFetcher
import kotlin.math.max
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

interface TilingState<Query : CursorQuery, Item> {

    val tilingData: Data<Query, Item>

    @Serializable
    data class Data<Query : CursorQuery, Item>(
        val currentQuery: Query,
        val numColumns: Int = 1,
        val status: Status = Status.Initial,
        @Transient
        val items: TiledList<Query, Item> = emptyTiledList(),
    )

    sealed class Action {
        data class GridSize(
            val numColumns: Int,
        ) : Action()

        data class LoadAround(
            val query: CursorQuery,
        ) : Action()

        data object Refresh : Action()
    }

    sealed interface Status {
        data object Initial : Status

        data class Refreshing(
            val cursorAnchor: Instant,
        ) : Status

        data class Refreshed(
            val cursorAnchor: Instant,
        ) : Status
    }
}

val TilingState<*, *>.isRefreshing
    get() = tilingData.status is TilingState.Status.Refreshing

val <Query : CursorQuery, Item> TilingState<Query, Item>.tiledItems
    get() = tilingData.items

fun <Query : CursorQuery, Item> TilingState.Data<Query, Item>.withRefreshedStatus() = copy(
    status = refreshedStatus(),
)

fun <Item, Query : CursorQuery> TilingState.Data<Query, Item>.refreshedStatus() =
    TilingState.Status.Refreshed(
        cursorAnchor = currentQuery.data.cursorAnchor,
    )

/**
 * Feed mutations as a function of the user's scroll position
 */
suspend inline fun <reified Query : CursorQuery, Item, State : TilingState<Query, Item>> Flow<TilingState.Action>.tilingMutations(
    isRefreshedOnNewItems: Boolean = true,
    crossinline currentState: suspend () -> State,
    noinline updateQueryData: Query.(CursorQuery.Data) -> Query,
    crossinline refreshQuery: Query.() -> Query,
    noinline cursorListLoader: (Query, Cursor) -> Flow<CursorList<Item>>,
    crossinline onNewItems: (TiledList<Query, Item>) -> TiledList<Query, Item>,
    crossinline onTilingDataUpdated: State.(TilingState.Data<Query, Item>) -> State,
    noinline queryRefreshBy: (Query) -> Any = { it.data.cursorAnchor },
): Flow<Mutation<State>> {
    // Read the starting state at the time of subscription
    val startingState = currentState().tilingData
    return scan(
        initial = Pair(
            MutableStateFlow(startingState.currentQuery),
            MutableStateFlow(startingState.numColumns),
        ),
    ) { accumulator, action ->
        val (queries, numColumns) = accumulator
        // update backing states as a side effect
        when (action) {
            is TilingState.Action.GridSize -> {
                numColumns.value = action.numColumns
            }

            is TilingState.Action.LoadAround -> {
                if (action.query !is Query) throw IllegalArgumentException(
                    "Expected query of ${Query::class}, got ${action.query::class}",
                )
                val lastQuery = queries.value

                // Everything is okay, proceed.
                val hasSameAnchor = !lastQuery.hasDifferentAnchor(action.query)

                // Favor the query that was requested with a more current anchor.
                // The query with the older anchor was most likely triggered by a scroll
                // at a boundary.
                val isNewerQuery = action.query.data.cursorAnchor > lastQuery.data.cursorAnchor

                if (hasSameAnchor || isNewerQuery) queries.update {
                    action.query
                }
            }

            is TilingState.Action.Refresh -> {
                queries.value = refreshQuery(queries.value)
            }
        }
        // Emit the same item with each action
        accumulator
    }
        // Only emit once
        .distinctUntilChanged()
        .flatMapLatest { (queries, numColumns) ->
            // Refreshes need tear down the tiling pipeline all over
            val refreshes = queries.distinctUntilChangedBy(queryRefreshBy)
            merge(
                queries.mapToMutation { newQuery ->
                    copy(
                        currentQuery = newQuery,
                        status = when {
                            currentQuery.hasDifferentAnchor(newQuery) -> TilingState.Status.Refreshing(
                                cursorAnchor = newQuery.data.cursorAnchor,
                            )

                            else -> status
                        },
                    )
                },
                numColumns.mapToMutation {
                    copy(numColumns = it)
                },
                refreshes.flatMapLatest { refreshedQuery ->
                    cursorTileInputs<Query, Item>(
                        numColumns = numColumns,
                        queries = queries,
                        updatePage = updateQueryData,
                    )
                        .toTiledList(
                            cursorListTiler(
                                startingQuery = refreshedQuery,
                                cursorListLoader = cursorListLoader,
                                updatePage = updateQueryData,
                            ),
                        )
                }
                    .mapToMutation<TiledList<Query, Item>, TilingState.Data<Query, Item>> { items ->
                        // Ignore results from stale queries
                        if (items.isValidFor(currentQuery)) copy(
                            items = onNewItems(items),
                            status = when {
                                isRefreshedOnNewItems && items.isNotEmpty() -> {
                                    val fetchedQuery = items.queryAt(0)
                                    if (fetchedQuery.hasDifferentAnchor(currentQuery)) status
                                    else TilingState.Status.Refreshed(
                                        cursorAnchor = fetchedQuery.data.cursorAnchor,
                                    )
                                }

                                else -> status
                            },
                        )
                        else this
                    },
            )
        }
        .flowOn(currentCoroutineContext().requireStateProducingBackgroundDispatcher())
        .mapToMutation { tilingDataMutation ->
            val updatedTilingData = tilingDataMutation(this.tilingData)
            onTilingDataUpdated(updatedTilingData)
        }
}

inline fun <Query : CursorQuery, T, R> ((Query, Cursor) -> Flow<CursorList<T>>).mapCursorList(
    crossinline mapper: (T) -> R,
): (Query, Cursor) -> Flow<CursorList<R>> = { query, cursor ->
    invoke(query, cursor).map { cursorList ->
        cursorList.mapCursorList(mapper)
    }
}

fun CursorQuery.Data.reset() = copy(page = 0, cursorAnchor = Clock.System.now())

fun CursorQuery.hasDifferentAnchor(newQuery: CursorQuery) =
    data.cursorAnchor != newQuery.data.cursorAnchor

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
            val maxNumColumns = max(1, columns)
            PivotRequest(
                onCount = maxNumColumns * 3,
                offCount = maxNumColumns * 2,
                comparator = cursorQueryComparator(),
                previousQuery = {
                    if ((data.page - 1) < 0) null
                    else updatePage(data.copy(page = data.page - 1))
                },
                nextQuery = {
                    updatePage(data.copy(page = data.page + 1))
                },
            )
        },
    ),
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
    ),
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
            startingQuery to Cursor.Initial,
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
                            ),
                        ),
                        items = networkCursorList,
                    )
                }
        },
    )

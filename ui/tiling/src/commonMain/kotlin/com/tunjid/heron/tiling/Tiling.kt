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

import androidx.compose.runtime.Stable
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.mapCursorList
import com.tunjid.heron.ui.coroutines.launchAndCollectLatestWithState
import com.tunjid.heron.ui.coroutines.launchAndCollectWithState
import com.tunjid.heron.ui.coroutines.requireStateProducingBackgroundDispatcher
import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable
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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Stable
interface TilingState<Query : CursorQuery, Item> {

    val tilingData: Data<Query, Item>

    @Stable
    @Snapshottable
    @Serializable(with = DataSerializer::class)
    sealed interface Data<Query : CursorQuery, Item> {
        @Serializable
        @SnapshotSpec
        data class Immutable<Query : CursorQuery, Item>(
            val currentQuery: Query,
            val numColumns: Int = 1,
            val status: Status = Status.Initial,
            @Transient
            val items: TiledList<Query, Item> = emptyTiledList(),
        ) : Data<Query, Item>

        companion object {
            operator fun <Query : CursorQuery, Item> invoke(
                currentQuery: Query,
                numColumns: Int = 1,
                status: Status = Status.Initial,
                items: TiledList<Query, Item> = emptyTiledList(),
            ): Data<Query, Item> = SnapshotMutable(
                currentQuery = currentQuery,
                numColumns = numColumns,
                status = status,
                items = items,
            ) as Data<Query, Item>
        }
    }

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

@Stable
val TilingState<*, *>.isRefreshing
    get() = tilingData.status is TilingState.Status.Refreshing

@Stable
val <Query : CursorQuery, Item> TilingState<Query, Item>.tiledItems
    get() = tilingData.items

inline fun <reified Query : CursorQuery, reified Item> TilingState.Data<Query, Item>.withRefreshedStatus(): TilingState.Data<Query, Item> {
    check(this is TilingState.Data.SnapshotMutable<Query, Item>)
    return update(
        status = refreshedStatus(),
    )
}

inline fun <reified Query : CursorQuery, reified Item, State : TilingState<Query, Item>> State.updateItems(
    block: TilingState.Data<Query, Item>.() -> TiledList<Query, Item>,
): State {
    tilingData.updateItems(block)
    return this
}

inline fun <reified Query : CursorQuery, reified Item> TilingState.Data<Query, Item>.updateItems(
    block: TilingState.Data<Query, Item>.() -> TiledList<Query, Item>,
): TilingState.Data<Query, Item> {
    check(this is TilingState.Data.SnapshotMutable<Query, Item>)
    items = block()

    return this
}

fun <Item, Query : CursorQuery> TilingState.Data<Query, Item>.refreshedStatus() =
    TilingState.Status.Refreshed(
        cursorAnchor = currentQuery.data.cursorAnchor,
    )

/**
 * Feed mutations as a function of the user's scroll position.
 *
 * The function launches a coroutine in [productionScope] that mutates
 * `currentState().tilingData` (which must be a [TilingState.Data.SnapshotMutable]) directly via
 * Compose snapshot writes. The call returns immediately so the caller's producer block does not
 * stall.
 *
 * Two transform hooks are provided:
 * - [onNewItems] runs on a background dispatcher; use it for CPU-heavy work that does not need to
 *   read mutable state (dedupe, sort, group).
 * - [onWriteItems] runs on [productionScope]'s dispatcher with the live [State] as receiver,
 *   immediately before the snapshot write. Use it to read other snapshot-state fields race-free
 *   and produce the final [TiledList] that gets committed.
 */
context(productionScope: CoroutineScope)
inline fun <reified Query : CursorQuery, Item, State : TilingState<Query, Item>> Flow<TilingState.Action>.launchTilingMutations(
    isRefreshedOnNewItems: Boolean = true,
    state: State,
    noinline updateQueryData: Query.(CursorQuery.Data) -> Query,
    crossinline refreshQuery: Query.() -> Query,
    noinline cursorListLoader: (Query, Cursor) -> Flow<CursorList<Item>>,
    crossinline onNewItems: (TiledList<Query, Item>) -> TiledList<Query, Item>,
    crossinline onWriteItems: State.(TiledList<Query, Item>) -> TiledList<Query, Item> = { it },
    noinline queryRefreshBy: (Query) -> Any = { it.data.cursorAnchor },
) {
    productionScope.launch {
        // Read the starting state at the time of subscription
        val startingState: TilingState.Data<Query, Item> = state.tilingData
        check(startingState is TilingState.Data.SnapshotMutable) {
            "Tiling state must be snapshot mutable"
        }

        scan(
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
            .collectLatest { (queries, numColumns) ->
                coroutineScope {
                    val backgroundDispatcher =
                        currentCoroutineContext().requireStateProducingBackgroundDispatcher()
                    // Refreshes need tear down the tiling pipeline all over
                    val refreshes = queries.distinctUntilChangedBy(queryRefreshBy)
                    queries.launchAndCollectWithState(startingState) { newQuery ->
                        status = when {
                            currentQuery.hasDifferentAnchor(newQuery) -> TilingState.Status.Refreshing(
                                cursorAnchor = newQuery.data.cursorAnchor,
                            )

                            else -> status
                        }
                        currentQuery = newQuery
                    }
                    numColumns.launchAndCollectWithState(startingState) {
                        update(numColumns = it)
                    }
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
                        .debounce { items ->
                            if (items.isEmpty()) 1.seconds
                            else 500.milliseconds
                        }
                        .flowOn(backgroundDispatcher)
                        .launchAndCollectLatestWithState(startingState) { items ->
                            // Ignore results from stale queries
                            if (items.isValidFor(currentQuery)) {
                                // Heavy work on the background dispatcher
                                val deduped = withContext(backgroundDispatcher) {
                                    onNewItems(items)
                                }
                                // Final transform on the production dispatcher with State as
                                // receiver, so callers can read live snapshot-state fields
                                // race-free with other producer-scope writers.
                                val toCommit = with(state) { onWriteItems(deduped) }
                                update(
                                    items = toCommit,
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
                            }
                        }
                }
            }
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
): Boolean {
    if (isEmpty()) return true

    // Ignore results from stale queries
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

object DataSerializer : KSerializer<TilingState.Data<*, *>> {
    private val delegate = PolymorphicSerializer(TilingState.Data::class)
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: TilingState.Data<*, *>) {
        val immutable: TilingState.Data.Immutable<out CursorQuery, out Any?> = when (value) {
            is TilingState.Data.Immutable<*, *> -> value
            is TilingState.Data.SnapshotMutable<*, *> -> value.toSnapshotSpec()
        }
        delegate.serialize(encoder, immutable)
    }

    override fun deserialize(decoder: Decoder): TilingState.Data<*, *> {
        return delegate.deserialize(decoder)
    }
}

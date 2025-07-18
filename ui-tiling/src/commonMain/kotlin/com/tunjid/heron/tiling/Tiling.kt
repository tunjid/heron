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
import com.tunjid.heron.data.core.models.mapCursorList
import com.tunjid.heron.data.utilities.CursorQuery
import com.tunjid.heron.data.utilities.cursorListTiler
import com.tunjid.heron.data.utilities.cursorTileInputs
import com.tunjid.heron.data.utilities.hasDifferentAnchor
import com.tunjid.heron.data.utilities.isValidFor
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.emptyTiledList
import com.tunjid.tiler.toTiledList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Instant
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

val <Query: CursorQuery, Item> TilingState<Query, Item>.tiledItems
    get() = tilingData.items

fun <Action : Any, State : TilingState<*, *>> ActionStateMutator<Action, StateFlow<State>>.tilingAction(
    tilingAction: TilingState.Action,
    stateHolderAction: (TilingState.Action) -> Action,
) = accept(stateHolderAction(tilingAction))

/**
 * Feed mutations as a function of the user's scroll position
 */
suspend inline fun <reified Query : CursorQuery, Item, State : TilingState<Query, Item>> Flow<TilingState.Action>.tilingMutations(
    crossinline currentState: suspend () -> State,
    crossinline onRefreshQuery: (Query) -> Query,
    crossinline onNewItems: (TiledList<Query, Item>) -> TiledList<Query, Item>,
    crossinline onTilingDataUpdated: State.(TilingState.Data<Query, Item>) -> State,
    noinline updatePage: Query.(CursorQuery.Data) -> Query,
    noinline cursorListLoader: (Query, Cursor) -> Flow<CursorList<Item>>,
    noinline queryRefreshBy: (Query) -> Any = { it.data.cursorAnchor },
): Flow<Mutation<State>> {
    // Read the starting state at the time of subscription
    val startingState = currentState().tilingData
    return scan(
        initial = Pair(
            MutableStateFlow(startingState.currentQuery),
            MutableStateFlow(startingState.numColumns),
        )
    ) { accumulator, action ->
        val (queries, numColumns) = accumulator
        // update backing states as a side effect
        when (action) {
            is TilingState.Action.GridSize -> {
                numColumns.value = action.numColumns
            }

            is TilingState.Action.LoadAround -> {
                if (action.query !is Query) throw IllegalArgumentException(
                    "Expected query of ${Query::class}, got ${action.query::class}"
                )
                val lastQuery = queries.value
                val hasSameAnchor = !lastQuery.hasDifferentAnchor(action.query)
                val isNewerQuery = action.query.data.cursorAnchor > lastQuery.data.cursorAnchor
                if (hasSameAnchor || isNewerQuery) queries.update {
                    action.query
                }
            }

            is TilingState.Action.Refresh -> {
                queries.value = onRefreshQuery(queries.value)
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
                queries.mapToMutation<Query, TilingState.Data<Query, Item>> { newQuery ->
                    copy(
                        currentQuery = newQuery,
                        status =
                            if (currentQuery.hasDifferentAnchor(newQuery)) TilingState.Status.Refreshing(
                                cursorAnchor = newQuery.data.cursorAnchor,
                            )
                            else status
                    )
                },
                numColumns.mapToMutation {
                    copy(numColumns = it)
                },
                refreshes.flatMapLatest { refreshedQuery ->
                    cursorTileInputs<Query, Item>(
                        numColumns = numColumns,
                        queries = queries,
                        updatePage = updatePage,
                    )
                        .toTiledList(
                            cursorListTiler(
                                startingQuery = refreshedQuery,
                                cursorListLoader = cursorListLoader,
                                updatePage = updatePage,
                            )
                        )
                }
                    .mapToMutation<TiledList<Query, Item>, TilingState.Data<Query, Item>> { items ->
                        // Ignore results from stale queries
                        if (items.isValidFor(currentQuery)) copy(items = onNewItems(items))
                        else this
                    },
            )
        }
        .mapToMutation { tilingDataMutation ->
            val updatedTilingData = tilingDataMutation(this.tilingData)
            onTilingDataUpdated(updatedTilingData)
        }
}

inline fun <Query : CursorQuery, T, R> ((Query, Cursor) -> Flow<CursorList<T>>).mapCursorList(
    crossinline mapper: (T) -> R
): (Query, Cursor) -> Flow<CursorList<R>> = { query, cursor ->
    invoke(query, cursor).map { cursorList ->
        cursorList.mapCursorList(mapper)
    }
}
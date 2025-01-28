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

package com.tunjid.heron.domain.timeline

import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.repository.TimelineQuery
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.utilities.CursorQuery
import com.tunjid.heron.data.utilities.cursorListTiler
import com.tunjid.heron.data.utilities.cursorTileInputs
import com.tunjid.heron.data.utilities.hasDifferentAnchor
import com.tunjid.heron.data.utilities.isValidFor
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.distinctBy
import com.tunjid.tiler.emptyTiledList
import com.tunjid.tiler.filter
import com.tunjid.tiler.toTiledList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

sealed class TimelineStatus {
    data object Initial : TimelineStatus()

    data class Refreshing(
        val cursorAnchor: Instant,
    ) : TimelineStatus()

    data class Refreshed(
        val cursorAnchor: Instant,
    ) : TimelineStatus()
}

data class TimelineState(
    val timeline: Timeline,
    val currentQuery: TimelineQuery,
    val numColumns: Int,
    val hasUpdates: Boolean,
    val status: TimelineStatus,
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
            data = CursorQuery.Data(
                page = 0,
                cursorAnchor = when (timeline) {
                    is Timeline.Home -> timeline.lastRefreshed ?: Clock.System.now()
                    is Timeline.Profile -> Clock.System.now()
                },
            ),
        ),
        numColumns = startNumColumns,
        hasUpdates = false,
        status = TimelineStatus.Initial,
        items = emptyTiledList(),
    ),
    inputs = listOf(
        hasUpdatesMutations(
            timeline = timeline,
            timelineRepository = timelineRepository,
        )
    ),
    actionTransform = transform@{ actions ->
        actions.toMutationStream(keySelector = TimelineLoadAction::key) {
            type().flow.timelineMutations(
                stateHolder = this@transform,
                cursorListLoader = timelineRepository::timelineItems,
            )
        }
    }
)

sealed interface TimelineLoadAction {

    // Every action uses the same key, so they're processed sequentially
    val key get() = "TimelineLoadAction"

    data class GridSize(
        val numColumns: Int,
    ) : TimelineLoadAction

    data class LoadAround(
        val query: TimelineQuery,
    ) : TimelineLoadAction

    data object Refresh : TimelineLoadAction
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
                is TimelineLoadAction.GridSize -> {
                    numColumns.value = action.numColumns
                }

                is TimelineLoadAction.LoadAround -> {
                    if (!queries.value.hasDifferentAnchor(action.query))
                        queries.value = action.query
                }

                is TimelineLoadAction.Refresh -> {
                    queries.value = TimelineQuery(
                        timeline = queries.value.timeline,
                        data = queries.value.data.copy(
                            page = 0,
                            cursorAnchor = Clock.System.now(),
                        ),
                    )
                }
            }
            // Emit the same item with each action
            accumulator
        }
            // Only emit once
            .distinctUntilChanged()
            .flatMapLatest { (queries, numColumns) ->
                merge(
                    queryMutations(queries),
                    numColumnMutations(numColumns),
                    itemMutations(
                        queries = queries,
                        numColumns = numColumns,
                        cursorListLoader = cursorListLoader
                    ),
                )
            }
    }
}

private fun queryMutations(queries: MutableStateFlow<TimelineQuery>) =
    queries.mapToMutation<TimelineQuery, TimelineState> { newQuery ->
        copy(
            currentQuery = newQuery,
            status =
            if (currentQuery.hasDifferentAnchor(newQuery)) TimelineStatus.Refreshing(
                cursorAnchor = newQuery.data.cursorAnchor,
            )
            else status
        )
    }

private fun numColumnMutations(numColumns: Flow<Int>): Flow<Mutation<TimelineState>> =
    numColumns.mapToMutation { copy(numColumns = it) }

private fun itemMutations(
    queries: Flow<TimelineQuery>,
    numColumns: Flow<Int>,
    cursorListLoader: (TimelineQuery, Cursor) -> Flow<CursorList<TimelineItem>>,
): Flow<Mutation<TimelineState>> {
    // Refreshes need tear down the tiling pipeline all over
    val refreshes = queries.distinctUntilChangedBy {
        it.data.cursorAnchor
    }
    val updatePage: TimelineQuery.(CursorQuery.Data) -> TimelineQuery = {
        TimelineQuery(
            timeline = timeline,
            data = it
        )
    }
    val tiledListMutations = refreshes.flatMapLatest { refreshedQuery ->
        cursorTileInputs<TimelineQuery, TimelineItem>(
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
        .map(TiledList<TimelineQuery, TimelineItem>::filterThreadDuplicates)
        .mapToMutation<TiledList<TimelineQuery, TimelineItem>, TimelineState> {
            // Ignore results from stale queries
            if (it.isValidFor(currentQuery)) copy(
                status = TimelineStatus.Refreshed(
                    cursorAnchor = currentQuery.data.cursorAnchor
                ),
                items = it.distinctBy(TimelineItem::id)
            )
            else this
        }
    return tiledListMutations
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

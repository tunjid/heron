/*
 * Copyright 2024 Adetunji Dahunsi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.heron.postdetail


import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Constants
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.FeedItem
import com.tunjid.heron.data.repository.FeedQuery
import com.tunjid.heron.data.repository.FeedRepository
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.PivotRequest
import com.tunjid.tiler.QueryFetcher
import com.tunjid.tiler.Tile
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.distinctBy
import com.tunjid.tiler.listTiler
import com.tunjid.tiler.queries
import com.tunjid.tiler.toPivotedTileInputs
import com.tunjid.tiler.toTiledList
import com.tunjid.tiler.utilities.NeighboredFetchResult
import com.tunjid.tiler.utilities.neighboredQueryFetcher
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import kotlinx.datetime.Clock
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

typealias PostDetailStateHolder = ActionStateMutator<Action, StateFlow<State>>

@Inject
class PostDetailStateHolderCreator(
    private val creator: (scope: CoroutineScope, route: Route) -> ActualPostDetailStateHolder
) : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route
    ): ActualPostDetailStateHolder = creator.invoke(scope, route)
}

@Inject
class ActualPostDetailStateHolder(
    feedRepository: FeedRepository,
    navActions: (NavigationMutation) -> Unit,
    @Assisted
    scope: CoroutineScope,
    @Suppress("UNUSED_PARAMETER")
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope), PostDetailStateHolder by scope.actionStateFlowMutator(
    initialState = State(
        currentQuery = FeedQuery(
            page = 0,
            source = Constants.timelineFeed,
            firstRequestInstant = Clock.System.now(),
        )
    ),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    inputs = listOf(
    ),
    actionTransform = transform@{ actions ->
        actions.toMutationStream(
            keySelector = Action::key
        ) {
            when (val action = type()) {
                is Action.LoadFeed -> action.flow.fetchListingFeedMutations(
                    stateHolder = this@transform,
                    feedRepository = feedRepository,
                )

                is Action.Navigate -> action.flow.consumeNavigationActions(
                    navigationMutationConsumer = navActions
                )
            }
        }
    }
)


/**
 * Feed mutations as a function of the user's scroll position
 */
private suspend fun Flow<Action.LoadFeed>.fetchListingFeedMutations(
    stateHolder: SuspendingStateHolder<State>,
    feedRepository: FeedRepository,
): Flow<Mutation<State>> = with(stateHolder) {
    // Read the starting state at the time of subscription
    val startingState = state()

    return scan(
        initial = Pair(
            MutableStateFlow(startingState.currentQuery),
            MutableStateFlow(startingState.numColumns)
        )
    ) { accumulator, action ->
        val (queries, numColumns) = accumulator
        // update backing states as a side effect
        when (action) {
            is Action.LoadFeed.GridSize -> numColumns.value = action.numColumns
            is Action.LoadFeed.LoadAround -> queries.value = action.query
        }
        // Emit the same item with each action
        accumulator
    }
        // Only emit once
        .distinctUntilChanged()
        // Flatmap to the fields defined earlier
        .flatMapLatest { (queries, numColumns) ->
            val tileInputs = merge(
                numColumns.map { columns ->
                    Tile.Limiter(
                        maxQueries = 3 * columns,
                        itemSizeHint = null,
                    )
                },
                queries.toPivotedTileInputs(
                    numColumns.map(::feedPivotRequest)
                )
            )
            // Merge all state changes that are a function of loading the list
            merge(
                queries.mapToMutation { copy(currentQuery = it) },
                numColumns.mapToMutation { copy(numColumns = it) },
                tileInputs.toTiledList(
                    feedItemListTiler(
                        startingQuery = queries.value,
                        feedRepository = feedRepository,
                    )
                )
                    .mapToMutation { fetchedList ->
                        // Queries update independently of each other, so duplicates may be emitted.
                        // The maximum amount of items returned is bound by the size of the
                        // view port. Typically << 100 items so the
                        // distinct operation is cheap and fixed.
                        if (!fetchedList.queries().contains(currentQuery)) this
                        else copy(
                            feed = fetchedList.distinctBy(FeedItem::id)
                        )
                    }
            )
        }
}

private fun feedPivotRequest(numColumns: Int) =
    PivotRequest<FeedQuery, FeedItem>(
        onCount = numColumns * 3,
        offCount = numColumns * 2,
        comparator = ListingQueryComparator,
        previousQuery = {
            if ((page - 1) < 0) null
            else copy(page = page - 1)
        },
        nextQuery = {
            copy(page = page + 1)
        }
    )

private fun feedItemListTiler(
    startingQuery: FeedQuery,
    feedRepository: FeedRepository,
) = listTiler(
    order = Tile.Order.PivotSorted(
        query = startingQuery,
        comparator = ListingQueryComparator,
    ),
    fetcher = feedItemQueryFetcher(startingQuery, feedRepository)
)

private val ListingQueryComparator = compareBy(FeedQuery::page)

fun feedItemQueryFetcher(
    startingQuery: FeedQuery,
    feedRepository: FeedRepository,
): QueryFetcher<FeedQuery, FeedItem> = neighboredQueryFetcher(
    // Since the API doesn't allow for paging backwards, hold the tokens for a 50 pages
    // in memory
    maxTokens = 50,
    // Make sure the first page has an entry for its cursor/token
    seedQueryTokenMap = mapOf(
        startingQuery to CursorList.DoubleCursor(
            local = startingQuery.firstRequestInstant,
            remote = null,
        )
    ),
    fetcher = { query, cursor ->
        feedRepository
            .timeline(query.copy(nextItemCursor = cursor))
            .map { feedItemCursorList ->
                NeighboredFetchResult(
                    // Set the cursor for the next page and any other page with data available.
                    // This will cause the fetcher for the pages to be invoked if they are in scope.
                    mapOf(
                        query.copy(page = query.page + 1) to feedItemCursorList.nextCursor
                    ),
                    items = feedItemCursorList
                )
            }
    }
)


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
//    fetchedList: TiledList<ListingQuery, FeedItem>
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
//            is FeedItem.Loaded -> addAll(
//                query = fetchedQuery,
//                items = fetchedList.subList(
//                    fromIndex = fetchedTile.start,
//                    toIndex = fetchedTile.end,
//                )
//            )
//            // Placeholder chunk in fetched list, check if loaded items are in the previous list
//            is FeedItem.Preview,
//            is FeedItem.Loading -> when (val existingChunk = existingMap[fetchedQuery]) {
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


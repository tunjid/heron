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

package com.tunjid.heron.home


import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Constants
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.FeedItem
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.repository.FeedQuery
import com.tunjid.heron.data.repository.FeedRepository
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.QueryFetcher
import com.tunjid.tiler.utilities.NeighboredFetchResult
import com.tunjid.tiler.utilities.neighboredQueryFetcher
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

typealias HomeStateHolder = ActionStateMutator<Action, StateFlow<State>>

@Inject
class HomeStateHolderCreator(
    private val creator: (scope: CoroutineScope, route: Route) -> ActualHomeStateHolder
) : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route
    ): ActualHomeStateHolder = creator.invoke(scope, route)
}

@Inject
class ActualHomeStateHolder(
    feedRepository: FeedRepository,
    navActions: (NavigationMutation) -> Unit,
    @Assisted
    scope: CoroutineScope,
    @Suppress("UNUSED_PARAMETER")
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope), HomeStateHolder by scope.actionStateFlowMutator(
    initialState = State(),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    inputs = listOf(
        flow {
            feedRepository.timeline(
                FeedQuery(
                    page = 0,
                    source = Constants.timelineFeed,
                    firstRequestInstant = Clock.System.now(),
                )
            ).collect()
        },
    ),
    actionTransform = { actions ->
        actions.toMutationStream(
            keySelector = Action::key
        ) {
            when (val action = type()) {

                is Action.Navigate -> action.flow.consumeNavigationActions(
                    navigationMutationConsumer = navActions
                )
            }
        }
    }
)


fun feedItemQueryFetcher(
    source: Uri,
    feedRepository: FeedRepository,
): QueryFetcher<FeedQuery, FeedItem> = neighboredQueryFetcher(
    // 5 tokens are held in a LIFO queue
    maxTokens = 5,
    // Make sure the first page has an entry for its cursor/token
    seedQueryTokenMap = mapOf<FeedQuery, CursorList.DoubleCursor?>(
        FeedQuery(
            page = 0,
            source = source,
            firstRequestInstant = Clock.System.now(),
        ) to null
    ),
    fetcher = { query, cursor ->
        feedRepository
            .timeline(query.copy(nextItemCursor = cursor))
            .map { feedItemCursorList ->
                NeighboredFetchResult(
                    // Set the cursor for the next page and any other page with data available.
                    // This will cause the fetcher for the pages to be invoked if they are in scope.
                    mapOf(query.copy(page = query.page + 1) to feedItemCursorList.nextCursor),
                    items = feedItemCursorList
                )
            }
    }
)

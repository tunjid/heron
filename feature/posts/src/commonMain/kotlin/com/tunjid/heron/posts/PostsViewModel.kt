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

package com.tunjid.heron.posts

import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.AppliedLabels
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.appliedLabels
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.repository.PostDataQuery
import com.tunjid.heron.data.repository.PostRepository
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.posts.di.PostsRequest
import com.tunjid.heron.posts.di.postsRequest
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.heron.ui.text.Memo
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.mutator.mutationOf
import com.tunjid.tiler.buildTiledList
import com.tunjid.tiler.emptyTiledList
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

internal typealias PostsStateHolder = ActionStateMutator<Action, StateFlow<State>>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualPostsViewModel
}

@Inject
class ActualPostsViewModel(
    navActions: (NavigationMutation) -> Unit,
    postsRepository: PostRepository,
    @Assisted
    scope: CoroutineScope,
    @Suppress("UNUSED_PARAMETER")
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope),
    PostsStateHolder by scope.actionStateFlowMutator(
        initialState = State(),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        inputs = listOf(
            postsMutations(
                request = route.postsRequest,
                postsRepository = postsRepository,
            ),
        ),
        actionTransform = transform@{ actions ->
            actions.toMutationStream(
                keySelector = Action::key,
            ) {
                when (val action = type()) {
                    is Action.SnackbarDismissed -> action.flow.snackbarDismissalMutations()
                    is Action.Navigate -> action.flow.consumeNavigationActions(
                        navigationMutationConsumer = navActions,
                    )
                }
            }
        },
    )

private fun postsMutations(
    request: PostsRequest,
    postsRepository: PostRepository,
): Flow<Mutation<State>> = when (request) {
    is PostsRequest.Saved -> {
        // TODO: Fetch Saved posts - return empty for now
        flowOf(mutationOf { copy(posts = emptyTiledList()) })
    }
    is PostsRequest.Quotes -> {
        val initialQuery = PostDataQuery(
            profileId = request.profileHandleOrId,
            postRecordKey = request.postRecordKey,
            data = CursorQuery.Data(
                page = 0,
                cursorAnchor = Clock.System.now(),
            ),
        )

        postsRepository.quotes(
            query = initialQuery,
            cursor = Cursor.Initial,
        ).mapToMutation { quotesResult ->
            val timelineItems = quotesResult.items.map { post ->
                TimelineItem.Single(
                    id = post.uri.uri,
                    post = post,
                    appliedLabels = post.appliedLabels(
                        labelers = emptyList(),
                        labelPreferences = emptyList(),
                    ),
                )
            }

            val tiledList = buildTiledList<PostDataQuery, TimelineItem> {
                addAll(initialQuery, timelineItems)
            }
            copy(posts = tiledList)
        }
    }
}

private fun Flow<Action.SnackbarDismissed>.snackbarDismissalMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(messages = messages - action.message)
    }

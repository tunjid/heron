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

package com.tunjid.heron.profiles


import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Constants
import com.tunjid.heron.data.core.models.ProfileWithRelationship
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.PostDataQuery
import com.tunjid.heron.data.repository.PostRepository
import com.tunjid.heron.data.utilities.CursorQuery
import com.tunjid.heron.data.utilities.cursorListTiler
import com.tunjid.heron.data.utilities.cursorTileInputs
import com.tunjid.heron.data.utilities.isValidFor
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.profiles.di.load
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.toTiledList
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

typealias ProfilesStateHolder = ActionStateMutator<Action, StateFlow<State>>

@Inject
class ProfilesStateHolderCreator(
    private val creator: (scope: CoroutineScope, route: Route) -> ActualProfilesViewModel,
) : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualProfilesViewModel = creator.invoke(scope, route)
}

@Inject
class ActualProfilesViewModel(
    navActions: (NavigationMutation) -> Unit,
    authRepository: AuthRepository,
    postRepository: PostRepository,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope), ProfilesStateHolder by scope.actionStateFlowMutator(
    initialState = State(
        currentQuery = when (val load = route.load) {
            is Load.Post -> PostDataQuery(
                postId = load.postId,
                data = CursorQuery.Data(
                    page = 0,
                    cursorAnchor = Clock.System.now(),
                )
            )

            // TODO: Actually query for profiles
            is Load.Profile -> PostDataQuery(
                postId = Constants.unknownPostId,
                data = CursorQuery.Data(
                    page = 0,
                    cursorAnchor = Clock.System.now(),
                )
            )
        }
    ),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    inputs = listOf(
        loadSignedInProfileIdMutations(
            authRepository = authRepository
        )
    ),
    actionTransform = transform@{ actions ->
        actions.toMutationStream(
            keySelector = Action::key
        ) {
            when (val action = type()) {

                is Action.LoadAround -> action.flow.profilesLoadMutations(
                    load = route.load,
                    stateHolder = this@transform,
                    postRepository = postRepository,
                )

                is Action.Navigate -> action.flow.consumeNavigationActions(
                    navigationMutationConsumer = navActions
                )
            }
        }
    }
)

private fun loadSignedInProfileIdMutations(
    authRepository: AuthRepository,
): Flow<Mutation<State>> =
    authRepository.signedInUser.mapToMutation {
        copy(signedInProfileId = it?.did)
    }

suspend fun Flow<Action.LoadAround>.profilesLoadMutations(
    load: Load,
    stateHolder: SuspendingStateHolder<State>,
    postRepository: PostRepository,
): Flow<Mutation<State>> {
    val startingQuery = stateHolder.state().currentQuery
    val updatePage: CursorQuery.(CursorQuery.Data) -> CursorQuery = {
        when (this) {
            is PostDataQuery -> copy(data = it)
            else -> throw IllegalArgumentException("Invalid query")
        }

    }
    return cursorTileInputs<CursorQuery, ProfileWithRelationship>(
        numColumns = flowOf(1),
        queries = map { it.query },
        updatePage = updatePage,
    )
        .toTiledList(
            cursorListTiler(
                startingQuery = startingQuery,
                cursorListLoader = { query, cursor ->
                    when (load) {
                        is Load.Post.Likes -> {
                            check(query is PostDataQuery)
                            postRepository.likedBy(query, cursor)
                        }

                        is Load.Post.Reposts -> {
                            check(query is PostDataQuery)
                            postRepository.repostedBy(query, cursor)
                        }

                        is Load.Profile.Followers -> {
                            // TODO()
                            emptyFlow()
                        }

                        is Load.Profile.Following -> {
                            // TODO()
                            emptyFlow()
                        }
                    }
                },
                updatePage = updatePage,
            )
        )
        .mapToMutation {
            if (it.isValidFor(currentQuery)) copy(profiles = it)
            else this
        }
}
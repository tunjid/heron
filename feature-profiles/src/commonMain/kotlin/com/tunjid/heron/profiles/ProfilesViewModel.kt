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
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.NotificationsQuery
import com.tunjid.heron.data.repository.PostDataQuery
import com.tunjid.heron.data.repository.PostRepository
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.ProfilesQuery
import com.tunjid.heron.data.utilities.CursorQuery
import com.tunjid.heron.data.utilities.cursorListTiler
import com.tunjid.heron.data.utilities.cursorTileInputs
import com.tunjid.heron.data.utilities.hasDifferentAnchor
import com.tunjid.heron.data.utilities.isValidFor
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.profiles.di.load
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.distinctBy
import com.tunjid.tiler.toTiledList
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import kotlinx.datetime.Clock

internal typealias ProfilesStateHolder = ActionStateMutator<Action, StateFlow<State>>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualProfilesViewModel
}

@Inject
class ActualProfilesViewModel(
    navActions: (NavigationMutation) -> Unit,
    authRepository: AuthRepository,
    postRepository: PostRepository,
    profileRepository: ProfileRepository,
    writeQueue: WriteQueue,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope), ProfilesStateHolder by scope.actionStateFlowMutator(
    initialState = State(
        currentQuery = when (val load = route.load) {
            is Load.Post -> PostDataQuery(
                profileId = load.profileId,
                postRecordKey = load.postRecordKey,
                data = CursorQuery.Data(
                    page = 0,
                    cursorAnchor = Clock.System.now(),
                )
            )

            is Load.Profile -> ProfilesQuery(
                profileId = load.profileId,
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

                is Action.Fetch -> action.flow.profilesLoadMutations(
                    load = route.load,
                    stateHolder = this@transform,
                    postRepository = postRepository,
                    profileRepository = profileRepository,
                )

                is Action.ToggleViewerState -> action.flow.toggleViewerStateMutations(
                    writeQueue = writeQueue,
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

suspend fun Flow<Action.Fetch>.profilesLoadMutations(
    load: Load,
    stateHolder: SuspendingStateHolder<State>,
    postRepository: PostRepository,
    profileRepository: ProfileRepository,
): Flow<Mutation<State>> = scan(
    initial = MutableStateFlow(stateHolder.state().currentQuery)
) { queries, action ->
    // update backing states as a side effect
    when (action) {
        is Action.Fetch.LoadAround -> {
            if (!queries.value.hasDifferentAnchor(action.query))
                queries.value = action.query
        }

        is Action.Fetch.Refresh -> {
            queries.value = NotificationsQuery(
                data = queries.value.data.copy(
                    page = 0,
                    cursorAnchor = Clock.System.now(),
                ),
            )
        }
    }
    // Emit the same item with each action
    queries
}
    // Only emit once
    .distinctUntilChanged()
    .flatMapLatest { queries ->
        merge(
            queryMutations(queries),
            itemMutations(
                load = load,
                queries = queries,
                profileRepository = profileRepository,
                postRepository = postRepository,
            ),
        )
    }

private fun queryMutations(queries: MutableStateFlow<CursorQuery>) =
    queries.mapToMutation<CursorQuery, State> { newQuery ->
        copy(
            currentQuery = newQuery,
            isRefreshing = if (currentQuery.hasDifferentAnchor(newQuery)) true else isRefreshing
        )
    }

private fun itemMutations(
    load: Load,
    queries: Flow<CursorQuery>,
    profileRepository: ProfileRepository,
    postRepository: PostRepository,
): Flow<Mutation<State>> {
    // Refreshes need to tear down the tiling pipeline all over
    val refreshes = queries.distinctUntilChangedBy {
        it.data.cursorAnchor
    }
    val updatePage: CursorQuery.(CursorQuery.Data) -> CursorQuery = {
        when (this) {
            is PostDataQuery -> copy(data = it)
            is ProfilesQuery -> copy(data = it)
            else -> throw IllegalArgumentException("Invalid query")
        }

    }

    return refreshes.flatMapLatest { refreshedQuery ->
        cursorTileInputs<CursorQuery, ProfileWithViewerState>(
            numColumns = flowOf(1),
            queries = queries,
            updatePage = updatePage,
        )
            .toTiledList(
                cursorListTiler(
                    startingQuery = refreshedQuery,
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
                                check(query is ProfilesQuery)
                                profileRepository.followers(query, cursor)
                            }

                            is Load.Profile.Following -> {
                                check(query is ProfilesQuery)
                                profileRepository.following(query, cursor)
                            }
                        }
                    },
                    updatePage = updatePage,
                )
            )
    }
        .mapToMutation { profiles ->
            if (profiles.isValidFor(currentQuery)) copy(
                profiles = profiles.distinctBy { it.profile.did }
            )
            else this
        }
}

private fun Flow<Action.ToggleViewerState>.toggleViewerStateMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> =
    mapToManyMutations { action ->
        writeQueue.enqueue(
            Writable.Connection(
                when (val following = action.following) {
                    null -> Profile.Connection.Follow(
                        signedInProfileId = action.signedInProfileId,
                        profileId = action.viewedProfileId,
                        followedBy = action.followedBy,
                    )

                    else -> Profile.Connection.Unfollow(
                        signedInProfileId = action.signedInProfileId,
                        profileId = action.viewedProfileId,
                        followUri = following,
                        followedBy = action.followedBy,
                    )
                }
            )
        )
    }
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
import com.tunjid.heron.data.core.models.DataQuery
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.PostDataQuery
import com.tunjid.heron.data.repository.PostRepository
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.ProfilesQuery
import com.tunjid.heron.data.repository.RecordRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.profiles.di.load
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.tiling.launchTilingMutations
import com.tunjid.heron.tiling.reset
import com.tunjid.heron.timeline.utilities.launchAndCollectEnqueueMutations
import com.tunjid.heron.ui.coroutines.launchAndCollect
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.tiler.distinctBy
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map

internal typealias ProfilesStateHolder = ActionSuspendingStateMutator<Action, State>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualProfilesViewModel
}

@AssistedInject
class ActualProfilesViewModel(
    navActions: (NavigationMutation) -> Unit,
    authRepository: AuthRepository,
    postRepository: PostRepository,
    profileRepository: ProfileRepository,
    recordRepository: RecordRepository,
    writeQueue: WriteQueue,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope),
    ProfilesStateHolder by scope.actionSuspendingStateMutator(
        initialState = State(route).toSnapshotMutable(),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        producer = { state, actions ->
            launchLoadSignedInProfileIdMutations(
                state = state,
                authRepository = authRepository,
            )
            actions.launchMutationsIn(
                productionScope = this,
                keySelector = Action::key,
            ) {
                when (val action = type()) {
                    is Action.Tile -> action.flow.launchProfilesLoadMutations(
                        state = state,
                        load = route.load,
                        postRepository = postRepository,
                        profileRepository = profileRepository,
                        recordRepository = recordRepository,
                    )
                    is Action.ToggleViewerState -> action.flow.launchToggleViewerStateMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is Action.SnackbarDismissed -> action.flow.launchSnackbarDismissalMutations(state)
                    is Action.Navigate -> action.flow.collect {
                        navActions(it.navigationMutation)
                    }
                }
            }
        },
    )

context(productionScope: CoroutineScope)
private fun launchLoadSignedInProfileIdMutations(
    state: State.SnapshotMutable,
    authRepository: AuthRepository,
) = authRepository.signedInUser.launchAndCollect {
    state.signedInProfileId = it?.did
}

context(productionScope: CoroutineScope)
internal fun Flow<Action.Tile>.launchProfilesLoadMutations(
    state: State.SnapshotMutable,
    load: Load,
    postRepository: PostRepository,
    profileRepository: ProfileRepository,
    recordRepository: RecordRepository,
) = map { it.tilingAction }
    .launchTilingMutations(
        state = state,
        updateQueryData = {
            when (this) {
                is PostDataQuery -> copy(data = it)
                is ProfilesQuery -> copy(data = it)
                is DataQuery -> copy(data = it)
                else -> throw IllegalArgumentException("Invalid query")
            }
        },
        refreshQuery = {
            when (this) {
                is PostDataQuery -> copy(data = data.reset())
                is ProfilesQuery -> copy(data = data.reset())
                is DataQuery -> copy(data = data.reset())
                else -> throw IllegalArgumentException("Invalid query")
            }
        },
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
                is Load.Moderation.Blocks -> {
                    check(query is DataQuery)
                    recordRepository.blocks(query, cursor)
                }
                is Load.Moderation.Mutes -> {
                    check(query is DataQuery)
                    profileRepository.mutes(query, cursor)
                }
            }
        },
        onNewItems = { profiles -> profiles.distinctBy { it.profile.did } },
    )

context(productionScope: CoroutineScope)
private fun Flow<Action.ToggleViewerState>.launchToggleViewerStateMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { action ->
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
            },
        )
    },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.SnackbarDismissed>.launchSnackbarDismissalMutations(
    state: State.SnapshotMutable,
) = launchAndCollect { event ->
    state.messages -= event.message
}

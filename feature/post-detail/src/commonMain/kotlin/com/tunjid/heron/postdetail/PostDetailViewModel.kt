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

package com.tunjid.heron.postdetail

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.PostUri
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ThreadViewPreference.Companion.order
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.types.recordKey
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.RecordRepository
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.data.utilities.writequeue.toSubscriptionWritable
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.postdetail.di.postRecordKey
import com.tunjid.heron.postdetail.di.profileHandleOrId
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.timeline.utilities.launchAndCollectEnqueueMutations
import com.tunjid.heron.ui.coroutines.launchAndCollect
import com.tunjid.heron.ui.coroutines.launchAndCollectLatest
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart

internal typealias PostDetailStateHolder = ActionSuspendingStateMutator<Action, State>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualPostDetailViewModel
}

@Stable
@AssistedInject
class ActualPostDetailViewModel(
    authRepository: AuthRepository,
    profileRepository: ProfileRepository,
    recordRepository: RecordRepository,
    timelineRepository: TimelineRepository,
    userDataRepository: UserDataRepository,
    writeQueue: WriteQueue,
    navActions: (NavigationMutation) -> Unit,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope),
    PostDetailStateHolder by scope.actionSuspendingStateMutator(
        state = State(route).toSnapshotMutable(),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        producer = { state, actions ->
            launchSignedInProfileIdMutations(
                state = state,
                authRepository = authRepository,
            )
            launchLoadPreferencesMutations(
                state = state,
                userDataRepository = userDataRepository,
            )
            actions
                .onStart {
                    emit(Action.Load.Initial)
                }
                .launchMutationsIn(
                    productionScope = this,
                    keySelector = Action::key,
                ) {
                    when (val action = type()) {
                        is Action.Load -> action.flow.launchPostThreadsMutations(
                            state = state,
                            route = route,
                            profileRepository = profileRepository,
                            timelineRepository = timelineRepository,
                            userDataRepository = userDataRepository,
                        )
                        is Action.SendPostInteraction -> action.flow.launchPostInteractionMutations(
                            state = state,
                            writeQueue = writeQueue,
                        )
                        is Action.TogglePublicationSubscription -> action.flow.launchTogglePublicationSubscriptionMutations(
                            state = state,
                            writeQueue = writeQueue,
                        )
                        is Action.SnackbarDismissed -> action.flow.launchSnackbarDismissalMutations(state)

                        is Action.Navigate -> action.flow.collect { navAction ->
                            navActions(navAction.navigationMutation)
                        }
                        is Action.BlockAccount -> action.flow.launchBlockAccountMutations(
                            state = state,
                            writeQueue = writeQueue,
                        )
                        is Action.MuteAccount -> action.flow.launchMuteAccountMutations(
                            state = state,
                            writeQueue = writeQueue,
                        )
                        is Action.DeleteRecord -> action.flow.launchDeleteRecordMutations(
                            state = state,
                            writeQueue = writeQueue,
                        )
                    }
                }
        },
    )

context(productionScope: CoroutineScope)
private fun Flow<Action.Load>.launchPostThreadsMutations(
    state: State.SnapshotMutable,
    route: Route,
    profileRepository: ProfileRepository,
    timelineRepository: TimelineRepository,
    userDataRepository: UserDataRepository,
) = launchAndCollectLatest { action ->
    val postUri = profileRepository.profile(route.profileHandleOrId)
        .first()
        .let {
            PostUri(
                profileId = it.did,
                postRecordKey = route.postRecordKey,
            )
        }
    val order = when (action) {
        Action.Load.Initial ->
            state.order ?: userDataRepository.preferences
                .first()
                .threadViewPreferences
                .order()
        is Action.Load.Order -> action.order
        is Action.Load.ViewMode ->
            state.order ?: userDataRepository.preferences
                .first()
                .threadViewPreferences
                .order()
    }
    val viewMode = when (action) {
        Action.Load.Initial -> state.viewMode
        is Action.Load.Order -> state.viewMode
        is Action.Load.ViewMode -> action.viewMode
    }
    state.order = order
    state.viewMode = viewMode
    timelineRepository.postThreadedItems(
        postUri = postUri,
        order = order,
        viewMode = viewMode,
    ).collect { timelineItems ->
        if (timelineItems.isEmpty()) return@collect
        state.items = timelineItems
        state.anchorPost = timelineItems.firstNotNullOfOrNull anchor@{ item ->
            when (item) {
                is TimelineItem.Pinned,
                is TimelineItem.Repost,
                is TimelineItem.Single,
                is TimelineItem.Threaded.Tree,
                -> item.post.takeIf {
                    it.uri.recordKey == route.postRecordKey
                }
                is TimelineItem.Threaded.Linear -> item.nodes.firstOrNull {
                    it.post.uri.recordKey == route.postRecordKey
                }?.post
                is TimelineItem.Placeholder -> null
            }
        }
    }
}

context(productionScope: CoroutineScope)
private fun launchSignedInProfileIdMutations(
    state: State.SnapshotMutable,
    authRepository: AuthRepository,
) = authRepository.signedInUser.launchAndCollect { signedInProfile ->
    state.signedInProfileId = signedInProfile?.did
}

context(productionScope: CoroutineScope)
private fun launchLoadPreferencesMutations(
    state: State.SnapshotMutable,
    userDataRepository: UserDataRepository,
) = userDataRepository.preferences.launchAndCollect {
    state.preferences = it
}
context(productionScope: CoroutineScope)
private fun Flow<Action.SendPostInteraction>.launchPostInteractionMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { Writable.Interaction(it.interaction) },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.TogglePublicationSubscription>.launchTogglePublicationSubscriptionMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { it.publication.toSubscriptionWritable() },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.BlockAccount>.launchBlockAccountMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { action ->
        Writable.Restriction(
            Profile.Restriction.Block.Add(
                signedInProfileId = action.signedInProfileId,
                profileId = action.profileId,
            ),
        )
    },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.MuteAccount>.launchMuteAccountMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { action ->
        Writable.Restriction(
            Profile.Restriction.Mute.Add(
                signedInProfileId = action.signedInProfileId,
                profileId = action.profileId,
            ),
        )
    },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.DeleteRecord>.launchDeleteRecordMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { Writable.RecordDeletion(recordUri = it.recordUri) },
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

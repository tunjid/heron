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

package com.tunjid.heron.list

import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.ListMember
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.ListMemberQuery
import com.tunjid.heron.data.repository.MessageRepository
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.repository.TimelineRequest
import com.tunjid.heron.data.repository.recentConversations
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.list.di.timelineRequest
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.heron.scaffold.scaffold.duplicateWriteMessage
import com.tunjid.heron.scaffold.scaffold.failedWriteMessage
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.reset
import com.tunjid.heron.tiling.tilingMutations
import com.tunjid.heron.timeline.state.timelineStateHolder
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.distinctBy
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.datetime.Clock

internal typealias ListStateHolder = ActionStateMutator<Action, StateFlow<State>>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualListViewModel
}

@Inject
class ActualListViewModel(
    navActions: (NavigationMutation) -> Unit,
    writeQueue: WriteQueue,
    messageRepository: MessageRepository,
    timelineRepository: TimelineRepository,
    profileRepository: ProfileRepository,
    authRepository: AuthRepository,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope),
    ListStateHolder by scope.actionStateFlowMutator(
        initialState = State(route),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        inputs = listOf(
            signedInProfileIdMutations(
                authRepository = authRepository,
            ),
            recentConversationMutations(
                messageRepository = messageRepository,
            ),
        ),
        actionTransform = transform@{ actions ->
            merge(
                timelineStateHolderMutations(
                    request = route.timelineRequest,
                    scope = scope,
                    timelineRepository = timelineRepository,
                    profileRepository = profileRepository,
                ),
                listMemberStateHolderMutations(
                    request = route.timelineRequest,
                    scope = scope,
                    timelineRepository = timelineRepository,
                    profileRepository = profileRepository,
                    authRepository = authRepository,
                ),
                actions.toMutationStream(
                    keySelector = Action::key,
                ) {
                    when (val action = type()) {
                        is Action.SendPostInteraction -> action.flow.postInteractionMutations(
                            writeQueue = writeQueue,
                        )
                        is Action.SnackbarDismissed -> action.flow.snackbarDismissalMutations()

                        is Action.ToggleViewerState -> action.flow.toggleViewerStateMutations(
                            writeQueue = writeQueue,
                        )

                        is Action.Navigate -> action.flow.consumeNavigationActions(
                            navigationMutationConsumer = navActions,
                        )
                    }
                },
            )
        },
    )

fun signedInProfileIdMutations(
    authRepository: AuthRepository,
): Flow<Mutation<State>> =
    authRepository.signedInUser
        .mapToMutation { signedInProfile ->
            copy(signedInProfileId = signedInProfile?.did)
        }

private fun SuspendingStateHolder<State>.timelineStateHolderMutations(
    request: TimelineRequest.OfList,
    scope: CoroutineScope,
    timelineRepository: TimelineRepository,
    profileRepository: ProfileRepository,
): Flow<Mutation<State>> = flow {
    val existingHolder = state().stateHolders
        .filterIsInstance<ListScreenStateHolders.Timeline>()
        .firstOrNull()

    if (existingHolder != null) return@flow emitAll(
        merge(
            existingHolder.state.mapToMutation { copy(timelineState = it) },
            timelineCreatorMutations(
                timeline = existingHolder.state.value.timeline,
                profileRepository = profileRepository,
            ),
        ),
    )

    val timeline = timelineRepository.timeline(request)
        .first()

    val createdHolder = ListScreenStateHolders.Timeline(
        mutator = scope.timelineStateHolder(
            refreshOnStart = true,
            timeline = timeline,
            startNumColumns = 1,
            timelineRepository = timelineRepository,
        ),
    )
    emit {
        copy(
            stateHolders = stateHolders + createdHolder,
        )
    }
    emitAll(
        merge(
            createdHolder.state.mapToMutation { copy(timelineState = it) },
            timelineCreatorMutations(
                timeline = timeline,
                profileRepository = profileRepository,
            ),
        ),
    )
}

private fun SuspendingStateHolder<State>.listMemberStateHolderMutations(
    request: TimelineRequest.OfList,
    scope: CoroutineScope,
    timelineRepository: TimelineRepository,
    profileRepository: ProfileRepository,
    authRepository: AuthRepository,
): Flow<Mutation<State>> = flow {
    val existingHolder = state().stateHolders
        .filterIsInstance<ListScreenStateHolders.Members>()
        .firstOrNull()

    if (existingHolder != null) return@flow

    val timeline = timelineRepository.timeline(request)
        .map { timeline ->
            if (timeline is Timeline.StarterPack) timeline.listTimeline else timeline
        }
        .filterIsInstance<Timeline.Home.List>()
        .first()

    val createdHolder = ListScreenStateHolders.Members(
        mutator = scope.actionStateFlowMutator(
            initialState = MemberState(
                signedInProfileId = null,
                tilingData = TilingState.Data(
                    currentQuery = ListMemberQuery(
                        listUri = timeline.feedList.uri,
                        data = defaultQueryData(),
                    ),
                ),
            ),
            started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
            inputs = listOf(
                authRepository.signedInUser.mapToMutation {
                    copy(signedInProfileId = it?.did)
                },
            ),
            actionTransform = { actions ->
                actions.toMutationStream {
                    type().flow
                        .tilingMutations(
                            currentState = { state() },
                            updateQueryData = { copy(data = it) },
                            refreshQuery = { copy(data = data.reset()) },
                            cursorListLoader = profileRepository::listMembers,
                            onNewItems = { items ->
                                items.distinctBy(ListMember::uri)
                            },
                            onTilingDataUpdated = { copy(tilingData = it) },
                        )
                }
            },
        ),
    )
    emit {
        copy(
            stateHolders = listOf(createdHolder) + stateHolders,
        )
    }
}

private fun Flow<Action.SendPostInteraction>.postInteractionMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> =
    mapToManyMutations { action ->
        when (writeQueue.enqueue(Writable.Interaction(action.interaction))) {
            WriteQueue.Status.Dropped -> emit {
                copy(messages = messages + action.interaction.failedWriteMessage())
            }
            WriteQueue.Status.Duplicate -> emit {
                copy(messages = messages + action.interaction.duplicateWriteMessage())
            }
            WriteQueue.Status.Enqueued -> Unit
        }
    }

private fun Flow<Action.SnackbarDismissed>.snackbarDismissalMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(messages = messages - action.message)
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
                },
            ),
        )
    }

fun recentConversationMutations(
    messageRepository: MessageRepository,
): Flow<Mutation<State>> =
    messageRepository.recentConversations()
        .mapToMutation { conversations ->
            copy(recentConversations = conversations)
        }

private fun timelineCreatorMutations(
    timeline: Timeline,
    profileRepository: ProfileRepository,
): Flow<Mutation<State>> =
    when (timeline) {
        is Timeline.Home.Feed,
        is Timeline.Home.Following,
        is Timeline.Profile,
        -> emptyFlow()

        is Timeline.Home.List -> profileRepository.profile(
            profileId = timeline.feedList.creator.did,
        )

        is Timeline.StarterPack -> profileRepository.profile(
            profileId = timeline.starterPack.creator.did,
        )
    }
        .mapToMutation {
            copy(creator = it)
        }

private fun defaultQueryData() = CursorQuery.Data(
    page = 0,
    cursorAnchor = Clock.System.now(),
    limit = 15,
)

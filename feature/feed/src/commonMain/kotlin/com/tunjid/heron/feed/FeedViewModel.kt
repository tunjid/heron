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

package com.tunjid.heron.feed

import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.timelineRecordUri
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.MessageRepository
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.repository.TimelineRequest
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.data.repository.recentConversations
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.feed.di.timelineRequest
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.heron.scaffold.scaffold.duplicateWriteMessage
import com.tunjid.heron.scaffold.scaffold.failedWriteMessage
import com.tunjid.heron.timeline.state.timelineStateHolder
import com.tunjid.heron.timeline.ui.sheets.MutedWordsStateHolder
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge

internal typealias FeedStateHolder = ActionStateMutator<Action, StateFlow<State>>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualFeedViewModel
}

@AssistedInject
class ActualFeedViewModel(
    navActions: (NavigationMutation) -> Unit,
    writeQueue: WriteQueue,
    authRepository: AuthRepository,
    messageRepository: MessageRepository,
    timelineRepository: TimelineRepository,
    profileRepository: ProfileRepository,
    userDataRepository: UserDataRepository,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope),
    FeedStateHolder by scope.actionStateFlowMutator(
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
                moderationStateHolderMutations(
                    userDataRepository = userDataRepository,
                ),
                actions.toMutationStream(
                    keySelector = Action::key,
                ) {
                    when (val action = type()) {
                        is Action.SendPostInteraction -> action.flow.postInteractionMutations(
                            writeQueue = writeQueue,
                        )

                        is Action.UpdateFeedGeneratorStatus -> action.flow.feedGeneratorStatusMutations(
                            writeQueue = writeQueue,
                        )

                        is Action.ScrollToTop -> action.flow.scrollToTopMutations()
                        is Action.SnackbarDismissed -> action.flow.snackbarDismissalMutations()

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

fun recentConversationMutations(
    messageRepository: MessageRepository,
): Flow<Mutation<State>> =
    messageRepository.recentConversations()
        .mapToMutation { conversations ->
            copy(recentConversations = conversations)
        }

private fun SuspendingStateHolder<State>.timelineStateHolderMutations(
    request: TimelineRequest.OfFeed,
    scope: CoroutineScope,
    timelineRepository: TimelineRepository,
    profileRepository: ProfileRepository,
): Flow<Mutation<State>> = flow {
    val existingHolder = state().timelineStateHolder
    if (existingHolder != null) return@flow emitAll(
        merge(
            existingHolder.state.mapToMutation {
                copy(timelineState = it)
            },
            feedStatusMutations(
                timeline = existingHolder.state.value.timeline,
                timelineRepository = timelineRepository,
            ),
            timelineCreatorMutations(
                timeline = existingHolder.state.value.timeline,
                profileRepository = profileRepository,
            ),
        ),
    )

    val timeline = timelineRepository.timeline(request)
        .first()
    val createdHolder = scope.timelineStateHolder(
        refreshOnStart = true,
        timeline = timeline,
        startNumColumns = 1,
        timelineRepository = timelineRepository,
    )
    emit {
        copy(timelineStateHolder = createdHolder)
    }

    if (timeline !is Timeline.Home.Feed) return@flow

    emitAll(
        merge(
            createdHolder.state.mapToMutation {
                copy(timelineState = it)
            },
            feedStatusMutations(
                timeline = timeline,
                timelineRepository = timelineRepository,
            ),
            timelineCreatorMutations(
                timeline = timeline,
                profileRepository = profileRepository,
            ),
        ),
    )
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

@OptIn(ExperimentalUuidApi::class)
private fun Flow<Action.ScrollToTop>.scrollToTopMutations(): Flow<Mutation<State>> =
    mapToMutation {
        copy(scrollToTopRequestId = Uuid.random().toString())
    }

private fun Flow<Action.SnackbarDismissed>.snackbarDismissalMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(messages = messages - action.message)
    }

private fun Flow<Action.UpdateFeedGeneratorStatus>.feedGeneratorStatusMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> =
    mapToManyMutations { action ->
        writeQueue.enqueue(Writable.TimelineUpdate(action.update))
    }

private fun timelineCreatorMutations(
    timeline: Timeline,
    profileRepository: ProfileRepository,
): Flow<Mutation<State>> =
    timeline.withFeedTimelineOrNull { feedTimeline ->
        profileRepository.profile(
            profileId = feedTimeline.feedGenerator.creator.did,
        )
            .mapToMutation {
                copy(creator = it)
            }
    } ?: emptyFlow()

private fun feedStatusMutations(
    timeline: Timeline,
    timelineRepository: TimelineRepository,
): Flow<Mutation<State>> =
    timeline.withFeedTimelineOrNull { feedTimeline ->
        timelineRepository.preferences
            .distinctUntilChangedBy { it.timelinePreferences }
            .mapToMutation { preferences ->
                val pinned =
                    preferences.timelinePreferences.firstOrNull {
                        it.timelineRecordUri == feedTimeline.feedGenerator.uri
                    }
                        ?.pinned

                copy(
                    feedStatus = when (pinned) {
                        true -> Timeline.Home.Status.Pinned
                        false -> Timeline.Home.Status.Saved
                        null -> Timeline.Home.Status.None
                    },
                )
            }
    }
        ?: emptyFlow()

internal inline fun <T> Timeline.withFeedTimelineOrNull(
    block: (Timeline.Home.Feed) -> T,
) =
    if (this is Timeline.Home.Feed) block(this)
    else null

private fun moderationStateHolderMutations(
    userDataRepository: UserDataRepository,
): Flow<Mutation<State>> = flow {
    // Initialize all moderation state holders
    val mutedWordsStateHolder = MutedWordsStateHolder(
        userDataRepository = userDataRepository,
    )

    emit {
        copy(
            moderationState = moderationState.copy(
                mutedWordsStateHolder = mutedWordsStateHolder,
            ),
        )
    }
}

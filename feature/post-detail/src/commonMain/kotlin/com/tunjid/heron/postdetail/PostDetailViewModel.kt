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

import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.PostUri
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.types.recordKey
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.MessageRepository
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.data.repository.recentConversations
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.postdetail.di.postRecordKey
import com.tunjid.heron.postdetail.di.profileHandleOrId
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.heron.scaffold.scaffold.duplicateWriteMessage
import com.tunjid.heron.scaffold.scaffold.failedWriteMessage
import com.tunjid.heron.timeline.ui.sheets.MutedWordsStateHolder
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge

internal typealias PostDetailStateHolder = ActionStateMutator<Action, StateFlow<State>>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualPostDetailViewModel
}

@AssistedInject
class ActualPostDetailViewModel(
    authRepository: AuthRepository,
    messageRepository: MessageRepository,
    profileRepository: ProfileRepository,
    timelineRepository: TimelineRepository,
    userDataRepository: UserDataRepository,
    writeQueue: WriteQueue,
    navActions: (NavigationMutation) -> Unit,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope),
    PostDetailStateHolder by scope.actionStateFlowMutator(
        initialState = State(route),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        inputs = listOf(
            signedInProfileIdMutations(
                authRepository = authRepository,
            ),
            postThreadsMutations(
                route = route,
                profileRepository = profileRepository,
                timelineRepository = timelineRepository,
            ),
            recentConversationMutations(
                messageRepository = messageRepository,
            ),
        ),
        actionTransform = transform@{ actions ->
            merge(
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
                        is Action.SnackbarDismissed -> action.flow.snackbarDismissalMutations()

                        is Action.Navigate -> action.flow.consumeNavigationActions(
                            navigationMutationConsumer = navActions,
                        )
                    }
                },
            )
        },
    )

fun postThreadsMutations(
    route: Route,
    profileRepository: ProfileRepository,
    timelineRepository: TimelineRepository,
): Flow<Mutation<State>> = flow {
    val postUri = profileRepository.profile(route.profileHandleOrId)
        .first()
        .let {
            PostUri(
                profileId = it.did,
                postRecordKey = route.postRecordKey,
            )
        }
    emitAll(
        timelineRepository.postThreadedItems(postUri = postUri)
            .mapToMutation { timelineItems ->
                if (timelineItems.isEmpty()) this
                else copy(
                    items = timelineItems,
                    anchorPost = timelineItems.firstNotNullOfOrNull anchor@{ item ->
                        when (item) {
                            is TimelineItem.Pinned,
                            is TimelineItem.Repost,
                            is TimelineItem.Single,
                            -> item.post.takeIf {
                                it.uri.recordKey == route.postRecordKey
                            }
                            is TimelineItem.Thread -> item.posts.firstOrNull {
                                it.uri.recordKey == route.postRecordKey
                            }
                        }
                    },
                )
            },
    )
}

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

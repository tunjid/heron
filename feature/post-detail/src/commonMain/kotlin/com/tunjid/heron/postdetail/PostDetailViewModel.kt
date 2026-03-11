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
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ThreadViewPreference.Companion.order
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.types.recordKey
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.MessageRepository
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.RecordRepository
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
import com.tunjid.heron.timeline.utilities.enqueueMutations
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart

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
    PostDetailStateHolder by scope.actionStateFlowMutator(
        initialState = State(route),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        inputs = listOf(
            signedInProfileIdMutations(
                authRepository = authRepository,
            ),
            loadPreferencesMutations(
                userDataRepository = userDataRepository,
            ),
        ),
        actionTransform = transform@{ actions ->
            actions
                .onStart {
                    emit(Action.Load.Initial)
                }
                .toMutationStream(
                    keySelector = Action::key,
                ) {
                    when (val action = type()) {
                        is Action.Load -> action.flow.postThreadsMutations(
                            route = route,
                            state = state,
                            profileRepository = profileRepository,
                            timelineRepository = timelineRepository,
                            userDataRepository = userDataRepository,
                        )
                        is Action.SendPostInteraction -> action.flow.postInteractionMutations(
                            writeQueue = writeQueue,
                        )
                        is Action.SnackbarDismissed -> action.flow.snackbarDismissalMutations()

                        is Action.Navigate -> action.flow.consumeNavigationActions(
                            navigationMutationConsumer = navActions,
                        )
                        is Action.UpdateMutedWord -> action.flow.updateMutedWordMutations(
                            writeQueue = writeQueue,
                        )
                        is Action.BlockAccount -> action.flow.blockAccountMutations(
                            writeQueue = writeQueue,
                        )
                        is Action.MuteAccount -> action.flow.muteAccountMutations(
                            writeQueue = writeQueue,
                        )
                        is Action.UpdateRecentConversations -> action.flow.recentConversationMutations(
                            messageRepository = messageRepository,
                        )
                        is Action.UpdateRecentLists -> action.flow.recentListsMutations(
                            recordRepository = recordRepository,
                        )
                        is Action.DeleteRecord -> action.flow.deleteRecordMutations(
                            writeQueue = writeQueue,
                        )
                    }
                }
        },
    )

fun Flow<Action.Load>.postThreadsMutations(
    route: Route,
    state: suspend () -> State,
    profileRepository: ProfileRepository,
    timelineRepository: TimelineRepository,
    userDataRepository: UserDataRepository,
): Flow<Mutation<State>> = flatMapLatest { action ->
    val postUri = profileRepository.profile(route.profileHandleOrId)
        .first()
        .let {
            PostUri(
                profileId = it.did,
                postRecordKey = route.postRecordKey,
            )
        }
    val order = when (action) {
        Action.Load.Initial -> state().order ?: userDataRepository.preferences
            .first()
            .threadViewPreferences
            .order()
        is Action.Load.Order -> action.order
    }

    flow {
        emit { copy(order = order) }
        emitAll(
            timelineRepository.postThreadedItems(
                postUri = postUri,
                order = order,
            )
                .mapToMutation { timelineItems ->
                    if (timelineItems.isEmpty()) this
                    else copy(
                        items = timelineItems,
                        anchorPost = timelineItems.firstNotNullOfOrNull anchor@{ item ->
                            when (item) {
                                is TimelineItem.Pinned,
                                is TimelineItem.Repost,
                                is TimelineItem.Single,
                                is TimelineItem.ReplyTree,
                                -> item.post.takeIf {
                                    it.uri.recordKey == route.postRecordKey
                                }
                                is TimelineItem.Thread -> item.posts.firstOrNull {
                                    it.uri.recordKey == route.postRecordKey
                                }
                                is TimelineItem.Placeholder -> null
                            }
                        },
                    )
                },
        )
    }
}

fun signedInProfileIdMutations(
    authRepository: AuthRepository,
): Flow<Mutation<State>> =
    authRepository.signedInUser
        .mapToMutation { signedInProfile ->
            copy(signedInProfileId = signedInProfile?.did)
        }

fun Flow<Action.UpdateRecentConversations>.recentConversationMutations(
    messageRepository: MessageRepository,
): Flow<Mutation<State>> =
    flatMapLatest {
        messageRepository.recentConversations()
            .mapToMutation { conversations ->
                copy(recentConversations = conversations)
            }
    }

fun Flow<Action.UpdateRecentLists>.recentListsMutations(
    recordRepository: RecordRepository,
): Flow<Mutation<State>> =
    flatMapLatest {
        recordRepository.recentLists
            .mapToMutation { lists ->
                copy(recentLists = lists)
            }
    }

private fun loadPreferencesMutations(
    userDataRepository: UserDataRepository,
): Flow<Mutation<State>> =
    userDataRepository.preferences
        .mapToMutation {
            copy(preferences = it)
        }

private fun Flow<Action.SendPostInteraction>.postInteractionMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> =
    this.enqueueMutations(
        writeQueue,
        toWritable = { Writable.Interaction(it.interaction) },
    ) { _, memo ->
        if (memo != null) emit { copy(messages = messages + memo) }
    }

private fun Flow<Action.UpdateMutedWord>.updateMutedWordMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> = this.enqueueMutations(
    writeQueue,
    toWritable = {
        Writable.TimelineUpdate(
            Timeline.Update.OfMutedWord.ReplaceAll(
                mutedWordPreferences = it.mutedWordPreference,
            ),
        )
    },
) { _, memo ->
    if (memo != null) emit { copy(messages = messages + memo) }
}

private fun Flow<Action.BlockAccount>.blockAccountMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> = this.enqueueMutations(
    writeQueue,
    toWritable = {
        Writable.Restriction(
            Profile.Restriction.Block.Add(
                signedInProfileId = it.signedInProfileId,
                profileId = it.profileId,
            ),
        )
    },
) { _, memo ->
    if (memo != null) emit { copy(messages = messages + memo) }
}

private fun Flow<Action.MuteAccount>.muteAccountMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> = this.enqueueMutations(
    writeQueue,
    toWritable = {
        Writable.Restriction(
            Profile.Restriction.Mute.Add(
                signedInProfileId = it.signedInProfileId,
                profileId = it.profileId,
            ),
        )
    },
) { _, memo ->
    if (memo != null) emit { copy(messages = messages + memo) }
}

private fun Flow<Action.DeleteRecord>.deleteRecordMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> = this.enqueueMutations(
    writeQueue,
    toWritable = { Writable.RecordDeletion(recordUri = it.recordUri) },
) { _, memo ->
    if (memo != null) emit { copy(messages = messages + memo) }
}

private fun Flow<Action.SnackbarDismissed>.snackbarDismissalMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(messages = messages - action.message)
    }

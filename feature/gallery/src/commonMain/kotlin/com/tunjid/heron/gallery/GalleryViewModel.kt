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

package com.tunjid.heron.gallery

import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.PostUri
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.MessageRepository
import com.tunjid.heron.data.repository.PostRepository
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.repository.TimelineRequest
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.data.repository.recentConversations
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.gallery.di.postRecordKey
import com.tunjid.heron.gallery.di.profileId
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.heron.scaffold.navigation.model
import com.tunjid.heron.scaffold.navigation.sharedElementPrefix
import com.tunjid.heron.timeline.state.TimelineStateHolder
import com.tunjid.heron.timeline.state.timelineStateHolder
import com.tunjid.heron.timeline.utilities.writeStatusMessage
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapLatestToManyMutations
import com.tunjid.mutator.coroutines.mapToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.map
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

internal typealias GalleryStateHolder = ActionStateMutator<Action, StateFlow<State>>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualGalleryViewModel
}

@AssistedInject
class ActualGalleryViewModel(
    navActions: (NavigationMutation) -> Unit,
    authRepository: AuthRepository,
    messageRepository: MessageRepository,
    postRepository: PostRepository,
    profileRepository: ProfileRepository,
    userDataRepository: UserDataRepository,
    timelineRepository: TimelineRepository,
    writeQueue: WriteQueue,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope),
    GalleryStateHolder by scope.actionStateFlowMutator(
        initialState = State(route),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        inputs = listOf(
            loadSignedInProfileIdMutations(
                authRepository = authRepository,
            ),
            recentConversationMutations(
                messageRepository = messageRepository,
            ),
            loadPreferencesMutations(
                userDataRepository = userDataRepository,
            ),
        ),
        actionTransform = transform@{ actions ->
            merge(
                actions.toMutationStream(
                    keySelector = Action::key,
                ) {
                    when (val action = type()) {
                        is Action.SendPostInteraction -> action.flow.postInteractionMutations(
                            writeQueue = writeQueue,
                        )
                        is Action.ToggleViewerState -> action.flow.toggleViewerStateMutations(
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
                        is Action.DeleteRecord -> action.flow.deleteRecordMutations(
                            writeQueue = writeQueue,
                        )
                    }
                },
                profileRelationshipMutations(
                    currentState = state,
                    profileId = route.profileId,
                    profileRepository = profileRepository,
                ),
                loadPostMutations(
                    route = route,
                    currentState = state,
                    postRepository = postRepository,
                    profileRepository = profileRepository,
                ),
                verticalTimelineMutations(
                    route = route,
                    currentState = state,
                    coroutineScope = scope,
                    timelineRepository = timelineRepository,
                ),
            )
        },
    )

private fun loadPostMutations(
    route: Route,
    postRepository: PostRepository,
    profileRepository: ProfileRepository,
    currentState: suspend () -> State,
): Flow<Mutation<State>> = flow {
    val postUri = profileRepository.profile(route.profileId)
        .first()
        .let {
            PostUri(
                profileId = it.did,
                postRecordKey = route.postRecordKey,
            )
        }

    emitAll(
        postRepository.post(postUri)
            .mapLatestToManyMutations { post ->
                val state = currentState()
                if (state.canScrollVertically) currentCoroutineContext().cancel()
                else emit {
                    copy(
                        items = items.map { item ->
                            if (item is GalleryItem.Initial) item.copy(
                                post = post,
                            )
                            else item
                        },
                    )
                }
            },
    )
}

private fun loadPreferencesMutations(
    userDataRepository: UserDataRepository,
): Flow<Mutation<State>> =
    userDataRepository.preferences
        .mapToMutation {
            copy(preferences = it)
        }

fun recentConversationMutations(
    messageRepository: MessageRepository,
): Flow<Mutation<State>> =
    messageRepository.recentConversations()
        .mapToMutation { conversations ->
            copy(recentConversations = conversations)
        }

private fun loadSignedInProfileIdMutations(
    authRepository: AuthRepository,
): Flow<Mutation<State>> =
    authRepository.signedInUser.mapToMutation {
        copy(signedInProfileId = it?.did)
    }

private fun profileRelationshipMutations(
    profileId: Id.Profile,
    profileRepository: ProfileRepository,
    currentState: suspend () -> State,
): Flow<Mutation<State>> =
    profileRepository.profileRelationships(setOf(profileId))
        .mapLatestToManyMutations { relationships ->
            val state = currentState()
            if (state.canScrollVertically) currentCoroutineContext().cancel()
            else emit {
                copy(
                    items = items.map { item ->
                        if (item is GalleryItem.Initial) item.copy(
                            viewerState = relationships.firstOrNull(),
                        )
                        else item
                    },
                )
            }
        }

private fun Flow<Action.SendPostInteraction>.postInteractionMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> =
    mapToManyMutations { action ->
        val writable = Writable.Interaction(action.interaction)
        val status = writeQueue.enqueue(writable)
        writable.writeStatusMessage(status)?.let {
            emit { copy(messages = messages + it) }
        }
    }

private fun Flow<Action.UpdateMutedWord>.updateMutedWordMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> = mapToManyMutations {
    val writable = Writable.TimelineUpdate(
        Timeline.Update.OfMutedWord.ReplaceAll(
            mutedWordPreferences = it.mutedWordPreference,
        ),
    )
    val status = writeQueue.enqueue(writable)
    writable.writeStatusMessage(status)?.let {
        emit { copy(messages = messages + it) }
    }
}

private fun Flow<Action.BlockAccount>.blockAccountMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> = mapLatestToManyMutations { action ->
    val writable = Writable.Restriction(
        Profile.Restriction.Block.Add(
            signedInProfileId = action.signedInProfileId,
            profileId = action.profileId,
        ),
    )
    val status = writeQueue.enqueue(writable)
    writable.writeStatusMessage(status)?.let {
        emit { copy(messages = messages + it) }
    }
}

private fun Flow<Action.MuteAccount>.muteAccountMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> = mapLatestToManyMutations { action ->
    val writable = Writable.Restriction(
        Profile.Restriction.Mute.Add(
            signedInProfileId = action.signedInProfileId,
            profileId = action.profileId,
        ),
    )
    val status = writeQueue.enqueue(writable)
    writable.writeStatusMessage(status)?.let {
        emit { copy(messages = messages + it) }
    }
}

private fun Flow<Action.DeleteRecord>.deleteRecordMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> = mapToManyMutations { action ->
    val writable = Writable.RecordDeletion(
        recordUri = action.recordUri,
    )
    val status = writeQueue.enqueue(writable)
    writable.writeStatusMessage(status)?.let {
        emit { copy(messages = messages + it) }
    }
}

private fun Flow<Action.ToggleViewerState>.toggleViewerStateMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> =
    mapToManyMutations { action ->
        val writable = Writable.Connection(
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
        val status = writeQueue.enqueue(writable)
        writable.writeStatusMessage(status)?.let {
            emit { copy(messages = messages + it) }
        }
    }

private fun Flow<Action.SnackbarDismissed>.snackbarDismissalMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(messages = messages - action.message)
    }

private fun verticalTimelineMutations(
    route: Route,
    currentState: suspend () -> State,
    coroutineScope: CoroutineScope,
    timelineRepository: TimelineRepository,
): Flow<Mutation<State>> = flow {
    val state = currentState()

    // If there's no cursor data, most likely fetching
    // items for vertical scroll will fetch items other than
    // that being viewed and cause a disruptive experience
    state.cursorData ?: return@flow

    val timelineStateHolder = when (
        val existing = state.timelineStateHolder
    ) {
        null -> when (val source = route.model<Timeline.Source>()) {
            is Timeline.Source.Profile -> profileGalleryTimeline(
                source = source,
            )
            is Timeline.Source.Record.Feed -> feedGalleryTimeline(
                timelineRepository = timelineRepository,
                source = source,
            )
            is Timeline.Source.Following,
            is Timeline.Source.Record.List,
            null,
            -> null
        }?.let {
            coroutineScope.galleryTimelineStateHolder(
                timeline = it,
                timelineRepository = timelineRepository,
            )
        }
        else -> existing
    }

    if (timelineStateHolder == null) return@flow

    emit {
        copy(timelineStateHolder = timelineStateHolder)
    }
    emitAll(
        timelineStateHolder.state
            .map { it.tilingData.items }
            .distinctUntilChanged()
            .mapToMutation { fetched ->
                val initialItem = items.firstOrNull()
                    ?.takeIf { it is GalleryItem.Initial }

                val missingInitialItem = initialItem != null &&
                    fetched.none { it.post.uri == initialItem.post.uri }

                // If the the tile containing the initial item
                // is missing, wait for the tiling pipeline to catch up
                if (missingInitialItem) this
                else copy(
                    canScrollVertically = fetched.isNotEmpty(),
                    items = when {
                        fetched.isEmpty() -> items
                        else -> fetched.map { timelineItem ->
                            GalleryItem.Tiled(
                                post = timelineItem.post,
                                viewerState = timelineItem.post.viewerState,
                                // This can always be zero, UI PagerState is already
                                // created, user horizontal scroll won't reset
                                startIndex = 0,
                                media = timelineItem.post.embed.toGalleryMedia(),
                                threadGate = timelineItem.threadGate,
                                sharedElementPrefix = route.sharedElementPrefix,
                            )
                        }
                    },
                )
            },
    )
}

private fun profileGalleryTimeline(
    source: Timeline.Source.Profile,
): Timeline? =
    when (source.type) {
        Timeline.Profile.Type.Posts,
        Timeline.Profile.Type.Replies,
        Timeline.Profile.Type.Likes,
        -> null
        Timeline.Profile.Type.Media,
        Timeline.Profile.Type.Videos,
        -> Timeline.Profile(
            profileId = source.profileId,
            type = source.type,
            lastRefreshed = null,
            itemsAvailable = 0,
            presentation = Timeline.Presentation.Media.Expanded,
        )
    }

private suspend fun feedGalleryTimeline(
    timelineRepository: TimelineRepository,
    source: Timeline.Source.Record.Feed,
): Timeline? =
    timelineRepository.timeline(
        TimelineRequest.OfFeed.WithUri(source.uri),
    )
        .first()
        .takeIf { timeline ->
            timeline.supportedPresentations.any { presentation ->
                presentation is Timeline.Presentation.Media
            }
        }

private fun CoroutineScope.galleryTimelineStateHolder(
    timeline: Timeline,
    timelineRepository: TimelineRepository,
): TimelineStateHolder = timelineStateHolder(
    refreshOnStart = false,
    timeline = timeline,
    startNumColumns = 1,
    timelineRepository = timelineRepository,
)

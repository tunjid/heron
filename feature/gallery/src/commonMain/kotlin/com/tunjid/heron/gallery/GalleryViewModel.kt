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

import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.PostUri
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ThreadViewPreference.Companion.order
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.PostRepository
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.repository.TimelineRequest
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.data.utilities.writequeue.toSubscriptionWritable
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.gallery.di.postRecordKey
import com.tunjid.heron.gallery.di.profileId
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.model
import com.tunjid.heron.scaffold.navigation.sharedElementPrefix
import com.tunjid.heron.timeline.state.TimelineStateHolder
import com.tunjid.heron.timeline.state.timelineStateHolder
import com.tunjid.heron.timeline.utilities.launchAndCollectEnqueueMutations
import com.tunjid.heron.ui.coroutines.launchAndCollect
import com.tunjid.heron.ui.coroutines.launchAndCollectLatest
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.tiler.map
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

internal typealias GalleryStateHolder = ActionSuspendingStateMutator<Action, State>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualGalleryViewModel
}

@Stable
@AssistedInject
class ActualGalleryViewModel(
    navActions: (NavigationMutation) -> Unit,
    authRepository: AuthRepository,
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
    GalleryStateHolder by scope.actionSuspendingStateMutator(
        state = State(route).toSnapshotMutable(),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        producer = { state, actions ->
            launchLoadSignedInProfileIdMutations(
                state = state,
                authRepository = authRepository,
            )
            launchLoadPreferencesMutations(
                state = state,
                userDataRepository = userDataRepository,
            )
            launchProfileRelationshipMutations(
                state = state,
                profileId = route.profileId,
                profileRepository = profileRepository,
            )
            launchLoadPostMutations(
                state = state,
                route = route,
                postRepository = postRepository,
                profileRepository = profileRepository,
            )
            launchVerticalTimelineMutations(
                state = state,
                route = route,
                viewModelScope = scope,
                timelineRepository = timelineRepository,
            )
            actions.launchMutationsIn(
                productionScope = this,
                keySelector = Action::key,
            ) {
                when (val action = type()) {
                    is Action.SendPostInteraction -> action.flow.launchPostInteractionMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )

                    is Action.TogglePublicationSubscription -> action.flow.launchTogglePublicationSubscriptionMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )

                    is Action.ToggleViewerState -> action.flow.launchToggleViewerStateMutations(
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
                    is Action.TextChanged -> action.flow.collectLatest { event ->
                        state.inputText = event.inputText
                    }
                    is Action.SendReply -> action.flow.launchSendReplyMutations(
                        state = state,
                        writeQueue = writeQueue,
                    )
                    is Action.LoadComments -> action.flow.launchLoadCommentsMutations(
                        state = state,
                        timelineRepository = timelineRepository,
                        userDataRepository = userDataRepository,
                    )
                }
            }
        },
    )

context(productionScope: CoroutineScope)
private suspend fun launchLoadPostMutations(
    state: State.SnapshotMutable,
    route: Route,
    postRepository: PostRepository,
    profileRepository: ProfileRepository,
) {
    val postUri = profileRepository.profile(route.profileId)
        .first()
        .let {
            PostUri(
                profileId = it.did,
                postRecordKey = route.postRecordKey,
            )
        }

    postRepository.post(postUri).launchAndCollectLatest { post ->
        if (state.canScrollVertically) currentCoroutineContext().cancel()
        else state.items = state.items.map { item: GalleryItem ->
            if (item is GalleryItem.Initial) item.copy(post = post)
            else item
        }
    }
}

context(productionScope: CoroutineScope)
private fun launchLoadPreferencesMutations(
    state: State.SnapshotMutable,
    userDataRepository: UserDataRepository,
) = userDataRepository.preferences.launchAndCollect {
    state.preferences = it
}

context(productionScope: CoroutineScope)
private fun launchLoadSignedInProfileIdMutations(
    state: State.SnapshotMutable,
    authRepository: AuthRepository,
) = authRepository.signedInUser.launchAndCollect {
    state.signedInProfileId = it?.did
}

context(productionScope: CoroutineScope)
private fun launchProfileRelationshipMutations(
    state: State.SnapshotMutable,
    profileId: Id.Profile,
    profileRepository: ProfileRepository,
) = profileRepository.profileRelationships(setOf(profileId))
    .launchAndCollectLatest { relationships ->
        if (state.canScrollVertically) currentCoroutineContext().cancel()
        else state.items = state.items.map { item: GalleryItem ->
            if (item is GalleryItem.Initial) item.copy(
                viewerState = relationships.firstOrNull(),
            )
            else item
        }
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
    toWritable = {
        Writable.Restriction(
            Profile.Restriction.Block.Add(
                signedInProfileId = it.signedInProfileId,
                profileId = it.profileId,
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
    toWritable = {
        Writable.Restriction(
            Profile.Restriction.Mute.Add(
                signedInProfileId = it.signedInProfileId,
                profileId = it.profileId,
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

private fun Action.ToggleViewerState.toConnectionWritable(): Writable.Connection =
    Writable.Connection(
        when (val following = this.following) {
            null -> Profile.Connection.Follow(
                signedInProfileId = signedInProfileId,
                profileId = viewedProfileId,
                followedBy = followedBy,
            )
            else -> Profile.Connection.Unfollow(
                signedInProfileId = signedInProfileId,
                profileId = viewedProfileId,
                followUri = following,
                followedBy = followedBy,
            )
        },
    )

context(productionScope: CoroutineScope)
private fun Flow<Action.ToggleViewerState>.launchToggleViewerStateMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { it.toConnectionWritable() },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

private fun Action.SendReply.toReplyWritable(): Writable.Create =
    Writable.Create(
        request = Post.Create.Request(
            authorId = authorId,
            text = text,
            links = links,
            metadata = Post.Create.Metadata(
                reply = Post.Create.Reply(
                    parent = parent,
                ),
            ),
        ),
    )

context(productionScope: CoroutineScope)
private fun Flow<Action.SendReply>.launchSendReplyMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { it.toReplyWritable() },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
        state.inputText = androidx.compose.ui.text.input.TextFieldValue()
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.SnackbarDismissed>.launchSnackbarDismissalMutations(
    state: State.SnapshotMutable,
) = launchAndCollect { event ->
    state.messages -= event.message
}

context(productionScope: CoroutineScope)
private fun Flow<Action.LoadComments>.launchLoadCommentsMutations(
    state: State.SnapshotMutable,
    timelineRepository: TimelineRepository,
    userDataRepository: UserDataRepository,
) = launchAndCollectLatest { action ->
    val post = action.post
    val order = action.order
        ?: state.order
        ?: userDataRepository.preferences
            .first()
            .threadViewPreferences
            .order()

    state.order = order
    state.commentsPost = post
    state.comments = TimelineItem.LoadingItems
    timelineRepository.postThreadedItems(
        postUri = post.uri,
        order = order,
        viewMode = TimelineItem.Threaded.ViewMode.Linear,
    ).collectLatest { timelineItems ->
        if (timelineItems.isEmpty()) {
            delay(3.seconds)
            state.comments = TimelineItem.EmptyThreadItems
        } else {
            val fetchingPostIndex = timelineItems.indexOfFirst {
                it.post.uri == post.uri
            }
            state.comments =
                if (fetchingPostIndex < 0) TimelineItem.EmptyThreadItems
                else timelineItems.drop(fetchingPostIndex + 1)
                    .ifEmpty(TimelineItem::EmptyThreadItems)
        }
    }
}

context(productionScope: CoroutineScope)
private suspend fun launchVerticalTimelineMutations(
    state: State.SnapshotMutable,
    route: Route,
    viewModelScope: CoroutineScope,
    timelineRepository: TimelineRepository,
) {
    state.cursorData ?: return
    delay(VerticalItemDelay)

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
            viewModelScope.galleryTimelineStateHolder(
                timeline = it,
                timelineRepository = timelineRepository,
            )
        }
        else -> existing
    }

    if (timelineStateHolder == null) return

    state.timelineStateHolder = timelineStateHolder

    with(productionScope) {
        snapshotFlow { timelineStateHolder.state.tilingData.items }
            .distinctUntilChanged()
            .launchAndCollect { fetched ->
                val initialItem = state.items.firstOrNull()
                    ?.takeIf { it is GalleryItem.Initial }

                val missingInitialItem = initialItem != null &&
                    fetched.none { it.post.uri == initialItem.post.uri }

                if (missingInitialItem) return@launchAndCollect
                state.canScrollVertically = fetched.isNotEmpty()
                state.items = when {
                    fetched.isEmpty() -> state.items
                    else -> fetched.map { timelineItem ->
                        GalleryItem.Tiled(
                            post = timelineItem.post,
                            viewerState = timelineItem.post.viewerState,
                            startIndex = 0,
                            media = timelineItem.post.embed.toGalleryMedia(),
                            threadGate = timelineItem.threadGate,
                            sharedElementPrefix = route.sharedElementPrefix,
                        )
                    }
                }
            }
    }
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

private val VerticalItemDelay = 1.4.seconds

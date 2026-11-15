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

package com.tunjid.heron.externalrecord

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Event
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Rsvp
import com.tunjid.heron.data.core.models.StandardDocument
import com.tunjid.heron.data.core.models.ThreadViewPreference.Companion.order
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.types.EmbeddableRecordUri
import com.tunjid.heron.data.core.types.EventUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.StandardPublicationUri
import com.tunjid.heron.data.core.types.recordUriOrNull
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.RecordRepository
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.externalrecord.di.ExternalRecordRequest
import com.tunjid.heron.externalrecord.di.externalRecordRequest
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.tiling.launchTilingMutations
import com.tunjid.heron.tiling.reset
import com.tunjid.heron.timeline.ui.toWritable
import com.tunjid.heron.timeline.utilities.launchAndCollectEnqueueMutations
import com.tunjid.heron.ui.scaffold.navigation.NavigationMutation
import com.tunjid.heron.ui.stateproduction.RouteStateHolder
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.mutator.coroutines.launchedCollect
import com.tunjid.mutator.coroutines.launchedCollectLatest
import com.tunjid.tiler.distinctBy
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart

@Stable
internal interface ExternalRecordStateHolder :
    RouteStateHolder,
    ActionSuspendingStateMutator<Action, State>

@AssistedFactory
fun interface ExternalRecordViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualExternalRecordViewModel
}

@Stable
class ActualExternalRecordViewModel(
    mutator: ActionSuspendingStateMutator<Action, State>,
    scope: CoroutineScope,
) : ViewModel(viewModelScope = scope),
    ExternalRecordStateHolder,
    ActionSuspendingStateMutator<Action, State> by mutator {

    @AssistedInject
    constructor(
        navActions: (NavigationMutation) -> Unit,
        profileRepository: ProfileRepository,
        recordRepository: RecordRepository,
        timelineRepository: TimelineRepository,
        userDataRepository: UserDataRepository,
        writeQueue: WriteQueue,
        @Assisted scope: CoroutineScope,
        @Assisted route: Route,
    ) : this(
        mutator = scope.actionSuspendingStateMutator(
            state = State(route).toSnapshotMutable(),
            started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
            producer = { state, actions ->
                userDataRepository.preferences.launchedCollect {
                    state.preferences = it
                }
                launchRecordMutations(
                    state = state,
                    route = route,
                    viewModelScope = scope,
                    profileRepository = profileRepository,
                    recordRepository = recordRepository,
                    timelineRepository = timelineRepository,
                    userDataRepository = userDataRepository,
                )
                actions.launchMutationsIn(
                    productionScope = this,
                    keySelector = Action::key,
                ) {
                    when (val action = type()) {
                        is Action.SnackbarDismissed -> action.flow.launchSnackbarDismissalMutations(state)
                        is Action.Navigate -> action.flow.collect {
                            navActions(it.navigationMutation)
                        }
                        is Action.RecordInteraction -> action.flow.launchRecordInteractionMutations(
                            state = state,
                            writeQueue = writeQueue,
                        )
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
        ),
        scope = scope,
    )
}

context(productionScope: CoroutineScope)
private suspend fun launchRecordMutations(
    state: State.Mutable,
    route: Route,
    viewModelScope: CoroutineScope,
    profileRepository: ProfileRepository,
    recordRepository: RecordRepository,
    timelineRepository: TimelineRepository,
    userDataRepository: UserDataRepository,
) {
    val recordUri = state.record?.embeddableRecordUri ?: when (val request = route.externalRecordRequest) {
        is ExternalRecordRequest.WithUri -> request.uri
        is ExternalRecordRequest.WithProfile ->
            recordUriOrNull(
                profileId = profileRepository.profile(request.profileHandleOrId)
                    .first()
                    .did,
                namespace = request.namespace,
                recordKey = request.recordKey,
            ) as? EmbeddableRecordUri
    } ?: return

    when (recordUri) {
        is StandardPublicationUri -> {
            if (state.stateHolders.isEmpty()) {
                state.stateHolders = listOf(
                    ExternalRecordStateHolders.Documents(
                        mutator = viewModelScope.documentsStateHolder(
                            publicationUri = recordUri,
                            recordRepository = recordRepository,
                        ),
                    ),
                )
            }
            recordRepository.publication(recordUri).launchedCollect {
                state.record = it
            }
        }
        is EventUri -> {
            if (state.stateHolders.isEmpty()) {
                state.stateHolders = EventRsvpStatuses.map { status ->
                    ExternalRecordStateHolders.Rsvps(
                        mutator = viewModelScope.rsvpsStateHolder(
                            eventUri = recordUri,
                            status = status,
                            recordRepository = recordRepository,
                        ),
                    )
                }
            }
            recordRepository.event(recordUri).launchedCollect { event ->
                state.record = event

                val commentsPostUri = event.bskyPostRef
                    ?.takeIf(Event.BskyPostRef::showComments)
                    ?.uri
                    ?: return@launchedCollect
                if (state.stateHolders.none { it is ExternalRecordStateHolders.Comments }) {
                    state.stateHolders += ExternalRecordStateHolders.Comments(
                        mutator = viewModelScope.commentsStateHolder(
                            postUri = commentsPostUri,
                            timelineRepository = timelineRepository,
                            userDataRepository = userDataRepository,
                        ),
                    )
                }
            }
        }
        else -> Unit
    }
}

context(productionScope: CoroutineScope)
private fun Flow<Action.RecordInteraction>.launchRecordInteractionMutations(
    state: State.Mutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { it.interaction.toWritable() },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.SnackbarDismissed>.launchSnackbarDismissalMutations(
    state: State.Mutable,
) = launchedCollect { event ->
    state.messages -= event.message
}

private fun CoroutineScope.documentsStateHolder(
    publicationUri: StandardPublicationUri,
    recordRepository: RecordRepository,
): DocumentsStateHolder = actionSuspendingStateMutator(
    state = DocumentsTilingState(publicationUri = publicationUri).toSnapshotMutable(),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    producer = { state, actions ->
        actions.launchTilingMutations(
            state = state,
            updateQueryData = { copy(data = it) },
            refreshQuery = { copy(data = data.reset()) },
            cursorListLoader = recordRepository::publicationDocuments,
            onNewItems = { items -> items.distinctBy(StandardDocument::uri) },
        )
    },
)

private fun CoroutineScope.rsvpsStateHolder(
    eventUri: EventUri,
    status: Rsvp.Status,
    recordRepository: RecordRepository,
): RsvpsStateHolder = actionSuspendingStateMutator(
    state = RsvpsTilingState(
        eventUri = eventUri,
        status = status,
    ).toSnapshotMutable(),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    producer = { state, actions ->
        actions.launchTilingMutations(
            state = state,
            updateQueryData = { copy(data = it) },
            refreshQuery = { copy(data = data.reset()) },
            cursorListLoader = recordRepository::rsvps,
            onNewItems = { items -> items.distinctBy(Rsvp::uri) },
        )
    },
)

private fun CoroutineScope.commentsStateHolder(
    postUri: PostUri,
    timelineRepository: TimelineRepository,
    userDataRepository: UserDataRepository,
): CommentsStateHolder = actionSuspendingStateMutator(
    state = CommentsState(postUri = postUri).toSnapshotMutable(),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    producer = { state, actions ->
        actions
            .onStart { emit(CommentsAction.Refresh) }
            .launchedCollectLatest {
                state.items = TimelineItem.LoadingItems
                timelineRepository.postThreadedItems(
                    postUri = postUri,
                    order = userDataRepository.preferences
                        .first()
                        .threadViewPreferences
                        .order(),
                    viewMode = TimelineItem.Threaded.ViewMode.Linear,
                ).collectLatest { timelineItems ->
                    if (timelineItems.isEmpty()) {
                        delay(3.seconds)
                        state.items = TimelineItem.EmptyThreadItems
                    } else {
                        // Comments are the replies, so drop the post and anything above it.
                        val eventPostIndex = timelineItems.indexOfFirst {
                            it.post.uri == postUri
                        }
                        state.items =
                            if (eventPostIndex < 0) TimelineItem.EmptyThreadItems
                            else timelineItems.drop(eventPostIndex + 1)
                                .ifEmpty(TimelineItem::EmptyThreadItems)
                    }
                }
            }
    },
)

context(productionScope: CoroutineScope)
private fun Flow<Action.BlockAccount>.launchBlockAccountMutations(
    state: State.Mutable,
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
    state: State.Mutable,
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
    state: State.Mutable,
    writeQueue: WriteQueue,
) = launchAndCollectEnqueueMutations(
    writeQueue = writeQueue,
    toWritable = { Writable.RecordDeletion(recordUri = it.recordUri) },
    postEnqueue = { _, memo ->
        if (memo != null) state.messages += memo
    },
)

private val EventRsvpStatuses = listOf(
    Rsvp.Status.Going,
    Rsvp.Status.Interested,
    Rsvp.Status.NotGoing,
)

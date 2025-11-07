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

package com.tunjid.heron.profile

import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.StarterPack
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelinePreference
import com.tunjid.heron.data.core.models.feedGeneratorUri
import com.tunjid.heron.data.core.models.path
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.MessageRepository
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.ProfilesQuery
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.repository.TimelineRequest
import com.tunjid.heron.data.repository.recentConversations
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.profile.di.profileHandleOrId
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
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.distinctBy
import com.tunjid.treenav.push
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.routeString
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import heron.feature.profile.generated.resources.Res
import heron.feature.profile.generated.resources.feeds
import heron.feature.profile.generated.resources.lists
import heron.feature.profile.generated.resources.starter_packs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.take
import kotlinx.datetime.Clock

internal typealias ProfileStateHolder = ActionStateMutator<Action, StateFlow<State>>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualProfileViewModel
}

@Inject
class ActualProfileViewModel(
    authRepository: AuthRepository,
    messageRepository: MessageRepository,
    profileRepository: ProfileRepository,
    timelineRepository: TimelineRepository,
    writeQueue: WriteQueue,
    navActions: (NavigationMutation) -> Unit,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope),
    ProfileStateHolder by scope.actionStateFlowMutator(
        initialState = State(route),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        inputs = listOf(
            commonFollowerMutations(
                profileId = route.profileHandleOrId,
                profileRepository = profileRepository,
            ),
            profileRelationshipMutations(
                profileId = route.profileHandleOrId,
                profileRepository = profileRepository,
            ),
            feedGeneratorUrisToStatusMutations(
                timelineRepository = timelineRepository,
            ),
            recentConversationMutations(
                messageRepository = messageRepository,
            ),
        ),
        actionTransform = transform@{ actions ->
            merge(
                loadProfileMutations(
                    currentState = { state() },
                    profileId = route.profileHandleOrId,
                    scope = scope,
                    authRepository = authRepository,
                    profileRepository = profileRepository,
                    timelineRepository = timelineRepository,
                ),
                actions.toMutationStream(
                    keySelector = Action::key,
                ) {
                    when (val action = type()) {
                        is Action.UpdatePageWithUpdates -> action.flow.pageWithUpdateMutations()
                        is Action.SendPostInteraction -> action.flow.postInteractionMutations(
                            writeQueue = writeQueue,
                        )
                        is Action.SnackbarDismissed -> action.flow.snackbarDismissalMutations()

                        is Action.ToggleViewerState -> action.flow.toggleViewerStateMutations(
                            writeQueue = writeQueue,
                        )

                        is Action.UpdateFeedGeneratorStatus -> action.flow.feedGeneratorStatusMutations(
                            writeQueue = writeQueue,
                        )

                        is Action.Navigate -> action.flow.consumeNavigationActions(
                            navigationMutationConsumer = navActions,
                        )
                        is Action.BioLinkClicked -> action.flow.mapToManyMutations {
                            when (val target = action.target) {
                                is LinkTarget.Navigable -> navActions {
                                    routeString(target.path, queryParams = emptyMap())
                                        .toRoute
                                        .let(navState::push)
                                }
                                else -> Unit
                            }
                        }
                    }
                },
            )
        },
    )

fun recentConversationMutations(
    messageRepository: MessageRepository,
): Flow<Mutation<State>> =
    messageRepository.recentConversations()
        .mapToMutation { conversations ->
            copy(recentConversations = conversations)
        }

private fun commonFollowerMutations(
    profileId: Id.Profile,
    profileRepository: ProfileRepository,
): Flow<Mutation<State>> =
    profileRepository.commonFollowers(
        otherProfileId = profileId,
        limit = 6,
    ).mapToMutation {
        copy(commonFollowers = it)
    }

private fun loadProfileMutations(
    currentState: suspend () -> State,
    profileId: Id.Profile,
    scope: CoroutineScope,
    authRepository: AuthRepository,
    profileRepository: ProfileRepository,
    timelineRepository: TimelineRepository,
): Flow<Mutation<State>> =
    combine(
        profileRepository.profile(profileId),
        authRepository.signedInUser,
        ::Pair,
    )
        .distinctUntilChanged()
        .mapToManyMutations { (profile, signedInProfile) ->
            val isSignedIn = signedInProfile != null
            val isSignedInProfile = signedInProfile?.let {
                it.did.id == profileId.id || it.handle.id == profileId.id
            } ?: false

            emit {
                copy(
                    profile = profile,
                    signedInProfileId = signedInProfile?.did,
                    isSignedInProfile = isSignedInProfile,
                )
            }

            val state = currentState()
            val hasProfileStateHolders = state.stateHolders.isNotEmpty()

            // TODO: This logic assumes that the tabs for the profile are static.
            // Revisit this.
            if (hasProfileStateHolders) return@mapToManyMutations

            emitAll(
                Timeline.Profile.Type.entries
                    .filter {
                        when (it) {
                            Timeline.Profile.Type.Posts -> true
                            Timeline.Profile.Type.Replies -> isSignedIn
                            Timeline.Profile.Type.Likes -> isSignedInProfile
                            Timeline.Profile.Type.Media -> true
                            Timeline.Profile.Type.Videos -> true
                        }
                    }
                    // Map to a list of flows for each timeline
                    .map { type ->
                        timelineRepository.timeline(
                            TimelineRequest.OfProfile(
                                profileHandleOrDid = profileId,
                                type = type,
                            ),
                        )
                            // Only take 1 emission, timelines should be loaded lazily
                            .take(1)
                    }
                    .let { timelineFlows ->
                        combine<Timeline, List<Timeline>>(
                            flows = timelineFlows,
                            transform = Array<Timeline>::toList,
                        )
                    }
                    .mapToMutation { timelines ->
                        when {
                            stateHolders.isNotEmpty() -> this
                            else -> copy(
                                stateHolders = timelines.map { timeline ->
                                    ProfileScreenStateHolders.Timeline(
                                        scope.timelineStateHolder(
                                            refreshOnStart = true,
                                            timeline = timeline,
                                            startNumColumns = 1,
                                            timelineRepository = timelineRepository,
                                        ),
                                    )
                                } + scope.recordStateHolders(
                                    profileId = profileId,
                                    profileRepository = profileRepository,
                                ),
                            )
                        }
                    },
            )
        }

private fun profileRelationshipMutations(
    profileId: Id.Profile,
    profileRepository: ProfileRepository,
): Flow<Mutation<State>> =
    profileRepository.profileRelationships(setOf(profileId)).mapToMutation {
        copy(viewerState = it.firstOrNull())
    }

private fun feedGeneratorUrisToStatusMutations(
    timelineRepository: TimelineRepository,
): Flow<Mutation<State>> =
    timelineRepository.preferences
        .distinctUntilChangedBy { it.timelinePreferences }
        .mapToMutation { preferences ->
            copy(
                feedGeneratorUrisToPinnedStatus = preferences.timelinePreferences
                    .associateBy(
                        keySelector = TimelinePreference::feedGeneratorUri,
                        valueTransform = TimelinePreference::pinned,
                    ),
            )
        }

private fun Flow<Action.UpdatePageWithUpdates>.pageWithUpdateMutations(): Flow<Mutation<State>> =
    mapToMutation { (sourceId, hasUpdates) ->
        copy(sourceIdsToHasUpdates = sourceIdsToHasUpdates + (sourceId to hasUpdates))
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

private fun Flow<Action.UpdateFeedGeneratorStatus>.feedGeneratorStatusMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> =
    mapToManyMutations { action ->
        writeQueue.enqueue(Writable.TimelineUpdate(action.update))
    }

private fun CoroutineScope.recordStateHolders(
    profileId: Id.Profile,
    profileRepository: ProfileRepository,
): List<ProfileScreenStateHolders.Records<*>> =
    listOfNotNull(
        ProfileScreenStateHolders.Records.Feeds(
            mutator = recordStateHolder(
                initialState = RecordState(
                    stringResource = Res.string.feeds,
                    tilingData = TilingState.Data(
                        currentQuery = ProfilesQuery(
                            profileId = profileId,
                            data = defaultQueryData(),
                        ),
                    ),
                ),
                itemId = FeedGenerator::cid,
                cursorListLoader = profileRepository::feedGenerators,
            ),
        ),
        ProfileScreenStateHolders.Records.StarterPacks(
            mutator = recordStateHolder(
                initialState = RecordState(
                    stringResource = Res.string.starter_packs,
                    tilingData = TilingState.Data(
                        currentQuery = ProfilesQuery(
                            profileId = profileId,
                            data = defaultQueryData(),
                        ),
                    ),
                ),
                itemId = StarterPack::cid,
                cursorListLoader = profileRepository::starterPacks,
            ),
        ),
        ProfileScreenStateHolders.Records.Lists(
            mutator = recordStateHolder(
                initialState = RecordState(
                    stringResource = Res.string.lists,
                    tilingData = TilingState.Data(
                        currentQuery = ProfilesQuery(
                            profileId = profileId,
                            data = defaultQueryData(),
                        ),
                    ),
                ),
                itemId = FeedList::cid,
                cursorListLoader = profileRepository::lists,
            ),
        ),
    )

private fun defaultQueryData() = CursorQuery.Data(
    page = 0,
    cursorAnchor = Clock.System.now(),
    limit = 15,
)

private fun <T : Record> CoroutineScope.recordStateHolder(
    initialState: RecordState<T>,
    itemId: (T) -> Any,
    cursorListLoader: (ProfilesQuery, Cursor) -> Flow<CursorList<T>>,
): RecordStateHolder<T> = actionStateFlowMutator(
    initialState = initialState,
    actionTransform = transform@{ actions ->
        actions.toMutationStream {
            type().flow
                .tilingMutations(
                    currentState = { state() },
                    updateQueryData = { copy(data = it) },
                    refreshQuery = { copy(data = data.reset()) },
                    cursorListLoader = cursorListLoader,
                    onNewItems = { items ->
                        items.distinctBy(itemId)
                    },
                    onTilingDataUpdated = { copy(tilingData = it) },
                )
        }
    },
)

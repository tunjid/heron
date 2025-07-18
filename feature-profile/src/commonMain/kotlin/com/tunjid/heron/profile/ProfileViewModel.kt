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
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.stubProfile
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.ProfilesQuery
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.repository.TimelineRequest
import com.tunjid.heron.data.utilities.CursorQuery
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.timeline.state.update
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.profile.di.avatarSharedElementKey
import com.tunjid.heron.profile.di.profile
import com.tunjid.heron.profile.di.profileHandleOrId
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.mapCursorList
import com.tunjid.heron.tiling.tilingMutations
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.distinctBy
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import heron.feature_profile.generated.resources.Res
import heron.feature_profile.generated.resources.feeds
import heron.feature_profile.generated.resources.lists
import heron.feature_profile.generated.resources.starter_packs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
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
    profileRepository: ProfileRepository,
    timelineRepository: TimelineRepository,
    writeQueue: WriteQueue,
    navActions: (NavigationMutation) -> Unit,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope), ProfileStateHolder by scope.actionStateFlowMutator(
    initialState = State(
        avatarSharedElementKey = route.avatarSharedElementKey ?: "",
        profile = route.profile ?: stubProfile(
            did = ProfileId(route.profileHandleOrId.id),
            handle = ProfileHandle(route.profileHandleOrId.id),
            avatar = null,
        ),
    ),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    inputs = listOf(
        loadProfileMutations(
            profileId = route.profileHandleOrId,
            scope = scope,
            profileRepository = profileRepository,
        ),
        loadSignedInProfileMutations(
            profileId = route.profileHandleOrId,
            scope = scope,
            authRepository = authRepository,
            timelineRepository = timelineRepository,
        ),
        profileRelationshipMutations(
            profileId = route.profileHandleOrId,
            profileRepository = profileRepository,
        )
    ),
    actionTransform = transform@{ actions ->
        actions.toMutationStream(
            keySelector = Action::key
        ) {
            when (val action = type()) {
                is Action.UpdatePageWithUpdates -> action.flow.pageWithUpdateMutations()
                is Action.SendPostInteraction -> action.flow.postInteractionMutations(
                    writeQueue = writeQueue,
                )

                is Action.ToggleViewerState -> action.flow.toggleViewerStateMutations(
                    writeQueue = writeQueue,
                )

                is Action.Navigate -> action.flow.consumeNavigationActions(
                    navigationMutationConsumer = navActions
                )
            }
        }
    }
)

private fun loadProfileMutations(
    profileId: Id.Profile,
    scope: CoroutineScope,
    profileRepository: ProfileRepository,
): Flow<Mutation<State>> =
    merge(
        profileRepository.profile(profileId).mapToMutation {
            copy(
                profile = it,
                collectionStateHolders = collectionStateHolders.ifEmpty {
                    // Only replace collectionStateHolders if they were previously empty
                    profileCollectionStateHolders(
                        coroutineScope = scope,
                        profileId = profileId,
                        metadata = it.metadata,
                        profileRepository = profileRepository,
                    )
                }
            )
        },
        profileRepository.commonFollowers(
            otherProfileId = profileId,
            limit = 6
        ).mapToMutation {
            copy(commonFollowers = it)
        }
    )

private fun loadSignedInProfileMutations(
    profileId: Id.Profile,
    scope: CoroutineScope,
    authRepository: AuthRepository,
    timelineRepository: TimelineRepository,
): Flow<Mutation<State>> =
    authRepository.signedInUser
        .distinctUntilChangedBy { it?.handle }
        .mapToManyMutations { signedInProfile ->
            val isSignedInProfile = signedInProfile?.did?.id == profileId.id ||
                    signedInProfile?.handle?.id == profileId.id
            emit {
                copy(
                    signedInProfileId = signedInProfile?.did,
                    isSignedInProfile = isSignedInProfile
                )
            }
            emitAll(
                Timeline.Profile.Type.entries
                    .filter {
                        when (it) {
                            Timeline.Profile.Type.Posts -> true
                            Timeline.Profile.Type.Replies -> true
                            Timeline.Profile.Type.Likes -> isSignedInProfile
                            Timeline.Profile.Type.Media -> true
                            Timeline.Profile.Type.Videos -> true
                        }
                    }
                    .map { type ->
                        timelineRepository.timeline(
                            TimelineRequest.OfProfile(
                                profileHandleOrDid = profileId,
                                type = type,
                            )
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
                        copy(
                            timelineStateHolders = timelineStateHolders.update(
                                scope = scope,
                                refreshOnStart = true,
                                startNumColumns = 1,
                                updatedTimelines = timelines,
                                timelineRepository = timelineRepository,
                            ),
                        )
                    }
            )
        }

private fun profileRelationshipMutations(
    profileId: Id.Profile,
    profileRepository: ProfileRepository,
): Flow<Mutation<State>> =
    profileRepository.profileRelationships(setOf(profileId)).mapToMutation {
        copy(viewerState = it.firstOrNull())
    }

private fun Flow<Action.UpdatePageWithUpdates>.pageWithUpdateMutations(): Flow<Mutation<State>> =
    mapToMutation { (sourceId, hasUpdates) ->
        copy(sourceIdsToHasUpdates = sourceIdsToHasUpdates + (sourceId to hasUpdates))
    }

private fun Flow<Action.SendPostInteraction>.postInteractionMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> =
    mapToManyMutations { action ->
        writeQueue.enqueue(Writable.Interaction(action.interaction))
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
                }
            )
        )
    }

private fun profileCollectionStateHolders(
    coroutineScope: CoroutineScope,
    profileId: Id.Profile,
    profileRepository: ProfileRepository,
    metadata: Profile.Metadata,
): List<ProfileCollectionStateHolder> = listOfNotNull(
    if (metadata.createdFeedGeneratorCount > 0) ProfileCollectionState(
        stringResource = Res.string.feeds,
        tilingData = TilingState.Data(
            currentQuery = ProfilesQuery(
                profileId = profileId,
                data = defaultQueryData(),
            ),
        ),
    ) to profileRepository::feedGenerators.mapCursorList(ProfileCollection::OfFeedGenerators)
    else null,
    if (metadata.createdStarterPackCount > 0) ProfileCollectionState(
        stringResource = Res.string.starter_packs,
        tilingData = TilingState.Data(
            currentQuery = ProfilesQuery(
                profileId = profileId,
                data = defaultQueryData(),
            ),
        ),
    ) to profileRepository::starterPacks.mapCursorList(ProfileCollection::OfStarterPacks)
    else null,
    if (metadata.createdListCount > 0) ProfileCollectionState(
        stringResource = Res.string.lists,
        tilingData = TilingState.Data(
            currentQuery = ProfilesQuery(
                profileId = profileId,
                data = defaultQueryData(),
            ),
        ),
    ) to profileRepository::lists.mapCursorList(ProfileCollection::OfLists)
    else null,
).map { (state, cursorListLoader) ->
    coroutineScope.actionStateFlowMutator(
        initialState = state,
        actionTransform = transform@{ actions ->
            actions.toMutationStream {
                type().flow.map { TilingState.Action.LoadAround(it) }
                    .tilingMutations(
                        currentState = { state() },
                        onRefreshQuery = { query ->
                            query.copy(data = query.data.copy(page = 0))
                        },
                        onNewItems = { items ->
                            items.distinctBy(ProfileCollection::id)
                        },
                        onTilingDataUpdated = { copy(tilingData = it) },
                        updatePage = { newData -> copy(data = newData) },
                        cursorListLoader = cursorListLoader,
                    )
            }
        }
    )
}

private fun defaultQueryData() = CursorQuery.Data(
    page = 0,
    cursorAnchor = Clock.System.now(),
    limit = 15
)

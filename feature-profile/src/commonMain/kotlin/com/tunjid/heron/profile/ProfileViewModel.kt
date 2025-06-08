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
import com.tunjid.heron.data.repository.TimelineRequest
import com.tunjid.heron.data.core.models.stubProfile
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.repository.AuthTokenRepository
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.domain.timeline.update
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.profile.di.avatarSharedElementKey
import com.tunjid.heron.profile.di.profile
import com.tunjid.heron.profile.di.profileId
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.take
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

internal typealias ProfileStateHolder = ActionStateMutator<Action, StateFlow<State>>

@Inject
class ViewModelCreator(
    private val creator: (scope: CoroutineScope, route: Route) -> ActualProfileViewModel,
) : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualProfileViewModel = creator.invoke(scope, route)
}

@Inject
class ActualProfileViewModel(
    authTokenRepository: AuthTokenRepository,
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
            did = ProfileId(route.profileId.id),
            handle = ProfileHandle(route.profileId.id),
            avatar = null,
        ),
    ),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    inputs = listOf(
        loadProfileMutations(
            profileId = route.profileId,
            profileRepository = profileRepository,
        ),
        loadSignedInProfileMutations(
            profileId = route.profileId,
            scope = scope,
            authTokenRepository = authTokenRepository,
            timelineRepository = timelineRepository,
        ),
        profileRelationshipMutations(
            profileId = route.profileId,
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
    profileRepository: ProfileRepository,
): Flow<Mutation<State>> =
    profileRepository.profile(profileId).mapToMutation {
        copy(profile = it)
    }

private fun loadSignedInProfileMutations(
    profileId: Id.Profile,
    scope: CoroutineScope,
    authTokenRepository: AuthTokenRepository,
    timelineRepository: TimelineRepository,
): Flow<Mutation<State>> =
    merge(
        authTokenRepository.isSignedInProfile(profileId).mapToManyMutations { isSignedInProfile ->
            emit { copy(isSignedInProfile = isSignedInProfile) }
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
                            )
                        )
                    }
            )
        },
        authTokenRepository.signedInUser.mapToMutation { signedInProfile ->
            copy(signedInProfileId = signedInProfile?.did)
        }
    )

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

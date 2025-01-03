/*
 * Copyright 2024 Adetunji Dahunsi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.heron.profile


import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.stubProfile
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.repository.AuthTokenRepository
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.domain.timeline.timelineStateHolder
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
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

typealias ProfileStateHolder = ActionStateMutator<Action, StateFlow<State>>

@Inject
class ProfileStateHolderCreator(
    private val creator: (scope: CoroutineScope, route: Route) -> ActualProfileStateHolder,
) : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualProfileStateHolder = creator.invoke(scope, route)
}

@Inject
class ActualProfileStateHolder(
    authTokenRepository: AuthTokenRepository,
    profileRepository: ProfileRepository,
    timelineRepository: TimelineRepository,
    navActions: (NavigationMutation) -> Unit,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope), ProfileStateHolder by scope.actionStateFlowMutator(
    initialState = State(
        avatarSharedElementKey = route.avatarSharedElementKey ?: "",
        profile = route.profile ?: stubProfile(
            did = route.profileId,
            handle = route.profileId,
            avatar = null,
        ),
        timelines = timelines(route),
        timelineStateHolders = timelines(route).map {
            timelineStateHolder(
                timeline = it,
                startNumColumns = 1,
                scope = scope,
                timelineRepository = timelineRepository,
            )
        },
    ),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    inputs = listOf(
        loadProfileMutations(
            profileId = route.profileId,
            profileRepository = profileRepository,
        ),
        loadIsSignedInProfileMutations(
            profileId = route.profileId,
            authTokenRepository = authTokenRepository,
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
                is Action.Navigate -> action.flow.consumeNavigationActions(
                    navigationMutationConsumer = navActions
                )
            }
        }
    }
)

private fun loadProfileMutations(
    profileId: Id,
    profileRepository: ProfileRepository,
): Flow<Mutation<State>> =
    profileRepository.profile(profileId).mapToMutation {
        copy(profile = it)
    }

private fun loadIsSignedInProfileMutations(
    profileId: Id,
    authTokenRepository: AuthTokenRepository,
): Flow<Mutation<State>> =
    authTokenRepository.isSignedInProfile(profileId).mapToMutation {
        copy(isSignedInProfile = it)
    }

private fun profileRelationshipMutations(
    profileId: Id,
    profileRepository: ProfileRepository,
): Flow<Mutation<State>> =
    profileRepository.profileRelationship(profileId).mapToMutation {
        copy(profileRelationship = it)
    }

private fun Flow<Action.UpdatePageWithUpdates>.pageWithUpdateMutations(): Flow<Mutation<State>> =
    mapToMutation { (sourceId, hasUpdates) ->
        copy(sourceIdsToHasUpdates = sourceIdsToHasUpdates + (sourceId to hasUpdates))
    }

private fun timelines(route: Route) = listOf(
    Timeline.Profile.Posts(
        name = "Posts",
        profileId = route.profileId,
    ),
    Timeline.Profile.Replies(
        name = "Replies",
        profileId = route.profileId,
    ),
    Timeline.Profile.Media(
        name = "Media",
        profileId = route.profileId,
    ),
)
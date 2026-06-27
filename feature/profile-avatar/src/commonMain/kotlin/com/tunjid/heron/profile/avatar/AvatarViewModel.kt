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

package com.tunjid.heron.profile.avatar

import com.tunjid.heron.data.core.models.stubProfile
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.profile.avatar.di.avatarSharedElementKey
import com.tunjid.heron.profile.avatar.di.profile
import com.tunjid.heron.profile.avatar.di.profileHandleOrId
import com.tunjid.heron.ui.coroutines.RouteViewModel
import com.tunjid.heron.ui.scaffold.navigation.NavigationMutation
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.mutator.coroutines.launchedCollect
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted

internal typealias ProfileStateHolder = ActionSuspendingStateMutator<Action, State>

@AssistedFactory
fun interface ProfileAvatarViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualProfileAvatarViewModel
}

@AssistedInject
class ActualProfileAvatarViewModel(
    profileRepository: ProfileRepository,
    navActions: (NavigationMutation) -> Unit,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : RouteViewModel(scope, route),
    ProfileStateHolder by scope.actionSuspendingStateMutator(
        state = State.Immutable(
            avatarSharedElementKey = route.avatarSharedElementKey ?: "",
            profile = route.profile ?: stubProfile(
                did = ProfileId(route.profileHandleOrId.id),
                handle = ProfileHandle(route.profileHandleOrId.id),
                avatar = null,
            ),
        ).toSnapshotMutable(),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        producer = { state, actions ->
            launchLoadProfileMutations(
                state = state,
                profileId = route.profileHandleOrId,
                profileRepository = profileRepository,
            )
            actions.launchMutationsIn(
                productionScope = this,
                keySelector = Action::key,
            ) {
                when (val action = type()) {
                    is Action.SnackbarDismissed -> action.flow.launchSnackbarDismissalMutations(
                        state = state,
                    )
                    is Action.Navigate -> action.flow.collect {
                        navActions(it.navigationMutation)
                    }
                }
            }
        },
    )

context(productionScope: CoroutineScope)
private fun launchLoadProfileMutations(
    state: State.SnapshotMutable,
    profileId: Id.Profile,
    profileRepository: ProfileRepository,
) = profileRepository.profile(profileId).launchedCollect {
    state.profile = it
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SnackbarDismissed>.launchSnackbarDismissalMutations(
    state: State.SnapshotMutable,
) = launchedCollect { action ->
    state.messages -= action.message
}

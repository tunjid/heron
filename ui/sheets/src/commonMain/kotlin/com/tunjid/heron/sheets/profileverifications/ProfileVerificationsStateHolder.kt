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

package com.tunjid.heron.sheets.profileverifications

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileVerification
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.sheets.utilities.SheetWhileSubscribed
import com.tunjid.heron.ui.stateproduction.SheetStateHolder
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.mutator.coroutines.launchedCollectLatest
import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

@Stable
internal interface ProfileVerificationsStateHolder :
    SheetStateHolder,
    ActionSuspendingStateMutator<ProfileVerificationsAction, ProfileVerificationsState>

@AssistedFactory
internal fun interface ProfileVerificationsViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
    ): ProfileVerificationsViewModel
}

internal class ProfileVerificationsViewModel(
    mutator: ActionSuspendingStateMutator<ProfileVerificationsAction, ProfileVerificationsState>,
    scope: CoroutineScope,
) : ViewModel(viewModelScope = scope),
    ProfileVerificationsStateHolder,
    ActionSuspendingStateMutator<ProfileVerificationsAction, ProfileVerificationsState> by mutator {
    @AssistedInject
    constructor(
        profileRepository: ProfileRepository,
        @Assisted scope: CoroutineScope,
    ) : this(
        mutator = scope.actionSuspendingStateMutator(
            state = ProfileVerificationsState.Immutable().toSnapshotMutable(),
            started = SharingStarted.WhileSubscribed(SheetWhileSubscribed),
            producer = { state, actions ->
                actions.launchMutationsIn(
                    productionScope = this,
                    keySelector = ProfileVerificationsAction::key,
                ) {
                    when (val action = type()) {
                        is ProfileVerificationsAction.Load -> action.flow.launchLoadMutations(
                            state = state,
                            profileRepository = profileRepository,
                        )
                    }
                }
            },
        ),
        scope = scope,
    )
}

context(productionScope: CoroutineScope)
private fun Flow<ProfileVerificationsAction.Load>.launchLoadMutations(
    state: ProfileVerificationsState.SnapshotMutable,
    profileRepository: ProfileRepository,
) = distinctUntilChanged()
    .launchedCollectLatest { action ->
        // Clear stale data immediately when switching subjects so the previous profile's
        // name/verifiers don't linger while the new data loads.
        if (state.profileId != action.profileId) {
            state.profileId = action.profileId
            state.subject = null
            state.verifications = emptyList()
        }
        // The subject profile drives the header (its name, and whether it is a trusted verifier
        // vs simply verified); the verifications drive the "Verified by" list.
        combine(
            profileRepository.profile(action.profileId),
            profileRepository.verifications(action.profileId),
            ::Pair,
        ).collect { (subject, verifications) ->
            state.subject = subject
            state.verifications = verifications
        }
    }

@Snapshottable
internal interface ProfileVerificationsState {
    @SnapshotSpec
    data class Immutable(
        val profileId: Id.Profile? = null,
        val subject: Profile? = null,
        val verifications: List<ProfileVerification> = emptyList(),
    ) : ProfileVerificationsState
}

internal sealed class ProfileVerificationsAction(
    val key: String,
) {
    data class Load(
        val profileId: Id.Profile,
    ) : ProfileVerificationsAction("Load")
}

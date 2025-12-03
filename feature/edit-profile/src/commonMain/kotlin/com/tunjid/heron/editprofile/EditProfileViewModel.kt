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

package com.tunjid.heron.editprofile

import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.utilities.File
import com.tunjid.heron.data.files.FileManager
import com.tunjid.heron.data.files.RestrictedFile
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.heron.ui.text.Memo
import com.tunjid.heron.ui.text.copyWithValidation
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapLatestToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import heron.feature.edit_profile.generated.resources.Res
import heron.feature.edit_profile.generated.resources.duplicate_profile_update
import heron.feature.edit_profile.generated.resources.failed_profile_update
import heron.feature.edit_profile.generated.resources.profile_background_update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOf

internal typealias EditProfileStateHolder = ActionStateMutator<Action, StateFlow<State>>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualEditProfileViewModel
}

@AssistedInject
class ActualEditProfileViewModel(
    profileRepository: ProfileRepository,
    fileManager: FileManager,
    writeQueue: WriteQueue,
    navActions: (NavigationMutation) -> Unit,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope),
    EditProfileStateHolder by scope.actionStateFlowMutator(
        initialState = State(route),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        inputs = listOf(
            loadProfileMutations(
                profileRepository = profileRepository,
            ),
            pendingUpdateSubmissionMutations(
                writeQueue = writeQueue,
            ),
        ),
        actionTransform = transform@{ actions ->
            actions.toMutationStream(
                keySelector = Action::key,
            ) {
                when (val action = type()) {
                    is Action.Navigate -> action.flow.consumeNavigationActions(
                        navigationMutationConsumer = navActions,
                    )
                    is Action.AvatarPicked -> action.flow.avatarPickedMutations()
                    is Action.BannerPicked -> action.flow.bannerPickedMutations()
                    is Action.FieldChanged -> action.flow.formEditMutations()
                    is Action.SnackbarDismissed -> action.flow.snackbarDismissalMutations()
                    is Action.SaveProfile -> action.flow.saveProfileMutations(
                        navActions = navActions,
                        writeQueue = writeQueue,
                        fileManager = fileManager,
                    )
                }
            }
        },
    )

private fun loadProfileMutations(
    profileRepository: ProfileRepository,
): Flow<Mutation<State>> =
    profileRepository.signedInProfile()
        .mapToMutation { signedInProfile ->
            copy(
                profile = signedInProfile,
                fields = fields
                    .copyWithValidation(
                        id = DisplayName,
                        text = signedInProfile.displayName ?: "",
                    )
                    .copyWithValidation(
                        id = Description,
                        text = signedInProfile.description ?: "",
                    ),
            )
        }

private fun pendingUpdateSubmissionMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> =
    writeQueue.queueChanges.mapToMutation { writes ->
        copy(submitting = writes.any { it is Writable.ProfileUpdate })
    }

private fun Flow<Action.AvatarPicked>.avatarPickedMutations(): Flow<Mutation<State>> =
    mapToMutation {
        copy(updatedAvatar = it.item)
    }

private fun Flow<Action.BannerPicked>.bannerPickedMutations(): Flow<Mutation<State>> =
    mapToMutation {
        copy(updatedBanner = it.item)
    }

private fun Flow<Action.FieldChanged>.formEditMutations(): Flow<Mutation<State>> =
    mapToMutation { (id, text) ->
        copy(fields = fields.copyWithValidation(id, text))
    }

private fun Flow<Action.SnackbarDismissed>.snackbarDismissalMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(messages = messages - action.message)
    }

private fun Flow<Action.SaveProfile>.saveProfileMutations(
    navActions: (NavigationMutation) -> Unit,
    fileManager: FileManager,
    writeQueue: WriteQueue,
): Flow<Mutation<State>> =
    mapLatestToManyMutations { action ->
        emit { copy(submitting = true) }

        val updateWrite = Writable.ProfileUpdate(
            update = Profile.Update(
                profileId = action.profileId,
                displayName = action.displayName,
                description = action.description,
                avatarFile = action.avatar?.let {
                    fileManager.toUnrestrictedPhotoFile(it)
                },
                bannerFile = action.banner?.let {
                    fileManager.toUnrestrictedPhotoFile(it)
                },
            ),
        )

        val writeStatus = writeQueue.enqueue(updateWrite)
        when (writeStatus) {
            WriteQueue.Status.Dropped -> emit {
                copy(
                    messages = messages + Memo.Resource(Res.string.failed_profile_update),
                    submitting = false,
                )
            }
            WriteQueue.Status.Duplicate -> emit {
                copy(
                    messages = messages + Memo.Resource(Res.string.duplicate_profile_update),
                    submitting = false,
                )
            }
            WriteQueue.Status.Enqueued -> emit {
                copy(messages = messages + Memo.Resource(Res.string.profile_background_update))
            }
        }

        if (writeStatus != WriteQueue.Status.Enqueued) return@mapLatestToManyMutations

        writeQueue.awaitDequeue(updateWrite)
        emitAll(
            flowOf(Action.Navigate.Pop).consumeNavigationActions(
                navigationMutationConsumer = navActions,
            ),
        )
    }

private suspend fun FileManager.toUnrestrictedPhotoFile(
    file: RestrictedFile.Media.Photo,
): File.Media.Photo? = when (val unrestrictedFile = cacheWithoutRestrictions(file)) {
    is File.Media.Photo -> unrestrictedFile
    is File.Media.Video,
    null,
    -> null
}

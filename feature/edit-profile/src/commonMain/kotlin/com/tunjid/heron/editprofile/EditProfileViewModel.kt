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
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileTab
import com.tunjid.heron.data.core.utilities.File
import com.tunjid.heron.data.files.FileManager
import com.tunjid.heron.data.files.RestrictedFile
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.RecordRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.editprofile.di.profileHandleOrId
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.profile.stringResource
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.heron.timeline.state.recordStateHolder
import com.tunjid.heron.ui.text.Memo
import com.tunjid.heron.ui.text.copyWithValidation
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.SuspendingStateHolder
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge

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
    authRepository: AuthRepository,
    profileRepository: ProfileRepository,
    recordRepository: RecordRepository,
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
                authRepository = authRepository,
            ),
            pendingUpdateSubmissionMutations(
                writeQueue = writeQueue,
            ),
            profileTabMutations(
                route = route,
                profileRepository = profileRepository,
            ),
        ),
        actionTransform = transform@{ actions ->
            merge(
                screenTabMutations(
                    scope = scope,
                    stateHolder = this,
                    authRepository = authRepository,
                    recordRepository = recordRepository,
                ),
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
                        is Action.UpdateTabsToSave -> action.flow.pinnedTabMutations()
                        is Action.ToggleFeed -> action.flow.toggleFeedMutations()
                        is Action.SaveProfile -> action.flow.saveProfileMutations(
                            navActions = navActions,
                            writeQueue = writeQueue,
                            fileManager = fileManager,
                        )
                    }
                },
            )
        },
    )

private fun loadProfileMutations(
    authRepository: AuthRepository,
): Flow<Mutation<State>> =
    authRepository.signedInUser
        .filterNotNull()
        .mapLatestToManyMutations { signedInProfile ->
            emit {
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
        }

private fun pendingUpdateSubmissionMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<State>> =
    writeQueue.queueChanges.mapToMutation { writes ->
        copy(submitting = writes.any { it is Writable.ProfileUpdate })
    }

private fun profileTabMutations(
    route: Route,
    profileRepository: ProfileRepository,
): Flow<Mutation<State>> =
    profileRepository.tabs(route.profileHandleOrId)
        .distinctUntilChanged()
        .mapToMutation { tabs ->
            val tabsSet = tabs.toSet()
            val missingTabs = ProfileTab.Static.minus(tabsSet)

            copy(
                currentProfileTabs = tabsSet,
                editableTabs = tabs + missingTabs,
            )
        }

private fun screenTabMutations(
    scope: CoroutineScope,
    stateHolder: SuspendingStateHolder<State>,
    authRepository: AuthRepository,
    recordRepository: RecordRepository,
): Flow<Mutation<State>> = flow {
    if (stateHolder.state().tabs.none { it is EditProfileScreenTabs.Feeds }) {
        val profileId = authRepository.signedInUser.filterNotNull().first().did
        emit {
            copy(
                tabs = tabs + EditProfileScreenTabs.Feeds(
                    mutator = scope.recordStateHolder(
                        profileId = profileId,
                        stringResource = ProfileTab.Bluesky.FeedGenerators.All.stringResource,
                        itemId = FeedGenerator::uri,
                        cursorListLoader = recordRepository::feedGenerators,
                    ),
                ),
            )
        }
    }
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
                tabs = action.selectedProfileTabs,
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

private fun Flow<Action.UpdateTabsToSave>.pinnedTabMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(
            tabsToSave = action.tabs,
        )
    }

private fun Flow<Action.ToggleFeed>.toggleFeedMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(
            feedUrisToFeeds =
            if (feedUrisToFeeds.contains(action.feedGenerator.uri)) feedUrisToFeeds - action.feedGenerator.uri
            else feedUrisToFeeds + (action.feedGenerator.uri to action.feedGenerator),
            editableTabs = ProfileTab.Bluesky.FeedGenerators.FeedGenerator(action.feedGenerator.uri)
                .let {
                    if (it in editableTabs) editableTabs - it
                    else editableTabs + it
                },
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

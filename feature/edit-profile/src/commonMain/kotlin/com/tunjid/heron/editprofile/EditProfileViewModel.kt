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

import androidx.compose.runtime.Stable
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
import com.tunjid.heron.timeline.state.recordStateHolder
import com.tunjid.heron.ui.scaffold.navigation.NavigationMutation
import com.tunjid.heron.ui.text.Memo
import com.tunjid.heron.ui.text.copyWithValidation
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.mutator.coroutines.launchedCollect
import com.tunjid.mutator.coroutines.launchedCollectLatest
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.take

internal typealias EditProfileStateHolder = ActionSuspendingStateMutator<Action, State.SnapshotMutable>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualEditProfileViewModel
}

@Stable
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
    EditProfileStateHolder by scope.actionSuspendingStateMutator(
        state = State(route).toSnapshotMutable(),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        producer = { state, actions ->
            launchLoadProfileMutations(
                state = state,
                authRepository = authRepository,
            )
            launchPendingUpdateSubmissionMutations(
                state = state,
                writeQueue = writeQueue,
            )
            launchProfileTabMutations(
                state = state,
                route = route,
                profileRepository = profileRepository,
            )
            launchScreenTabMutations(
                state = state,
                viewModelScope = scope,
                authRepository = authRepository,
                recordRepository = recordRepository,
            )
            actions.launchMutationsIn(
                productionScope = this,
                keySelector = Action::key,
            ) {
                when (val action = type()) {
                    is Action.Navigate -> action.flow.collect {
                        navActions(it.navigationMutation)
                    }
                    is Action.AvatarPicked -> action.flow.launchAvatarPickedMutations(state)
                    is Action.BannerPicked -> action.flow.launchBannerPickedMutations(state)
                    is Action.FieldChanged -> action.flow.launchFormEditMutations(state)
                    is Action.SnackbarDismissed -> action.flow.launchSnackbarDismissalMutations(state)
                    is Action.UpdateTabsToSave -> action.flow.launchPinnedTabMutations(state)
                    is Action.ToggleFeed -> action.flow.launchToggleFeedMutations(state)
                    is Action.SaveProfile -> action.flow.launchSaveProfileMutations(
                        state = state,
                        navActions = navActions,
                        writeQueue = writeQueue,
                        fileManager = fileManager,
                    )
                }
            }
        },
    )

context(productionScope: CoroutineScope)
private fun launchLoadProfileMutations(
    state: State.SnapshotMutable,
    authRepository: AuthRepository,
) = authRepository.signedInUser
    .filterNotNull()
    .launchedCollectLatest { signedInProfile ->
        state.profile = signedInProfile
        state.fields = state.fields
            .copyWithValidation(
                id = DisplayName,
                text = signedInProfile.displayName ?: "",
            )
            .copyWithValidation(
                id = Pronouns,
                text = signedInProfile.pronouns ?: "",
            )
            .copyWithValidation(
                id = Description,
                text = signedInProfile.description ?: "",
            )
    }

context(productionScope: CoroutineScope)
private fun launchPendingUpdateSubmissionMutations(
    state: State.SnapshotMutable,
    writeQueue: WriteQueue,
) = writeQueue.queueChanges.launchedCollect { writes ->
    state.submitting = writes.any { it is Writable.ProfileUpdate }
}

context(productionScope: CoroutineScope)
private fun launchProfileTabMutations(
    state: State.SnapshotMutable,
    route: Route,
    profileRepository: ProfileRepository,
) = profileRepository.tabs(route.profileHandleOrId)
    .distinctUntilChanged()
    .launchedCollect { tabs ->
        val tabsSet = tabs.toSet()
        val missingTabs = ProfileTab.Static.minus(tabsSet)
        state.currentProfileTabs = tabsSet
        state.editableTabs = tabs + missingTabs
    }

context(productionScope: CoroutineScope)
private fun launchScreenTabMutations(
    state: State.SnapshotMutable,
    viewModelScope: CoroutineScope,
    authRepository: AuthRepository,
    recordRepository: RecordRepository,
) {
    if (state.tabs.none { it is EditProfileScreenTabs.Feeds }) {
        authRepository.signedInUser
            .filterNotNull()
            .take(1)
            .launchedCollect { profile ->
                val profileId = profile.did
                if (state.tabs.any { it is EditProfileScreenTabs.Feeds }) return@launchedCollect
                state.tabs += EditProfileScreenTabs.Feeds(
                    mutator = viewModelScope.recordStateHolder(
                        profileId = profileId,
                        stringResource = ProfileTab.Bluesky.FeedGenerators.All.stringResource,
                        itemId = FeedGenerator::uri,
                        cursorListLoader = recordRepository::feedGenerators,
                    ),
                )
            }
    }
}

context(productionScope: CoroutineScope)
private fun Flow<Action.AvatarPicked>.launchAvatarPickedMutations(
    state: State.SnapshotMutable,
) = launchedCollect { action ->
    state.updatedAvatar = action.item
}

context(productionScope: CoroutineScope)
private fun Flow<Action.BannerPicked>.launchBannerPickedMutations(
    state: State.SnapshotMutable,
) = launchedCollect { action ->
    state.updatedBanner = action.item
}

context(productionScope: CoroutineScope)
private fun Flow<Action.FieldChanged>.launchFormEditMutations(
    state: State.SnapshotMutable,
) = launchedCollect { action ->
    state.fields = state.fields.copyWithValidation(action.id, action.text)
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SnackbarDismissed>.launchSnackbarDismissalMutations(
    state: State.SnapshotMutable,
) = launchedCollect { event ->
    state.messages -= event.message
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SaveProfile>.launchSaveProfileMutations(
    state: State.SnapshotMutable,
    navActions: (NavigationMutation) -> Unit,
    fileManager: FileManager,
    writeQueue: WriteQueue,
) = launchedCollectLatest { action ->
    state.submitting = true

    val updateWrite = Writable.ProfileUpdate(
        update = Profile.Update(
            profileId = action.profileId,
            displayName = action.displayName,
            description = action.description,
            pronouns = action.pronouns,
            avatarFile = action.avatar?.let {
                fileManager.toUnrestrictedPhotoFile(it)
            },
            bannerFile = action.banner?.let {
                fileManager.toUnrestrictedPhotoFile(it)
            },
            tabs = action.selectedProfileTabs,
        ),
    )

    when (writeQueue.enqueue(updateWrite)) {
        WriteQueue.Status.Dropped -> {
            state.messages = state.messages + Memo.Resource(Res.string.failed_profile_update)
            state.submitting = false
        }
        WriteQueue.Status.Duplicate -> {
            state.messages = state.messages + Memo.Resource(Res.string.duplicate_profile_update)
            state.submitting = false
        }
        WriteQueue.Status.Enqueued -> {
            state.messages = state.messages + Memo.Resource(Res.string.profile_background_update)
            writeQueue.awaitDequeue(updateWrite)
            navActions(Action.Navigate.Pop.navigationMutation)
        }
    }
}

context(productionScope: CoroutineScope)
private fun Flow<Action.UpdateTabsToSave>.launchPinnedTabMutations(
    state: State.SnapshotMutable,
) = launchedCollect { action ->
    state.tabsToSave = action.tabs
}

context(productionScope: CoroutineScope)
private fun Flow<Action.ToggleFeed>.launchToggleFeedMutations(
    state: State.SnapshotMutable,
) = launchedCollect { action ->
    state.feedUrisToFeeds =
        if (state.feedUrisToFeeds.contains(action.feedGenerator.uri)) state.feedUrisToFeeds - action.feedGenerator.uri
        else state.feedUrisToFeeds + (action.feedGenerator.uri to action.feedGenerator)
    state.editableTabs = ProfileTab.Bluesky.FeedGenerators.FeedGenerator(action.feedGenerator.uri)
        .let {
            if (it in state.editableTabs) state.editableTabs - it
            else state.editableTabs + it
        }
}

private suspend fun FileManager.toUnrestrictedPhotoFile(
    file: RestrictedFile.Media.Photo,
): File.Media.Photo? = when (val unrestrictedFile = cacheWithoutRestrictions(file)) {
    is File.Media.Photo -> unrestrictedFile
    is File.Media.Video,
    null,
    -> null
}

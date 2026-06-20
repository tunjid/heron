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

package com.tunjid.heron.compose

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.types.EmbeddableRecordUri
import com.tunjid.heron.data.core.types.asEmbeddableRecordUriOrNull
import com.tunjid.heron.data.core.utilities.File
import com.tunjid.heron.data.files.FileManager
import com.tunjid.heron.data.files.RestrictedFile
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.RecordRepository
import com.tunjid.heron.data.repository.SearchQuery
import com.tunjid.heron.data.repository.SearchRepository
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.model
import com.tunjid.heron.scaffold.navigation.sharedUri
import com.tunjid.heron.timeline.utilities.writeStatusMessage
import com.tunjid.heron.ui.text.Memo
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.mutator.coroutines.launchedCollect
import com.tunjid.mutator.coroutines.launchedCollectLatest
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import heron.feature.compose.generated.resources.Res
import heron.feature.compose.generated.resources.sending_post
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.withContext

internal typealias ComposeStateHolder = ActionSuspendingStateMutator<Action, State>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualComposeViewModel
}

@Stable
@AssistedInject
class ActualComposeViewModel(
    navActions: (NavigationMutation) -> Unit,
    authRepository: AuthRepository,
    searchRepository: SearchRepository,
    userDataRepository: UserDataRepository,
    recordRepository: RecordRepository,
    fileManager: FileManager,
    writeQueue: WriteQueue,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope),
    ComposeStateHolder by scope.actionSuspendingStateMutator(
        state = State(route).toSnapshotMutable(),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        producer = { state, actions ->
            launchLoadSignedInProfileMutations(
                state = state,
                authRepository = authRepository,
            )
            launchInteractionSettingsMutations(
                state = state,
                userDataRepository = userDataRepository,
            )
            launchEmbeddedRecordMutations(
                state = state,
                embeddedRecordUri = when (val creationType = route.model<Post.Create.Quote>()) {
                    is Post.Create.Quote -> creationType.interaction.postUri
                    else -> route.sharedUri?.asEmbeddableRecordUriOrNull()
                },
                recordRepository = recordRepository,
            )

            actions.launchMutationsIn(
                productionScope = this,
                keySelector = Action::key,
            ) {
                when (val action = type()) {
                    is Action.PostTextChanged -> action.flow.launchPostTextMutations(
                        state = state,
                    )
                    is Action.SetFabExpanded -> action.flow.launchFabExpansionMutations(
                        state = state,
                    )
                    is Action.SnackbarDismissed -> action.flow.launchSnackbarDismissalMutations(
                        state = state,
                    )
                    is Action.UpdateInteractionSettings -> action.flow.launchUpdateInteractionSettingsMutations(
                        state = state,
                    )
                    is Action.EditMedia -> action.flow.launchEditMediaMutations(
                        state = state,
                    )
                    is Action.CreatePost -> action.flow.launchCreatePostMutations(
                        state = state,
                        navActions = navActions,
                        writeQueue = writeQueue,
                        fileManager = fileManager,
                    )
                    is Action.SearchProfiles -> action.flow.launchSearchMutations(
                        state = state,
                        searchRepository = searchRepository,
                    )
                    is Action.ClearSuggestions -> action.flow.launchClearSuggestionsMutations(
                        state = state,
                    )
                    is Action.RemoveEmbeddedRecord -> action.flow.launchRemoveEmbeddedMutations(
                        state = state,
                    )
                    is Action.EmbedUrl -> action.flow.launchEmbedUrlMutations(
                        state = state,
                        recordRepository = recordRepository,
                    )
                    is Action.Navigate -> action.flow.collect { navAction ->
                        navActions(navAction.navigationMutation)
                    }
                }
            }
        },
    )

context(productionScope: CoroutineScope)
private fun launchLoadSignedInProfileMutations(
    state: State.SnapshotMutable,
    authRepository: AuthRepository,
) = authRepository.signedInUser.launchedCollect {
    state.signedInProfile = it
}

context(productionScope: CoroutineScope)
private fun launchInteractionSettingsMutations(
    state: State.SnapshotMutable,
    userDataRepository: UserDataRepository,
) = userDataRepository.preferences.launchedCollect {
    state.interactionsPreference = it.postInteractionSettings
}

context(productionScope: CoroutineScope)
private fun launchEmbeddedRecordMutations(
    state: State.SnapshotMutable,
    embeddedRecordUri: EmbeddableRecordUri?,
    recordRepository: RecordRepository,
) {
    embeddedRecordUri?.let { uri ->
        recordRepository.embeddableRecord(uri).launchedCollect {
            state.embeddedRecord = it as? Record.Embeddable.Native
        }
    }
}

context(productionScope: CoroutineScope)
private fun Flow<Action.EmbedUrl>.launchEmbedUrlMutations(
    state: State.SnapshotMutable,
    recordRepository: RecordRepository,
) = debounce(400.milliseconds)
    .launchedCollectLatest { action ->
        val uri = action.url.asEmbeddableRecordUriOrNull() ?: return@launchedCollectLatest
        recordRepository.embeddableRecord(uri)
            .take(1)
            .collect { state.embeddedRecord = it as? Record.Embeddable.Native }
    }

context(productionScope: CoroutineScope)
private fun Flow<Action.PostTextChanged>.launchPostTextMutations(
    state: State.SnapshotMutable,
) = launchedCollect { action ->
    state.postText = action.textFieldValue
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SetFabExpanded>.launchFabExpansionMutations(
    state: State.SnapshotMutable,
) = launchedCollect { action ->
    state.fabExpanded = action.expanded
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SnackbarDismissed>.launchSnackbarDismissalMutations(
    state: State.SnapshotMutable,
) = launchedCollect { action ->
    state.messages -= action.message
}

context(productionScope: CoroutineScope)
private fun Flow<Action.RemoveEmbeddedRecord>.launchRemoveEmbeddedMutations(
    state: State.SnapshotMutable,
) = launchedCollect { action ->
    state.embeddedRecord = null
    state.dismissedEmbedUrl = action.url
}

context(productionScope: CoroutineScope)
private fun Flow<Action.UpdateInteractionSettings>.launchUpdateInteractionSettingsMutations(
    state: State.SnapshotMutable,
) = launchedCollect { action ->
    state.interactionsPreference = action.interactionSettingsPreference
}

context(productionScope: CoroutineScope)
private fun Flow<Action.EditMedia>.launchEditMediaMutations(
    state: State.SnapshotMutable,
) = launchedCollect { action ->
    // Invoke in IO context as creating media items may perform IO
    val media = withContext(Dispatchers.IO) {
        when (action) {
            is Action.EditMedia.AddPhotos -> action.photos
            is Action.EditMedia.AddVideo -> listOfNotNull(action.video)
            is Action.EditMedia.RemoveMedia -> emptyList()
            is Action.EditMedia.UpdateMedia -> listOfNotNull(action.media)
        }
    }
    when (action) {
        is Action.EditMedia.AddPhotos -> {
            state.photos += media.filterIsInstance<RestrictedFile.Media.Photo>()
            state.video = null
        }

        is Action.EditMedia.AddVideo -> {
            state.photos = emptyList()
            state.video = media.filterIsInstance<RestrictedFile.Media.Video>().firstOrNull()
        }

        is Action.EditMedia.RemoveMedia -> {
            state.photos = state.photos.filter { it != action.media }
            state.video = state.video?.takeIf { it != action.media }
        }

        is Action.EditMedia.UpdateMedia -> when (val item = media.firstOrNull()) {
            is RestrictedFile.Media.Photo -> state.photos = state.photos.map { photo ->
                if (photo.path == item.path) item
                else photo
            }

            is RestrictedFile.Media.Video -> state.video = item
            else -> Unit
        }
    }
}

context(productionScope: CoroutineScope)
private fun Flow<Action.CreatePost>.launchCreatePostMutations(
    state: State.SnapshotMutable,
    navActions: (NavigationMutation) -> Unit,
    fileManager: FileManager,
    writeQueue: WriteQueue,
) = launchedCollect { action ->
    val postWrite = withContext(Dispatchers.IO) {
        Writable.Create(
            request = Post.Create.Request(
                authorId = action.authorId,
                text = action.text,
                links = action.links,
                metadata = Post.Create.Metadata(
                    reply = action.postType as? Post.Create.Reply,
                    embeddedRecordReference = action.embeddedRecordReference,
                    embeddedMedia = action.media.mapNotNull { item ->
                        when (item) {
                            is RestrictedFile.Media.Photo ->
                                if (item.hasSize) fileManager.cacheWithoutRestrictions(item)
                                else null

                            is RestrictedFile.Media.Video -> fileManager.cacheWithoutRestrictions(
                                item,
                            )
                        }
                    }.filterIsInstance<File.Media>(),
                    allowed = action.interactionPreference?.threadGateAllowed,
                ),
            ),
        )
    }

    val status = writeQueue.enqueue(postWrite)
    val memo = postWrite.writeStatusMessage(status)
    if (memo != null) state.messages += memo

    if (status !is WriteQueue.Status.Enqueued) return@launchedCollect

    state.messages += Memo.Resource(stringResource = Res.string.sending_post)

    // Wait for the user to read the message
    delay(1400.milliseconds)

    navActions(Action.Navigate.Pop.navigationMutation)
}

context(productionScope: CoroutineScope)
private fun Flow<Action.SearchProfiles>.launchSearchMutations(
    state: State.SnapshotMutable,
    searchRepository: SearchRepository,
) = debounce(SEARCH_DEBOUNCE_MILLIS)
    .launchedCollectLatest { action ->
        searchRepository.autoCompleteProfileSearch(
            query = SearchQuery.OfProfiles(
                query = action.query,
                isLocalOnly = false,
                data = CursorQuery.Data(
                    page = 0,
                    cursorAnchor = Clock.System.now(),
                    limit = MAX_SUGGESTED_PROFILES.toLong(),
                ),
            ),
            cursor = Cursor.Initial,
        ).collect { profiles ->
            state.suggestedProfiles = profiles.map(ProfileWithViewerState::profile)
        }
    }

context(productionScope: CoroutineScope)
private fun Flow<Action.ClearSuggestions>.launchClearSuggestionsMutations(
    state: State.SnapshotMutable,
) = launchedCollect {
    state.suggestedProfiles = emptyList()
}

private const val SEARCH_DEBOUNCE_MILLIS = 300L
const val MAX_SUGGESTED_PROFILES = 5

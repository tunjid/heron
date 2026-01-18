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

import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.types.EmbeddableRecordUri
import com.tunjid.heron.data.core.types.asEmbeddableRecordUriOrNull
import com.tunjid.heron.data.core.utilities.File
import com.tunjid.heron.data.files.FileManager
import com.tunjid.heron.data.files.RestrictedFile
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.EmbeddableRecordRepository
import com.tunjid.heron.data.repository.SearchQuery
import com.tunjid.heron.data.repository.SearchRepository
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.heron.scaffold.navigation.model
import com.tunjid.heron.scaffold.navigation.sharedUri
import com.tunjid.heron.timeline.utilities.writeStatusMessage
import com.tunjid.heron.ui.text.Memo
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal typealias ComposeStateHolder = ActionStateMutator<Action, StateFlow<State>>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualComposeViewModel
}

@AssistedInject
class ActualComposeViewModel(
    navActions: (NavigationMutation) -> Unit,
    authRepository: AuthRepository,
    searchRepository: SearchRepository,
    userDataRepository: UserDataRepository,
    embeddableRecordRepository: EmbeddableRecordRepository,
    fileManager: FileManager,
    writeQueue: WriteQueue,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope),
    ComposeStateHolder by scope.actionStateFlowMutator(
        initialState = State(route),
        started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
        inputs = listOf(
            loadSignedInProfileMutations(
                authRepository = authRepository,
            ),
            embeddedRecordMutations(
                embeddedRecordUri = when (val creationType = route.model<Post.Create.Quote >()) {
                    is Post.Create.Quote -> creationType.interaction.postUri
                    else -> route.sharedUri?.asEmbeddableRecordUriOrNull()
                },
                embeddableRecordRepository = embeddableRecordRepository,
            ),
            interactionSettingsMutations(
                userDataRepository = userDataRepository,
            ),
        ),
        actionTransform = transform@{ actions ->
            actions.toMutationStream(
                keySelector = Action::key,
            ) {
                when (val action = type()) {
                    is Action.PostTextChanged -> action.flow.postTextMutations()
                    is Action.SetFabExpanded -> action.flow.fabExpansionMutations()
                    is Action.SnackbarDismissed -> action.flow.snackbarDismissalMutations()
                    is Action.UpdateInteractionSettings -> action.flow.updateInteractionSettingsMutations()
                    is Action.EditMedia -> action.flow.editMediaMutations()
                    is Action.CreatePost -> action.flow.createPostMutations(
                        navActions = navActions,
                        writeQueue = writeQueue,
                        fileManager = fileManager,
                    )

                    is Action.Navigate -> action.flow.consumeNavigationActions(
                        navigationMutationConsumer = navActions,
                    )
                    is Action.SearchProfiles -> action.flow.searchMutations(
                        searchRepository = searchRepository,
                    )
                    is Action.ClearSuggestions -> action.flow.clearSuggestionsMutations()
                    is Action.RemoveEmbeddedRecord -> action.flow.removeEmbeddedMutations()
                }
            }
        },
    )

private fun loadSignedInProfileMutations(
    authRepository: AuthRepository,
): Flow<Mutation<State>> =
    authRepository.signedInUser.mapToMutation {
        copy(signedInProfile = it)
    }

private fun interactionSettingsMutations(
    userDataRepository: UserDataRepository,
): Flow<Mutation<State>> =
    userDataRepository.preferences.mapToMutation {
        copy(interactionsPreference = it.postInteractionSettings)
    }

private fun embeddedRecordMutations(
    embeddedRecordUri: EmbeddableRecordUri?,
    embeddableRecordRepository: EmbeddableRecordRepository,
): Flow<Mutation<State>> =
    embeddedRecordUri?.let { uri ->
        embeddableRecordRepository.embeddableRecord(uri).mapToMutation {
            copy(embeddedRecord = it)
        }
    }
        ?: emptyFlow()

private fun Flow<Action.PostTextChanged>.postTextMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(postText = action.textFieldValue)
    }

private fun Flow<Action.SetFabExpanded>.fabExpansionMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(fabExpanded = action.expanded)
    }

private fun Flow<Action.SnackbarDismissed>.snackbarDismissalMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(messages = messages - action.message)
    }

private fun Flow<Action.RemoveEmbeddedRecord>.removeEmbeddedMutations(): Flow<Mutation<State>> =
    mapToMutation {
        copy(embeddedRecord = null)
    }

private fun Flow<Action.UpdateInteractionSettings>.updateInteractionSettingsMutations(): Flow<Mutation<State>> =
    mapToMutation {
        copy(interactionsPreference = it.interactionSettingsPreference)
    }

private fun Flow<Action.EditMedia>.editMediaMutations(): Flow<Mutation<State>> =
    map { action ->
        // Invoke in IO context as creating media items may perform IO
        withContext(Dispatchers.IO) {
            action to when (action) {
                is Action.EditMedia.AddPhotos -> action.photos
                is Action.EditMedia.AddVideo -> listOfNotNull(action.video)
                is Action.EditMedia.RemoveMedia -> emptyList()
                is Action.EditMedia.UpdateMedia -> listOfNotNull(action.media)
            }
        }
    }
        .mapToMutation { (action, media) ->
            when (action) {
                is Action.EditMedia.AddPhotos -> copy(
                    photos = photos + media.filterIsInstance<RestrictedFile.Media.Photo>(),
                    video = null,
                )

                is Action.EditMedia.AddVideo -> copy(
                    photos = emptyList(),
                    video = media.filterIsInstance<RestrictedFile.Media.Video>().firstOrNull(),
                )

                is Action.EditMedia.RemoveMedia -> copy(
                    photos = photos.filter { it != action.media },
                    video = video?.takeIf { it != action.media },
                )

                is Action.EditMedia.UpdateMedia -> when (val item = media.first()) {
                    is RestrictedFile.Media.Photo -> copy(
                        photos = photos.map { photo ->
                            if (photo.path == item.path) item
                            else photo
                        },
                    )

                    is RestrictedFile.Media.Video -> copy(
                        video = item,
                    )
                }
            }
        }

private fun Flow<Action.CreatePost>.createPostMutations(
    navActions: (NavigationMutation) -> Unit,
    fileManager: FileManager,
    writeQueue: WriteQueue,
): Flow<Mutation<State>> =
    mapToManyMutations { action ->
        val postWrite = Writable.Create(
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

        val status = writeQueue.enqueue(postWrite)
        postWrite.writeStatusMessage(status)?.let {
            emit { copy(messages = messages + it) }
        }

        if (status !is WriteQueue.Status.Enqueued) return@mapToManyMutations

        emit { copy(messages = messages + Memo.Resource(stringResource = Res.string.sending_post)) }

        // Wait for the user to read the message
        delay(1400.milliseconds)

        emitAll(
            flowOf(Action.Navigate.Pop).consumeNavigationActions(
                navigationMutationConsumer = navActions,
            ),
        )
    }

private fun Flow<Action.SearchProfiles>.searchMutations(
    searchRepository: SearchRepository,
): Flow<Mutation<State>> =
    debounce(SEARCH_DEBOUNCE_MILLIS)
        .flatMapLatest { action ->
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
            ).mapToMutation { profiles ->
                copy(
                    suggestedProfiles = profiles.map(ProfileWithViewerState::profile),
                )
            }
        }

private fun Flow<Action.ClearSuggestions>.clearSuggestionsMutations(): Flow<Mutation<State>> =
    mapToMutation {
        copy(suggestedProfiles = emptyList())
    }

private const val SEARCH_DEBOUNCE_MILLIS = 300L
const val MAX_SUGGESTED_PROFILES = 5

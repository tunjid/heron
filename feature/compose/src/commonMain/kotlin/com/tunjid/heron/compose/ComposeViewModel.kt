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

import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.MediaFile
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.PostRepository
import com.tunjid.heron.data.repository.SearchQuery
import com.tunjid.heron.data.repository.SearchRepository
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.media.picker.MediaItem
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.heron.scaffold.navigation.model
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.treenav.strings.Route
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
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
import kotlinx.datetime.Clock

internal typealias ComposeStateHolder = ActionStateMutator<Action, StateFlow<State>>

@AssistedFactory
fun interface RouteViewModelInitializer : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualComposeViewModel
}

@Inject
class ActualComposeViewModel(
    navActions: (NavigationMutation) -> Unit,
    authRepository: AuthRepository,
    postRepository: PostRepository,
    searchRepository: SearchRepository,
    timelineRepository: TimelineRepository,
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
            quotedPostMutations(
                quotedPostUri = when (val creationType = route.model) {
                    is Post.Create.Quote -> creationType.interaction.postUri
                    else -> null
                },
                postRepository = postRepository,
            ),
            labelPreferencesMutations(
                timelineRepository = timelineRepository,
            ),
            labelerMutations(
                timelineRepository = timelineRepository,
            ),
        ),
        actionTransform = transform@{ actions ->
            actions.toMutationStream(
                keySelector = Action::key,
            ) {
                when (val action = type()) {
                    is Action.PostTextChanged -> action.flow.postTextMutations()
                    is Action.SetFabExpanded -> action.flow.fabExpansionMutations()
                    is Action.EditMedia -> action.flow.editMediaMutations()
                    is Action.CreatePost -> action.flow.createPostMutations(
                        navActions = navActions,
                        writeQueue = writeQueue,
                    )

                    is Action.Navigate -> action.flow.consumeNavigationActions(
                        navigationMutationConsumer = navActions,
                    )
                    is Action.SearchProfiles -> action.flow.searchMutations(
                        searchRepository = searchRepository,
                    )
                    is Action.ClearSuggestions -> action.flow.clearSuggestionsMutations()
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

private fun quotedPostMutations(
    quotedPostUri: PostUri?,
    postRepository: PostRepository,
): Flow<Mutation<State>> =
    quotedPostUri?.let { postUri ->
        postRepository.post(postUri).mapToMutation {
            copy(quotedPost = it)
        }
    }
        ?: emptyFlow()

private fun labelPreferencesMutations(
    timelineRepository: TimelineRepository,
): Flow<Mutation<State>> =
    timelineRepository.preferences()
        .mapToMutation { copy(labelPreferences = it.contentLabelPreferences) }

private fun labelerMutations(
    timelineRepository: TimelineRepository,
): Flow<Mutation<State>> =
    timelineRepository.labelers()
        .mapToMutation { copy(labelers = it) }

private fun Flow<Action.PostTextChanged>.postTextMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(postText = action.textFieldValue)
    }

private fun Flow<Action.SetFabExpanded>.fabExpansionMutations(): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(fabExpanded = action.expanded)
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
                    photos = photos + media.filterIsInstance<MediaItem.Photo>(),
                )

                is Action.EditMedia.AddVideo -> copy(
                    photos = emptyList(),
                    video = media.filterIsInstance<MediaItem.Video>().firstOrNull(),
                )

                is Action.EditMedia.RemoveMedia -> copy(
                    photos = photos.filter { it != action.media },
                    video = video?.takeIf { it != action.media },
                )

                is Action.EditMedia.UpdateMedia -> when (val item = media.first()) {
                    is MediaItem.Photo -> copy(
                        photos = photos.map { photo ->
                            if (photo.path == item.path) item
                            else photo
                        },
                    )

                    is MediaItem.Video -> copy(
                        video = item,
                    )
                }
            }
        }

private fun Flow<Action.CreatePost>.createPostMutations(
    navActions: (NavigationMutation) -> Unit,
    writeQueue: WriteQueue,
): Flow<Mutation<State>> =
    mapToManyMutations { action ->
        val postWrite = withContext(Dispatchers.IO) {
            Writable.Create(
                request = Post.Create.Request(
                    authorId = action.authorId,
                    text = action.text,
                    links = action.links,
                    metadata = Post.Create.Metadata(
                        reply = action.postType as? Post.Create.Reply,
                        quote = action.postType as? Post.Create.Quote,
                        mediaFiles = action.media.mapNotNull { item ->
                            when (item) {
                                is MediaItem.Photo -> if (item.size != IntSize.Zero) MediaFile.Photo(
                                    data = item.readBytes(),
                                    width = item.size.width.toLong(),
                                    height = item.size.height.toLong(),
                                )
                                else null

                                is MediaItem.Video -> MediaFile.Video(
                                    data = item.readBytes(),
                                    width = item.size.width.toLong(),
                                    height = item.size.height.toLong(),
                                )
                            }
                        },
                    ),
                ),
            )
        }

        writeQueue.enqueue(postWrite)
        writeQueue.awaitDequeue(postWrite)
        emitAll(
            flowOf(Action.Navigate.Pop).consumeNavigationActions(
                navigationMutationConsumer = navActions,
            ),
        )
    }

private fun Flow<Action.SearchProfiles>.searchMutations(
    searchRepository: SearchRepository,
): Flow<Mutation<State>> =
    debounce(300)
        .flatMapLatest { action ->
            searchRepository.autoCompleteProfileSearch(
                query = SearchQuery.OfProfiles(
                    query = action.query,
                    isLocalOnly = false,
                    data = CursorQuery.Data(
                        page = 0,
                        cursorAnchor = Clock.System.now(),
                        limit = 10,
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

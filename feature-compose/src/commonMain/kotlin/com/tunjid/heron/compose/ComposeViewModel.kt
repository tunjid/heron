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


import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import com.tunjid.heron.compose.di.creationType
import com.tunjid.heron.compose.di.sharedElementPrefix
import com.tunjid.heron.data.core.models.MediaFile
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.repository.AuthTokenRepository
import com.tunjid.heron.data.repository.PostRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.treenav.strings.Route
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

internal typealias ComposeStateHolder = ActionStateMutator<Action, StateFlow<State>>

@Inject
class RouteViewModelFactory(
    private val creator: (scope: CoroutineScope, route: Route) -> ActualComposeViewModel,
) : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualComposeViewModel = creator.invoke(scope, route)
}

@Inject
class ActualComposeViewModel(
    navActions: (NavigationMutation) -> Unit,
    authTokenRepository: AuthTokenRepository,
    postRepository: PostRepository,
    writeQueue: WriteQueue,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope), ComposeStateHolder by scope.actionStateFlowMutator(
    initialState = State(
        postText = TextFieldValue(
            AnnotatedString(
                when (val postType = route.creationType) {
                    is Post.Create.Mention -> "@${postType.profile.handle}"
                    is Post.Create.Reply,
                    is Post.Create.Quote,
                    Post.Create.Timeline,
                    null,
                        -> ""
                }
            )
        ),
        sharedElementPrefix = route.sharedElementPrefix,
        postType = route.creationType,
    ),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    inputs = listOf(
        loadSignedInProfileMutations(
            authTokenRepository = authTokenRepository,
        ),
        quotedPostMutations(
            quotedPostId = when (val creationType = route.creationType) {
                is Post.Create.Quote -> creationType.interaction.postId
                is Post.Create.Mention,
                is Post.Create.Reply,
                Post.Create.Timeline,
                null,
                    -> null
            },
            postRepository = postRepository,
        )
    ),
    actionTransform = transform@{ actions ->
        actions.toMutationStream(
            keySelector = Action::key
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
                    navigationMutationConsumer = navActions
                )
            }
        }
    }
)

private fun loadSignedInProfileMutations(
    authTokenRepository: AuthTokenRepository,
): Flow<Mutation<State>> =
    authTokenRepository.signedInUser.mapToMutation {
        copy(signedInProfile = it)
    }

private fun quotedPostMutations(
    quotedPostId: PostId?,
    postRepository: PostRepository,
): Flow<Mutation<State>> =
    quotedPostId?.let { id ->
        postRepository.post(id).mapToMutation {
            copy(quotedPost = it)
        }
    }
        ?: emptyFlow()

private fun Flow<Action.PostTextChanged>.postTextMutations(
): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(postText = action.textFieldValue)
    }

private fun Flow<Action.SetFabExpanded>.fabExpansionMutations(
): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(fabExpanded = action.expanded)
    }

private fun Flow<Action.EditMedia>.editMediaMutations(
): Flow<Mutation<State>> =
    map { action ->
        // Invoke in IO context as creating media items may perform IO
        withContext(Dispatchers.IO) {
            action to when (action) {
                is Action.EditMedia.AddPhotos -> action.photos.map(MediaItem::Photo)
                is Action.EditMedia.AddVideo -> listOfNotNull(action.video?.let(MediaItem::Video))
                is Action.EditMedia.RemoveMedia -> emptyList()
                is Action.EditMedia.UpdateMedia -> listOfNotNull(action.media)
            }
        }
    }
        .mapToMutation { (action, media) ->
            when (action) {
                is Action.EditMedia.AddPhotos -> copy(
                    photos = photos + media.filterIsInstance<MediaItem.Photo>()
                )

                is Action.EditMedia.AddVideo -> copy(
                    photos = emptyList(),
                    video = media.filterIsInstance<MediaItem.Video>().firstOrNull(),
                )

                is Action.EditMedia.RemoveMedia -> copy(
                    photos = photos.filter { it != action.media },
                    video = video?.takeIf { it != action.media }
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
                                    data = item.file.readBytes(),
                                    width = item.size.width.toLong(),
                                    height = item.size.height.toLong(),
                                )
                                else null

                                is MediaItem.Video -> MediaFile.Video(
                                    data = item.file.readBytes(),
                                    width = item.size.width.toLong(),
                                    height = item.size.height.toLong(),
                                )
                            }
                        }
                    ),
                ),
            )
        }

        writeQueue.enqueue(postWrite)
        writeQueue.awaitDequeue(postWrite)
        emitAll(
            flowOf(Action.Navigate.Pop).consumeNavigationActions(
                navigationMutationConsumer = navActions
            )
        )
    }
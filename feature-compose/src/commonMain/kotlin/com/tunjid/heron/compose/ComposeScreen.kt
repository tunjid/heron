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

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.tunjid.heron.compose.ui.MediaUploadItems
import com.tunjid.heron.data.core.models.ContentLabelPreferences
import com.tunjid.heron.data.core.models.Labeler
import com.tunjid.heron.ui.text.links
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.contentDescription
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.timeline.ui.avatarSharedElementKey
import com.tunjid.heron.timeline.ui.post.feature.QuotedPost
import com.tunjid.heron.timeline.ui.post.blurredMediaLabels
import com.tunjid.heron.timeline.ui.profile.ProfileName
import com.tunjid.heron.ui.AvatarSize
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.text.formatTextPost
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableStickySharedElementOf
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Composable
internal fun ComposeScreen(
    paneScaffoldState: PaneScaffoldState,
    modifier: Modifier = Modifier,
    state: State,
    actions: (Action) -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        val postText = state.postText
        ReplyingTo(
            paneMovableElementSharedTransitionScope = paneScaffoldState,
            type = state.postType,
            sharedElementPrefix = state.sharedElementPrefix
        )
        Post(
            signedInProfile = state.signedInProfile,
            postText = postText,
            quotedPost = state.quotedPost,
            labelPreferences = state.labelPreferences,
            labelers = state.labelers,
            paneMovableElementSharedTransitionScope = paneScaffoldState,
            onPostTextChanged = { actions(Action.PostTextChanged(it)) },
            onCreatePost = onCreatePost@{
                val authorId = state.signedInProfile?.did ?: return@onCreatePost
                actions(
                    Action.CreatePost(
                        postType = state.postType,
                        authorId = authorId,
                        text = postText.text,
                        links = postText.annotatedString.links(),
                        media = state.video?.let(::listOf) ?: state.photos,
                    )
                )
            }
        )
        MediaUploadItems(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            photos = state.photos,
            video = state.video,
            removeMediaItem = { item ->
                actions(Action.EditMedia.RemoveMedia(item))
            },
            onMediaItemUpdated = { item ->
                actions(Action.EditMedia.UpdateMedia(item))
            }
        )
        Spacer(
            modifier = Modifier
                .height(56.dp)
        )

        LaunchedEffect(Unit) {
            snapshotFlow { state.postText.text.length }
                .collect {
                    scrollState.scrollTo(Int.MAX_VALUE)
                }
        }
    }
}

@Composable
private fun Post(
    modifier: Modifier = Modifier,
    signedInProfile: Profile?,
    postText: TextFieldValue,
    quotedPost: Post?,
    labelPreferences: ContentLabelPreferences,
    labelers: List<Labeler>,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    onPostTextChanged: (TextFieldValue) -> Unit,
    onCreatePost: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = spacedBy(8.dp)
    ) {
        AuthorAndPost(
            modifier = modifier,
            avatar = {
                AsyncImage(
                    modifier = Modifier.size(UiTokens.avatarSize),
                    args = remember(signedInProfile?.avatar) {
                        ImageArgs(
                            url = signedInProfile?.avatar?.uri,
                            contentDescription = signedInProfile?.contentDescription,
                            contentScale = ContentScale.Crop,
                            shape = RoundedPolygonShape.Circle,
                        )
                    },
                )
            },
            postContent = {
                PostComposition(
                    modifier = Modifier
                        .fillMaxWidth(),
                    postText = postText,
                    onPostTextChanged = onPostTextChanged,
                    onCreatePost = onCreatePost@{
                        onCreatePost()
                    }
                )
            },
        )

        val isBlurred = remember(
            key1 = quotedPost,
            key2 = labelers,
            key3 = labelPreferences
        ) {
            quotedPost?.blurredMediaLabels(
                labelers = labelers,
                contentPreferences = labelPreferences,
            )?.isNotEmpty() ?: false
        }
        if (quotedPost != null) QuotedPost(
            modifier = Modifier.padding(
                horizontal = 24.dp,
            ),
            now = remember { Clock.System.now() },
            quotedPost = quotedPost,
            sharedElementPrefix = NeverMatchedSharedElementPrefix,
            isBlurred = isBlurred,
            paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
            onClick = {},
            onLinkTargetClicked = { _, _ -> },
            onProfileClicked = { _, _ -> },
            onPostMediaClicked = { _, _, _ -> },
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ReplyingTo(
    modifier: Modifier = Modifier,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    type: Post.Create?,
    sharedElementPrefix: String?,
) {
    when (type) {
        is Post.Create.Mention -> Unit
        is Post.Create.Reply -> AuthorAndPost(
            modifier = modifier,
            avatar = {
                paneMovableElementSharedTransitionScope.updatedMovableStickySharedElementOf(
                    modifier = Modifier
                        .size(AvatarSize),
                    sharedContentState = with(paneMovableElementSharedTransitionScope) {
                        rememberSharedContentState(
                            key = type.parent.avatarSharedElementKey(sharedElementPrefix),
                        )
                    },
                    state = remember(type.parent.author.avatar) {
                        ImageArgs(
                            url = type.parent.author.avatar?.uri,
                            contentScale = ContentScale.Crop,
                            contentDescription = type.parent.author.displayName
                                ?: type.parent.author.handle.id,
                            shape = RoundedPolygonShape.Circle,
                        )
                    },
                    sharedElement = { state, modifier ->
                        AsyncImage(state, modifier)
                    }
                )
            },
            postContent = {
                Column {
                    ProfileName(
                        profile = type.parent.author,
                        ellipsize = true,
                    )
                    Text(text = type.parent.record?.text ?: "")
                }
            }
        )

        Post.Create.Timeline -> Unit
        else -> Unit
    }
}

@Composable
private inline fun AuthorAndPost(
    modifier: Modifier = Modifier,
    avatar: @Composable () -> Unit,
    postContent: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .padding(16.dp),
        horizontalArrangement = spacedBy(16.dp),
    ) {
        avatar()
        postContent()
    }
}

@Composable
private fun PostComposition(
    modifier: Modifier,
    postText: TextFieldValue,
    onPostTextChanged: (TextFieldValue) -> Unit,
    onCreatePost: () -> Unit,
) {
    val textFieldFocusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    BasicTextField(
        modifier = modifier
            .focusRequester(textFieldFocusRequester)
            .bringIntoViewRequester(bringIntoViewRequester),
        value = postText,
        onValueChange = {
            onPostTextChanged(
                it.copy(
                    annotatedString = formatTextPost(
                        text = it.text,
                        textLinks = it.annotatedString.links(),
                        onLinkTargetClicked = {

                        }
                    )
                )
            )
        },
        onTextLayout = {
            val cursorRect = it.getCursorRect(postText.selection.start)
            coroutineScope.launch {
                bringIntoViewRequester.bringIntoView(cursorRect)
            }
        },
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = LocalContentColor.current
        ),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Send,
        ),
        keyboardActions = KeyboardActions {
            if (postText.annotatedString.isNotEmpty()) {
                onCreatePost()
            }
        },
    )

    LaunchedEffect(Unit) {
        textFieldFocusRequester.requestFocus()
    }
}

@OptIn(ExperimentalUuidApi::class)
private val NeverMatchedSharedElementPrefix = Uuid.random().toString()

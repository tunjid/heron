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
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.tunjid.heron.compose.ui.links
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.contentDescription
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.ui.avatarSharedElementKey
import com.tunjid.heron.timeline.ui.profile.ProfileName
import com.tunjid.heron.timeline.utilities.byteOffsets
import com.tunjid.heron.ui.PanedSharedElementScope
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.text.formatTextPost
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableSharedElementOf
import de.cketti.codepoints.codePointCount
import kotlin.math.min

@Composable
internal fun ComposeScreen(
    panedSharedElementScope: PanedSharedElementScope,
    modifier: Modifier = Modifier,
    state: State,
    actions: (Action) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        val postText = state.postText
        ReplyingTo(
            panedSharedElementScope = panedSharedElementScope,
            type = state.postType,
            sharedElementPrefix = state.sharedElementPrefix
        )
        Post(
            signedInProfile = state.signedInProfile,
            postText = postText,
            onPostTextChanged = { actions(Action.PostTextChanged(it)) },
            onCreatePost = onCreatePost@{
                val authorId = state.signedInProfile?.did ?: return@onCreatePost
                actions(
                    Action.CreatePost(
                        postType = state.postType,
                        authorId = authorId,
                        text = postText.text,
                        links = postText.annotatedString.links(),
                    )
                )
            }
        )
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun Post(
    signedInProfile: Profile?,
    postText: TextFieldValue,
    onPostTextChanged: (TextFieldValue) -> Unit,
    onCreatePost: () -> Unit,
) {
    AuthorAndPost(
        avatar = {
            AsyncImage(
                modifier = Modifier.size(48.dp),
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
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ReplyingTo(
    panedSharedElementScope: PanedSharedElementScope,
    type: Post.Create?,
    sharedElementPrefix: String?,
) {
    when (type) {
        is Post.Create.Mention -> Unit
        is Post.Create.Reply -> AuthorAndPost(
            avatar = {
                panedSharedElementScope.updatedMovableSharedElementOf(
                    modifier = Modifier
                        .size(48.dp),
                    key = type.parent.avatarSharedElementKey(sharedElementPrefix),
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
    avatar: @Composable () -> Unit,
    postContent: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
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

    BasicTextField(
        modifier = modifier
            .focusRequester(textFieldFocusRequester),
        value = postText,
        onValueChange = {
            onPostTextChanged(
                it.copy(
                    annotatedString = formatTextPost(
                        text = it.text,
                        textLinks = it.annotatedString.links(),
                        onProfileClicked = {

                        }
                    )
                )
            )
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

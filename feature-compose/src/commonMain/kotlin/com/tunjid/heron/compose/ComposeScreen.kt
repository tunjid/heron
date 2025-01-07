/*
 * Copyright 2024 Adetunji Dahunsi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.heron.compose

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.contentDescription
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.utilities.byteOffsets
import com.tunjid.heron.ui.SharedElementScope
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.text.formatTextPost
import de.cketti.codepoints.codePointCount
import kotlin.math.min

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun ComposeScreen(
    sharedElementScope: SharedElementScope,
    modifier: Modifier = Modifier,
    state: State,
    actions: (Action) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            horizontalArrangement = spacedBy(16.dp),
        ) {

            AsyncImage(
                modifier = Modifier.size(48.dp),
                args = remember(state.signedInProfile?.avatar) {
                    ImageArgs(
                        url = state.signedInProfile?.avatar?.uri,
                        contentDescription = state.signedInProfile?.contentDescription,
                        contentScale = ContentScale.Crop,
                        shape = RoundedPolygonShape.Circle,
                    )
                },
            )

            var postText by remember {
                mutableStateOf(TextFieldValue(AnnotatedString("")))
            }

            Column {
                PostComposition(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    postText = postText,
                    onPostTextChanged = { postText = it }
                )
                ComposeBottomBar(
                    postText = postText,
                    postTextLimit = PostTextLimit,
                )
            }
        }
    }
}

@Composable
private fun PostComposition(
    modifier: Modifier,
    postText: TextFieldValue,
    onPostTextChanged: (TextFieldValue) -> Unit,
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
//                onPost(postPayload)
            }
        },
    )

    LaunchedEffect(Unit) {
        textFieldFocusRequester.requestFocus()
    }
}

@Composable
private fun ComposeBottomBar(
    postText: TextFieldValue,
    postTextLimit: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = spacedBy(16.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val postByteCount = postText.text.codePointCount(0, postText.text.length)
        val unboundedProgress = postByteCount / postTextLimit.toFloat()

        Text(
            modifier = Modifier.padding(top = 12.dp),
            textAlign = TextAlign.Right,
            text = (postTextLimit - postByteCount).toString(),
        )

        val progress = min(1f, unboundedProgress)
        val easing = remember { CubicBezierEasing(.42f, 0f, 1f, 0.58f) }

        CircularProgressIndicator(
            modifier = Modifier.height(24.dp),
            progress = { progress },
            strokeWidth = lerp(
                start = 8.dp,
                stop = 24.dp,
                fraction = ((unboundedProgress - 1) * 4).coerceIn(0f, 1f),
            ),
            color = lerp(
                start = MaterialTheme.colorScheme.primary,
                stop = Color.Red,
                fraction = easing.transform(progress),
            )
        )
    }
}

private fun AnnotatedString.links(): List<Post.Link> {
    val byteOffsets = text.byteOffsets()

    val mentions = handleRegex.findAll(text)
        .map {
            Post.Link(
                start = byteOffsets.indexOf(it.range.first),
                end = byteOffsets.indexOf(it.range.last + 1),
                // Ok this is actually a handle for now, but it is resolved to a Did later on.
                target = Post.LinkTarget.UserHandleMention(Id(it.groupValues[3])),
            )
        }

    val hashtags = hashtagRegex.findAll(text)
        .map {
            Post.Link(
                start = byteOffsets.indexOf(it.range.first),
                end = byteOffsets.indexOf(it.range.last + 1),
                target = Post.LinkTarget.Hashtag(it.groupValues[3]),
            )
        }

    val hyperlinks = hyperlinkRegex.findAll(text)
        .map {
            var url = it.groupValues[2]
            if (!url.startsWith("http")) {
                url = "https://$url"
            }
            url = url.dropLastWhile { c -> c in ".,;!?" }
            if (url.endsWith(')') && '(' !in url) {
                url = url.dropLast(1)
            }

            Post.Link(
                start = byteOffsets.indexOf(it.range.first),
                end = byteOffsets.indexOf(it.range.last + 1),
                target = Post.LinkTarget.ExternalLink(Uri(url)),
            )
        }

    return (mentions + hashtags + hyperlinks).toList()
}

private const val PostTextLimit = 300

private val handleRegex = Regex(
    "(^|\\s|\\()(@)([a-zA-Z0-9.-]+)(\\b)",
)

private val hashtagRegex = Regex(
    "(^|\\s|\\()(#)([a-zA-Z0-9]+)(\\b)",
)

private val hyperlinkRegex = Regex(
    "(^|\\s|\\()((https?://\\S+)|(([a-z][a-z0-9]*(\\.[a-z0-9]+)+)\\S*))",
    setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
)
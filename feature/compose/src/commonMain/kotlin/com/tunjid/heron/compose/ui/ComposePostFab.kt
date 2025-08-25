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

package com.tunjid.heron.compose.ui

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.tunjid.heron.compose.Action
import com.tunjid.heron.compose.State
import com.tunjid.heron.compose.hasLongPost
import com.tunjid.heron.scaffold.scaffold.PaneFab
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.text.links
import heron.feature.compose.generated.resources.Res
import heron.feature.compose.generated.resources.post
import org.jetbrains.compose.resources.stringResource

@Composable
fun PaneScaffoldState.TopAppBarFab(
    modifier: Modifier,
    state: State,
    onCreatePost: (Action.CreatePost) -> Unit,
) {
    ComposePostFab(
        modifier = modifier
            .height(36.dp),
        state = state,
        isInTopAppBar = true,
        onCreatePost = onCreatePost,
    )
}

@Composable
fun PaneScaffoldState.BottomAppBarFab(
    modifier: Modifier,
    state: State,
    onCreatePost: (Action.CreatePost) -> Unit,
) {
    ComposePostFab(
        modifier = modifier,
        state = state,
        isInTopAppBar = false,
        onCreatePost = onCreatePost,
    )
}

@Composable
private fun PaneScaffoldState.ComposePostFab(
    modifier: Modifier,
    state: State,
    isInTopAppBar: Boolean,
    onCreatePost: (Action.CreatePost) -> Unit,
) {
    PaneFab(
        modifier = modifier
            .alpha(if (state.postText.text.isNotBlank()) 1f else 0.6f),
        expanded = state.fabExpanded,
        text = stringResource(Res.string.post),
        icon = Icons.AutoMirrored.Rounded.Send.takeIf { !isInTopAppBar },
        visible = if (isInTopAppBar) state.hasLongPost else !state.hasLongPost,
        onClick = onClick@{
            val authorId = state.signedInProfile?.did ?: return@onClick
            val postText = state.postText

            onCreatePost(
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
}
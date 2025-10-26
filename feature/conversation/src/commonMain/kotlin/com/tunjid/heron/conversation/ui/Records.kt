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

package com.tunjid.heron.conversation.ui

import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.timeline.ui.PostActions
import com.tunjid.heron.timeline.ui.post.feature.QuotedPost
import kotlinx.datetime.Clock

@Composable
internal fun PostRecord(
    modifier: Modifier = Modifier,
    post: Post,
    sharedElementPrefix: String,
    paneScaffoldState: PaneScaffoldState,
    postActions: PostActions,
) {
    OutlinedCard(
        modifier = modifier,
    ) {
        QuotedPost(
            paneMovableElementSharedTransitionScope = paneScaffoldState,
            now = remember { Clock.System.now() },
            quotedPost = post,
            isBlurred = false,
            sharedElementPrefix = sharedElementPrefix,
            onClick = {
                postActions.onPostClicked(
                    post = post,
                    quotingPostUri = null,
                )
            },
            onLinkTargetClicked = postActions::onLinkTargetClicked,
            onProfileClicked = { post, profile ->
                postActions.onProfileClicked(
                    profile = profile,
                    post = post,
                    quotingPostUri = null,
                )
            },
            onPostMediaClicked = { mediaEmbed, index, post ->
                postActions.onPostMediaClicked(
                    media = mediaEmbed,
                    index = index,
                    post = post,
                    quotingPostUri = null,
                )
            },
        )
    }
}

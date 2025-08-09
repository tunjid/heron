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

package com.tunjid.heron.search.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.search.SearchResult
import com.tunjid.heron.timeline.ui.post.Post
import com.tunjid.heron.timeline.ui.postActions
import com.tunjid.heron.timeline.utilities.createdAt
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import kotlinx.datetime.Instant

@Composable
internal fun PostSearchResult(
    modifier: Modifier = Modifier,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    now: Instant,
    result: SearchResult.OfPost,
    onLinkTargetClicked: (SearchResult.OfPost, LinkTarget) -> Unit,
    onProfileClicked: (SearchResult.OfPost) -> Unit,
    onPostClicked: (SearchResult.OfPost) -> Unit,
    onReplyToPost: (SearchResult.OfPost) -> Unit,
    onMediaClicked: (media: Embed.Media, index: Int, result: SearchResult.OfPost, quotingPostId: PostId?) -> Unit,
    onPostInteraction: (Post.Interaction) -> Unit,
) {
    ElevatedCard(
        modifier = modifier,
        onClick = {
            onPostClicked(result)
        },
        content = {
            Post(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 16.dp,
                        bottom = 8.dp,
                    ),
                paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                presentationLookaheadScope = paneMovableElementSharedTransitionScope,
                now = now,
                post = result.post,
                isAnchoredInTimeline = false,
                avatarShape = RoundedPolygonShape.Circle,
                sharedElementPrefix = result.sharedElementPrefix,
                createdAt = result.post.createdAt,
                presentation = Timeline.Presentation.Text.WithEmbed,
                labelVisibilitiesToDefinitions = result.labelVisibilitiesToDefinitions,
                postActions = remember(result, onPostInteraction) {
                    postActions(
                        onLinkTargetClicked = { _, linkTarget ->
                            onLinkTargetClicked(result, linkTarget)
                        },
                        onPostClicked = { _, _ ->
                            onPostClicked(result)
                        },
                        onProfileClicked = { _, _, _ ->
                            onProfileClicked(result)
                        },
                        onPostMediaClicked = { media, index, _, quotingPostId ->
                            onMediaClicked(media, index, result, quotingPostId)
                        },
                        onReplyToPost = {
                            onReplyToPost(result)
                        },
                        onPostInteraction = onPostInteraction,
                    )
                },
            )
        },
    )
}
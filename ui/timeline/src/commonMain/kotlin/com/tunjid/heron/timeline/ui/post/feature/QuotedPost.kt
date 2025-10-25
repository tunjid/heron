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

package com.tunjid.heron.timeline.ui.post.feature

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.ExternalEmbed
import com.tunjid.heron.data.core.models.ImageList
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.UnknownEmbed
import com.tunjid.heron.data.core.models.Video
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.ui.avatarSharedElementKey
import com.tunjid.heron.timeline.ui.post.PostExternal
import com.tunjid.heron.timeline.ui.post.PostHeadline
import com.tunjid.heron.timeline.ui.post.PostImages
import com.tunjid.heron.timeline.ui.post.PostText
import com.tunjid.heron.timeline.ui.post.PostVideo
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun QuotedPost(
    modifier: Modifier = Modifier,
    now: Instant,
    quotedPost: Post,
    sharedElementPrefix: String,
    isBlurred: Boolean,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    onClick: () -> Unit,
    onLinkTargetClicked: (Post, LinkTarget) -> Unit,
    onProfileClicked: (Post, Profile) -> Unit,
    onPostMediaClicked: (Embed.Media, Int, Post) -> Unit,
) = with(paneMovableElementSharedTransitionScope) {
    val author = quotedPost.author
    Box(
        modifier = modifier,
    ) {
        FeatureContainer(
            modifier = Modifier.padding(16.dp),
            onClick = onClick,
        ) {
            Row(
                horizontalArrangement = spacedBy(8.dp),
            ) {
                AsyncImage(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.CenterVertically)
                        .paneStickySharedElement(
                            sharedContentState = rememberSharedContentState(
                                key = quotedPost.avatarSharedElementKey(sharedElementPrefix),
                            ),
                        )
                        .clickable {
                            onProfileClicked(quotedPost, author)
                        },
                    args = ImageArgs(
                        url = author.avatar?.uri,
                        contentDescription = author.displayName ?: author.handle.id,
                        contentScale = ContentScale.Crop,
                        shape = RoundedPolygonShape.Circle,
                    ),
                )
                PostHeadline(
                    now = now,
                    createdAt = quotedPost.record?.createdAt ?: remember { Clock.System.now() },
                    author = author,
                    postId = quotedPost.cid,
                    sharedElementPrefix = sharedElementPrefix,
                    paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                )
            }
            Spacer(Modifier.height(2.dp))
            PostText(
                post = quotedPost,
                sharedElementPrefix = sharedElementPrefix,
                paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                maxLines = 3,
                modifier = Modifier,
                onClick = onClick,
                onLinkTargetClicked = onLinkTargetClicked,
            )

            Spacer(Modifier.height(8.dp))

            val uriHandler = LocalUriHandler.current
            when (val embed = quotedPost.embed) {
                is ExternalEmbed -> PostExternal(
                    feature = embed,
                    postUri = quotedPost.uri,
                    sharedElementPrefix = sharedElementPrefix,
                    isBlurred = isBlurred,
                    paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                    // Quotes are exclusively in blog view types
                    presentation = Timeline.Presentation.Text.WithEmbed,
                    onClick = {
                        uriHandler.openUri(embed.uri.uri)
                    },
                )

                is ImageList -> PostImages(
                    modifier = Modifier
                        .heightIn(max = 140.dp),
                    feature = embed,
                    postUri = quotedPost.uri,
                    sharedElementPrefix = sharedElementPrefix,
                    isBlurred = isBlurred,
                    paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                    onImageClicked = { index ->
                        onPostMediaClicked(embed, index, quotedPost)
                    },
                    // Quotes are exclusively in blog view types
                    presentation = Timeline.Presentation.Text.WithEmbed,
                )

                UnknownEmbed -> UnknownPostPost(onClick = {})
                is Video -> PostVideo(
                    modifier = Modifier
                        .heightIn(max = 140.dp),
                    video = embed,
                    postUri = quotedPost.uri,
                    paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                    sharedElementPrefix = sharedElementPrefix,
                    isBlurred = isBlurred,
                    // Quote videos only show in text and embeds
                    presentation = Timeline.Presentation.Text.WithEmbed,
                    onClicked = {
                        onPostMediaClicked(embed, 0, quotedPost)
                    },
                )

                null -> Unit
            }
        }
    }
}

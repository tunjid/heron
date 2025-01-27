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

package com.tunjid.heron.timeline.ui.post

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.ui.avatarSharedElementKey
import com.tunjid.heron.timeline.utilities.createdAt
import com.tunjid.heron.timeline.utilities.format
import com.tunjid.heron.ui.AttributionLayout
import com.tunjid.heron.ui.PanedSharedElementScope
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableSharedElementOf
import kotlinx.datetime.Instant

@Composable
fun Post(
    panedSharedElementScope: PanedSharedElementScope,
    modifier: Modifier = Modifier,
    now: Instant,
    post: Post,
    embed: Embed?,
    isAnchoredInTimeline: Boolean,
    avatarShape: RoundedPolygonShape,
    sharedElementPrefix: String,
    createdAt: Instant,
    onProfileClicked: (Post, Profile) -> Unit,
    onPostClicked: (Post) -> Unit,
    onPostMediaClicked: (Post, Embed.Media, Int) -> Unit,
    onReplyToPost: () -> Unit,
    onPostInteraction: (Post.Interaction) -> Unit,
    onPostMetadataClicked: (Post.Metadata) -> Unit = {},
    timeline: @Composable (BoxScope.() -> Unit) = {},
) {
    Box(modifier = modifier) {
        timeline()
        Column(
            modifier = Modifier,
        ) {
            PostAttribution(
                panedSharedElementScope = panedSharedElementScope,
                avatarShape = avatarShape,
                onProfileClicked = onProfileClicked,
                post = post,
                sharedElementPrefix = sharedElementPrefix,
                now = now,
                createdAt = createdAt,
            )
            Spacer(Modifier.height(4.dp))
            Column(
                modifier = Modifier.padding(
                    start = 24.dp,
                    bottom = 8.dp
                ),
                verticalArrangement = spacedBy(8.dp),
            ) {
                PostText(
                    post = post,
                    sharedElementPrefix = sharedElementPrefix,
                    panedSharedElementScope = panedSharedElementScope,
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = { onPostClicked(post) },
                    onProfileClicked = onProfileClicked
                )
                PostEmbed(
                    now = now,
                    embed = embed,
                    quote = post.quote,
                    postId = post.cid,
                    sharedElementPrefix = sharedElementPrefix,
                    panedSharedElementScope = panedSharedElementScope,
                    onPostMediaClicked = { media, index ->
                        onPostMediaClicked(post, media, index)
                    },
                    onPostClicked = onPostClicked,
                )
                if (isAnchoredInTimeline) PostMetadata(
                    modifier = Modifier.padding(
                        vertical = 8.dp,
                    ),
                    time = post.createdAt,
                    postId = post.cid,
                    profileId = post.author.did,
                    reposts = post.repostCount,
                    quotes = post.quoteCount,
                    likes = post.likeCount,
                    onMetadataClicked = onPostMetadataClicked,
                )
                PostActions(
                    replyCount = format(post.replyCount),
                    repostCount = format(post.repostCount),
                    likeCount = format(post.likeCount),
                    repostUri = post.viewerStats?.repostUri,
                    likeUri = post.viewerStats?.likeUri,
                    iconSize = 16.dp,
                    postId = post.cid,
                    postUri = post.uri,
                    sharedElementPrefix = sharedElementPrefix,
                    panedSharedElementScope = panedSharedElementScope,
                    onReplyToPost = onReplyToPost,
                    onPostInteraction = onPostInteraction,
                )
            }
        }
    }
}


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PostAttribution(
    panedSharedElementScope: PanedSharedElementScope,
    avatarShape: RoundedPolygonShape,
    onProfileClicked: (Post, Profile) -> Unit,
    post: Post,
    sharedElementPrefix: String,
    now: Instant,
    createdAt: Instant,
) = with(panedSharedElementScope) {
    AttributionLayout(
        avatar = {
            updatedMovableSharedElementOf(
                modifier = Modifier
                    .size(48.dp)
                    .clip(avatarShape)
                    .clickable { onProfileClicked(post, post.author) },
                key = post.avatarSharedElementKey(sharedElementPrefix),
                state = remember(post.author.avatar) {
                    ImageArgs(
                        url = post.author.avatar?.uri,
                        contentScale = ContentScale.Crop,
                        contentDescription = post.author.displayName ?: post.author.handle.id,
                        shape = avatarShape,
                    )
                },
                sharedElement = { state, modifier ->
                    AsyncImage(state, modifier)
                }
            )
        },
        label = {
            PostHeadline(
                now = now,
                createdAt = createdAt,
                author = post.author,
                postId = post.cid,
                sharedElementPrefix = sharedElementPrefix,
                panedSharedElementScope = panedSharedElementScope,
            )
        }
    )
    //                if (item is TimelineItem.Reply) {
//                    PostReplyLine(item.parentPost.author, onProfileClicked)
//                }
}
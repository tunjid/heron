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

package com.tunjid.heron.notifications.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Badge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.ui.avatarSharedElementKey
import com.tunjid.heron.timeline.ui.post.PostInteractions
import com.tunjid.heron.timeline.ui.post.PostHeadline
import com.tunjid.heron.timeline.ui.post.PostText
import com.tunjid.heron.timeline.utilities.format
import com.tunjid.heron.ui.AttributionLayout
import com.tunjid.heron.ui.PanedSharedElementScope
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableSharedElementOf
import kotlinx.datetime.Instant

@Composable
internal fun NotificationPostScaffold(
    modifier: Modifier = Modifier,
    panedSharedElementScope: PanedSharedElementScope,
    now: Instant,
    isRead: Boolean,
    notification: Notification.PostAssociated,
    onProfileClicked: (Notification.PostAssociated, Profile) -> Unit,
    onPostClicked: (Notification.PostAssociated) -> Unit,
    onPostMediaClicked: (Post, Embed.Media, Int) -> Unit,
    onReplyToPost: (Notification.PostAssociated) -> Unit,
    onPostInteraction: (Post.Interaction) -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        PostAttribution(
            panedSharedElementScope = panedSharedElementScope,
            avatarShape = RoundedPolygonShape.Circle,
            onProfileClicked = onProfileClicked,
            notification = notification,
            sharedElementPrefix = notification.sharedElementPrefix(),
            now = now,
            createdAt = notification.indexedAt
        )
        Spacer(Modifier.height(4.dp))
        Row(
            horizontalArrangement = spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(UiTokens.avatarSize)
            ) {
                if (!isRead) Badge(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(vertical = 8.dp)
                        .size(4.dp)
                )
            }
            Column(
                modifier = Modifier.padding(
                    bottom = 8.dp
                ),
                verticalArrangement = spacedBy(8.dp),
            ) {
                PostText(
                    post = notification.associatedPost,
                    sharedElementPrefix = notification.sharedElementPrefix(),
                    panedSharedElementScope = panedSharedElementScope,
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = { onPostClicked(notification) },
                    onProfileClicked = { _, profile ->
                        onProfileClicked(notification, profile)
                    }
                )
//                PostEmbed(
//                    now = now,
//                    embed = embed,
//                    quote = post.quote,
//                    postId = post.cid,
//                    sharedElementPrefix = sharedElementPrefix,
//                    sharedElementScope = sharedElementScope,
//                    onPostMediaClicked = onPostMediaClicked,
//                    onPostClicked = onPostClicked,
//                )

                PostInteractions(
                    replyCount = format(notification.associatedPost.replyCount),
                    repostCount = format(notification.associatedPost.repostCount),
                    likeCount = format(notification.associatedPost.likeCount),
                    repostUri = notification.associatedPost.viewerStats?.repostUri,
                    likeUri = notification.associatedPost.viewerStats?.likeUri,
                    postId = notification.associatedPost.cid,
                    postUri = notification.associatedPost.uri,
                    sharedElementPrefix = notification.sharedElementPrefix(),
                    presentation = Timeline.Presentation.Text.WithEmbed,
                    panedSharedElementScope = panedSharedElementScope,
                    presentationLookaheadScope = panedSharedElementScope,
                    onReplyToPost = {
                        onReplyToPost(notification)
                    },
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
    onProfileClicked: (Notification.PostAssociated, Profile) -> Unit,
    notification: Notification.PostAssociated,
    sharedElementPrefix: String,
    now: Instant,
    createdAt: Instant,
) = with(panedSharedElementScope) {
    val post = notification.associatedPost
    AttributionLayout(
        avatar = {
            updatedMovableSharedElementOf(
                modifier = Modifier
                    .size(UiTokens.avatarSize)
                    .clip(avatarShape)
                    .clickable { onProfileClicked(notification, post.author) },
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
//    if (item is TimelineItem.Reply) {
//                    PostReplyLine(item.parentPost.author, onProfileClicked)
//                }
}

fun Notification.PostAssociated.sharedElementPrefix(
): String = "notification-${cid.id}"
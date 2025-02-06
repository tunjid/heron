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

package com.tunjid.heron.timeline.ui

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.ui.post.PostStatistics
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import kotlinx.datetime.Instant

@Composable
fun FeedThreadItem(
    modifier: Modifier = Modifier,
    now: Instant,
    post: Post,
    onProfileClicked: Post?.(Profile) -> Unit,
    onPostMediaClicked: (Post, Embed.Media, Int) -> Unit,
    onOpenPost: (Post) -> Unit,
    onReplyToPost: (Post) -> Unit,
) {
    Column(modifier = modifier.padding(top = 16.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = spacedBy(16.dp),
        ) {
            val author: Profile = post.author
            AsyncImage(
                modifier = Modifier
                    .size(UiTokens.avatarSize),
                args = ImageArgs(
                    url = author.avatar?.uri,
                    contentDescription = author.displayName ?: author.handle.id,
                    contentScale = ContentScale.Crop,
                    shape = RoundedPolygonShape.Circle,
                ),
            )
            Column(Modifier.weight(1f)) {
//                PostHeadline(now, post.createdAt, author)
//                PostReasonLine(post.reason, onOpenUser)
                Column(
                    modifier = Modifier.padding(bottom = 8.dp),
                    verticalArrangement = spacedBy(8.dp),
                ) {
                    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.headlineSmall) {
//                        PostText(post, {}, onProfileClicked)
                    }
//                    PostEmbed(
//                        now = now,
//                        embed = post.embed,
//                        quote = post.quote,
//                        onPostMediaClicked = onPostMediaClicked,
//                        onPostClicked = onOpenPost,
//                    )
//                    post.record?.let {
//                        PostDate(it.createdAt)
//                    }
                }
            }
        }

        Column {
            if (post.hasInteractions()) {
                HorizontalDivider(thickness = 1.dp)

                Box(modifier = Modifier.padding(start = 80.dp)) {
                    PostStatistics(
                        post = post,
                        onReplyToPost = onReplyToPost,
                    )
                }
            }

            HorizontalDivider(thickness = 1.dp)

            Box(modifier = Modifier.padding(start = 80.dp, top = 8.dp, bottom = 8.dp)) {
//                PostActions(
//                    replyCount = null,
//                    repostCount = null,
//                    likeCount = null,
//                    reposted = false,
//                    liked = false,
//                    iconSize = 24.dp,
//                    onReplyToPost = onReplyToPost,
//                )
            }

            HorizontalDivider(thickness = 1.dp)
        }
    }
}

fun Post.hasInteractions(): Boolean {
    return replyCount > 0 || repostCount > 0 || likeCount > 0
}

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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DynamicFeed
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.empty_timeline_feed
import heron.ui.timeline.generated.resources.empty_timeline_feed_description
import heron.ui.timeline.generated.resources.empty_timeline_generic
import heron.ui.timeline.generated.resources.empty_timeline_generic_description
import heron.ui.timeline.generated.resources.empty_timeline_likes
import heron.ui.timeline.generated.resources.empty_timeline_likes_description
import heron.ui.timeline.generated.resources.empty_timeline_list
import heron.ui.timeline.generated.resources.empty_timeline_list_description
import heron.ui.timeline.generated.resources.empty_timeline_media
import heron.ui.timeline.generated.resources.empty_timeline_media_description
import heron.ui.timeline.generated.resources.empty_timeline_posts
import heron.ui.timeline.generated.resources.empty_timeline_posts_description
import heron.ui.timeline.generated.resources.empty_timeline_replies
import heron.ui.timeline.generated.resources.empty_timeline_replies_description
import heron.ui.timeline.generated.resources.empty_timeline_videos
import heron.ui.timeline.generated.resources.empty_timeline_videos_description
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun EmptyPost(
    modifier: Modifier = Modifier,
    item: TimelineItem.Empty,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(
                vertical = 36.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxHeight(0.6f)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        modifier = Modifier.size(36.dp),
                        imageVector = item.timeline.emptyIcon(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(item.timeline.emptyTextRes()),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(item.timeline.emptyDescriptionRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun Timeline.emptyTextRes(): StringResource = when (this) {
    is Timeline.Home.Following -> Res.string.empty_timeline_generic
    is Timeline.Home.Feed -> Res.string.empty_timeline_feed
    is Timeline.Home.List -> Res.string.empty_timeline_list
    is Timeline.StarterPack -> Res.string.empty_timeline_list
    is Timeline.Profile -> when (type) {
        Timeline.Profile.Type.Posts -> Res.string.empty_timeline_posts
        Timeline.Profile.Type.Replies -> Res.string.empty_timeline_replies
        Timeline.Profile.Type.Likes -> Res.string.empty_timeline_likes
        Timeline.Profile.Type.Media -> Res.string.empty_timeline_media
        Timeline.Profile.Type.Videos -> Res.string.empty_timeline_videos
    }
}

private fun Timeline.emptyDescriptionRes(): StringResource = when (this) {
    is Timeline.Home.Following -> Res.string.empty_timeline_generic_description
    is Timeline.Home.Feed -> Res.string.empty_timeline_feed_description
    is Timeline.Home.List -> Res.string.empty_timeline_list_description
    is Timeline.StarterPack -> Res.string.empty_timeline_list_description
    is Timeline.Profile -> when (type) {
        Timeline.Profile.Type.Posts -> Res.string.empty_timeline_posts_description
        Timeline.Profile.Type.Replies -> Res.string.empty_timeline_replies_description
        Timeline.Profile.Type.Likes -> Res.string.empty_timeline_likes_description
        Timeline.Profile.Type.Media -> Res.string.empty_timeline_media_description
        Timeline.Profile.Type.Videos -> Res.string.empty_timeline_videos_description
    }
}

private fun Timeline.emptyIcon(): ImageVector = when (this) {
    is Timeline.Home.Following -> Icons.Rounded.Dashboard
    is Timeline.Home.Feed -> Icons.Rounded.DynamicFeed
    is Timeline.Home.List -> Icons.AutoMirrored.Rounded.List
    is Timeline.StarterPack -> Icons.Rounded.Group
    is Timeline.Profile -> when (type) {
        Timeline.Profile.Type.Posts -> Icons.AutoMirrored.Rounded.Article
        Timeline.Profile.Type.Replies -> Icons.Rounded.Forum
        Timeline.Profile.Type.Likes -> Icons.Rounded.Favorite
        Timeline.Profile.Type.Media -> Icons.Rounded.Image
        Timeline.Profile.Type.Videos -> Icons.Rounded.Videocam
    }
}

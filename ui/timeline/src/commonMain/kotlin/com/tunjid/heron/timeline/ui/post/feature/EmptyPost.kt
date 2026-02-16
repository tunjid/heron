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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.empty_timeline_feed
import heron.ui.timeline.generated.resources.empty_timeline_generic
import heron.ui.timeline.generated.resources.empty_timeline_likes
import heron.ui.timeline.generated.resources.empty_timeline_list
import heron.ui.timeline.generated.resources.empty_timeline_media
import heron.ui.timeline.generated.resources.empty_timeline_posts
import heron.ui.timeline.generated.resources.empty_timeline_replies
import heron.ui.timeline.generated.resources.empty_timeline_videos
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun EmptyPost(
    modifier: Modifier = Modifier,
    item: TimelineItem.Empty,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(item.timeline.emptyTextRes()),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

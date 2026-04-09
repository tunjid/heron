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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.timeline.ui.EmptyContent
import com.tunjid.heron.ui.icons.Article
import com.tunjid.heron.ui.icons.Dashboard
import com.tunjid.heron.ui.icons.DynamicFeed
import com.tunjid.heron.ui.icons.Favorite
import com.tunjid.heron.ui.icons.Forum
import com.tunjid.heron.ui.icons.Group
import com.tunjid.heron.ui.icons.HeronIcons
import com.tunjid.heron.ui.icons.Image
import com.tunjid.heron.ui.icons.List
import com.tunjid.heron.ui.icons.ModeComment
import com.tunjid.heron.ui.icons.Videocam
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
import heron.ui.timeline.generated.resources.empty_timeline_thread
import heron.ui.timeline.generated.resources.empty_timeline_thread_description
import heron.ui.timeline.generated.resources.empty_timeline_videos
import heron.ui.timeline.generated.resources.empty_timeline_videos_description
import org.jetbrains.compose.resources.StringResource

@Composable
internal fun EmptyPost(
    modifier: Modifier = Modifier,
    item: TimelineItem.Empty,
) {
    EmptyContent(
        modifier = modifier,
        titleRes = item.emptyTextRes(),
        descriptionRes = item.emptyDescriptionRes(),
        icon = item.emptyIcon(),
    )
}

private fun TimelineItem.Empty.emptyTextRes(): StringResource = when (this) {
    is TimelineItem.Empty.Thread -> Res.string.empty_timeline_thread
    is TimelineItem.Empty.Timeline -> when (val timeline = timeline) {
        is Timeline.Home.Following -> Res.string.empty_timeline_generic
        is Timeline.Home.Feed -> Res.string.empty_timeline_feed
        is Timeline.Home.List -> Res.string.empty_timeline_list
        is Timeline.StarterPack -> Res.string.empty_timeline_list
        is Timeline.Profile -> when (timeline.type) {
            Timeline.Profile.Type.Posts -> Res.string.empty_timeline_posts
            Timeline.Profile.Type.Replies -> Res.string.empty_timeline_replies
            Timeline.Profile.Type.Likes -> Res.string.empty_timeline_likes
            Timeline.Profile.Type.Media -> Res.string.empty_timeline_media
            Timeline.Profile.Type.Videos -> Res.string.empty_timeline_videos
        }
    }
}

private fun TimelineItem.Empty.emptyDescriptionRes(): StringResource = when (this) {
    TimelineItem.Empty.Thread -> Res.string.empty_timeline_thread_description
    is TimelineItem.Empty.Timeline -> when (val timeline = timeline) {
        is Timeline.Home.Following -> Res.string.empty_timeline_generic_description
        is Timeline.Home.Feed -> Res.string.empty_timeline_feed_description
        is Timeline.Home.List -> Res.string.empty_timeline_list_description
        is Timeline.StarterPack -> Res.string.empty_timeline_list_description
        is Timeline.Profile -> when (timeline.type) {
            Timeline.Profile.Type.Posts -> Res.string.empty_timeline_posts_description
            Timeline.Profile.Type.Replies -> Res.string.empty_timeline_replies_description
            Timeline.Profile.Type.Likes -> Res.string.empty_timeline_likes_description
            Timeline.Profile.Type.Media -> Res.string.empty_timeline_media_description
            Timeline.Profile.Type.Videos -> Res.string.empty_timeline_videos_description
        }
    }
}

private fun TimelineItem.Empty.emptyIcon(): ImageVector = when (this) {
    TimelineItem.Empty.Thread -> HeronIcons.ModeComment
    is TimelineItem.Empty.Timeline -> when (val timeline = timeline) {
        is Timeline.Home.Following -> HeronIcons.Dashboard
        is Timeline.Home.Feed -> HeronIcons.DynamicFeed
        is Timeline.Home.List -> HeronIcons.List
        is Timeline.StarterPack -> HeronIcons.Group
        is Timeline.Profile -> when (timeline.type) {
            Timeline.Profile.Type.Posts -> HeronIcons.Article
            Timeline.Profile.Type.Replies -> HeronIcons.Forum
            Timeline.Profile.Type.Likes -> HeronIcons.Favorite
            Timeline.Profile.Type.Media -> HeronIcons.Image
            Timeline.Profile.Type.Videos -> HeronIcons.Videocam
        }
    }
}

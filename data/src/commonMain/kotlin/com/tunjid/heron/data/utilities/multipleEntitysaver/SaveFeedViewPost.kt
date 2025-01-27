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

package com.tunjid.heron.data.utilities.multipleEntitysaver

import app.bsky.feed.FeedViewPost
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.database.entities.PostThreadEntity
import com.tunjid.heron.data.network.models.feedItemEntity
import com.tunjid.heron.data.network.models.postEntity
import com.tunjid.heron.data.network.models.postViewerStatisticsEntity
import com.tunjid.heron.data.network.models.profileEntity

internal fun MultipleEntitySaver.add(
    viewingProfileId: Id,
    timeline: Timeline,
    feedViewPosts: List<FeedViewPost>,
) {
    for (feedView in feedViewPosts) {
        // Extract data from feed
        add(feedView.feedItemEntity(timeline.sourceId))

        // Extract data from post
        add(
            viewingProfileId = viewingProfileId,
            postView = feedView.post,
        )
        feedView.reason?.profileEntity()?.let(::add)

        feedView.reply?.let {
            it.root.postEntity().let(::add)
            it.root.profileEntity()?.let(::add)
            it.root.postViewerStatisticsEntity()?.let(::add)

            val parentPostEntity = it.parent.postEntity().also(::add)
            it.parent.profileEntity()?.let(::add)
            it.parent.postViewerStatisticsEntity()?.let(::add)

            it.grandparentAuthor?.profileEntity()?.let(::add)

            add(
                PostThreadEntity(
                    postId = feedView.post.postEntity().cid,
                    parentPostId = parentPostEntity.cid,
                )
            )
        }
    }
}
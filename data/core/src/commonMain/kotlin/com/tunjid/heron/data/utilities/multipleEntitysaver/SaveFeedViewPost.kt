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
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.id
import com.tunjid.heron.data.core.models.offset
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.PostThreadEntity
import com.tunjid.heron.data.network.models.feedItemEntity
import com.tunjid.heron.data.network.models.postEntity
import com.tunjid.heron.data.network.models.postView
import com.tunjid.heron.data.network.models.postViewerStatisticsEntity
import com.tunjid.heron.data.network.models.profileEntity

internal fun MultipleEntitySaver.add(
    viewingProfileId: ProfileId?,
    query: CursorQuery,
    source: Timeline.Source,
    feedViewPosts: List<FeedViewPost>,
) {
    for (index in feedViewPosts.indices) {
        val feedView = feedViewPosts[index]
        // Extract data from feed
        add(
            feedView.feedItemEntity(
                sourceId = source.id,
                itemSort = query.itemSortKey(index),
                viewingProfileId = viewingProfileId,
            )
        )

        // Extract data from post
        add(viewingProfileId = viewingProfileId, postView = feedView.post)
        feedView.reason?.profileEntity()?.let(::add)

        feedView.reply?.let {
            when (val rootPostView = it.root.postView()) {
                null -> {
                    it.root.postEntity().let(::add)
                    it.root.profileEntity()?.let(::add)
                    it.root.postViewerStatisticsEntity(viewingProfileId)?.let(::add)
                }
                else -> add(viewingProfileId = viewingProfileId, postView = rootPostView)
            }

            val parentPostUri =
                when (val parentPostView = it.parent.postView()) {
                    null -> {
                        it.parent.profileEntity()?.let(::add)
                        it.parent.postViewerStatisticsEntity(viewingProfileId)?.let(::add)
                        it.parent.postEntity().also(::add).uri
                    }
                    else -> {
                        add(viewingProfileId = viewingProfileId, postView = parentPostView)
                        parentPostView.uri.atUri.let(::PostUri)
                    }
                }

            it.grandparentAuthor?.profileEntity()?.let(::add)

            add(
                PostThreadEntity(
                    postUri = feedView.post.postEntity().uri,
                    parentPostUri = parentPostUri,
                )
            )
        }
    }
}

/**
 * Allows up to [ITEM_SORT_BUFFER] items (333 pages, 30 items per page) before sorting logic breaks.
 * Safe from Long overflow for ~29,000 years.
 */
private fun CursorQuery.itemSortKey(index: Int) =
    (data.cursorAnchor.toEpochMilliseconds() * ITEM_SORT_BUFFER) - (data.offset + index)

private const val ITEM_SORT_BUFFER = 10_000L

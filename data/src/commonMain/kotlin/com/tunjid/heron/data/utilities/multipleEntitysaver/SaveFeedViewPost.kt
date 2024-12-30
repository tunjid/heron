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
package com.tunjid.heron.data.core.models

import kotlinx.datetime.Instant

sealed class TimelineItem {

    abstract val id: String
    abstract val post: Post

    val indexedAt
        get() = when (this) {
            is Pinned,
            is Thread,
            is Single -> post.indexedAt

            is Repost -> at
        }

    data class Pinned(
        override val id: String,
        override val post: Post,
    ) : TimelineItem()

    data class Repost(
        override val id: String,
        override val post: Post,
        val by: Profile,
        val at: Instant,
    ) : TimelineItem()

    data class Thread(
        override val id: String,
        val anchorPostIndex: Int,
        val posts: List<Post>,
    ) : TimelineItem() {
        override val post: Post
            get() = posts[anchorPostIndex]
    }

    data class Single(
        override val id: String,
        override val post: Post,
    ) : TimelineItem()
}

package com.tunjid.heron.data.core.models

import kotlinx.datetime.Instant

sealed class TimelineItem {

    abstract val id: String
    abstract val post: Post
    abstract val sourceId: String

    val indexedAt
        get() = when (this) {
            is Pinned,
            is Reply,
            is Single -> post.indexedAt

            is Repost -> at
        }

    data class Pinned(
        override val id: String,
        override val post: Post,
        override val sourceId: String,
    ) : TimelineItem()

    data class Repost(
        override val id: String,
        override val post: Post,
        override val sourceId: String,
        val by: Profile,
        val at: Instant,
    ) : TimelineItem()

    data class Reply(
        override val id: String,
        override val post: Post,
        override val sourceId: String,
        val rootPost: Post,
        val parentPost: Post,
    ) : TimelineItem()

    data class Single(
        override val id: String,
        override val post: Post,
        override val sourceId: String,
    ) : TimelineItem()
}

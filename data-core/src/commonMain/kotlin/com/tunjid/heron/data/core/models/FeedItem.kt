package com.tunjid.heron.data.core.models

import kotlinx.datetime.Instant

sealed class FeedItem {

    abstract val id: String
    abstract val post: Post

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
    ) : FeedItem()

    data class Repost(
        override val id: String,
        override val post: Post,
        val by: Profile,
        val at: Instant,
    ) : FeedItem()

    data class Reply(
        override val id: String,
        override val post: Post,
        val rootPost: Post,
        val parentPost: Post,
    ) : FeedItem()

    data class Single(
        override val id: String,
        override val post: Post,
    ) : FeedItem()
}

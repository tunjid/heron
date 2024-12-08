package com.tunjid.heron.data.core.models

sealed class FeedItem {

    abstract val post: Post

    data class Repost(
        override val post: Post,
    ) : FeedItem()

    data class Reply(
        override val post: Post,
        val rootPost: Post,
        val parentPost: Post,
    ) : FeedItem()

    data class Single(
        override val post: Post,
    ) : FeedItem()
}


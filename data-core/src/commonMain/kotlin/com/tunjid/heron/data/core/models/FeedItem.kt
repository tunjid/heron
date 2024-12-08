package com.tunjid.heron.data.core.models

data class FeedItem(
    val post: Post,
    val reply: Reply?
) {
    data class Reply(
        val rootPost: Post,
        val parentPost: Post,
    )
}


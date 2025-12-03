package com.tunjid.heron.fakes

import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.core.types.PostUri
import kotlin.time.Instant

fun samplePost(): Post {
    return Post(
        cid = PostId("p-cid-123"),
        uri = PostUri("at://post/123"),
        author = sampleProfile(),
        replyCount = 0,
        repostCount = 0,
        likeCount = 0,
        quoteCount = 0,
        bookmarkCount = 0,
        indexedAt = Instant.parse("2024-01-01T00:00:00Z"),
        embed = null,
        quote = null,
        record = Post.Record(
            text = "Hello World!",
            createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        ),
        viewerStats = Post.ViewerStats(
            likeUri = null,
            repostUri = null,
            threadMuted = false,
            replyDisabled = false,
            embeddingDisabled = false,
            pinned = false,
            bookmarked = false,
        ),
        labels = emptyList(),
        embeddedRecord = null,
    )
}

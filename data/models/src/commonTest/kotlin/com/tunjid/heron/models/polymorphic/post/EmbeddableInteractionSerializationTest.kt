package com.tunjid.heron.models.polymorphic.post

import app.cash.burst.Burst
import app.cash.burst.burstValues
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.LikeUri
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.RepostUri
import com.tunjid.heron.helpers.SerializationTestHelper
import kotlin.test.Test
import kotlin.test.assertEquals

@Burst
class EmbeddableInteractionSerializationTest(
    val format: SerializationTestHelper.Format = burstValues(
        SerializationTestHelper.Format.CBOR,
        SerializationTestHelper.Format.PROTOBUF,
    ),
    val original: Post.Interaction = burstValues(
        Post.Interaction.Create.Like(PostId("pid-1"), PostUri("at://post/like")),
        Post.Interaction.Create.Repost(PostId("pid-2"), PostUri("at://post/repost")),
        Post.Interaction.Create.Bookmark(PostId("pid-3"), PostUri("at://post/bookmark")),
        Post.Interaction.Delete.Unlike(PostUri("at://post/unlike"), LikeUri("at://like/1")),
        Post.Interaction.Delete.RemoveRepost(PostUri("at://post/removeRepost"), RepostUri("at://repost/1")),
        Post.Interaction.Delete.RemoveBookmark(PostUri("at://post/removeBookmark")),
    ),
) {
    @Test
    fun roundTrip() {
        val decoded = SerializationTestHelper.roundTrip(
            format = format,
            value = original,
            serializer = Post.Interaction.serializer(),
        )
        assertEquals(original, decoded)
    }
}

package com.tunjid.heron.models.polymorphic.post

import app.cash.burst.Burst
import app.cash.burst.burstValues
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.fakes.samplePost
import com.tunjid.heron.fakes.sampleProfile
import com.tunjid.heron.helpers.SerializationTestHelper
import kotlin.test.Test
import kotlin.test.assertEquals

@Burst
class PostCreateSerializationTest(
    val format: SerializationTestHelper.Format = burstValues(
        SerializationTestHelper.Format.CBOR,
        SerializationTestHelper.Format.PROTOBUF,
    ),
    val original: Post.Create = burstValues(
        Post.Create.Reply(parent = samplePost()),
        Post.Create.Mention(profile = sampleProfile()),
        Post.Create.Quote(
            interaction = Post.Interaction.Create.Repost(
                postId = PostId("pid-1"),
                postUri = PostUri("at://post/xyz"),
            ),
        ),
        Post.Create.Timeline,
    ),
) {
    @Test
    fun roundTrip() {
        val decoded = SerializationTestHelper.roundTrip(
            format = format,
            value = original,
            serializer = Post.Create.serializer(),
        )
        assertEquals(original, decoded)
    }
}

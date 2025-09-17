package com.tunjid.heron.models.polymorphic.post

import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.fakes.samplePost
import com.tunjid.heron.fakes.sampleProfile
import com.tunjid.heron.helpers.SerializationTestHelper
import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class PostCreateSerializationTest {

    @ParameterizedTest
    @EnumSource(SerializationTestHelper.Format::class)
    fun `round trip Post Create Reply`(format: SerializationTestHelper.Format) {
        val original = Post.Create.Reply(parent = samplePost())
        val decoded = SerializationTestHelper.roundTrip(format, original, Post.Create.serializer())
        assertEquals(original, decoded)
    }

    @ParameterizedTest
    @EnumSource(SerializationTestHelper.Format::class)
    fun `round trip Post Create Mention`(format: SerializationTestHelper.Format) {
        val original = Post.Create.Mention(profile = sampleProfile())
        val decoded = SerializationTestHelper.roundTrip(format, original, Post.Create.serializer())
        assertEquals(original, decoded)
    }

    @ParameterizedTest
    @EnumSource(SerializationTestHelper.Format::class)
    fun `round trip Post Create Quote`(format: SerializationTestHelper.Format) {
        val original = Post.Create.Quote(
            interaction = Post.Interaction.Create.Repost(
                postId = PostId("pid-1"),
                postUri = PostUri("at://post/xyz"),
            ),
        )
        val decoded = SerializationTestHelper.roundTrip(format, original, Post.Create.serializer())
        assertEquals(original, decoded)
    }

    @ParameterizedTest
    @EnumSource(SerializationTestHelper.Format::class)
    fun `round trip Post Create Timeline`(format: SerializationTestHelper.Format) {
        val original = Post.Create.Timeline
        val decoded = SerializationTestHelper.roundTrip(format, original, Post.Create.serializer())
        assertEquals(original, decoded)
    }
}

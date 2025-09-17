package com.tunjid.heron.models.polymorphic.post

import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.fakes.samplePost
import com.tunjid.heron.fakes.sampleProfile
import com.tunjid.heron.helpers.SerializationTestHelper
import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource

class PostCreateSerializationTest {

    @ParameterizedTest(name = "[{index}] {1} can be serialized with {0}")
    @MethodSource("postCreateCases")
    fun `round trip Post Create`(
        format: SerializationTestHelper.Format,
        original: Post.Create
    ) {
        val decoded = SerializationTestHelper.roundTrip(
            format = format,
            value = original,
            serializer = Post.Create.serializer()
        )
        assertEquals(original, decoded)
    }

    companion object {
        @JvmStatic
        private fun postCreateCases(): List<Arguments> {
            val postCreates = listOf(
                Post.Create.Reply(parent = samplePost()),
                Post.Create.Mention(profile = sampleProfile()),
                Post.Create.Quote(
                    interaction = Post.Interaction.Create.Repost(
                        postId = PostId("pid-1"),
                        postUri = PostUri("at://post/xyz"),
                    ),
                ),
                Post.Create.Timeline
            )
            return SerializationTestHelper.Format.entries.flatMap { format ->
                postCreates.map { postCreate ->
                    Arguments.of(format, postCreate)
                }
            }
        }
    }
}

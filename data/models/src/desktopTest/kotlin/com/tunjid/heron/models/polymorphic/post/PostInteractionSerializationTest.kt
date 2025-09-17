package com.tunjid.heron.models.polymorphic.post

import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.helpers.SerializationTestHelper
import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class PostInteractionSerializationTest {

    @ParameterizedTest(name = "[{index}] {1} can be serialized with {0}")
    @MethodSource("postInteractionCases")
    fun `round trip Post Interaction`(
        format: SerializationTestHelper.Format,
        original: Post.Interaction,
    ) {
        val decoded = SerializationTestHelper.roundTrip(
            format = format,
            value = original,
            serializer = Post.Interaction.serializer(),
        )
        assertEquals(original, decoded)
    }

    companion object {
        @JvmStatic
        private fun postInteractionCases(): List<Arguments> {
            val postInteractions = listOf(
                Post.Interaction.Create.Like(
                    postId = PostId("pid-1"),
                    postUri = PostUri("at://post/like"),
                ),
                Post.Interaction.Create.Repost(
                    postId = PostId("pid-2"),
                    postUri = PostUri("at://post/repost"),
                ),
                Post.Interaction.Create.Bookmark(
                    postId = PostId("pid-3"),
                    postUri = PostUri("at://post/bookmark"),
                ),
                Post.Interaction.Delete.Unlike(
                    postUri = PostUri("at://post/unlike"),
                    likeUri = GenericUri("at://like/1"),
                ),
                Post.Interaction.Delete.RemoveRepost(
                    postUri = PostUri("at://post/removeRepost"),
                    repostUri = GenericUri("at://repost/1"),
                ),
                Post.Interaction.Delete.RemoveBookmark(
                    postUri = PostUri("at://post/removeBookmark"),
                ),
            )
            return SerializationTestHelper.Format.entries.flatMap { format ->
                postInteractions.map { interaction ->
                    Arguments.of(format, interaction)
                }
            }
        }
    }
}

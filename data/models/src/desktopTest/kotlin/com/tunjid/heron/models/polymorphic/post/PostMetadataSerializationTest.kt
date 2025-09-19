package com.tunjid.heron.models.polymorphic.post

import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.helpers.SerializationTestHelper
import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class PostMetadataSerializationTest {

    @ParameterizedTest(name = "[{index}] {1} can be serialized with {0}")
    @MethodSource("postMetadataCases")
    fun `round trip Post Metadata`(
        format: SerializationTestHelper.Format,
        original: Post.Metadata,
    ) {
        val decoded = SerializationTestHelper.roundTrip(
            format = format,
            value = original,
            serializer = Post.Metadata.serializer(),
        )
        assertEquals(original, decoded)
    }

    companion object {
        @JvmStatic
        private fun postMetadataCases(): List<Arguments> {
            val postMetadatas = listOf(
                Post.Metadata.Likes(
                    profileId = ProfileId("profile-1"),
                    postRecordKey = RecordKey("record-1"),
                ),
                Post.Metadata.Reposts(
                    profileId = ProfileId("profile-2"),
                    postRecordKey = RecordKey("record-2"),
                ),
                Post.Metadata.Quotes(
                    profileId = ProfileId("profile-3"),
                    postRecordKey = RecordKey("record-3"),
                ),
            )
            return SerializationTestHelper.Format.entries.flatMap { format ->
                postMetadatas.map { metadata ->
                    Arguments.of(format, metadata)
                }
            }
        }
    }
}

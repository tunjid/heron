package com.tunjid.heron.models.polymorphic.post

import app.cash.burst.Burst
import app.cash.burst.burstValues
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.helpers.SerializationTestHelper
import kotlin.test.Test
import kotlin.test.assertEquals

@Burst
class PostMetadataSerializationTest(
    val format: SerializationTestHelper.Format = burstValues(
        SerializationTestHelper.Format.CBOR,
        SerializationTestHelper.Format.PROTOBUF,
    ),
    val original: Post.Metadata = burstValues(
        Post.Metadata.Likes(ProfileId("profile-1"), RecordKey("record-1")),
        Post.Metadata.Reposts(ProfileId("profile-2"), RecordKey("record-2")),
        Post.Metadata.Quotes(ProfileId("profile-3"), RecordKey("record-3")),
    ),
) {
    @Test
    fun roundTrip() {
        val decoded = SerializationTestHelper.roundTrip(
            format = format,
            value = original,
            serializer = Post.Metadata.serializer(),
        )
        assertEquals(original, decoded)
    }
}

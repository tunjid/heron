package com.tunjid.heron.models.polymorphic.profile

import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.helpers.SerializationTestHelper
import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class ProfileConnectionSerializationTest {

    @ParameterizedTest(name = "[{index}] {1} can be serialized with {0}")
    @MethodSource("profileConnectionCases")
    fun `round trip Profile Connection`(
        format: SerializationTestHelper.Format,
        original: Profile.Connection,
    ) {
        val decoded = SerializationTestHelper.roundTrip(
            format = format,
            value = original,
            serializer = Profile.Connection.serializer(),
        )
        assertEquals(original, decoded)
    }

    companion object {
        @JvmStatic
        private fun profileConnectionCases(): List<Arguments> {
            val connections = listOf(
                Profile.Connection.Follow(
                    signedInProfileId = ProfileId("did:example:me"),
                    profileId = ProfileId("did:example:target1"),
                    followedBy = GenericUri("at://follow/123"),
                ),
                Profile.Connection.Unfollow(
                    signedInProfileId = ProfileId("did:example:me"),
                    profileId = ProfileId("did:example:target2"),
                    followedBy = GenericUri("at://follow/456"),
                    followUri = GenericUri("at://unfollow/456"),
                ),
            )
            return SerializationTestHelper.Format.entries.flatMap { format ->
                connections.map { connection ->
                    Arguments.of(format, connection)
                }
            }
        }
    }
}

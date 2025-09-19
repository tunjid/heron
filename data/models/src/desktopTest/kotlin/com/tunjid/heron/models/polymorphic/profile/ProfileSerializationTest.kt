package com.tunjid.heron.models.polymorphic.profile

import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.stubProfile
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.fakes.sampleProfile
import com.tunjid.heron.helpers.SerializationTestHelper
import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class ProfileSerializationTest {

    @ParameterizedTest(name = "[{index}] Profile can be serialized with {0}")
    @MethodSource("profileCases")
    fun `round trip Profile`(
        format: SerializationTestHelper.Format,
        original: Profile,
    ) {
        val decoded = SerializationTestHelper.roundTrip(
            format = format,
            value = original,
            serializer = Profile.serializer(),
        )
        assertEquals(original, decoded)
    }

    companion object {
        @JvmStatic
        private fun profileCases(): List<Arguments> {
            val profiles = listOf(
                sampleProfile(),
                stubProfile(
                    did = ProfileId("did:example:stub"),
                    handle = ProfileHandle("stubuser"),
                    displayName = "Stub User",
                ),
            )
            return SerializationTestHelper.Format.entries.flatMap { format ->
                profiles.map { profile ->
                    Arguments.of(format, profile)
                }
            }
        }
    }
}

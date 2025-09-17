package com.tunjid.heron.models.polymorphic.profile

import com.tunjid.heron.data.core.models.ProfileViewerState
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.models.stubProfile
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.fakes.sampleProfile
import com.tunjid.heron.helpers.SerializationTestHelper
import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class ProfileWithViewerStateSerializationTest {

    @ParameterizedTest(name = "[{index}] ProfileWithViewerState can be serialized with {0}")
    @MethodSource("profileWithViewerStateCases")
    fun `round trip ProfileWithViewerState`(
        format: SerializationTestHelper.Format,
        original: ProfileWithViewerState,
    ) {
        val decoded = SerializationTestHelper.roundTrip(
            format = format,
            value = original,
            serializer = ProfileWithViewerState.serializer(),
        )
        assertEquals(original, decoded)
    }

    companion object {
        @JvmStatic
        private fun profileWithViewerStateCases(): List<Arguments> {
            val cases = listOf(
                ProfileWithViewerState(
                    profile = sampleProfile(),
                    viewerState = null, // no viewer state
                ),
                ProfileWithViewerState(
                    profile = stubProfile(
                        did = ProfileId("did:example:stub2"),
                        handle = ProfileHandle("vieweruser"),
                        displayName = "Viewer User",
                    ),
                    viewerState = ProfileViewerState(
                        following = GenericUri("at://profile/following/1"),
                        followedBy = GenericUri("at://profile/followedby/2"),
                        commonFollowersCount = 60L, // included and correct type
                    ),
                ),
            )
            return SerializationTestHelper.Format.entries.flatMap { format ->
                cases.map { value ->
                    Arguments.of(format, value)
                }
            }
        }
    }
}


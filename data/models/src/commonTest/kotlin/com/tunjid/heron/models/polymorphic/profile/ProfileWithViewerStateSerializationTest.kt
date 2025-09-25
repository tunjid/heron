package com.tunjid.heron.models.polymorphic.profile

import app.cash.burst.Burst
import app.cash.burst.burstValues
import com.tunjid.heron.data.core.models.ProfileViewerState
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.models.stubProfile
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.fakes.sampleProfile
import com.tunjid.heron.helpers.SerializationTestHelper
import kotlin.test.Test
import kotlin.test.assertEquals

@Burst
class ProfileWithViewerStateSerializationTest(
    val format: SerializationTestHelper.Format = burstValues(
        SerializationTestHelper.Format.CBOR,
        SerializationTestHelper.Format.PROTOBUF,
    ),
    val original: ProfileWithViewerState = burstValues(
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
                commonFollowersCount = 60L,
            ),
        ),
    ),
) {
    @Test
    fun roundTrip() {
        val decoded = SerializationTestHelper.roundTrip(
            format = format,
            value = original,
            serializer = ProfileWithViewerState.serializer(),
        )
        assertEquals(original, decoded)
    }
}

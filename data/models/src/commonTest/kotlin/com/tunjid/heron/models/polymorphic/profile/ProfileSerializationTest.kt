package com.tunjid.heron.models.polymorphic.profile

import app.cash.burst.Burst
import app.cash.burst.burstValues
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.stubProfile
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.fakes.sampleProfile
import com.tunjid.heron.helpers.SerializationTestHelper
import kotlin.test.Test
import kotlin.test.assertEquals

@Burst
class ProfileSerializationTest(
    val format: SerializationTestHelper.Format = burstValues(
        SerializationTestHelper.Format.CBOR,
        SerializationTestHelper.Format.PROTOBUF,
    ),
    val original: Profile = burstValues(
        sampleProfile(),
        stubProfile(
            did = ProfileId("did:example:stub"),
            handle = ProfileHandle("stubuser"),
            displayName = "Stub User",
        ),
    ),
) {
    @Test
    fun roundTrip() {
        val decoded = SerializationTestHelper.roundTrip(
            format = format,
            value = original,
            serializer = Profile.serializer(),
        )
        assertEquals(original, decoded)
    }
}

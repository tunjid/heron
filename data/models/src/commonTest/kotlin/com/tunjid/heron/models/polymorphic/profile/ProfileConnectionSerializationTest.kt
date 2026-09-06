package com.tunjid.heron.models.polymorphic.profile

import app.cash.burst.Burst
import app.cash.burst.burstValues
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.types.FollowUri
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.StarterPackId
import com.tunjid.heron.data.core.types.StarterPackUri
import com.tunjid.heron.helpers.SerializationTestHelper
import kotlin.test.Test
import kotlin.test.assertEquals

@Burst
class ProfileConnectionSerializationTest(
    val format: SerializationTestHelper.Format = burstValues(
        SerializationTestHelper.Format.CBOR,
        SerializationTestHelper.Format.PROTOBUF,
    ),
    val original: Profile.Connection = burstValues(
        Profile.Connection.Follow(
            signedInProfileId = ProfileId("did:example:me"),
            profileId = ProfileId("did:example:target1"),
            followedBy = FollowUri("at://did:example:target1/${FollowUri.NAMESPACE}/2222222222222"),
        ),
        Profile.Connection.Unfollow(
            signedInProfileId = ProfileId("did:example:me"),
            profileId = ProfileId("did:example:target2"),
            followedBy = FollowUri("at://did:example:target2/${FollowUri.NAMESPACE}/2222222222223"),
            followUri = FollowUri("at://did:example:me/${FollowUri.NAMESPACE}/2222222222224"),
        ),
        Profile.Connection.FollowStarterPack(
            signedInProfileId = ProfileId("did:example:me"),
            starterPackUri = StarterPackUri(
                "at://did:example:creator/${StarterPackUri.NAMESPACE}/3333333333333",
            ),
            starterPackCid = StarterPackId("bafyreiexamplestarterpackcid"),
            listUri = ListUri("at://did:example:creator/${ListUri.NAMESPACE}/4444444444444"),
        ),
    ),
) {
    @Test
    fun roundTrip() {
        val decoded = SerializationTestHelper.roundTrip(
            format = format,
            value = original,
            serializer = Profile.Connection.serializer(),
        )
        assertEquals(original, decoded)
    }
}

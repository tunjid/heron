package com.tunjid.heron.fakes

import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import kotlinx.datetime.Instant

fun sampleProfile(): Profile {
    return Profile(
        did = ProfileId("did:example:123"),
        handle = ProfileHandle("joel"),
        displayName = "Joel Muraguri",
        description = "Just a test profile",
        avatar = null,
        banner = null,
        followersCount = 10,
        followsCount = 20,
        postsCount = 5,
        joinedViaStarterPack = null,
        indexedAt = Instant.parse("2024-01-01T00:00:00Z"),
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        metadata = Profile.Metadata(
            createdListCount = 0,
            createdFeedGeneratorCount = 0,
            createdStarterPackCount = 0,
        ),
    )
}

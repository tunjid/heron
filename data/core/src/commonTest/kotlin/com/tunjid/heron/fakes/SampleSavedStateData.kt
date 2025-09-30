package com.tunjid.heron.fakes

import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.models.Server
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.repository.SavedState
import kotlinx.datetime.Instant

object SampleSavedStateData {
    fun guestAuth(): SavedState.AuthTokens.Guest =
        SavedState.AuthTokens.Guest(
            server = Server(
                "https://guest.example",
                supportsOauth = false
            )
        )

    fun bearerAuth(): SavedState.AuthTokens.Authenticated.Bearer =
        SavedState.AuthTokens.Authenticated.Bearer(
            authProfileId = ProfileId("bearer-123"),
            auth = "access-token",
            refresh = "refresh-token",
            didDoc = didDoc(),
            authEndpoint = "https://auth.example"
        )

    fun dpopAuth(): SavedState.AuthTokens.Authenticated.DPoP =
        SavedState.AuthTokens.Authenticated.DPoP(
            authProfileId = ProfileId("dpop-123"),
            auth = "dpop-access",
            refresh = "dpop-refresh",
            pdsUrl = "https://pds.dpop",
            clientId = "client-xyz",
            nonce = "nonce-1",
            keyPair = SavedState.AuthTokens.Authenticated.DPoP.DERKeyPair(
                publicKey = byteArrayOf(0x01, 0x02),
                privateKey = byteArrayOf(0x03, 0x04),
            ),
            issuerEndpoint = "https://issuer.dpop"
        )

    fun didDoc(): SavedState.AuthTokens.DidDoc =
        SavedState.AuthTokens.DidDoc(
            verificationMethod = listOf(
                SavedState.AuthTokens.DidDoc.VerificationMethod(
                    id = "key1",
                    type = "Ed25519",
                    controller = "controller",
                    publicKeyMultibase = "zDummyKey"
                )
            ),
            service = listOf(
                SavedState.AuthTokens.DidDoc.Service(
                    id = "svc1",
                    type = "atproto",
                    serviceEndpoint = "https://pds.example"
                )
            )
        )

    fun navigation(): SavedState.Navigation =
        SavedState.Navigation(
            activeNav = 1,
            backStacks = listOf(listOf("home", "settings"))
        )

    fun notifications(): SavedState.Notifications =
        SavedState.Notifications(
            lastRead = Instant.DISTANT_PAST,
            lastRefreshed = Instant.DISTANT_FUTURE
        )

    fun writes(): SavedState.Writes =
        SavedState.Writes(
            pendingWrites = emptyList(),
            failedWrites = emptyList()
        )

    fun profileData(): SavedState.ProfileData =
        SavedState.ProfileData(
            preferences = Preferences(
                timelinePreferences = emptyList(),
                contentLabelPreferences = emptyList(),
                lastViewedHomeTimelineUri = GenericUri("feed://home/discover")
            ),
            notifications = notifications(),
            writes = writes()
        )

    fun savedStateWithGuest(): SavedState =
        object : SavedState() {
            override val auth = guestAuth()
            override val navigation = navigation()
            override val signedInProfileData: ProfileData? = null
        }

    fun savedStateWithBearer(): SavedState =
        object : SavedState() {
            override val auth = bearerAuth()
            override val navigation = navigation()
            override val signedInProfileData = profileData()
        }

    fun savedStateWithDPoP(): SavedState =
        object : SavedState() {
            override val auth = dpopAuth()
            override val navigation = navigation()
            override val signedInProfileData: ProfileData? = null
        }
}

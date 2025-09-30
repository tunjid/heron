package com.tunjid.heron.fakes

import com.tunjid.heron.data.core.models.Message
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Server
import com.tunjid.heron.data.core.types.ConversationId
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.MessageId
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.repository.SavedState
import com.tunjid.heron.data.utilities.writequeue.FailedWrite
import com.tunjid.heron.data.utilities.writequeue.Writable
import kotlinx.datetime.Instant

object SampleSavedStateData {
    fun guestAuth(): SavedState.AuthTokens.Guest =
        SavedState.AuthTokens.Guest(
            server = Server(
                "https://guest.example",
                supportsOauth = false,
            ),
        )

    fun bearerAuth(): SavedState.AuthTokens.Authenticated.Bearer =
        SavedState.AuthTokens.Authenticated.Bearer(
            authProfileId = ProfileId("bearer-123"),
            auth = "access-token",
            refresh = "refresh-token",
            didDoc = didDoc(),
            authEndpoint = "https://auth.example",
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
            issuerEndpoint = "https://issuer.dpop",
        )

    fun didDoc(): SavedState.AuthTokens.DidDoc =
        SavedState.AuthTokens.DidDoc(
            verificationMethod = listOf(
                SavedState.AuthTokens.DidDoc.VerificationMethod(
                    id = "key1",
                    type = "Ed25519",
                    controller = "controller",
                    publicKeyMultibase = "zDummyKey",
                ),
            ),
            service = listOf(
                SavedState.AuthTokens.DidDoc.Service(
                    id = "svc1",
                    type = "atproto",
                    serviceEndpoint = "https://pds.example",
                ),
            ),
        )

    fun navigation(): SavedState.Navigation =
        SavedState.Navigation(
            activeNav = 1,
            backStacks = listOf(listOf("home", "settings")),
        )

    fun notifications(): SavedState.Notifications =
        SavedState.Notifications(
            lastRead = Instant.DISTANT_PAST,
            lastRefreshed = Instant.DISTANT_FUTURE,
        )

    fun writes(): SavedState.Writes =
        SavedState.Writes(
            pendingWrites = listOf(
                Writable.Interaction(
                    interaction = Post.Interaction.Create.Like(
                        postId = PostId("p1"),
                        postUri = PostUri("at://post/123"),
                    ),
                ),
                Writable.Create(
                    request = Post.Create.Request(
                        authorId = ProfileId("did:example:123"),
                        text = "Hello world!",
                        links = emptyList(),
                        metadata = Post.Create.Metadata(),
                    ),
                ),
                Writable.Send(
                    request = Message.Create(
                        conversationId = ConversationId("convo-1"),
                        text = "Hey Bob!",
                        links = emptyList(),
                    ),
                ),
                Writable.Reaction(
                    update = Message.UpdateReaction.Add(
                        value = "like",
                        messageId = MessageId("msg-1"),
                        convoId = ConversationId("convo-1"),
                    ),
                ),
            ),
            failedWrites = listOf(
                FailedWrite(
                    writable = Writable.Connection(
                        connection = Profile.Connection.Follow(
                            signedInProfileId = ProfileId("did:example:123"),
                            profileId = ProfileId("did:example:alice"),
                            followedBy = GenericUri("at://follow/123"),
                        ),
                    ),
                    failedAt = Instant.fromEpochMilliseconds(1727746200000), // fixed timestamp
                    reason = FailedWrite.Reason.IO,
                ),
            ),
        )

    fun profileData(): SavedState.ProfileData =
        SavedState.ProfileData(
            preferences = Preferences(
                timelinePreferences = emptyList(),
                contentLabelPreferences = emptyList(),
                lastViewedHomeTimelineUri = GenericUri("feed://home/discover"),
            ),
            notifications = notifications(),
            writes = writes(),
        )

    fun savedStateWithGuest(): SavedState =
        GuestSavedState(
            auth = guestAuth(),
            navigation = navigation(),
        )

    fun savedStateWithBearer(): SavedState =
        BearerSavedState(
            auth = bearerAuth(),
            navigation = navigation(),
            signedInProfileData = profileData(),
        )

    fun savedStateWithDPoP(): SavedState =
        DPoPSavedState(
            auth = dpopAuth(),
            navigation = navigation(),
        )
}

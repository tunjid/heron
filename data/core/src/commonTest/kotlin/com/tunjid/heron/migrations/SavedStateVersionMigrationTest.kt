package com.tunjid.heron.migrations

import app.cash.burst.Burst
import app.cash.burst.burstValues
import com.tunjid.heron.data.core.models.Server
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.datastore.migrations.SavedStateVersion
import com.tunjid.heron.data.datastore.migrations.SavedStateVersion0
import com.tunjid.heron.data.datastore.migrations.SavedStateVersion1
import com.tunjid.heron.data.datastore.migrations.SavedStateVersion2
import com.tunjid.heron.data.datastore.migrations.SavedStateVersion3
import com.tunjid.heron.data.datastore.migrations.SavedStateVersion4
import com.tunjid.heron.data.datastore.migrations.SavedStateVersion5
import com.tunjid.heron.data.datastore.migrations.VersionedSavedStateOkioSerializer
import com.tunjid.heron.data.datastore.migrations.migrated.ProfileDataV0
import com.tunjid.heron.data.repository.SavedState
import com.tunjid.heron.fakes.sampleNotifications
import com.tunjid.heron.fakes.samplePreferences
import com.tunjid.heron.helper.SavedStateSerializationHelper
import com.tunjid.heron.helper.toBufferedSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalSerializationApi::class)
@Burst
internal class SavedStateVersionMigrationTest(
    val oldVersion: SavedStateVersion = burstValues(
        sampleVersion0UnAuth(),
        sampleVersion0BearerAuth(),
        sampleVersion1BearerAuth(),
        sampleVersion2BearerAuth(),
        sampleVersion2GuestAuth(),
        sampleVersion3GuestAuth(),
        sampleVersion3BearerAuth(),
        sampleVersion4(),
        sampleVersion5(),
    ),
) {

    private val proto = SavedStateSerializationHelper.proto
    private val serializer = VersionedSavedStateOkioSerializer(proto)

    @Test
    fun migrateToLatestVersion_noDataLoss() = runBlocking {
        val encoded = when (oldVersion) {
            is SavedStateVersion0 -> SavedStateSerializationHelper.encode(oldVersion, SavedStateVersion0.serializer())
            is SavedStateVersion1 -> SavedStateSerializationHelper.encode(oldVersion, SavedStateVersion1.serializer())
            is SavedStateVersion2 -> SavedStateSerializationHelper.encode(oldVersion, SavedStateVersion2.serializer())
            is SavedStateVersion3 -> SavedStateSerializationHelper.encode(oldVersion, SavedStateVersion3.serializer())
            is SavedStateVersion4 -> SavedStateSerializationHelper.encode(oldVersion, SavedStateVersion4.serializer())
            is SavedStateVersion5 -> SavedStateSerializationHelper.encode(oldVersion, SavedStateVersion5.serializer())
        }

        val migrated = serializer.readFrom(encoded.toBufferedSource())
        val expected = oldVersion.toVersionedSavedState(currentVersion = 5)

        assertEquals(expected.auth, migrated.auth)
        assertEquals(expected.navigation, migrated.navigation)
        assertEquals(expected.profileData, migrated.profileData)
        assertEquals(5, migrated.version)
    }
}

private fun sampleVersion0UnAuth() = SavedStateVersion0(
    auth = null,
    navigation = SavedState.Navigation(activeNav = 1),
    profileData = mapOf(
        "p1" to ProfileDataV0(
            preferences = samplePreferences().asV0(),
            notifications = sampleNotifications(),
        ),
    ),
)

private fun sampleVersion0BearerAuth() = SavedStateVersion0(
    auth = SavedStateVersion0.AuthTokensV0(
        authProfileId = ProfileId("id2"),
        auth = "auth",
        refresh = "refresh",
    ),
    navigation = SavedState.Navigation(activeNav = 1),
    profileData = mapOf(
        "p1" to ProfileDataV0(
            preferences = samplePreferences().asV0(),
            notifications = sampleNotifications(),
        ),
    ),
)

private fun sampleVersion1BearerAuth() = SavedStateVersion1(
    version = 1,
    auth = SavedStateVersion1.AuthTokensV1(
        authProfileId = ProfileId("id1"),
        auth = "token",
        refresh = "refresh",
    ),
    navigation = SavedState.Navigation(activeNav = 2),
    profileData = mapOf(
        ProfileId("p1") to ProfileDataV0(
            preferences = samplePreferences().asV0(),
            notifications = sampleNotifications(),
        ),
    ),
)

private fun sampleVersion2BearerAuth() = SavedStateVersion2(
    version = 2,
    auth = SavedStateVersion2.AuthTokensV2.Authenticated.Bearer(
        authProfileId = ProfileId("id2"),
        auth = "token2",
        refresh = "refresh2",
    ),
    navigation = SavedState.Navigation(activeNav = 3),
    profileData = mapOf(
        ProfileId("p2") to ProfileDataV0(
            preferences = samplePreferences().asV0(),
            notifications = sampleNotifications(),
        ),
    ),
)

private fun sampleVersion2GuestAuth() = SavedStateVersion2(
    version = 2,
    auth = SavedStateVersion2.AuthTokensV2.Guest,
    navigation = SavedState.Navigation(activeNav = 3),
    profileData = mapOf(
        ProfileId("p2") to ProfileDataV0(
            preferences = samplePreferences().asV0(),
            notifications = sampleNotifications(),
        ),
    ),
)

private fun sampleVersion3GuestAuth() = SavedStateVersion3(
    version = 3,
    auth = SavedState.AuthTokens.Guest(Server.BlueSky),
    navigation = SavedState.Navigation(activeNav = 4),
    profileData = mapOf(
        ProfileId("p3") to ProfileDataV0(
            preferences = samplePreferences().asV0(),
            notifications = sampleNotifications(),
        ),
    ),
)

private fun sampleVersion3BearerAuth() = SavedStateVersion3(
    version = 3,
    auth = SavedState.AuthTokens.Authenticated.Bearer(
        auth = "auth",
        refresh = "refresh",
        authEndpoint = "https://example.com",
        authProfileId = ProfileId("p3"),
    ),
    navigation = SavedState.Navigation(activeNav = 4),
    profileData = mapOf(
        ProfileId("p3") to ProfileDataV0(
            preferences = samplePreferences().asV0(),
            notifications = sampleNotifications(),
        ),
    ),
)

private fun sampleVersion4() = SavedStateVersion4(
    version = 4,
    navigation = SavedState.Navigation(activeNav = 4),
    profileData = mapOf(
        ProfileId("p4") to SavedStateVersion4.ProfileDataV4(
            preferences = samplePreferences().asV0(),
            notifications = sampleNotifications(),
        ),
    ),
    activeProfileId = ProfileId("p4"),
)

private fun sampleVersion5() = SavedStateVersion5(
    version = 5,
    navigation = SavedState.Navigation(activeNav = 5),
    profileData = mapOf(
        ProfileId("p5") to SavedState.ProfileData(
            preferences = samplePreferences(),
            notifications = sampleNotifications(),
        ),
    ),
    activeProfileId = ProfileId("p5"),
)

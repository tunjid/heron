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
import com.tunjid.heron.data.datastore.migrations.VersionedSavedStateOkioSerializer
import com.tunjid.heron.data.repository.SavedState
import com.tunjid.heron.fakes.sampleNotifications
import com.tunjid.heron.fakes.samplePreferences
import com.tunjid.heron.fakes.sampleProfileData
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
        sampleVersion0(),
        sampleVersion1(),
        sampleVersion2(),
        sampleVersion3(),
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
        }

        val migrated = serializer.readFrom(encoded.toBufferedSource())
        val expected = oldVersion.toVersionedSavedState(currentVersion = 3)

        assertEquals(expected.auth, migrated.auth)
        assertEquals(expected.navigation, migrated.navigation)
        assertEquals(expected.profileData, migrated.profileData)
        assertEquals(3, migrated.version)
    }
}

private fun sampleVersion0() = SavedStateVersion0(
    auth = null,
    navigation = SavedState.Navigation(activeNav = 1),
    profileData = mapOf(
        "p1" to SavedState.ProfileData(
            preferences = samplePreferences(),
            notifications = sampleNotifications(),
        ),
    ),
)

private fun sampleVersion1() = SavedStateVersion1(
    version = 1,
    auth = SavedStateVersion1.AuthTokens(
        authProfileId = ProfileId("id1"),
        auth = "token",
        refresh = "refresh",
    ),
    navigation = SavedState.Navigation(activeNav = 2),
    profileData = mapOf(ProfileId("p1") to sampleProfileData()),
)

private fun sampleVersion2() = SavedStateVersion2(
    version = 2,
    auth = SavedStateVersion2.AuthTokensV2.Authenticated.Bearer(
        authProfileId = ProfileId("id2"),
        auth = "token2",
        refresh = "refresh2",
    ),
    navigation = SavedState.Navigation(activeNav = 3),
    profileData = mapOf(ProfileId("p2") to sampleProfileData()),
)

private fun sampleVersion3() = SavedStateVersion3(
    version = 3,
    auth = SavedState.AuthTokens.Guest(Server.BlueSky),
    navigation = SavedState.Navigation(activeNav = 4),
    profileData = mapOf(ProfileId("p3") to sampleProfileData()),
)

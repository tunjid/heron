/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tunjid.heron.migrations

import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.datastore.migrations.SavedStateVersion4
import com.tunjid.heron.data.datastore.migrations.VersionedSavedStateOkioSerializer
import com.tunjid.heron.data.repository.SavedState
import com.tunjid.heron.fakes.sampleNotifications
import com.tunjid.heron.fakes.samplePreferences
import com.tunjid.heron.helper.SavedStateSerializationHelper
import com.tunjid.heron.helper.toBufferedSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import okio.Buffer

@OptIn(ExperimentalSerializationApi::class)
class SavedStateVersion4To5MigrationTest {

    private val proto = SavedStateSerializationHelper.proto
    private val serializer = VersionedSavedStateOkioSerializer(proto)

    @Test
    fun migrateV4ToV5_withAuth() = runBlocking {
        val profileId = ProfileId("p1")
        val v4 =
            SavedStateVersion4(
                version = 4,
                navigation = SavedState.Navigation(activeNav = 1),
                profileData =
                    mapOf(
                        profileId to
                            SavedStateVersion4.ProfileDataV4(
                                preferences =
                                    samplePreferences()
                                        .asV0()
                                        .copy(
                                            useDynamicTheming = true,
                                            refreshHomeTimelineOnLaunch = true,
                                        ),
                                notifications = sampleNotifications(),
                                auth =
                                    SavedState.AuthTokens.Authenticated.Bearer(
                                        authProfileId = profileId,
                                        auth = "auth",
                                        refresh = "refresh",
                                        authEndpoint = "https://example.com",
                                    ),
                            )
                    ),
                activeProfileId = profileId,
            )

        val v4Bytes = SavedStateSerializationHelper.encode(v4, SavedStateVersion4.serializer())
        val migrated = serializer.readFrom(v4Bytes.toBufferedSource())

        assertEquals(5, migrated.version)
        assertEquals(profileId, migrated.activeProfileId)

        val migratedProfileData = migrated.profileData[profileId]
        assertNotNull(migratedProfileData)

        // Verify local preferences moved correctly
        assertEquals(true, migratedProfileData.preferences.local.useDynamicTheming)
        assertEquals(true, migratedProfileData.preferences.local.refreshHomeTimelineOnLaunch)

        // Verify auth persisted
        assertNotNull(migratedProfileData.auth)

        // Save as V5
        val v5Bytes = Buffer()
        serializer.writeTo(migrated, v5Bytes)

        // Read back
        val reRead = serializer.readFrom(v5Bytes)
        assertEquals(migrated, reRead)
        assertEquals(5, reRead.version)
    }

    @Test
    fun migrateV4ToV5_withoutAuth() = runBlocking {
        val profileId = ProfileId("p1")
        val v4 =
            SavedStateVersion4(
                version = 4,
                navigation = SavedState.Navigation(activeNav = 1),
                profileData =
                    mapOf(
                        profileId to
                            SavedStateVersion4.ProfileDataV4(
                                preferences =
                                    samplePreferences().asV0().copy(useCompactNavigation = true),
                                notifications = sampleNotifications(),
                                auth = null,
                            )
                    ),
                activeProfileId = profileId,
            )

        val v4Bytes = SavedStateSerializationHelper.encode(v4, SavedStateVersion4.serializer())
        val migrated = serializer.readFrom(v4Bytes.toBufferedSource())

        assertEquals(5, migrated.version)

        val migratedProfileData = migrated.profileData[profileId]
        assertNotNull(migratedProfileData)

        // Verify local preferences moved correctly
        assertEquals(true, migratedProfileData.preferences.local.useCompactNavigation)

        // Verify auth is null
        assertEquals(null, migratedProfileData.auth)
    }
}

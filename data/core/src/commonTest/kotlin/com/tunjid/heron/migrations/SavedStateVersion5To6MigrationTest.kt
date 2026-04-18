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

import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.datastore.migrations.SavedStateVersion5
import com.tunjid.heron.data.datastore.migrations.VersionedSavedStateOkioSerializer
import com.tunjid.heron.data.repository.SavedState
import com.tunjid.heron.data.repository.SavedStateEncryption
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
class SavedStateVersion5To6MigrationTest {

    private val proto = SavedStateSerializationHelper.proto
    private val serializer = VersionedSavedStateOkioSerializer(
        protoBuf = proto,
        encryption = SavedStateEncryption.None,
    )

    @Test
    fun migrateV5ToV6_preservesThemeOrdinal() {
        runBlocking {
            val profileId = ProfileId("p1")
            val v5 = SavedStateVersion5(
                version = 5,
                navigation = SavedState.Navigation(activeNav = 1),
                profileData = mapOf(
                    profileId to SavedState.ProfileData(
                        preferences = samplePreferences().copy(
                            local = Preferences.Local(
                                // Theme.Default
                                currentThemeOrdinal = 0,
                                refreshHomeTimelineOnLaunch = true,
                            ),
                        ),
                        notifications = sampleNotifications(),
                        auth = SavedState.AuthTokens.Authenticated.Bearer(
                            authProfileId = profileId,
                            auth = "auth",
                            refresh = "refresh",
                            authEndpoint = "https://example.com",
                        ),
                    ),
                ),
                activeProfileId = profileId,
            )

            val migrated = v5.toVersionedSavedState(currentVersion = 6)

            assertEquals(6, migrated.version)
            assertEquals(profileId, migrated.activeProfileId)

            val migratedProfileData = migrated.profileData[profileId]
            assertNotNull(migratedProfileData)

            // Theme ordinal carried over unchanged.
            assertEquals(0, migratedProfileData.preferences.local.currentThemeOrdinal)
            assertEquals(true, migratedProfileData.preferences.local.refreshHomeTimelineOnLaunch)

            // Auth preserved.
            assertNotNull(migratedProfileData.auth)
        }
    }

    @Test
    fun migrateV5BooleanThemeBytes_decodesAsOrdinal() {
        runBlocking {
            // The V5 schema used to store theme as a Boolean `useDynamicTheming`
            // at @ProtoNumber(3). The V6 schema replaces that field with an Int
            // `currentThemeOrdinal` at the same @ProtoNumber(3). Protobuf encodes
            // both Boolean and Int32 as varint, so `true` -> 1 and `false` -> 0 on
            // the wire. This test asserts that a V5-encoded `useDynamicTheming = true`
            // reads back as `currentThemeOrdinal == 1` (Dynamic) under the V6 schema.
            val profileId = ProfileId("p1")
            val v5WithDynamicTheming = SavedStateVersion5(
                version = 5,
                navigation = SavedState.Navigation(activeNav = 1),
                profileData = mapOf(
                    profileId to SavedState.ProfileData(
                        preferences = samplePreferences().copy(
                            local = Preferences.Local(
                                currentThemeOrdinal = 1,
                            ),
                        ),
                        notifications = sampleNotifications(),
                        auth = null,
                    ),
                ),
                activeProfileId = profileId,
            )

            val v5Bytes = SavedStateSerializationHelper.encode(
                v5WithDynamicTheming,
                SavedStateVersion5.serializer(),
            )
            val migrated = serializer.readFrom(v5Bytes.toBufferedSource())

            assertEquals(6, migrated.version)
            assertEquals(
                1,
                migrated.profileData[profileId]?.preferences?.local?.currentThemeOrdinal,
            )
        }
    }

    @Test
    fun v6Bytes_roundTrip() {
        runBlocking {
            val profileId = ProfileId("p1")
            val v5 = SavedStateVersion5(
                version = 5,
                navigation = SavedState.Navigation(activeNav = 1),
                profileData = mapOf(
                    profileId to SavedState.ProfileData(
                        preferences = samplePreferences().copy(
                            local = Preferences.Local(
                                currentThemeOrdinal = 5,
                                useCompactNavigation = true,
                            ),
                        ),
                        notifications = sampleNotifications(),
                        auth = null,
                    ),
                ),
                activeProfileId = profileId,
            )
            val v5Bytes = SavedStateSerializationHelper.encode(v5, SavedStateVersion5.serializer())
            val migrated = serializer.readFrom(v5Bytes.toBufferedSource())

            // Round-trip V6 bytes through the serializer and assert equivalence.
            val outBytes = Buffer()
            serializer.writeTo(migrated, outBytes)

            val reRead = serializer.readFrom(outBytes)
            assertEquals(migrated, reRead)
            assertEquals(6, reRead.version)
        }
    }
}

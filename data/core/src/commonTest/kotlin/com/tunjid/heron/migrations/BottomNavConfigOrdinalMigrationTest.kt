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
import com.tunjid.heron.helper.SavedStateSerializationHelper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * `Preferences.Local.bottomNavConfigOrdinal` replaced the Boolean
 * `autoHideBottomNavigation` in place at @ProtoNumber(5). These tests pin the
 * wire compatibility between the two.
 */
class BottomNavConfigOrdinalMigrationTest {

    private val proto = SavedStateSerializationHelper.proto

    @Test
    fun legacyAutoHideFalse_decodesAsOrdinalZero() {
        val legacyBytes = proto.encodeToByteArray(
            serializer = LegacyLocal.serializer(),
            value = LegacyLocal(
                autoHideBottomNavigation = false,
            ),
        )
        val migrated = proto.decodeFromByteArray(
            deserializer = Preferences.Local.serializer(),
            bytes = legacyBytes,
        )

        assertEquals(
            expected = 0,
            actual = migrated.bottomNavConfigOrdinal,
        )
    }

    @Test
    fun legacyAutoHideTrue_decodesAsOrdinalOne() {
        val legacyBytes = proto.encodeToByteArray(
            serializer = LegacyLocal.serializer(),
            value = LegacyLocal(
                autoHideBottomNavigation = true,
            ),
        )
        val migrated = proto.decodeFromByteArray(
            deserializer = Preferences.Local.serializer(),
            bytes = legacyBytes,
        )

        assertEquals(
            expected = 1,
            actual = migrated.bottomNavConfigOrdinal,
        )
    }

    @Test
    fun newOrdinals_roundTrip() {
        (0..2).forEach { ordinal ->
            val local = Preferences.Local(
                bottomNavConfigOrdinal = ordinal,
            )
            val decoded = proto.decodeFromByteArray(
                deserializer = Preferences.Local.serializer(),
                bytes = proto.encodeToByteArray(
                    serializer = Preferences.Local.serializer(),
                    value = local,
                ),
            )

            assertEquals(
                expected = local,
                actual = decoded,
            )
        }
    }

    @Serializable
    private data class LegacyLocal(
        @ProtoNumber(5)
        val autoHideBottomNavigation: Boolean = true,
    )
}

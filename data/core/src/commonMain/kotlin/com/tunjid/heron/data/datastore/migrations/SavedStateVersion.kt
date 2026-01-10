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

package com.tunjid.heron.data.datastore.migrations

import androidx.datastore.core.okio.OkioSerializer
import com.tunjid.heron.data.core.models.Constants
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.repository.SavedState
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import okio.BufferedSink
import okio.BufferedSource

private const val CurrentVersion = 5

/**
 * Defines a method of versioning the proto based saved state.
 *
 * Migrations are done in place with the implementations of this sealed interface, where each
 * implementation represents a snapshot of what the [VersionedSavedState] class looked like
 * at the time.
 *
 * The snapshots are then converted to the latest version iteration of [VersionedSavedState]
 * and saved lazily when data is eventually written.
 */
internal sealed interface SavedStateVersion {

    fun toVersionedSavedState(
        currentVersion: Int,
    ): VersionedSavedState
}

/**
 * An implementation of [SavedState] that tracks its version.
 */
@Serializable
internal data class VersionedSavedState(
    @ProtoNumber(1)
    val version: Int,
    // @ProtoNumber(2) is deliberately not used, its deprecated.
    // It used to be auth: SavedState.AuthTokens?
    @ProtoNumber(3)
    override val navigation: Navigation,
    @ProtoNumber(4)
    val profileData: Map<ProfileId, ProfileData>,
    @ProtoNumber(5)
    val activeProfileId: ProfileId?,
) : SavedState() {

    override val signedInProfileData: ProfileData?
        get() = when (val profileId = activeProfileId) {
            null,
            Constants.unknownAuthorId,
            Constants.pendingProfileId,
            Constants.guestProfileId,
            -> null
            else -> profileData[profileId]
        }

    override val auth: AuthTokens?
        get() = activeProfileId
            ?.let(profileData::get)
            ?.auth

    companion object {
        internal val Initial: VersionedSavedState = VersionedSavedState(
            version = CurrentVersion,
            activeProfileId = null,
            navigation = Navigation(activeNav = -1),
            profileData = emptyMap(),
        )

        internal val Empty: VersionedSavedState = VersionedSavedState(
            version = CurrentVersion,
            activeProfileId = null,
            navigation = Navigation(activeNav = 0),
            profileData = emptyMap(),
        )
    }
}

internal class VersionedSavedStateOkioSerializer(
    private val protoBuf: ProtoBuf,
) : OkioSerializer<VersionedSavedState> {
    override val defaultValue: VersionedSavedState = VersionedSavedState.Empty

    override suspend fun readFrom(
        source: BufferedSource,
    ): VersionedSavedState = with(protoBuf) {
        val data = source.readByteArray()

        return if (data.isEmpty()) defaultValue
        else try {
            when (data.savedStateVersion()) {
                SavedStateVersion0.SnapshotVersion -> decodeFromByteArray<SavedStateVersion0>(data)
                SavedStateVersion1.SnapshotVersion -> decodeFromByteArray<SavedStateVersion1>(data)
                SavedStateVersion2.SnapshotVersion -> decodeFromByteArray<SavedStateVersion2>(data)
                SavedStateVersion3.SnapshotVersion -> decodeFromByteArray<SavedStateVersion3>(data)
                SavedStateVersion4.SnapshotVersion -> decodeFromByteArray<SavedStateVersion4>(data)
                SavedStateVersion5.SnapshotVersion -> decodeFromByteArray<SavedStateVersion5>(data)
                else -> throw IllegalArgumentException("Unknown saved state version")
            }.toVersionedSavedState(CurrentVersion)
        } catch (e: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(
        t: VersionedSavedState,
        sink: BufferedSink,
    ) {
        sink.write(protoBuf.encodeToByteArray(value = t))
    }
}

private fun ByteArray.savedStateVersion(): Int {
    val tag = this[0].toInt() and 0xFF
    val wireType = tag and 0x07

    return when (wireType) {
        // A varint means VersionedSavedState has started encoding its version
        0 -> encodedSavedStateVersion()
        else -> Int.MIN_VALUE
    }
}

/**
 * Read the encoded saved state version from the first entry in the saved state protobuf.
 *
 */
private fun ByteArray.encodedSavedStateVersion(): Int {
    var result = 0
    var shift = 0
    var currentOffset = 1

    // A varint can be at most 5 bytes for a 32-bit integer
    for (i in 0..4) {
        if (currentOffset >= size) throw IndexOutOfBoundsException(
            "Malformed saved state: Unexpected end of stream",
        )

        val byte = this[currentOffset++].toInt() and 0xFF

        // Get the lower 7 bits and shift them into position
        val payload = (byte and 0x7F)
        result = result or (payload shl shift)

        // If the MSB is 0, this is the last byte
        if ((byte and 0x80) == 0) return result

        // Increment shift for the next 7 bits
        shift += 7
    }

    // If we loop 5 times and still haven't finished, the data is malformed
    throw IllegalArgumentException("Malformed saved state version")
}

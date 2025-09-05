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

import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.repository.SavedState
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class SavedStateVersion1(
    @Suppress("unused")
    @ProtoNumber(1)
    private val version: Int,
    @ProtoNumber(2)
    private val auth: SavedState.AuthTokens?,
    @ProtoNumber(3)
    private val navigation: SavedState.Navigation,
    @ProtoNumber(4)
    private val profileData: Map<ProfileId, SavedState.ProfileData>,
) : SavedStateVersion {

    override fun toVersionedSavedState(
        currentVersion: Int,
    ): VersionedSavedState =
        VersionedSavedState(
            version = currentVersion,
            auth = auth,
            navigation = navigation,
            profileData = profileData,
        )

    companion object {
        const val SnapshotVersion = 1
    }
}

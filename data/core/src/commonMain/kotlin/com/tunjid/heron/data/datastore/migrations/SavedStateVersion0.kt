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

import com.tunjid.heron.data.core.models.Constants
import com.tunjid.heron.data.core.models.Server
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.datastore.migrations.migrated.ProfileDataV0
import com.tunjid.heron.data.repository.SavedState
import com.tunjid.heron.data.repository.SavedState.AuthTokens.DidDoc
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class SavedStateVersion0(
    @ProtoNumber(1)
    private val auth: AuthTokensV0?,
    @ProtoNumber(2)
    private val navigation: SavedState.Navigation,
    @ProtoNumber(3)
    private val profileData: Map<String, ProfileDataV0>,
) : SavedStateVersion {

    override fun toVersionedSavedState(
        currentVersion: Int,
    ): VersionedSavedState =
        VersionedSavedState(
            version = currentVersion,
            navigation = navigation,
            profileData = profileData.entries.associate { (profileId, profileData) ->
                ProfileId(profileId) to when (auth?.authProfileId?.id) {
                    profileId -> profileData.asProfileData(
                        auth = auth.asBearerToken(),
                    )
                    else -> profileData.asProfileData(
                        auth = null,
                    )
                }
            },
            activeProfileId = when (val authProfileId = auth?.authProfileId) {
                null -> null
                Constants.unknownAuthorId -> Constants.guestProfileId
                else -> authProfileId
            },
        )

    @Serializable
    data class AuthTokensV0(
        val authProfileId: ProfileId,
        val auth: String,
        val refresh: String,
        val didDoc: DidDoc = DidDoc(),
    )

    companion object {
        const val SnapshotVersion = Int.MIN_VALUE

        private fun AuthTokensV0.asBearerToken() =
            SavedState.AuthTokens.Authenticated.Bearer(
                authProfileId = authProfileId,
                auth = auth,
                refresh = refresh,
                didDoc = didDoc,
                authEndpoint = Server.BlueSky.endpoint,
            )
    }
}

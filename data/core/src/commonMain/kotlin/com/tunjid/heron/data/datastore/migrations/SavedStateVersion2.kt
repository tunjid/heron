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
import com.tunjid.heron.data.local.models.Server
import com.tunjid.heron.data.repository.SavedState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal class SavedStateVersion2(
    @Suppress("unused")
    @ProtoNumber(1)
    private val version: Int,
    @ProtoNumber(2)
    private val auth: AuthTokensV2?,
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
            auth = when (auth) {
                is AuthTokensV2.Authenticated.Bearer -> SavedState.AuthTokens.Authenticated.Bearer(
                    authProfileId = auth.authProfileId,
                    auth = auth.auth,
                    refresh = auth.refresh,
                    didDoc = auth.didDoc,
                    authEndpoint = Server.BlueSky.endpoint,
                )
                is AuthTokensV2.Authenticated.DPoP -> SavedState.AuthTokens.Authenticated.DPoP(
                    authProfileId = auth.authProfileId,
                    auth = auth.auth,
                    refresh = auth.refresh,
                    pdsUrl = auth.pdsUrl,
                    clientId = auth.clientId,
                    nonce = auth.nonce,
                    keyPair = auth.keyPair,
                    issuerEndpoint = Server.BlueSky.endpoint,
                )
                AuthTokensV2.Guest -> SavedState.AuthTokens.Guest(
                    server = Server.BlueSky,
                )
                null -> null
            },
            navigation = navigation,
            profileData = profileData,
        )

    @Serializable
    @SerialName("com.tunjid.heron.data.repository.SavedState.AuthTokens")
    sealed class AuthTokensV2 {

        @Serializable
        @SerialName("com.tunjid.heron.data.repository.SavedState.AuthTokens.Guest")
        data object Guest : AuthTokensV2()

        @Serializable
        @SerialName("com.tunjid.heron.data.repository.SavedState.AuthTokens.Authenticated")
        sealed class Authenticated : AuthTokensV2() {

            @Serializable
            @SerialName("com.tunjid.heron.data.repository.SavedState.AuthTokens.Authenticated.Bearer")
            data class Bearer(
                val authProfileId: ProfileId,
                val auth: String,
                val refresh: String,
                val didDoc: SavedState.AuthTokens.DidDoc = SavedState.AuthTokens.DidDoc(),
            ) : Authenticated()

            @Serializable
            @SerialName("com.tunjid.heron.data.repository.SavedState.AuthTokens.Authenticated.DPoP")
            data class DPoP(
                val authProfileId: ProfileId,
                val auth: String,
                val refresh: String,
                val pdsUrl: String,
                val clientId: String,
                val nonce: String,
                val keyPair: SavedState.AuthTokens.Authenticated.DPoP.DERKeyPair,
            ) : Authenticated()
        }
    }

    companion object {
        const val SnapshotVersion = 2
    }
}

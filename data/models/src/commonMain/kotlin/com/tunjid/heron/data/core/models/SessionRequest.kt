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

package com.tunjid.heron.data.core.models

import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileHandle
import kotlinx.serialization.Serializable

@Serializable
sealed class SessionRequest {

    abstract val server: Server

    @Serializable
    data class Credentials(
        val handle: ProfileHandle,
        val password: String,
        override val server: Server,
    ) : SessionRequest()

    @Serializable
    data class Oauth(
        val callbackUri: GenericUri,
        override val server: Server,
    ) : SessionRequest()

    @Serializable
    data object Guest : SessionRequest() {
        override val server: Server = Server.BlueSky
    }
}

@Serializable
data class Server(
    val endpoint: String,
    val supportsOauth: Boolean,
) {

    companion object {
        val BlueSky = Server(
            endpoint = "https://bsky.social",
            supportsOauth = true,
        )

        val BlackSky = Server(
            endpoint = "https://blacksky.app",
            supportsOauth = false,
        )

        val KnownServers = setOf(
            BlueSky,
            BlackSky,
        )
    }
}

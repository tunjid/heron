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

package com.tunjid.heron.data.network

import com.tunjid.heron.data.repository.SavedState
import com.tunjid.heron.data.utilities.runCatchingUnlessCancelled
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Url
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import sh.christian.ozone.api.Did

internal interface PdsResolver {
    suspend fun resolve(did: Did): Url?
}

internal class PlcDirectoryPdsResolver @Inject constructor(
    private val httpClient: HttpClient,
) : PdsResolver {

    private val cache = mutableMapOf<Did, Url>()
    private val cacheMutex = Mutex()

    override suspend fun resolve(did: Did): Url? {
        cacheMutex.withLock { cache[did] }?.let { return it }

        val resolved = runCatchingUnlessCancelled {
            val responseText = httpClient.get("$PlcDirectoryUrl/${did.did}")
                .bodyAsText()
            BlueskyJson.decodeFromString<SavedState.AuthTokens.DidDoc>(responseText)
                .service
                .firstOrNull()
                ?.serviceEndpoint
                ?.let(::Url)
        }.getOrNull()

        if (resolved != null) {
            cacheMutex.withLock { cache[did] = resolved }
        }

        return resolved
    }
}

private const val PlcDirectoryUrl = "https://plc.directory"

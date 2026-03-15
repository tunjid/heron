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

import com.atproto.identity.ResolveHandleResponse
import com.tunjid.heron.data.core.models.Server
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.di.AppMainScope
import com.tunjid.heron.data.repository.SavedState
import com.tunjid.heron.data.utilities.runCatchingUnlessCancelled
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Url
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Handle

internal interface PdsResolver {
    suspend fun resolve(did: Did): Url?
    suspend fun resolveServer(handle: Handle): Server?
}

internal class PlcDirectoryPdsResolver @Inject constructor(
    private val httpClient: HttpClient,
    @param:AppMainScope
    private val scope: kotlinx.coroutines.CoroutineScope,
) : PdsResolver {

    private val cache = LinkedHashMap<Did, kotlinx.coroutines.Deferred<Url?>>()
    private val cacheMutex = Mutex()

    override suspend fun resolve(did: Did): Url? {
        val deferred = cacheMutex.withLock {
            val existing = cache.remove(did)
            val deferred = existing ?: scope.async {
                runCatchingUnlessCancelled {
                    val responseText = httpClient.get("$PlcDirectoryUrl/${did.did}")
                        .bodyAsText()
                    BlueskyJson.decodeFromString<SavedState.AuthTokens.DidDoc>(responseText)
                        .service
                        .firstOrNull()
                        ?.serviceEndpoint
                        ?.let(::Url)
                }.getOrNull()
            }
            // Place at the end to mark as most recently used
            cache[did] = deferred

            if (existing == null && cache.size > MaxCacheSize) {
                cache.remove(cache.keys.first())
            }
            deferred
        }

        return deferred.await()
    }

    override suspend fun resolveServer(
        handle: Handle,
    ): Server? = runCatchingUnlessCancelled {
        val handleResponse: ResolveHandleResponse = httpClient.get(
            urlString = "$PublicApiUrl/xrpc/com.atproto.identity.resolveHandle?handle=${handle.handle}",
        )
            .takeIf { it.status.isSuccess() }
            ?.bodyAsText()
            ?.let(BlueskyJson::decodeFromString)
            ?: return@runCatchingUnlessCancelled null

        val didDoc: SavedState.AuthTokens.DidDoc = httpClient.get(
            urlString = "$PlcDirectoryUrl/${handleResponse.did.did}",
        )
            .takeIf { it.status.isSuccess() }
            ?.bodyAsText()
            ?.let(BlueskyJson::decodeFromString)
            ?: return@runCatchingUnlessCancelled null

        val endpoint = didDoc.service
            .firstOrNull()
            ?.serviceEndpoint ?: return null

        Server.KnownServers
            .firstOrNull { it.endpoint == endpoint }
            ?: endpoint.takeIf { it.startsWith(Uri.Host.Https.prefix) }
                ?.let {
                    Server(
                        endpoint = it,
                        supportsOauth = true,
                    )
                }
    }.getOrNull()
}

private const val PublicApiUrl = "https://public.api.bsky.app"
private const val PlcDirectoryUrl = "https://plc.directory"
private const val MaxCacheSize = 20

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
import io.ktor.http.decodeURLPart
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

@Inject
internal class PlcDirectoryPdsResolver(
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
                    did.resolvePdsEndpoint()?.let(::Url)
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

        val endpoint = handleResponse.did.resolvePdsEndpoint()
            ?: return@runCatchingUnlessCancelled null

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

    private suspend fun Did.resolvePdsEndpoint(): String? {
        val didDocumentUrl = didDocumentUrl() ?: return null
        val responseText = httpClient.get(didDocumentUrl)
            .takeIf { it.status.isSuccess() }
            ?.bodyAsText()
            ?: return null
        return BlueskyJson.decodeFromString<SavedState.AuthTokens.DidDoc>(responseText)
            .atprotoPdsEndpoint(did = this)
    }
}

/**
 * The URL of the [Did]'s DID document, resolved according to its DID method:
 *  - `did:plc:*` is fetched from the PLC directory.
 *  - `did:web:*` is fetched from the host's DID document per the did:web method spec.
 *
 * Returns `null` for unsupported DID methods.
 */
internal fun Did.didDocumentUrl(): String? = when {
    did.startsWith(DidPlcPrefix) -> "$PlcDirectoryUrl/$did"
    did.startsWith(DidWebPrefix) -> webDidDocumentUrl(
        methodSpecificId = did.removePrefix(DidWebPrefix),
    )
    else -> null
}

private fun webDidDocumentUrl(methodSpecificId: String): String {
    // The did:web spec percent-encodes a port's colon as %3A but leaves path-delimiter colons
    // plain, so splitting on ':' cleanly separates the host from any path segments; the host's
    // own port is then decoded. (atproto only permits a localhost port and no path segments, but
    // the general form is kept for correctness.)
    val segments = methodSpecificId.split(':')
    val host = segments.first().replace(
        oldValue = EncodedColon,
        newValue = ":",
        ignoreCase = true,
    )
    val pathSegments = segments.drop(1)
    return buildString {
        append(Uri.Host.Https.prefix)
        append(host)
        if (pathSegments.isEmpty()) {
            append(WellKnownDidDocumentPath)
        } else {
            pathSegments.forEach { segment ->
                append('/')
                append(segment.decodeURLPart())
            }
            append(DidDocumentPath)
        }
    }
}

/**
 * The account's PDS endpoint from its DID document: the `serviceEndpoint` of the service entry with
 * type `AtprotoPersonalDataServer` and id `#atproto_pds` (either the bare fragment or the
 * DID-qualified form). Returns `null` if no such service is declared.
 */
internal fun SavedState.AuthTokens.DidDoc.atprotoPdsEndpoint(did: Did): String? =
    service.firstOrNull { service ->
        service.type == AtprotoPdsServiceType &&
            (service.id == AtprotoPdsServiceId || service.id == "${did.did}$AtprotoPdsServiceId")
    }?.serviceEndpoint

private const val PublicApiUrl = "https://public.api.bsky.app"
private const val PlcDirectoryUrl = "https://plc.directory"
private const val DidPlcPrefix = "did:plc:"
private const val DidWebPrefix = "did:web:"
private const val EncodedColon = "%3A"
private const val WellKnownDidDocumentPath = "/.well-known/did.json"
private const val DidDocumentPath = "/did.json"
private const val AtprotoPdsServiceId = "#atproto_pds"
private const val AtprotoPdsServiceType = "AtprotoPersonalDataServer"
private const val MaxCacheSize = 20

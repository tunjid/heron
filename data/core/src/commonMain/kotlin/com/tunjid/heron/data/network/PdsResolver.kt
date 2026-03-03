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

import com.tunjid.heron.data.di.AppMainScope
import com.tunjid.heron.data.repository.SavedState
import com.tunjid.heron.data.utilities.runCatchingUnlessCancelled
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Url
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import sh.christian.ozone.api.Did

internal interface PdsResolver {
    suspend fun resolve(did: Did): Url?
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
            var existingDeferred = cache[did]

            if (existingDeferred != null) {
                // LRU Access: Remove and re-insert to move it to the "most recently used" position (end of the map)
                cache.remove(did)
                cache[did] = existingDeferred
            } else {
                // Create new deferred task
                existingDeferred = scope.async {
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

                // Insert into cache
                cache[did] = existingDeferred

                // LRU Eviction: Remove the eldest entry (first item) if we exceed our max size
                if (cache.size > MaxCacheSize) {
                    val eldestKey = cache.keys.first()
                    cache.remove(eldestKey)
                }
            }

            // Return the non-null deferred we either found or just created
            existingDeferred
        }

        return deferred.await()
    }
}

private const val PlcDirectoryUrl = "https://plc.directory"
private const val MaxCacheSize = 20

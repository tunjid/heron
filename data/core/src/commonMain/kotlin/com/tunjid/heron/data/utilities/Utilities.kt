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

package com.tunjid.heron.data.utilities

import androidx.collection.MutableObjectIntMap
import com.tunjid.heron.data.network.NetworkMonitor
import io.ktor.client.plugins.ResponseException
import kotlin.jvm.JvmInline
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.io.IOException
import sh.christian.ozone.api.response.AtpResponse

internal inline fun <R> runCatchingUnlessCancelled(block: () -> R): Result<R> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e)
}

internal suspend inline fun <T : Any> NetworkMonitor.runCatchingWithNetworkRetry(
    times: Int = 3,
    initialDelay: Long = 100, // 0.1 second
    maxDelay: Long = 5000, // 1 second
    factor: Double = 2.0,
    crossinline block: suspend () -> AtpResponse<T>,
): Result<T> = coroutineScope scope@{
    var connected = true
    // Monitor connection status async
    val connectivityJob = launch {
        isConnected.collect { connected = it }
    }
    var currentDelay = initialDelay
    repeat(times) { retry ->
        try {
            return@scope when (val atpResponse = block()) {
                is AtpResponse.Failure -> Result.failure(
                    Exception(atpResponse.error?.message),
                )

                is AtpResponse.Success -> Result.success(
                    atpResponse.response,
                )
            }.also { connectivityJob.cancel() }
        } catch (e: IOException) {
            // TODO: Log this exception
            e.printStackTrace()
        } catch (e: ResponseException) {
            // TODO: Log this exception
            e.printStackTrace()
        }
        if (retry != times) {
            if (connected) {
                delay(currentDelay)
            } // Wait for a network connection
            else {
                isConnected.first(true::equals)
            }
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
    }
    // Cancel the connectivity job before returning
    connectivityJob.cancel()
    // TODO: Be more descriptive with this error
    return@scope Result.failure(Exception("There was an error")) // last attempt
}

/**
 * A memory-efficient list implementation that defers memory allocation
 * for the list storage until the first element is explicitly added.
 *
 * @param T The type of elements contained in the list.
 */
@JvmInline
internal value class LazyList<T>(
    val lazyList: Lazy<MutableList<T>> = lazy(
        mode = LazyThreadSafetyMode.SYNCHRONIZED,
        initializer = ::mutableListOf,
    ),
) {
    val list: List<T>
        get() = if (lazyList.isInitialized()) lazyList.value else emptyList()

    fun add(element: T): Boolean = lazyList.value.add(element)
}

internal inline fun <T, R> Collection<T>.toFlowOrEmpty(
    crossinline block: (Collection<T>) -> Flow<List<R>>,
): Flow<List<R>> = when {
    isEmpty() -> emptyFlow()
    else -> block(this)
}
    .onStart { emit(emptyList()) }
    .distinctUntilChanged()

internal inline fun <T, R, K> List<T>.sortedWithNetworkList(
    networkList: List<R>,
    crossinline databaseId: (T) -> K,
    crossinline networkId: (R) -> K,
): List<T> {
    val idToIndices = networkList.foldIndexed(MutableObjectIntMap<K>()) { index, map, networkItem ->
        map[networkId(networkItem)] = index
        map
    }
    return sortedBy { idToIndices[databaseId(it)] }
}

// Heuristically defined method for debouncing flows produced by
// Room's invalidation tracker
internal const val InvalidationTrackerDebounceMillis = 120L

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
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.logging.LogPriority
import com.tunjid.heron.data.logging.logcat
import com.tunjid.heron.data.logging.loggableText
import com.tunjid.heron.data.network.NetworkConnectionException
import com.tunjid.heron.data.network.NetworkMonitor
import io.ktor.client.plugins.ResponseException
import kotlin.jvm.JvmInline
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
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

internal inline fun <R> runCatchingUnlessCancelled(block: () -> R): Result<R> {
    return try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
}

internal inline fun <R, T> Result<T>.mapCatchingUnlessCancelled(
    transform: (value: T) -> R,
): Result<R> = fold(
    onSuccess = {
        runCatchingUnlessCancelled { transform(it) }
    },
    onFailure = { Result.failure(it) },
)

/**
 * Catches network related exceptions and wraps them in a failure result.
 */
internal suspend inline fun <T : Any> NetworkMonitor.runCatchingWithNetworkRetry(
    times: Int = 3,
    initialDelay: Duration = 100.milliseconds,
    maxDelay: Duration = 4.seconds,
    factor: Double = 2.0,
    crossinline block: suspend () -> T,
): Result<T> = coroutineScope scope@{
    var connected = true
    // Monitor connection status async
    val connectivityJob = launch {
        isConnected.collect { connected = it }
    }
    var currentDelay = initialDelay
    var lastError: Throwable? = null
    repeat(times) { retry ->
        try {
            return@scope Result.success(block()).also { connectivityJob.cancel() }
        } catch (e: Exception) {
            when (e) {
                is NetworkConnectionException,
                is IOException,
                is ResponseException,
                -> {
                    lastError = e
                    logcat(LogPriority.VERBOSE) {
                        "Network error on ${retry + 1} of $times retries. Cause:\n${e.loggableText()}"
                    }
                }

                else -> throw e
            }
        }
        if (retry != times) {
            if (connected) delay(currentDelay)
            // Wait for a network connection
            else isConnected.first(true::equals)
            currentDelay = (currentDelay * factor).coerceAtMost(maxDelay)
        }
    }
    // Cancel the connectivity job before returning
    connectivityJob.cancel()
    logcat(LogPriority.WARN) {
        "Exponential backoff failed after $times retries. Cause: ${lastError?.loggableText()} "
    }
    return@scope Result.failure(lastError ?: Exception("There was an error")) // last attempt
}

/**
 * A memory-efficient list implementation that defers memory allocation
 * for the list storage until the first element is explicitly added.
 *
 * @param T The type of elements contained in the list.
 */
@JvmInline
internal value class LazyList<T>(
    private val lazyList: Lazy<MutableList<T>> = lazy(
        mode = LazyThreadSafetyMode.SYNCHRONIZED,
        initializer = ::mutableListOf,
    ),
) {
    val list: List<T>
        get() = if (lazyList.isInitialized()) lazyList.value else emptyList()

    fun add(element: T): Boolean =
        lazyList.value.add(element)
}

internal inline fun <K, V> Map<K, V>.updateOrPutValue(
    key: K,
    update: V.() -> V,
    put: () -> V? = { null },
): Map<K, V> =
    when (val existingValue = this[key]) {
        null -> when (val newValue = put()) {
            null -> this
            else -> this + Pair(key, newValue)
        }
        else -> this + Pair(key, existingValue.update())
    }

internal inline fun <T> Iterable<T>.triage(
    crossinline firstPredicate: (T) -> Boolean,
    crossinline secondPredicate: (T) -> Boolean,
): Triple<List<T>, List<T>, List<T>> {
    var first: ArrayList<T>? = null
    var second: ArrayList<T>? = null
    var third: ArrayList<T>? = null

    for (element in this) {
        when {
            firstPredicate(element) -> first ?: ArrayList<T>().also { first = it }
            secondPredicate(element) -> second ?: ArrayList<T>().also { second = it }
            else -> third ?: ArrayList<T>().also { third = it }
        }.add(element)
    }
    return Triple(
        first = first ?: emptyList(),
        second = second ?: emptyList(),
        third = third ?: emptyList(),
    )
}

internal inline fun <T, R> Collection<T>.toDistinctUntilChangedFlowOrEmpty(
    crossinline block: (Collection<T>) -> Flow<List<R>>,
): Flow<List<R>> =
    when {
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

internal inline fun <T> Result<T>.toOutcome(
    onSuccess: (T) -> Unit = {},
): Outcome = fold(
    onSuccess = {
        try {
            onSuccess(it)
            Outcome.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Outcome.Failure(e)
        }
    },
    onFailure = Outcome::Failure,
)

internal class InvalidTokenException : Exception("Invalid tokens")

internal data class AtProtoException(
    val statusCode: Int,
    val error: String?,
    override val message: String?,
) : Exception(message)

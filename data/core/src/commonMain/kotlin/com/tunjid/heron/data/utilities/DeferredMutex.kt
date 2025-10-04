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

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class DeferredMutex<K, V> {

    private val mutex = Mutex()
    private var lastDeferred: Pair<K, Deferred<V>>? = null
    private var lastResult: Pair<K, V>? = null

    suspend inline fun withSingleAccess(
        key: K,
        crossinline block: suspend () -> V,
    ): V {
        checkResult(key)?.let { return@withSingleAccess it }

        return coroutineScope {
            checkResult(key)?.let {
                return@coroutineScope async { it }
            }
            checkDeferred(key)?.let {
                return@coroutineScope it
            }

            mutex.withLock {
                checkResult(key)?.let {
                    return@coroutineScope async { it }
                }
                checkDeferred(key)?.let {
                    return@coroutineScope it
                }
                async {
                    block().also { lastResult = key to it }
                }.also { lastDeferred = key to it }
            }
        }.await()
    }

    private fun checkResult(key: K): V? {
        val result = lastResult
        return if (result != null && result.first == key) result.second
        else null
    }

    private fun checkDeferred(key: K): Deferred<V>? {
        val existingDeferred = lastDeferred
        return if (existingDeferred != null && existingDeferred.first == key && existingDeferred.second.isActive) existingDeferred.second
        else null
    }
}

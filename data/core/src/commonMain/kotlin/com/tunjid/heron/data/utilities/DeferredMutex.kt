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

internal class DeferredMutex<T> {

    private val mutex = Mutex()
    private var deferred: Deferred<T>? = null

    suspend inline fun withSingleAccess(
        crossinline block: suspend () -> T,
    ): T = coroutineScope {
        val existingDeferred = deferred
        if (existingDeferred != null && existingDeferred.isActive) {
            return@coroutineScope existingDeferred.await()
        }

        mutex.withLock {
            // Double-check inside the lock. Another coroutine might have
            // created the refresh while this one was waiting for the lock.
            val currentDeferred = deferred
            if (currentDeferred != null && currentDeferred.isActive) {
                return@withLock currentDeferred.await()
            }

            // This is the chosen coroutine. Start the new refresh operation.
            val newRefresh = async {
                block()
            }
            deferred = newRefresh
            newRefresh.await()
        }
    }
}

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

import kotlinx.coroutines.delay

suspend fun <T> exponentialBackoff(
    times: Int = Int.MAX_VALUE,
    initialDelay: Long = 1_000,
    maxDelay: Long = 1_000 * 60 * 60,
    factor: Double = 2.0,
    default: T,
    block: suspend () -> T,
): T {
    var currentDelay = initialDelay
    repeat(times) {
        try {
            return block()
        } catch (e: Exception) {
//            println("Exponential backoff error")
//            e.printStackTrace()
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    return default
}


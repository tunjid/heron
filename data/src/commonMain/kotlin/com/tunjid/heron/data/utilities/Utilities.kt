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

import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.coroutines.delay
import kotlinx.io.IOException
import sh.christian.ozone.api.response.AtpResponse

internal suspend inline fun <T : Any> runCatchingWithNetworkRetry(
    times: Int = 3,
    initialDelay: Long = 100, // 0.1 second
    maxDelay: Long = 5000,    // 1 second
    factor: Double = 2.0,
    block: () -> AtpResponse<T>,
): Result<T> {
    var currentDelay = initialDelay
    repeat(times) { retry ->
        try {
            return when (val atpResponse = block()) {
                is AtpResponse.Failure -> Result.failure(
                    Exception(atpResponse.error?.message)
                )

                is AtpResponse.Success -> Result.success(
                    atpResponse.response
                )
            }
        } catch (e: IOException) {
            // TODO: Log this exception
            e.printStackTrace()
        }
        catch (e: ResponseException) {
            // TODO: Log this exception
            e.printStackTrace()
        }
        if (retry != times) {
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
    }
    // TODO: Be more descriptive with this error
    return Result.failure(Exception("There was an error")) // last attempt
}

// Heuristically defined method for debouncing flows produced by
// Room's invalidation tracker
internal const val InvalidationTrackerDebounceMillis = 120L
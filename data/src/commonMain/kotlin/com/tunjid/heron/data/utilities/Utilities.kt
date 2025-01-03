package com.tunjid.heron.data.utilities

import kotlinx.coroutines.delay
import kotlinx.io.IOException
import sh.christian.ozone.api.response.AtpResponse

internal suspend inline fun <T : Any> runCatchingWithNetworkRetry(
    times: Int = 3,
    initialDelay: Long = 100, // 0.1 second
    maxDelay: Long = 5000,    // 1 second
    factor: Double = 2.0,
    block: () -> AtpResponse<T>
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
            // TODO: Be more descriptive with this error
            // you can log an error here and/or make a more finer-grained
            // analysis of the cause to see if retry is needed
        }
        if (retry != times) {
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
    }
    // TODO: Be more descriptive with this error
    return Result.failure(Exception("There was an error")) // last attempt
}


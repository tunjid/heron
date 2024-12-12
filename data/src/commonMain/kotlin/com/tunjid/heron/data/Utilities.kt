package com.tunjid.heron.data

import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import sh.christian.ozone.api.response.AtpResponse

internal inline fun < R: Any> runCatchingWithIoMessage(
    block: () -> AtpResponse<R>,
): Result<R> = try {
    when (val atpResponse = block()) {
        is AtpResponse.Failure -> Result.failure(
            Exception(atpResponse.error?.message)
        )

        is AtpResponse.Success -> Result.success(
            atpResponse.response
        )
    }
}
catch (cancellationException: CancellationException) {
    throw cancellationException
}
catch (ioe: IOException) {
    // TODO: Pare IO exception errors better
    Result.failure(Exception("There was an error"))
}
catch (e: Throwable) {
    Result.failure(e)
}

inline fun <R> runCatchingCoroutines(block: () -> R): Result<R> {
    return try {
        Result.success(block())
    }
    catch (cancellationException: CancellationException) {
        throw cancellationException
    }
    catch (e: Throwable) {
        Result.failure(e)
    }
}
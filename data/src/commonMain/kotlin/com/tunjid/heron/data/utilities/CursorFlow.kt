package com.tunjid.heron.data.utilities

import com.tunjid.heron.data.core.models.Cursor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import sh.christian.ozone.api.response.AtpResponse

internal fun <NetworkResponse : Any> nextCursorFlow(
    currentCursor: Cursor,
    currentRequestWithNextCursor: suspend () -> AtpResponse<NetworkResponse>,
    nextCursor: NetworkResponse.() -> String?,
    onResponse: suspend NetworkResponse.() -> Unit,
): Flow<Cursor> = flow {
    // Emit pending downstream
    emit(Cursor.Pending)

    // Do nothing, can't tell what the next items are
    if (currentCursor == Cursor.Pending) return@flow

    runCatchingWithNetworkRetry {
        currentRequestWithNextCursor()
    }
        .getOrNull()
        ?.let { response ->
            response.nextCursor()
                ?.let(Cursor::Next)
                ?.let { emit(it) }
            onResponse(response)
        }
}
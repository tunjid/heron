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
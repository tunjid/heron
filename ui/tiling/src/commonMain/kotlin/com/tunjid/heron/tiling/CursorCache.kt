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

package com.tunjid.heron.tiling

import androidx.compose.runtime.Stable
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Cursors
import com.tunjid.heron.data.core.models.canRequestData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

@Stable
class CursorCache<Query : CursorQuery> {

    private val queriesToCursors = MutableStateFlow<Map<Query, Cursor>>(emptyMap())

    /**
     * Records the real cursor that backed [query]'s fetch. Non-fetchable cursors
     * ([Cursor.Pending], [Cursor.Final]) are ignored, so they can never overwrite a known real one.
     */
    internal fun record(
        query: Query,
        cursor: Cursor,
    ) {
        if (!cursor.canRequestData) return
        queriesToCursors.update { it + (query to cursor) }
    }

    @PublishedApi
    internal fun reset() {
        queriesToCursors.value = emptyMap()
    }

    internal fun seedQueryTokenMap(): Map<Query, Cursor>? =
        queriesToCursors.value
            .takeUnless(Map<Query, Cursor>::isEmpty)

    internal fun toCursors(
        anchorData: CursorQuery.Data,
    ) = queriesToCursors.value
        .mapKeys { it.key.data }
        .takeUnless(Map<CursorQuery.Data, Cursor>::isEmpty)
        ?.let { pages ->
            Cursors(
                anchorData = anchorData,
                pages = pages,
            )
        }

    internal operator fun get(
        query: Query,
    ): Cursor? = queriesToCursors.value[query]
}

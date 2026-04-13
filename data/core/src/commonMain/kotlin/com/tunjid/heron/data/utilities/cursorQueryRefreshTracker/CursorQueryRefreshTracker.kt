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

package com.tunjid.heron.data.utilities.cursorQueryRefreshTracker

import com.tunjid.heron.data.core.models.CursorQuery
import dev.zacsweers.metro.Inject
import kotlin.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Tracks the [CursorQuery.Data.cursorAnchor] last observed for each caller-supplied
 * identity, entirely in memory. Used to detect "first page with a new anchor" events
 * so repositories can drop stale rows before inserting a fresh page.
 *
 * State is process-local: on cold start the tracker has no knowledge of prior anchors
 * and the first call for any identity will return `false`. Deletion must only be
 * triggered by an explicit refresh within the current session.
 */
internal interface CursorQueryRefreshTracker {

    /**
     * Returns `true` if
     *     - [query] is a first page (`page == 0`)
     *     - AND a prior anchor was recorded for [identity]
     *     - AND that prior anchor differs from [query]'s current anchor.
     * Always records the current anchor before returning, so the
     * next call compares against this one.
     *
     * The first call for a given identity returns `false` — on cold start the tracker
     * has no state, and deletion must be triggered by an explicit refresh only.
     */
    suspend fun isFirstPageForDifferentAnchor(
        query: CursorQuery,
        identity: () -> String,
    ): Boolean
}

internal class InMemoryCursorQueryRefreshTracker @Inject constructor() : CursorQueryRefreshTracker {

    private val anchors = mutableMapOf<String, Instant>()
    private val mutex = Mutex()

    override suspend fun isFirstPageForDifferentAnchor(
        query: CursorQuery,
        identity: () -> String,
    ): Boolean {
        if (query.data.page != 0) return false
        return mutex.withLock {
            val key = identity()
            val previous = anchors[key]
            anchors[key] = query.data.cursorAnchor
            previous != null && previous != query.data.cursorAnchor
        }
    }
}

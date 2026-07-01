/*
 *    Copyright 2026 Adetunji Dahunsi
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

package com.tunjid.heron.timeline.state.preview

import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.repository.TimelineQuery
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.timeline.state.TimelineState
import com.tunjid.heron.timeline.state.TimelineStateHolder
import com.tunjid.mutator.coroutines.asNoOpActionSuspendingStateMutator
import com.tunjid.tiler.buildTiledList
import com.tunjid.tiler.emptyTiledList
import kotlin.time.Instant

/**
 * Creates a no-op [TimelineStateHolder] for [timeline], seeded with [items], backed by an
 * [asNoOpActionSuspendingStateMutator] so its state is immutable and its actions are ignored.
 * This lets `@Preview`s render timelines without repositories, the tiling pipeline or the
 * dependency graph, mirroring how previews stub the identity, navigation and sheet state holders.
 *
 * Defaults to [TimelineItem.LoadingItems] so callers get a screen of shimmering placeholders.
 */
fun stubTimelineStateHolder(
    timeline: Timeline,
    items: List<TimelineItem> = TimelineItem.LoadingItems,
    numColumns: Int = 1,
): TimelineStateHolder {
    val query = TimelineQuery(
        source = timeline.source,
        data = CursorQuery.Data(
            page = 0,
            cursorAnchor = Instant.DISTANT_PAST,
        ),
    )
    return TimelineState.Immutable(
        timeline = timeline,
        hasUpdates = false,
        tilingData = TilingState.Data(
            currentQuery = query,
            numColumns = numColumns,
            items =
            if (items.isEmpty()) emptyTiledList()
            else buildTiledList { addAll(query, items) },
        ),
    ).asNoOpActionSuspendingStateMutator()
}

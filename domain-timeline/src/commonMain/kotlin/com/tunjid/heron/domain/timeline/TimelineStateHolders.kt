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

package com.tunjid.heron.domain.timeline

import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.repository.TimelineRepository
import kotlinx.coroutines.CoroutineScope

class TimelineStateHolders internal constructor(
    internal val timelineIdsToTimelineStates: Map<String, TimelineStateHolder>,
) {

    constructor() : this(emptyMap())

    private val stateHolders = timelineIdsToTimelineStates.entries.toList()

    val size get() = stateHolders.size

    fun keyAt(index: Int) = stateHolders[index].key

    fun stateHolderAt(index: Int) = stateHolders[index].value

    fun stateHolderAtOrNull(index: Int) = stateHolders.getOrNull(index)?.value


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as TimelineStateHolders

        return timelineIdsToTimelineStates == other.timelineIdsToTimelineStates
    }

    override fun hashCode(): Int {
        return timelineIdsToTimelineStates.hashCode()
    }
}


fun TimelineStateHolders.update(
    updatedTimelines: List<Timeline>,
    scope: CoroutineScope,
    refreshOnStart: Boolean,
    startNumColumns: Int,
    timelineRepository: TimelineRepository,
): TimelineStateHolders =
    TimelineStateHolders(
        timelineIdsToTimelineStates = updatedTimelines.fold(emptyMap()) { newTimelines, timeline ->
            newTimelines + Pair(
                timeline.sourceId,
                timelineIdsToTimelineStates[timeline.sourceId]
                    ?: timelineStateHolder(
                        refreshOnStart = refreshOnStart,
                        timeline = timeline,
                        startNumColumns = startNumColumns,
                        scope = scope,
                        timelineRepository = timelineRepository,
                    )
            )
        }
    )
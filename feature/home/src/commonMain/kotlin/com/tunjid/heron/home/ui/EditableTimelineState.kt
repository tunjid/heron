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

package com.tunjid.heron.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.sourceId
import com.tunjid.heron.ui.draganddrop.DragAndDropSelectorState
import com.tunjid.heron.ui.draganddrop.DragAndDropSelectorState.Companion.selectorDragAndDrop
import com.tunjid.heron.ui.draganddrop.DragAndDropSelectorState.Companion.selectorDropTarget

@Stable
internal class EditableTimelineState private constructor(
    val timelines: SnapshotStateList<Timeline.Home>,
) {
    private val dragAndDropSelectorState = DragAndDropSelectorState(
        items = timelines,
        id = Timeline.Home::sourceId,
        selected = Timeline.Home::isPinned,
    )

    val firstUnpinnedIndex get() = dragAndDropSelectorState.firstUnselectedIndex
    val isHintHovered get() = dragAndDropSelectorState.isHintHovered
    val shouldShowHint get() = dragAndDropSelectorState.firstUnselectedIndex == timelines.size

    @Stable
    fun isHoveredId(
        id: String,
    ) = dragAndDropSelectorState.isHoveredId(id)

    @Stable
    fun isDraggedId(
        id: String,
    ) = dragAndDropSelectorState.isDraggedId(id)

    fun remove(timeline: Timeline.Home) {
        dragAndDropSelectorState.remove(timeline)
    }

    fun timelinesToSave() = timelines.mapIndexed { index, timeline ->
        when (timeline) {
            is Timeline.Home.Feed -> timeline.copy(isPinned = index < dragAndDropSelectorState.firstUnselectedIndex)
            is Timeline.Home.Following -> timeline.copy(isPinned = index < dragAndDropSelectorState.firstUnselectedIndex)
            is Timeline.Home.List -> timeline.copy(isPinned = index < dragAndDropSelectorState.firstUnselectedIndex)
        }
    }

    companion object {

        @Composable
        fun rememberEditableTimelineState(
            timelines: List<Timeline.Home>,
        ): EditableTimelineState = remember(
            // Only timeline order should recreate state
            timelines.joinToString(
                separator = "-",
                transform = Timeline.Home::sourceId,
            ),
        ) {
            EditableTimelineState(
                timelines = timelines.toMutableStateList(),
            )
        }

        fun Modifier.timelineEditDragAndDrop(
            state: EditableTimelineState,
            sourceId: String,
        ) = selectorDragAndDrop(
            state = state.dragAndDropSelectorState,
            sourceId = sourceId,
        )

        fun Modifier.timelineEditDropTarget(
            state: EditableTimelineState,
        ) = selectorDropTarget(
            state = state.dragAndDropSelectorState,
        )
    }
}

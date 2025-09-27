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

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.home.draggedId
import com.tunjid.heron.home.timelineEditDragAndDropSource
import kotlin.math.max
import kotlin.math.min

@Stable
internal class EditableTimelineState private constructor(
    val timelines: SnapshotStateList<Timeline.Home>,
) {
    private var hoveredId by mutableStateOf<String?>(null)
    private var draggedId by mutableStateOf<String?>(null)

    var firstUnpinnedIndex by mutableStateOf(
        when (val index = timelines.indexOfFirst { !it.isPinned }) {
            in Int.MIN_VALUE..<0 -> timelines.size
            else -> index
        },
    )
        private set

    var isHintHovered by mutableStateOf(false)
        private set

    val shouldShowHint get() = firstUnpinnedIndex == timelines.size

    private val tabTargets = mutableStateMapOf<String, TabTarget>()
    private val hintTarget = HintTarget()

    @Stable
    fun isHoveredId(sourceId: String) = sourceId == hoveredId

    @Stable
    fun isDraggedId(sourceId: String) = sourceId == draggedId

    fun remove(timeline: Timeline.Home) {
        val index = timelines.indexOfFirst { it.sourceId == timeline.sourceId }
        if (index < 0) return

        timelines.removeAt(index)
        if (index <= firstUnpinnedIndex) firstUnpinnedIndex = max(
            a = firstUnpinnedIndex - 1,
            b = 0,
        )
    }

    fun timelinesToSave() = timelines.mapIndexed { index, timeline ->
        when (timeline) {
            is Timeline.Home.Feed -> timeline.copy(isPinned = index < firstUnpinnedIndex)
            is Timeline.Home.Following -> timeline.copy(isPinned = index < firstUnpinnedIndex)
            is Timeline.Home.List -> timeline.copy(isPinned = index < firstUnpinnedIndex)
        }
    }

    private fun DragAndDropEvent.draggedIndex() =
        draggedId()?.let { draggedId ->
            timelines.indexOfFirst { it.sourceId == draggedId }
        } ?: -1

    private fun dropItem(
        acceptedDrop: Boolean,
        draggedIndex: Int,
        droppedIndex: Int,
    ) {
        Snapshot.withMutableSnapshot {
            if (acceptedDrop) {
                timelines.add(
                    index = min(timelines.lastIndex, droppedIndex),
                    element = timelines.removeAt(draggedIndex),
                )
                firstUnpinnedIndex = when {
                    // Moved last saved item to pinned items
                    draggedIndex == firstUnpinnedIndex && draggedIndex == timelines.lastIndex -> timelines.size
                    // Dropped in hint box
                    droppedIndex >= timelines.size -> timelines.lastIndex
                    else -> when (firstUnpinnedIndex) {
                        // Moved out of pinned items
                        in draggedIndex..droppedIndex -> max(
                            a = firstUnpinnedIndex - 1,
                            b = 0,
                        )
                        // Moved into pinned items
                        in droppedIndex..draggedIndex -> min(
                            a = firstUnpinnedIndex + 1,
                            b = timelines.lastIndex,
                        )
                        else -> firstUnpinnedIndex
                    }
                }
            }
            hoveredId = null
            draggedId = null
        }
    }

    @Stable
    private inner class TabTarget(
        sourceId: String,
    ) : DragAndDropTarget {

        var sourceId by mutableStateOf(sourceId)

        override fun onStarted(event: DragAndDropEvent) {
            draggedId = event.draggedId()
        }

        override fun onEntered(event: DragAndDropEvent) {
            hoveredId = sourceId
        }

        override fun onExited(event: DragAndDropEvent) {
            if (isHoveredId(sourceId)) hoveredId = null
        }

        override fun onDrop(event: DragAndDropEvent): Boolean {
            val draggedIndex = event.draggedIndex()
            val droppedIndex = timelines.indexOfFirst {
                it.sourceId == sourceId
            }

            val acceptedDrop =
                // Make sure at least 1 item is always pinned
                if (firstUnpinnedIndex in draggedIndex..droppedIndex) firstUnpinnedIndex > 1
                else draggedIndex >= 0 && droppedIndex >= 0

            dropItem(
                acceptedDrop = acceptedDrop,
                draggedIndex = draggedIndex,
                droppedIndex = droppedIndex,
            )

            return acceptedDrop
        }

        override fun onEnded(event: DragAndDropEvent) {
            Snapshot.withMutableSnapshot {
                hoveredId = null
                draggedId = null
            }
        }
    }

    @Stable
    private inner class HintTarget : DragAndDropTarget {

        override fun onStarted(event: DragAndDropEvent) {
            draggedId = event.draggedId()
        }

        override fun onEntered(event: DragAndDropEvent) {
            isHintHovered = true
        }

        override fun onExited(event: DragAndDropEvent) {
            isHintHovered = false
        }

        override fun onDrop(event: DragAndDropEvent): Boolean {
            val acceptedDrop = timelines.size >= 2

            dropItem(
                acceptedDrop = acceptedDrop,
                draggedIndex = event.draggedIndex(),
                droppedIndex = timelines.size,
            )

            return acceptedDrop
        }

        override fun onEnded(event: DragAndDropEvent) {
            Snapshot.withMutableSnapshot {
                isHintHovered = false
                hoveredId = null
                draggedId = null
            }
        }
    }

    companion object {

        @Composable
        fun rememberEditableTimelineState(
            timelines: List<Timeline.Home>,
        ): EditableTimelineState = remember {
            EditableTimelineState(
                timelines = timelines.toMutableStateList(),
            )
        }

        fun Modifier.timelineEditDragAndDrop(
            state: EditableTimelineState,
            sourceId: String,
        ) = timelineEditDragAndDropSource(sourceId)
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    event.draggedId() != null
                },
                target = state.tabTargets.getOrPut(sourceId) {
                    state.TabTarget(sourceId)
                }.also { it.sourceId = sourceId },
            )

        fun Modifier.timelineEditDropTarget(
            state: EditableTimelineState,
        ) = dragAndDropTarget(
            shouldStartDragAndDrop = { event ->
                event.draggedId() != null
            },
            target = state.hintTarget,
        )
    }
}

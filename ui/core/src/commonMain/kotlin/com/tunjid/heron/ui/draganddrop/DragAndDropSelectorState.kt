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

package com.tunjid.heron.ui.draganddrop

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import kotlin.math.max
import kotlin.math.min

@Stable
class DragAndDropSelectorState<T>(
    val items: SnapshotStateList<T>,
    val id: T.() -> String,
    val selected: T.() -> Boolean,
) {
    private var hoveredId by mutableStateOf<String?>(null)
    private var draggedId by mutableStateOf<String?>(null)

    var firstUnselectedIndex by mutableStateOf(
        when (val index = items.indexOfFirst { !selected(it) }) {
            -1 -> items.size
            else -> index
        },
    )
        private set

    var isHintHovered by mutableStateOf(false)
        private set

    val partitioned: Pair<List<T>, List<T>> by derivedStateOf {
        val snapshot = items.toList()
        val index = firstUnselectedIndex.coerceIn(0, snapshot.size)

        if (index < 0) snapshot to emptyList()
        else snapshot.subList(0, index) to snapshot.subList(
            index,
            snapshot.size,
        )
    }

    private val tabTargets = mutableStateMapOf<String, TabTarget>()
    private val hintTarget = HintTarget()

    @Stable
    fun isHoveredId(id: String) = id == hoveredId

    @Stable
    fun isDraggedId(id: String) = id == draggedId

    fun remove(item: T) {
        val index = items.indexOfFirst { it.id() == item.id() }
        if (index < 0) return

        items.removeAt(index)
        if (index <= firstUnselectedIndex) firstUnselectedIndex = max(
            a = firstUnselectedIndex - 1,
            b = 0,
        )
    }

    private fun DragAndDropEvent.draggedIndex() =
        draggedId()?.let { draggedId ->
           this@DragAndDropSelectorState.items.indexOfFirst { it.id() == draggedId }
        } ?: -1

    private fun dropItem(
        acceptedDrop: Boolean,
        draggedIndex: Int,
        droppedIndex: Int,
    ) {
        Snapshot.withMutableSnapshot {
            if (acceptedDrop) {
                items.add(
                    index = min(items.lastIndex, droppedIndex),
                    element = items.removeAt(draggedIndex),
                )
                firstUnselectedIndex = when {
                    // Moved last saved item to pinned items
                    draggedIndex == firstUnselectedIndex && draggedIndex == items.lastIndex -> items.size
                    // Dropped in hint box
                    droppedIndex >= items.size -> items.lastIndex
                    else -> when (firstUnselectedIndex) {
                        // Moved out of pinned items
                        in draggedIndex..droppedIndex -> max(
                            a = firstUnselectedIndex - 1,
                            b = 0,
                        )
                        // Moved into pinned items
                        in droppedIndex..draggedIndex -> min(
                            a = firstUnselectedIndex + 1,
                            b = items.lastIndex,
                        )
                        else -> firstUnselectedIndex
                    }
                }
            }
            hoveredId = null
            draggedId = null
        }
    }

    @Stable
    private inner class TabTarget(
        id: String,
    ) : DragAndDropTarget {

        var id by mutableStateOf(id)

        override fun onStarted(event: DragAndDropEvent) {
            draggedId = event.draggedId()
        }

        override fun onEntered(event: DragAndDropEvent) {
            hoveredId = id
        }

        override fun onExited(event: DragAndDropEvent) {
            if (isHoveredId(id)) hoveredId = null
        }

        override fun onDrop(event: DragAndDropEvent): Boolean {
            val draggedIndex = event.draggedIndex()
            val droppedIndex = items.indexOfFirst {
                it.id() == id
            }

            val acceptedDrop =
                // Make sure at least 1 item is always pinned
                if (firstUnselectedIndex in draggedIndex..droppedIndex) firstUnselectedIndex > 1
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
            val acceptedDrop = items.size >= 2

            dropItem(
                acceptedDrop = acceptedDrop,
                draggedIndex = event.draggedIndex(),
                droppedIndex = items.size,
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
        fun <T> Modifier.selectorDragAndDrop(
            state: DragAndDropSelectorState<T>,
            id: String,
        ) = selectorDragAndDropSource(id)
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    event.draggedId() != null
                },
                target = state.tabTargets.getOrPut(id) {
                    state.TabTarget(id)
                }.also { it.id = id },
            )

        fun <T> Modifier.selectorDropTarget(
            state: DragAndDropSelectorState<T>,
        ) = dragAndDropTarget(
            shouldStartDragAndDrop = { event ->
                event.draggedId() != null
            },
            target = state.hintTarget,
        )
    }
}

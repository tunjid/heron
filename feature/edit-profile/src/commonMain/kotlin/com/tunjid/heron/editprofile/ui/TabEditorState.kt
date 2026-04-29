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

package com.tunjid.heron.editprofile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import com.tunjid.heron.data.core.models.ProfileTab
import com.tunjid.heron.ui.draganddrop.DragAndDropSelectorState
import com.tunjid.heron.ui.draganddrop.DragAndDropSelectorState.Companion.selectorDragAndDrop
import com.tunjid.heron.ui.draganddrop.DragAndDropSelectorState.Companion.selectorDropTarget

@Stable
internal class TabEditorState private constructor(
    val tabs: SnapshotStateList<ProfileTab>,
    val selected: (ProfileTab) -> Boolean,
) {
    private val dragAndDropSelectorState = DragAndDropSelectorState(
        items = tabs,
        id = ProfileTab::id,
        selected = selected,
    )

    val isHintHovered get() = dragAndDropSelectorState.isHintHovered
    val shouldShowHint get() = dragAndDropSelectorState.firstUnselectedIndex == tabs.size
    val partitioned get() = dragAndDropSelectorState.partitioned

    @Stable
    fun isHoveredId(
        tab: ProfileTab,
    ) = dragAndDropSelectorState.isHoveredId(tab.id)

    @Stable
    fun isDragged(
        tab: ProfileTab,
    ) = dragAndDropSelectorState.isDraggedId(tab.id)

    companion object {

        @Composable
        fun rememberTabEditorState(
            tabs: List<ProfileTab>,
            currentTabs: Set<ProfileTab>,
        ): TabEditorState = remember(
            key1 = tabs,
            key2 = currentTabs,
        ) {
            TabEditorState(
                tabs = tabs.toMutableStateList(),
                selected = currentTabs::contains,
            )
        }

        fun Modifier.tabEditorDragAndDrop(
            state: TabEditorState,
            tab: ProfileTab,
        ) = selectorDragAndDrop(
            state = state.dragAndDropSelectorState,
            id = tab.id,
        )

        fun Modifier.tabEditorDropTarget(
            state: TabEditorState,
        ) = selectorDropTarget(
            state = state.dragAndDropSelectorState,
        )
    }
}

internal val ProfileTab.id
    get() = when (this) {
        is ProfileTab.Bluesky.FeedGenerators.FeedGenerator -> uri.uri
        else -> this::class.toString()
    }

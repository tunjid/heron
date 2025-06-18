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

package com.tunjid.heron.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateBounds
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.domain.timeline.TimelineLoadAction
import com.tunjid.heron.domain.timeline.TimelineStateHolders
import com.tunjid.heron.home.DragAndDropTabsState.Companion.tabDragAndDrop
import com.tunjid.heron.ui.Tab
import com.tunjid.heron.ui.Tabs
import com.tunjid.heron.ui.TabsState
import com.tunjid.heron.ui.TabsState.Companion.rememberTabsState
import com.tunjid.heron.ui.tabIndex
import kotlinx.coroutines.launch

expect fun dragAndDropTransferData(title: String): DragAndDropTransferData

expect fun DragAndDropEvent.draggedTitle(): String?

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HomeTabs(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    pagerState: PagerState,
    timelines: List<Timeline>,
    currentSourceId: String?,
    timelineStateHolders: TimelineStateHolders,
    tabs: List<Tab>,
    onRefreshTabClicked: (Int) -> Unit,
) = with(sharedTransitionScope) {
    var isExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val tabsState = rememberTabsState(
        tabs = tabs,
        selectedTabIndex = pagerState.tabIndex,
        onTabSelected = {
            isExpanded = false
            scope.launch {
                pagerState.animateScrollToPage(it)
            }
        },
        onTabReselected = onRefreshTabClicked,
    )
    Box(
        modifier = modifier,
    ) {
        AnimatedContent(
            modifier = Modifier
                .animateContentSize(),
            targetState = isExpanded,
        ) { expanded ->
            if (expanded) ExpandedTabs(
                tabs = tabs,
                tabsState = tabsState,
                sharedTransitionScope = this@with,
                animatedContentScope = this@AnimatedContent,
                onDismissed = { isExpanded = false }
            )
            else CollapsedTabs(
                modifier = Modifier,
                tabsState = tabsState,
                sharedTransitionScope = this@with,
                animatedContentScope = this@AnimatedContent,
                currentSourceId = currentSourceId,
                timelines = timelines,
                timelineStateHolders = timelineStateHolders
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .renderInSharedTransitionScopeOverlay(
                    renderInOverlay = this@with::isTransitionActive
                ),
        ) {
            ExpandButton(
                isExpanded = isExpanded,
                onToggled = { isExpanded = !isExpanded }
            )
            Spacer(
                modifier = Modifier
                    .width(8.dp)
            )
        }
    }
}


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ExpandedTabs(
    tabs: List<Tab>,
    tabsState: TabsState,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    onDismissed: () -> Unit,
) = with(sharedTransitionScope) {
    val state = remember {
        DragAndDropTabsState(
            draggableTabs = mutableStateListOf(
                *Array(tabs.size) { tabs[it].title }
            ),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismissed
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Spacer(
                Modifier.height(40.dp)
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface
                    ),
                horizontalArrangement = Arrangement.Start,
            ) {
                state.draggableTabs.forEachIndexed { index, title ->
                    key(title) {
                        if (!state.isDraggedTab(title)) FilterChip(
                            modifier = Modifier.Companion
                                .animateBounds(sharedTransitionScope)
                                .sharedElement(
                                    sharedContentState = sharedTransitionScope.rememberSharedContentState(
                                        title
                                    ),
                                    animatedVisibilityScope = animatedContentScope,
                                )
                                .tabDragAndDrop(
                                    state = state,
                                    title = title,
                                ),
                            shape = CircleShape,
                            border = null,
                            selected = state.isHoveredTab(title),
                            onClick = click@{
                                tabsState.onTabSelected(index)
                            },
                            label = {
                                Text(title)
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CollapsedTabs(
    modifier: Modifier,
    tabsState: TabsState,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    currentSourceId: String?,
    timelines: List<Timeline>,
    timelineStateHolders: TimelineStateHolders,
) = with(sharedTransitionScope) {
    Row(
        modifier = modifier
            .clip(CircleShape),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Tabs(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .weight(1f)
                .clip(CircleShape),
            tabsState = tabsState,
            tabContent = { tab ->
                Tab(
                    modifier = Modifier.Companion
                        .sharedElement(
                            sharedContentState = sharedTransitionScope.rememberSharedContentState(
                                tab.title
                            ),
                            animatedVisibilityScope = animatedContentScope,
                        ),
                    tab = tab,
                )
            }
        )
        TimelinePresentationSelector(
            currentSourceId = currentSourceId,
            timelines = timelines,
            timelineStateHolders = timelineStateHolders,
        )
        Spacer(
            modifier = Modifier
                .width(48.dp),
        )
    }
}

@Composable
private fun ExpandButton(
    modifier: Modifier = Modifier,
    isExpanded: Boolean,
    onToggled: () -> Unit,
) {
    val rotationState = animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f
    )
    ElevatedCard(
        modifier = modifier,
        shape = CircleShape,
    ) {
        IconButton(
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer {
                    rotationZ = rotationState.value
                },
            onClick = onToggled,
            content = {
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = "",
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
        )
    }
}

@Composable
private fun TimelinePresentationSelector(
    modifier: Modifier = Modifier,
    currentSourceId: String?,
    timelines: List<Timeline>,
    timelineStateHolders: TimelineStateHolders,
) {
    val timeline = timelines.firstOrNull {
        it.sourceId == currentSourceId
    }
    if (timeline != null) Row(
        modifier = modifier.wrapContentWidth(),
        horizontalArrangement = Arrangement.aligned(Alignment.End)
    ) {
        com.tunjid.heron.timeline.ui.TimelinePresentationSelector(
            selected = timeline.presentation,
            available = timeline.supportedPresentations,
            onPresentationSelected = { presentation ->
                val index = timelines.indexOfFirst {
                    it.sourceId == currentSourceId
                }
                timelineStateHolders.stateHolderAtOrNull(index)
                    ?.accept
                    ?.invoke(
                        TimelineLoadAction.UpdatePreferredPresentation(
                            timeline = timeline,
                            presentation = presentation,
                        )
                    )
            }
        )
    }
}

@Stable
private class DragAndDropTabsState(
    val draggableTabs: SnapshotStateList<String>,
) {
    var hoveredTitle by mutableStateOf<String?>(null)
    var draggedTitle by mutableStateOf<String?>(null)

    val children = mutableStateMapOf<String, DragAndDropTabsState.Child>()

    fun isHoveredTab(title: String) = title == hoveredTitle

    fun isDraggedTab(title: String) = title == draggedTitle

    inner class Child(
        title: String,
    ) : DragAndDropTarget {

        var title by mutableStateOf(title)

        override fun onStarted(event: DragAndDropEvent) {
            draggedTitle = event.draggedTitle()
        }

        override fun onEntered(event: DragAndDropEvent) {
            hoveredTitle = title
        }

        override fun onExited(event: DragAndDropEvent) {
            if (isHoveredTab(title)) hoveredTitle = null
        }

        override fun onDrop(event: DragAndDropEvent): Boolean {
            val draggedIndex = event.draggedTitle()?.let(draggableTabs::indexOf) ?: -1
            val droppedIndex = draggableTabs.indexOf(title)

            val acceptedDrop = draggedIndex >= 0 && droppedIndex >= 0

            Snapshot.withMutableSnapshot {
                if (acceptedDrop) draggableTabs.add(
                    index = droppedIndex,
                    element = draggableTabs.removeAt(draggedIndex),
                )
                hoveredTitle = null
                draggedTitle = null
            }

            return acceptedDrop
        }

        override fun onEnded(event: DragAndDropEvent) {
            Snapshot.withMutableSnapshot {
                hoveredTitle = null
                draggedTitle = null
            }
        }
    }

    companion object {
        fun Modifier.tabDragAndDrop(
            state: DragAndDropTabsState,
            title: String,
        ) = dragAndDropSource {
            dragAndDropTransferData(title)
        }
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    event.draggedTitle() != null
                },
                target = state.children.getOrPut(title) {
                    state.Child(title)
                }.also { it.title = title },
            )
    }
}



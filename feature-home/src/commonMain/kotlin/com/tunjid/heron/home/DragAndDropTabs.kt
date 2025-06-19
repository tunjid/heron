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
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.domain.timeline.TimelineLoadAction
import com.tunjid.heron.domain.timeline.TimelineStateHolders
import com.tunjid.heron.home.DragAndDropTabsState.Companion.tabDragAndDrop
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.ui.Tab
import com.tunjid.heron.ui.Tabs
import com.tunjid.heron.ui.TabsState
import com.tunjid.heron.ui.TabsState.Companion.rememberTabsState
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.tabIndex
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

expect fun dragAndDropTransferData(title: String): DragAndDropTransferData

expect fun DragAndDropEvent.draggedId(): String?

expect fun Modifier.tabDragAndDropSource(sourceId: String): Modifier


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HomeTabs(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    pagerState: PagerState,
    timelines: List<Timeline.Home>,
    currentSourceId: String?,
    timelineStateHolders: TimelineStateHolders,
    sourceIdsToHasUpdates: Map<String, Boolean>,
    onRefreshTabClicked: (Int) -> Unit,
) = with(sharedTransitionScope) {
    var isExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val collapsedTabsState = rememberTabsState(
        tabs = remember(sourceIdsToHasUpdates, timelines) {
            timelines
                .filter(Timeline.Home::isPinned)
                .map { timeline ->
                    Tab(
                        title = timeline.name,
                        hasUpdate = sourceIdsToHasUpdates[timeline.sourceId] == true,
                    )
                }
        },
        selectedTabIndex = pagerState.tabIndex,
        onTabSelected = {
            isExpanded = false
            scope.launch {
                pagerState.animateScrollToPage(it)
            }
        },
        onTabReselected = onRefreshTabClicked,
    )
    val expandedTabsState = rememberTabsState(
        tabs = remember(timelines) {
            timelines.map { timeline ->
                Tab(
                    title = timeline.name,
                    hasUpdate = false,
                )
            }
        },
        selectedTabIndex = pagerState.tabIndex,
        onTabSelected = collapsedTabsState.onTabSelected,
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
                timelines = timelines,
                tabsState = expandedTabsState,
                sharedTransitionScope = this@with,
                animatedContentScope = this@AnimatedContent,
                onDismissed = { isExpanded = false }
            )
            else CollapsedTabs(
                modifier = Modifier,
                tabsState = collapsedTabsState,
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
            verticalAlignment = Alignment.CenterVertically,
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
    timelines: List<Timeline.Home>,
    tabsState: TabsState,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    onDismissed: () -> Unit,
) = with(sharedTransitionScope) {
    val state = remember {
        timelines.toMutableStateList()
        DragAndDropTabsState(
            timelines = timelines.toMutableStateList(),
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
                    .skipToLookaheadSize()
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.timelines.forEachIndexed { index, timeline ->
                    key(timeline.sourceId) {
                        if (!state.isDraggedId(timeline.sourceId)) tabsState.ExpandedTab(
                            modifier = Modifier
                                .animateBounds(this@with),
                            dragAndDropTabsState = state,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedContentScope = animatedContentScope,
                            timeline = timeline,
                            index = index,
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
                .skipToLookaheadSize()
                .background(MaterialTheme.colorScheme.surface)
                .weight(1f)
                .clip(CircleShape),
            tabsState = tabsState,
            tabContent = { tab ->
                FilterChip(
                    modifier = modifier,
                    shape = RoundedCornerShape(16.dp),
                    border = null,
                    selected = false,
                    onClick = click@{
                        val index = tabList.indexOf(tab)
                        if (index < 0) return@click

                        if (index != selectedTabIndex.roundToInt()) onTabSelected(index)
                        else onTabReselected(index)
                    },
                    label = {
                        Text(
                            modifier = Modifier
                                .sharedElement(
                                    sharedContentState = sharedTransitionScope.rememberSharedContentState(
                                        tab.title
                                    ),
                                    animatedVisibilityScope = animatedContentScope,
                                ),
                            text = tab.title
                        )
                    },
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun TabsState.ExpandedTab(
    modifier: Modifier = Modifier,
    dragAndDropTabsState: DragAndDropTabsState,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    timeline: Timeline.Home,
    index: Int,
) = with(sharedTransitionScope) {
    InputChip(
        modifier = modifier
            .skipToLookaheadSize()
            .tabDragAndDrop(
                state = dragAndDropTabsState,
                sourceId = timeline.sourceId,
            ),
        shape = CircleShape,
        selected = dragAndDropTabsState.isHoveredId(timeline.sourceId),
        onClick = click@{
            onTabSelected(index)
        },
        avatar = {
            val url = when (timeline) {
                is Timeline.Home.Feed -> timeline.feedGenerator.avatar?.uri
                is Timeline.Home.Following -> null
                is Timeline.Home.List -> timeline.feedList.avatar?.uri
            }
            if (url != null) AsyncImage(
                modifier = Modifier
                    .size(24.dp),
                args = remember(url) {
                    ImageArgs(
                        url = url,
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        shape = RoundedPolygonShape.Circle,
                    )
                }
            )
        },
        label = {
            Text(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .sharedElement(
                        sharedContentState = sharedTransitionScope.rememberSharedContentState(
                            timeline.name
                        ),
                        animatedVisibilityScope = animatedContentScope,
                    ),
                text = timeline.name,
                maxLines = 1,
            )
        },
        trailingIcon = {
            Icon(
                imageVector = Icons.Rounded.Cancel,
                contentDescription = "",
            )
        },
    )
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
    val timelines: SnapshotStateList<Timeline.Home>,
) {
    var hoveredId by mutableStateOf<String?>(null)
    var draggedId by mutableStateOf<String?>(null)

    val children = mutableStateMapOf<String, Child>()

    fun isHoveredId(sourceId: String) = sourceId == hoveredId

    fun isDraggedId(sourceId: String) = sourceId == draggedId

    inner class Child(
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
            val draggedIndex = event.draggedId()?.let { draggedId ->
                timelines.indexOfFirst { it.sourceId == draggedId }
            } ?: -1
            val droppedIndex = timelines.indexOfFirst {
                it.sourceId == sourceId
            }

            val acceptedDrop = draggedIndex >= 0 && droppedIndex >= 0

            Snapshot.withMutableSnapshot {
                if (acceptedDrop) timelines.add(
                    index = droppedIndex,
                    element = timelines.removeAt(draggedIndex),
                )
                hoveredId = null
                draggedId = null
            }

            return acceptedDrop
        }

        override fun onEnded(event: DragAndDropEvent) {
            Snapshot.withMutableSnapshot {
                hoveredId = null
                draggedId = null
            }
        }
    }

    companion object {
        fun Modifier.tabDragAndDrop(
            state: DragAndDropTabsState,
            sourceId: String,
        ) = tabDragAndDropSource(sourceId)
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    event.draggedId() != null
                },
                target = state.children.getOrPut(sourceId) {
                    state.Child(sourceId)
                }.also { it.sourceId = sourceId },
            )
    }
}



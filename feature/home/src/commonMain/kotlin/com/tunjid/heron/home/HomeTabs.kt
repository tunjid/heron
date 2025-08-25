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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateBounds
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animate
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tunjid.composables.accumulatedoffsetnestedscrollconnection.AccumulatedOffsetNestedScrollConnection
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.home.TimelinePreferencesState.Companion.timelinePreferenceDragAndDrop
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.scaffold.ui.verticalOffsetProgress
import com.tunjid.heron.ui.Tab
import com.tunjid.heron.ui.Tabs
import com.tunjid.heron.ui.TabsState
import com.tunjid.heron.ui.TabsState.Companion.rememberTabsState
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import heron.feature.home.generated.resources.Res
import heron.feature.home.generated.resources.pinned
import heron.feature.home.generated.resources.saved
import heron.feature.home.generated.resources.timeline_preferences
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import org.jetbrains.compose.resources.stringResource
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

expect fun timelinePreferenceDragAndDropTransferData(title: String): DragAndDropTransferData

expect fun DragAndDropEvent.draggedId(): String?

expect fun Modifier.timelinePreferenceDragAndDropSource(sourceId: String): Modifier


@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun HomeTabs(
    modifier: Modifier = Modifier,
    tabLayout: TabLayout,
    isSignedIn: Boolean,
    currentSourceId: String?,
    saveRequestId: String?,
    timelines: List<Timeline.Home>,
    sharedTransitionScope: SharedTransitionScope,
    sourceIdsToHasUpdates: Map<String, Boolean>,
    selectedTabIndex: () -> Float,
    onCollapsedTabSelected: (Int) -> Unit,
    onCollapsedTabReselected: (Int) -> Unit,
    onExpandedTabSelected: (Int) -> Unit,
    onLayoutChanged: (TabLayout) -> Unit,
    onTimelinePresentationUpdated: (Int, Timeline.Presentation) -> Unit,
    onTimelinePreferencesSaved: (List<Timeline.Home>) -> Unit,
    onSettingsIconClick: () -> Unit
) = with(sharedTransitionScope) {
    val isExpanded = tabLayout is TabLayout.Expanded
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
        isCollapsed = tabLayout is TabLayout.Collapsed.Selected,
        selectedTabIndex = selectedTabIndex,
        onTabSelected = {
            onLayoutChanged(TabLayout.Collapsed.All)
            onCollapsedTabSelected(it)
        },
        onTabReselected = onCollapsedTabReselected,
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
        selectedTabIndex = selectedTabIndex,
        onTabSelected = onExpandedTabSelected,
        onTabReselected = onExpandedTabSelected,
    )
    Box(
        modifier = modifier
            .background(
                animateColorAsState(
                    if (isExpanded) MaterialTheme.colorScheme.surface
                    else Color.Transparent
                ).value
            ),
    ) {
        AnimatedContent(
            modifier = Modifier
                .animateContentSize(),
            targetState = tabLayout is TabLayout.Expanded,
        ) { isExpanded ->
            if (isExpanded) ExpandedTabs(
                saveRequestId = saveRequestId,
                timelines = timelines,
                tabsState = expandedTabsState,
                sharedTransitionScope = this@with,
                animatedContentScope = this@AnimatedContent,
                onDismissed = { onLayoutChanged(TabLayout.Collapsed.All) },
                onTimelinePreferencesSaved = onTimelinePreferencesSaved,
            )
            else CollapsedTabs(
                modifier = Modifier,
                tabsState = collapsedTabsState,
                sharedTransitionScope = this@with,
                animatedContentScope = this@AnimatedContent,
                currentSourceId = currentSourceId,
                timelines = timelines,
                onTimelinePresentationUpdated = onTimelinePresentationUpdated,
            )
        }
        if (isSignedIn) Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 8.dp,
                    end = 8.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1f),
                text = if (isExpanded) stringResource(Res.string.timeline_preferences) else "",
                style = MaterialTheme.typography.titleMediumEmphasized
            )

            AnimatedVisibility(
                visible = isExpanded,
            ) {
                SettingsIconButton(
                    onActionClick = {
                        onSettingsIconClick()
                    },
                )
            }

            ExpandButton(
                isExpanded = isExpanded,
                onToggled = {
                    onLayoutChanged(
                        if (isExpanded) TabLayout.Collapsed.All
                        else TabLayout.Expanded
                    )
                }
            )
        }
    }
}

@Composable
internal fun AccumulatedOffsetNestedScrollConnection.TabsCollapseEffect(
    layout: TabLayout,
    onCollapsed: (TabLayout.Collapsed) -> Unit,
) {
    LaunchedEffect(layout) {
        if (layout is TabLayout.Collapsed) snapshotFlow {
            verticalOffsetProgress() < 0.5f
        }
            .distinctUntilChanged()
            .collect { showAllTabs ->
                onCollapsed(
                    if (showAllTabs) TabLayout.Collapsed.All
                    else TabLayout.Collapsed.Selected
                )
            }
    }
}

@Composable
internal fun AccumulatedOffsetNestedScrollConnection.TabsExpansionEffect(
    isExpanded: Boolean,
) {
    val density = LocalDensity.current
    val expandedHeight = rememberUpdatedState(
        with(density) {
            (UiTokens.statusBarHeight + UiTokens.toolbarHeight).toPx()
        }
    )

    LaunchedEffect(isExpanded) {
        if (!isExpanded) return@LaunchedEffect

        var cumulative = 0f
        animate(
            initialValue = cumulative,
            targetValue = expandedHeight.value,
        ) { current, _ ->
            val delta = current - cumulative
            cumulative += delta
            onPreScroll(
                available = Offset(
                    x = 0f,
                    y = delta,
                ),
                source = NestedScrollSource.SideEffect,
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ExpandedTabs(
    saveRequestId: String?,
    timelines: List<Timeline.Home>,
    tabsState: TabsState,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    onDismissed: () -> Unit,
    onTimelinePreferencesSaved: (List<Timeline.Home>) -> Unit,
) = with(sharedTransitionScope) {
    val timelinePreferencesState = remember {
        timelines.toMutableStateList()
        TimelinePreferencesState(
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
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                timelinePreferencesState.timelines.forEachIndexed { index, timeline ->
                    if (index == 0) {
                        Spacer(Modifier.height(24.dp).fillMaxWidth())
                        SectionTitle(stringResource(Res.string.pinned))
                    } else if (index == timelinePreferencesState.firstUnpinnedIndex) {
                        Spacer(Modifier.height(24.dp).fillMaxWidth())
                        SectionTitle(stringResource(Res.string.saved))
                    }

                    key(timeline.sourceId) {
                        if (!timelinePreferencesState.isDraggedId(timeline.sourceId)) tabsState.ExpandedTab(
                            modifier = Modifier
                                .animateBounds(this@with),
                            timelinePreferencesState = timelinePreferencesState,
                            currentTimelines = timelines,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedContentScope = animatedContentScope,
                            timeline = timeline,
                        )
                    }
                }
            }
        }
    }

    val currentSaveRequestState = rememberUpdatedState(saveRequestId)
    LaunchedEffect(Unit) {
        snapshotFlow {
            currentSaveRequestState.value
        }
            .drop(1)
            .collectLatest { requestId ->
                if (requestId != null) onTimelinePreferencesSaved(
                    timelinePreferencesState.timelinesToSave()
                )
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
    onTimelinePresentationUpdated: (Int, Timeline.Presentation) -> Unit,
) = with(sharedTransitionScope) {
    val backgroundColor = MaterialTheme.colorScheme.surface
    val backgroundProgress = animateFloatAsState(if (tabsState.isCollapsed) 0f else 1f)
    Row(
        modifier = modifier
            .drawBehind {
                drawRect(
                    color = backgroundColor,
                    size = size.copy(width = size.width * backgroundProgress.value),
                )
            }
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .skipToLookaheadSize()
                .weight(1f)
                .clip(CircleShape),
        ) {
            Tabs(
                modifier = Modifier
                    .drawBehind {
                        val chipHeight = ChipHeight.toPx()
                        drawRoundRect(
                            color = backgroundColor,
                            topLeft = Offset(x = 0f, y = (size.height - chipHeight) / 2),
                            size = size.copy(height = chipHeight),
                            cornerRadius = CornerRadius(size.maxDimension, size.maxDimension)
                        )
                    }
                    .wrapContentWidth()
                    .animateContentSize(),
                tabsState = tabsState,
                tabContent = { tab ->
                    CollapsedTab(tab, sharedTransitionScope, animatedContentScope)
                }
            )
        }
        TimelinePresentationSelector(
            currentSourceId = currentSourceId,
            timelines = timelines,
            onTimelinePresentationUpdated = onTimelinePresentationUpdated,
        )
        // Space for the Expand Button
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
    timelinePreferencesState: TimelinePreferencesState,
    currentTimelines: List<Timeline.Home>,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    timeline: Timeline.Home,
) = with(sharedTransitionScope) {
    InputChip(
        modifier = modifier
            .skipToLookaheadSize()
            .timelinePreferenceDragAndDrop(
                state = timelinePreferencesState,
                sourceId = timeline.sourceId,
            ),
        shape = CircleShape,
        selected = timelinePreferencesState.isHoveredId(timeline.sourceId),
        onClick = click@{
            val index = currentTimelines.indexOfFirst { it.sourceId == timeline.sourceId }
            if (index >= 0) onTabSelected(index)
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
                modifier = Modifier
                    .clickable {
                        timelinePreferencesState.remove(timeline)
                    },
                imageVector = Icons.Rounded.Remove,
                contentDescription = "",
            )
        },
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun TabsState.CollapsedTab(
    tab: Tab,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope
) = with(sharedTransitionScope) {
    FilterChip(
        modifier = Modifier,
        shape = CollapsedTabShape,
        border = null,
        selected = false,
        onClick = click@{
            val index = tabList.indexOf(tab)
            if (index < 0) return@click

            if (index != selectedTabIndex().roundToInt()) onTabSelected(index)
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
        modifier = modifier
            // TODO: This offset is needed bc of some awkward behavior in chips
            .offset(y = 4.dp),
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
private fun SettingsIconButton(
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .offset(y = 4.dp),
        shape = CircleShape,
    ) {
        IconButton(
            onClick = {
                onActionClick()
            },
            modifier = Modifier
                .size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun SectionTitle(
    title: String
) {
    Text(
        modifier = Modifier
            .padding(
                vertical = 8.dp
            )
            .fillMaxWidth(),
        text = title,
        style = MaterialTheme.typography.titleSmallEmphasized
    )
}

@Composable
private fun TimelinePresentationSelector(
    modifier: Modifier = Modifier,
    currentSourceId: String?,
    timelines: List<Timeline>,
    onTimelinePresentationUpdated: (Int, Timeline.Presentation) -> Unit,
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
                onTimelinePresentationUpdated(
                    index,
                    presentation,
                )
            }
        )
    }
}

@Stable
private class TimelinePreferencesState(
    val timelines: SnapshotStateList<Timeline.Home>,
) {
    var hoveredId by mutableStateOf<String?>(null)
    var draggedId by mutableStateOf<String?>(null)

    var firstUnpinnedIndex by mutableStateOf(timelines.indexOfFirst { !it.isPinned })

    val children = mutableStateMapOf<String, Child>()

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
            b = 0
        )
    }

    fun timelinesToSave() = timelines.mapIndexed { index, timeline ->
        when (timeline) {
            is Timeline.Home.Feed -> timeline.copy(isPinned = index < firstUnpinnedIndex)
            is Timeline.Home.Following -> timeline.copy(isPinned = index < firstUnpinnedIndex)
            is Timeline.Home.List -> timeline.copy(isPinned = index < firstUnpinnedIndex)
        }
    }

    @Stable
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

            val acceptedDrop =
                // Make sure at least 1 item is always pinned
                if (firstUnpinnedIndex in draggedIndex..droppedIndex) firstUnpinnedIndex > 1
                else draggedIndex >= 0 && droppedIndex >= 0

            Snapshot.withMutableSnapshot {
                if (acceptedDrop) {
                    timelines.add(
                        index = droppedIndex,
                        element = timelines.removeAt(draggedIndex),
                    )
                    when (firstUnpinnedIndex) {
                        // Moved out of pinned items
                        in draggedIndex..droppedIndex -> firstUnpinnedIndex = max(
                            a = firstUnpinnedIndex - 1,
                            b = 0,
                        )
                        // Moved into pinned items
                        in droppedIndex..draggedIndex -> firstUnpinnedIndex = min(
                            a = firstUnpinnedIndex + 1,
                            b = timelines.lastIndex,
                        )
                    }
                }
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
        fun Modifier.timelinePreferenceDragAndDrop(
            state: TimelinePreferencesState,
            sourceId: String,
        ) = timelinePreferenceDragAndDropSource(sourceId)
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

private val CollapsedTabShape = RoundedCornerShape(16.dp)
private val ChipHeight = 32.dp
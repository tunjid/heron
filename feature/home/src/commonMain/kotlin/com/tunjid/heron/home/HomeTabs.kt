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
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateBounds
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Bookmark
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
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.uri
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.home.ui.EditableTimelineState
import com.tunjid.heron.home.ui.EditableTimelineState.Companion.rememberEditableTimelineState
import com.tunjid.heron.home.ui.EditableTimelineState.Companion.timelineEditDragAndDrop
import com.tunjid.heron.home.ui.EditableTimelineState.Companion.timelineEditDropTarget
import com.tunjid.heron.home.ui.ExpandableTabsState
import com.tunjid.heron.home.ui.ExpandableTabsState.Companion.expandable
import com.tunjid.heron.home.ui.ExpandableTabsState.Companion.rememberExpandableTabsState
import com.tunjid.heron.home.ui.JiggleBox
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.ui.TimelinePresentationSelector
import com.tunjid.heron.ui.Tab
import com.tunjid.heron.ui.Tabs
import com.tunjid.heron.ui.TabsState
import com.tunjid.heron.ui.TabsState.Companion.rememberTabsState
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import heron.feature.home.generated.resources.Res
import heron.feature.home.generated.resources.bookmark
import heron.feature.home.generated.resources.collapse_timeline_settings
import heron.feature.home.generated.resources.expand_timeline_settings
import heron.feature.home.generated.resources.pinned
import heron.feature.home.generated.resources.saved
import heron.feature.home.generated.resources.settings
import heron.feature.home.generated.resources.timeline_drop_target_hint
import heron.feature.home.generated.resources.timeline_preferences
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import org.jetbrains.compose.resources.stringResource

expect fun timelineEditDragAndDropTransferData(title: String): DragAndDropTransferData

expect fun DragAndDropEvent.draggedId(): String?

expect fun Modifier.timelineEditDragAndDropSource(sourceId: String): Modifier

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun HomeTabs(
    modifier: Modifier = Modifier,
    tabLayout: TabLayout,
    isSignedIn: Boolean,
    currentTabUri: Uri?,
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
    onSettingsIconClick: () -> Unit,
    onBookmarkIconClick: () -> Unit,
) = with(sharedTransitionScope) {
    val expandableTabsState = rememberExpandableTabsState(
        tabLayout = tabLayout,
        onTabLayoutChanged = onLayoutChanged,
    )
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
            .fillMaxSize(),
    ) {
        val saveableStateHolder = rememberSaveableStateHolder()
        expandableTabsState.transition.AnimatedContent(
            modifier = Modifier,
            transitionSpec = {
                if (targetState) ExpandableTabsExpansionTransition
                else ExpandableTabsCollapseTransition
            },
        ) { isExpanding ->
            saveableStateHolder.SaveableStateProvider(isExpanding) {
                if (isExpanding) ExpandedTabs(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = ExpandedTabsShape,
                        )
                        .expandable(expandableTabsState),
                    saveRequestId = saveRequestId,
                    timelines = timelines,
                    tabsState = expandedTabsState,
                    sharedTransitionScope = this@with,
                    animatedContentScope = this@AnimatedContent,
                    onDismissed = { onLayoutChanged(TabLayout.Collapsed.All) },
                    onTimelinePreferencesSaved = onTimelinePreferencesSaved,
                )
                else CollapsedTabs(
                    modifier = Modifier
                        .fillMaxWidth()
                        .expandable(expandableTabsState),
                    tabsState = collapsedTabsState,
                    sharedTransitionScope = this@with,
                    animatedContentScope = this@AnimatedContent,
                    currentTabUri = currentTabUri,
                    timelines = timelines,
                    onTimelinePresentationUpdated = onTimelinePresentationUpdated,
                )
            }
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
            val alphaModifier = remember {
                Modifier.graphicsLayer {
                    alpha = expandableTabsState.expansionProgress
                }
            }
            Text(
                modifier = Modifier
                    .then(alphaModifier)
                    .padding(horizontal = 8.dp),
                text = stringResource(Res.string.timeline_preferences),
                style = MaterialTheme.typography.titleMediumEmphasized,
            )
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .animateContentSize(),
            )
            if (expandableTabsState.isPartiallyOrFullyExpanded) {
                HomeTimelineButton(
                    modifier = alphaModifier,
                    onActionClick = onSettingsIconClick,
                    icon = Icons.Rounded.Settings,
                    iconDescription = stringResource(Res.string.settings),
                )
                HomeTimelineButton(
                    modifier = alphaModifier,
                    onActionClick = onBookmarkIconClick,
                    icon = Icons.Rounded.Bookmark,
                    iconDescription = stringResource(Res.string.bookmark),
                )
            }
            HomeTimelineButton(
                modifier = Modifier
                    .renderInSharedTransitionScopeOverlay(
                        zIndexInOverlay = ExpandButtonSharedElementZIndex,
                    )
                    .graphicsLayer {
                        rotationZ = expandableTabsState.expansionProgress * 180f
                    },
                icon = Icons.Rounded.ArrowDropDown,
                iconDescription = stringResource(
                    if (expandableTabsState.isPartiallyOrFullyExpanded) Res.string.collapse_timeline_settings
                    else Res.string.expand_timeline_settings,
                ),
                onActionClick = {
                    onLayoutChanged(
                        if (expandableTabsState.isPartiallyOrFullyExpanded) TabLayout.Collapsed.All
                        else TabLayout.Expanded,
                    )
                },
            )
        }
    }
}

@Composable
private fun ExpandedTabs(
    modifier: Modifier = Modifier,
    saveRequestId: String?,
    timelines: List<Timeline.Home>,
    tabsState: TabsState,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    onDismissed: () -> Unit,
    onTimelinePreferencesSaved: (List<Timeline.Home>) -> Unit,
) = with(sharedTransitionScope) {
    val editableTimelineState = rememberEditableTimelineState(
        timelines = timelines,
    )
    Box(
        modifier = modifier
            .fillMaxSize(),
    ) {
        FlowRow(
            modifier = with(animatedContentScope) {
                Modifier
                    .animateEnterExit(
                        enter = ExpandedTabsContentEnterAnimation,
                    )
            }
                .fillMaxSize()
                .padding(
                    top = 40.dp,
                    start = 16.dp,
                    end = 16.dp,
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismissed,
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val (pinned, saved) = remember(
                editableTimelineState.firstUnpinnedIndex,
                editableTimelineState.timelines.toList(),
            ) {
                val allTimelines = editableTimelineState.timelines
                val index = editableTimelineState.firstUnpinnedIndex

                if (index < 0) allTimelines.toList() to emptyList<Timeline.Home>()
                else allTimelines.subList(0, index) to allTimelines.subList(index, allTimelines.size)
            }

            key(Res.string.pinned) {
                SectionTitle(
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .animateBounds(this@with),
                    title = stringResource(Res.string.pinned),
                )
            }
            pinned.forEach { timeline ->
                key(timeline.sourceId) {
                    if (!editableTimelineState.isDraggedId(timeline.sourceId)) tabsState.ExpandedTab(
                        modifier = Modifier
                            .animateBounds(this@with),
                        editableTimelineState = editableTimelineState,
                        currentTimelines = timelines,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedContentScope = animatedContentScope,
                        timeline = timeline,
                    )
                }
            }
            key(Res.string.saved) {
                SectionTitle(
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .animateBounds(this@with),
                    title = stringResource(Res.string.saved),
                )
            }
            saved.forEach { timeline ->
                key(timeline.sourceId) {
                    if (!editableTimelineState.isDraggedId(timeline.sourceId)) tabsState.ExpandedTab(
                        modifier = Modifier
                            .animateBounds(this@with),
                        editableTimelineState = editableTimelineState,
                        currentTimelines = timelines,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedContentScope = animatedContentScope,
                        timeline = timeline,
                    )
                }
            }
            AnimatedVisibility(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                visible = editableTimelineState.shouldShowHint,
            ) {
                DropTargetBox(
                    modifier = Modifier
                        .timelineEditDropTarget(
                            state = editableTimelineState,
                        )
                        .padding(horizontal = 8.dp)
                        .fillMaxWidth(),
                    isHovered = editableTimelineState.isHintHovered,
                )
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
                    editableTimelineState.timelinesToSave(),
                )
            }
    }
}

@Composable
private fun CollapsedTabs(
    modifier: Modifier = Modifier,
    tabsState: TabsState,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    currentTabUri: Uri?,
    timelines: List<Timeline>,
    onTimelinePresentationUpdated: (Int, Timeline.Presentation) -> Unit,
) {
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
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(CircleShape),
        ) {
            Tabs(
                modifier = Modifier
                    .chipBackground { backgroundColor }
                    .wrapContentWidth()
                    .animateContentSize(),
                tabsState = tabsState,
                tabContent = { tab ->
                    CollapsedTab(tab, sharedTransitionScope, animatedContentScope)
                },
            )
        }
        TimelinePresentationSelector(
            currentTabUri = currentTabUri,
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

@Composable
private fun TabsState.ExpandedTab(
    modifier: Modifier = Modifier,
    editableTimelineState: EditableTimelineState,
    currentTimelines: List<Timeline.Home>,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    timeline: Timeline.Home,
) = with(sharedTransitionScope) {
    JiggleBox {
        val isHovered = editableTimelineState.isHoveredId(timeline.sourceId)
        InputChip(
            modifier = modifier
                .chipBackground(
                    animateColorAsState(
                        if (isHovered) TabsState.TabBackgroundColor
                        else Color.Transparent,
                    ).let { it::value },
                )
                .skipToLookaheadSize()
                .timelineEditDragAndDrop(
                    state = editableTimelineState,
                    sourceId = timeline.sourceId,
                ),
            shape = CircleShape,
            selected = false,
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
                    },
                )
            },
            label = {
                Text(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .sharedElement(
                            sharedContentState = sharedTransitionScope.rememberSharedContentState(
                                timeline.name,
                            ),
                            animatedVisibilityScope = animatedContentScope,
                            boundsTransform = ExpandableTabsBoundsTransform,
                            zIndexInOverlay = TabsSharedElementZIndex,
                        ),
                    text = timeline.name,
                    maxLines = 1,
                )
            },
            trailingIcon = {
                Icon(
                    modifier = Modifier
                        .clickable {
                            editableTimelineState.remove(timeline)
                        },
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = "",
                )
            },
        )
    }
}

@Composable
private fun TabsState.CollapsedTab(
    tab: Tab,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
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
                            tab.title,
                        ),
                        animatedVisibilityScope = animatedContentScope,
                        boundsTransform = ExpandableTabsBoundsTransform,
                        zIndexInOverlay = TabsSharedElementZIndex,
                    ),
                text = tab.title,
            )
        },
    )
}

@Composable
private fun DropTargetBox(
    modifier: Modifier = Modifier,
    isHovered: Boolean,
) {
    Box(
        modifier = modifier
            .roundedBorder(
                isStroked = true,
                cornerRadius = 8::dp,
                borderColor = animateColorAsState(
                    if (isHovered) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                ).let { it::value },
                strokeWidth = animateDpAsState(
                    if (isHovered) 4.dp
                    else Dp.Hairline,
                ).let { it::value },
            ),
    ) {
        Text(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 16.dp),
            text = stringResource(Res.string.timeline_drop_target_hint),
        )
    }
}

@Composable
private fun HomeTimelineButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconDescription: String,
    onActionClick: () -> Unit,
) {
    ElevatedCard(
        modifier = modifier
            .padding(horizontal = 4.dp),
        shape = CircleShape,
    ) {
        IconButton(
            onClick = {
                onActionClick()
            },
            modifier = Modifier
                .size(40.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = iconDescription,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun SectionTitle(
    modifier: Modifier = Modifier,
    title: String,
) {
    Text(
        modifier = modifier
            .padding(
                vertical = 8.dp,
            )
            .fillMaxWidth(),
        text = title,
        style = MaterialTheme.typography.titleSmallEmphasized,
    )
}

@Composable
private fun TimelinePresentationSelector(
    modifier: Modifier = Modifier,
    currentTabUri: Uri?,
    timelines: List<Timeline>,
    onTimelinePresentationUpdated: (Int, Timeline.Presentation) -> Unit,
) {
    val timeline = timelines.firstOrNull {
        it.uri == currentTabUri
    }
    if (timeline != null) Row(
        modifier = modifier.wrapContentWidth(),
        horizontalArrangement = Arrangement.aligned(Alignment.End),
    ) {
        TimelinePresentationSelector(
            selected = timeline.presentation,
            available = timeline.supportedPresentations,
            onPresentationSelected = { presentation ->
                val index = timelines.indexOfFirst {
                    it.uri == currentTabUri
                }
                onTimelinePresentationUpdated(
                    index,
                    presentation,
                )
            },
        )
    }
}

private fun Modifier.roundedBorder(
    isStroked: Boolean,
    borderColor: () -> Color,
    cornerRadius: () -> Dp,
    strokeWidth: () -> Dp,
) = drawWithCache {
    val style = Stroke(
        width = strokeWidth().toPx(),
        pathEffect =
        if (isStroked) PathEffect.dashPathEffect(
            intervals = floatArrayOf(10f, 10f), // Dash length and gap length
            phase = 0f, // Optional: offset for the dash pattern
        )
        else null,
    )
    onDrawBehind {
        val radius = cornerRadius()
        drawRoundRect(
            cornerRadius = CornerRadius(
                x = radius.toPx(),
                y = radius.toPx(),
            ),
            color = borderColor(),
            style = style,
        )
    }
}

private fun Modifier.chipBackground(
    backgroundColor: () -> Color,
) = drawBehind {
    val chipHeight = ChipHeight.toPx()
    drawRoundRect(
        color = backgroundColor(),
        topLeft = Offset(x = 0f, y = (size.height - chipHeight) / 2),
        size = size.copy(height = chipHeight),
        cornerRadius = CornerRadius(size.maxDimension, size.maxDimension),
    )
}

private val ExpandableTabsBoundsTransform = BoundsTransform { _, _ ->
    spring(stiffness = Spring.StiffnessLow)
}

private val ExpandableTabsCollapseTransition =
    fadeIn(
        animationSpec = ExpandableTabsState.FloatAnimationSpec,
    ) togetherWith slideOutVertically(
        animationSpec = ExpandableTabsState.IntOffsetAnimationSpec,
    )

private val ExpandableTabsExpansionTransition =
    slideInVertically(
        animationSpec = ExpandableTabsState.IntOffsetAnimationSpec,
    ) togetherWith fadeOut(
        animationSpec = ExpandableTabsState.FloatAnimationSpec,
    )

private val ExpandedTabsContentEnterAnimation =
    fadeIn(
        animationSpec = ExpandableTabsState.FloatAnimationSpec,
    ) + scaleIn(
        initialScale = 0.92f,
        animationSpec = tween(
            durationMillis = 220,
            delayMillis = 90,
        ),
    )

private val CollapsedTabShape = RoundedCornerShape(16.dp)

private val ExpandedTabsShape = RoundedCornerShape(
    bottomStart = 16.dp,
    bottomEnd = 16.dp,
)

private val ChipHeight = 32.dp

private const val TabsSharedElementZIndex = 1f
private const val ExpandButtonSharedElementZIndex = 2f

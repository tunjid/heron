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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.sourceId
import com.tunjid.heron.data.core.models.uri
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.home.ui.EditableTimelineState
import com.tunjid.heron.home.ui.EditableTimelineState.Companion.rememberEditableTimelineState
import com.tunjid.heron.home.ui.EditableTimelineState.Companion.timelineEditDragAndDrop
import com.tunjid.heron.home.ui.EditableTimelineState.Companion.timelineEditDropTarget
import com.tunjid.heron.home.ui.ExpandableTabsState
import com.tunjid.heron.home.ui.ExpandableTabsState.Companion.expandable
import com.tunjid.heron.home.ui.shouldRenderAppBarButtonsInOverlay
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.ui.TimelinePresentationSelector
import com.tunjid.heron.ui.AppBarButton
import com.tunjid.heron.ui.JiggleBox
import com.tunjid.heron.ui.PaneTransitionScope
import com.tunjid.heron.ui.Tab
import com.tunjid.heron.ui.Tabs
import com.tunjid.heron.ui.TabsState
import com.tunjid.heron.ui.TabsState.Companion.rememberTabsState
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.fillMaxRestrictedWidth
import com.tunjid.heron.ui.modifiers.chipBackground
import com.tunjid.heron.ui.modifiers.roundedBorder
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.text.CommonStrings
import heron.feature.home.generated.resources.Res
import heron.feature.home.generated.resources.bookmark
import heron.feature.home.generated.resources.collapse_timeline_settings
import heron.feature.home.generated.resources.expand_timeline_settings
import heron.feature.home.generated.resources.pinned
import heron.feature.home.generated.resources.saved
import heron.feature.home.generated.resources.settings
import heron.feature.home.generated.resources.timeline_drop_target_hint
import heron.feature.home.generated.resources.timeline_preferences
import heron.ui.core.generated.resources.feed_generator_create
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun HomeTabs(
    modifier: Modifier = Modifier,
    tabLayout: TabLayout,
    isSignedIn: Boolean,
    isOffset: Boolean,
    currentTabUri: Uri?,
    saveRequestId: String?,
    timelines: List<Timeline.Home>,
    paneTransitionScope: PaneTransitionScope,
    expandableTabsState: ExpandableTabsState,
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
    onCreateFeedClicked: () -> Unit,
) = with(paneTransitionScope) {
    val collapsedTabsState = rememberTabsState(
        tabs = remember(sourceIdsToHasUpdates, timelines) {
            timelines
                .filter(Timeline.Home::isPinned)
                .map { timeline ->
                    Tab(
                        title = timeline.name,
                        id = timeline.sourceId,
                        hasUpdate = sourceIdsToHasUpdates[timeline.sourceId] == true,
                    )
                }
        },
        isCollapsed = tabLayout is TabLayout.Collapsed.Selected && !expandableTabsState.isPartiallyOrFullyExpanded,
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
                    id = timeline.sourceId,
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
                            color = MaterialTheme.colorScheme.surface.copy(
                                alpha = ExpandableTabsState.BackgroundAlpha,
                            ),
                            shape = ExpandableTabsState.Shape,
                        )
                        .expandable(expandableTabsState),
                    saveRequestId = saveRequestId,
                    timelines = timelines,
                    tabsState = expandedTabsState,
                    sharedTransitionScope = this@with,
                    animatedContentScope = this@AnimatedContent,
                    onCreateFeedClicked = onCreateFeedClicked,
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
                    isSignedIn = isSignedIn,
                    currentTabUri = currentTabUri,
                    timelines = timelines,
                    onTimelinePresentationUpdated = onTimelinePresentationUpdated,
                )
            }
        }
        val extraPadding by animateDpAsState(
            targetValue = if (isOffset) 8.dp else 0.dp,
            animationSpec = TabButtonPaddingAnimationSpec,
        )

        if (isSignedIn) Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    // Inset horizontally by 8 dp so it doesn't touch timeline cards
                    horizontal = 8.dp + extraPadding,
                    vertical = 4.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val alphaModifier = remember {
                Modifier.graphicsLayer {
                    alpha = expandableTabsState.expansionProgress
                }
            }
            Text(
                modifier = Modifier
                    .then(alphaModifier)
                    .padding(horizontal = 8.dp)
                    .offset {
                        IntOffset(
                            x = -(expandableTabsState.expansionProgress * 8.dp).roundToPx(),
                            y = 0,
                        )
                    },
                text = stringResource(Res.string.timeline_preferences),
                style = MaterialTheme.typography.titleMediumEmphasized,
            )
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .animateContentSize(),
            )
            if (expandableTabsState.isPartiallyOrFullyExpanded) {
                val expandedOptionsModifier = remember {
                    Modifier
                        .renderInSharedTransitionScopeOverlay(
                            zIndexInOverlay = HomeTimelineButtonSharedElementZIndex,
                            renderInOverlay = expandableTabsState::shouldRenderAppBarButtonsInOverlay,
                        )
                        .then(alphaModifier)
                }
                AppBarButton(
                    modifier = expandedOptionsModifier,
                    onClick = onSettingsIconClick,
                    colors = TabButtonColors,
                    icon = Icons.Rounded.Settings,
                    iconDescription = stringResource(Res.string.settings),
                )
                AppBarButton(
                    modifier = expandedOptionsModifier,
                    onClick = onBookmarkIconClick,
                    colors = TabButtonColors,
                    icon = Icons.Rounded.Bookmark,
                    iconDescription = stringResource(Res.string.bookmark),
                )
            }
            AppBarButton(
                modifier = Modifier
                    .renderInSharedTransitionScopeOverlay(
                        zIndexInOverlay = HomeTimelineButtonSharedElementZIndex,
                        renderInOverlay = expandableTabsState::shouldRenderAppBarButtonsInOverlay,
                    )
                    .graphicsLayer {
                        rotationZ = expandableTabsState.expansionProgress * 180f
                    },
                colors = TabButtonColors,
                icon = Icons.Rounded.ArrowDropDown,
                iconDescription = stringResource(
                    if (expandableTabsState.isPartiallyOrFullyExpanded) Res.string.collapse_timeline_settings
                    else Res.string.expand_timeline_settings,
                ),
                onClick = {
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
    sharedTransitionScope: PaneTransitionScope,
    animatedContentScope: AnimatedContentScope,
    onDismissed: () -> Unit,
    onTimelinePreferencesSaved: (List<Timeline.Home>) -> Unit,
    onCreateFeedClicked: () -> Unit,
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
                    .align(Alignment.Center)
                    .animateEnterExit(
                        enter = ExpandedTabsContentEnterAnimation,
                    )
            }
                .fillMaxHeight()
                .fillMaxRestrictedWidth()
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
            val (pinned, saved) = editableTimelineState.partitioned

            key(Res.string.pinned) {
                SectionTitle(
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .animateBounds(
                            lookaheadScope = this@with,
                            boundsTransform = childBoundsTransform,
                        ),
                    title = stringResource(Res.string.pinned),
                )
            }
            pinned.forEach { timeline ->
                key(timeline.sourceId) {
                    if (!editableTimelineState.isDraggedId(timeline.sourceId)) tabsState.ExpandedTab(
                        modifier = Modifier
                            .animateBounds(
                                lookaheadScope = this@with,
                                boundsTransform = childBoundsTransform,
                            ),
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
                        .animateBounds(
                            lookaheadScope = this@with,
                            boundsTransform = childBoundsTransform,
                        ),
                    title = stringResource(Res.string.saved),
                )
            }
            saved.forEach { timeline ->
                key(timeline.sourceId) {
                    if (!editableTimelineState.isDraggedId(timeline.sourceId)) tabsState.ExpandedTab(
                        modifier = Modifier
                            .animateBounds(
                                lookaheadScope = this@with,
                                boundsTransform = childBoundsTransform,
                            ),
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

            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .animateBounds(
                        lookaheadScope = this@with,
                        boundsTransform = childBoundsTransform,
                    ),
            ) {
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = onCreateFeedClicked,
                    content = {
                        Text(stringResource(CommonStrings.feed_generator_create))
                    },
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
    sharedTransitionScope: PaneTransitionScope,
    animatedContentScope: AnimatedContentScope,
    isSignedIn: Boolean,
    currentTabUri: Uri?,
    timelines: List<Timeline>,
    onTimelinePresentationUpdated: (Int, Timeline.Presentation) -> Unit,
) {
    val backgroundColor = MaterialTheme.colorScheme.surface
    val backgroundProgress = animateFloatAsState(
        targetValue = if (tabsState.isCollapsed) 0f else 1f,
    )
    Row(
        modifier = modifier
            .drawBehind {
                drawRect(
                    color = backgroundColor,
                    size = size.copy(width = size.width * backgroundProgress.value),
                )
            }
            // 8 dp would align perfectly with the scrolling content.
            // Inset it by 8 dp.
            .padding(horizontal = 16.dp),
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
        if (isSignedIn) Spacer(
            modifier = Modifier
                .width(UiTokens.appBarButtonSize),
        )
    }
}

@Composable
private fun TabsState.ExpandedTab(
    modifier: Modifier = Modifier,
    editableTimelineState: EditableTimelineState,
    currentTimelines: List<Timeline.Home>,
    sharedTransitionScope: PaneTransitionScope,
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
                    )::value,
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
                                timeline.sourceId,
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
    sharedTransitionScope: PaneTransitionScope,
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
                            tab.id,
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
            colors = TabButtonColors,
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
        initialOffsetY = { -it },
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

private val TabButtonPaddingAnimationSpec = tween<Dp>(
    durationMillis = 400,
)

private val CollapsedTabShape = RoundedCornerShape(16.dp)

private val TabButtonColors
    @Composable
    get() = CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )

private const val TabsSharedElementZIndex = 1f
private const val HomeTimelineButtonSharedElementZIndex = 2f

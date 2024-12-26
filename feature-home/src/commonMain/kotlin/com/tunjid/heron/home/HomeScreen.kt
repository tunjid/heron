/*
 * Copyright 2024 Adetunji Dahunsi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.heron.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.domain.timeline.TimelineLoadAction
import com.tunjid.heron.domain.timeline.TimelineStateHolder
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.scaffold.SharedElementScope
import com.tunjid.heron.timeline.ui.TimelineItem
import com.tunjid.heron.timeline.ui.avatarSharedElementKey
import com.tunjid.heron.timeline.ui.tabs.TimelineTabs
import com.tunjid.tiler.compose.PivotedTilingEffect
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@Composable
internal fun HomeScreen(
    sharedElementScope: SharedElementScope,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val updatedTimelineIdsToTimelineStates by rememberUpdatedState(
        state.timelineIdsToTimelineStates
    )
    val updatedPages by remember {
        derivedStateOf { updatedTimelineIdsToTimelineStates.entries.toList() }
    }

    val pagerState = rememberPagerState {
        updatedPages.size
    }
    Box(
        modifier = modifier
    ) {
        var tabsVisible by remember { mutableStateOf(true) }

        HorizontalPager(
            modifier = Modifier,
            state = pagerState,
            key = { page -> updatedPages[page].key },
            pageContent = { page ->
                val timelineStateHolder = remember { updatedPages[page].value }
                HomeTimeline(
                    sharedElementScope = sharedElementScope,
                    actions = actions,
                    timelineStateHolder = timelineStateHolder,
                    onScrolledForward = { tabsVisible = false },
                    onScrolledBackward = { tabsVisible = true },
                )
            }
        )
        HomeTabs(
            pagerState = pagerState,
            visible = tabsVisible,
            titles = remember(state.timelines) {
                state.timelines.map { it.name }
            }
        )
    }
}

@Composable
private fun HomeTabs(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    titles: List<String>,
    visible: Boolean,
) {
    val scope = rememberCoroutineScope()
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
    ) {
        TimelineTabs(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp)
                .fillMaxWidth(),
            titles = titles,
            selectedTabIndex = pagerState.currentPage + pagerState.currentPageOffsetFraction,
            onTabSelected = {
                scope.launch {
                    pagerState.animateScrollToPage(it)
                }
            }
        )
    }
}

@Composable
private fun HomeTimeline(
    sharedElementScope: SharedElementScope,
    timelineStateHolder: TimelineStateHolder,
    actions: (Action) -> Unit,
    onScrolledForward: () -> Unit,
    onScrolledBackward: () -> Unit,
) {

    val gridState = rememberLazyStaggeredGridState()
    val timelineState by timelineStateHolder.state.collectAsStateWithLifecycle()
    val items by rememberUpdatedState(timelineState.items)

    LazyVerticalStaggeredGrid(
        modifier = Modifier
            .fillMaxSize(),
        state = gridState,
        columns = StaggeredGridCells.Adaptive(340.dp),
        verticalItemSpacing = 8.dp,
        contentPadding = PaddingValues(
            top = 48.dp,
            start = 8.dp,
            end = 8.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = !sharedElementScope.isTransitionActive,
    ) {
        items(
            items = items,
            key = TimelineItem::id,
            itemContent = { item ->
                TimelineItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                    movableSharedElementScope = sharedElementScope,
                    animatedVisibilityScope = sharedElementScope,
                    now = remember { Clock.System.now() },
                    sharedElementPrefix = timelineState.timeline.sourceId,
                    item = item,
                    onPostClicked = { post ->
                        actions(
                            Action.Navigate.DelegateTo(
                                NavigationAction.Common.ToPost(
                                    referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                                    sharedElementPrefix = timelineState.timeline.sourceId,
                                    post = post,
                                )
                            )
                        )
                    },
                    onProfileClicked = { profile ->
                        actions(
                            Action.Navigate.DelegateTo(
                                NavigationAction.Common.ToProfile(
                                    referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                                    profile = profile,
                                    avatarSharedElementKey = this?.avatarSharedElementKey(
                                        prefix = timelineState.timeline.sourceId,
                                    )
                                )
                            )
                        )
                    },
                    onImageClicked = {},
                    onReplyToPost = {},
                )
            }
        )
    }

    gridState.PivotedTilingEffect(
        items = items,
        onQueryChanged = { query ->
            timelineStateHolder.accept(
                TimelineLoadAction.LoadAround(query ?: timelineState.currentQuery)
            )
        }
    )

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.lastScrolledForward }
            .collect { if (it) onScrolledForward() }
    }

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.lastScrolledBackward }
            .collect { if (it) onScrolledBackward() }

    }
}

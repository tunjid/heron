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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.domain.timeline.TimelineLoadAction
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.scaffold.SharedElementScope
import com.tunjid.heron.timeline.ui.TimelineItem
import com.tunjid.heron.timeline.ui.avatarSharedElementKey
import com.tunjid.tiler.compose.PivotedTilingEffect
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
    HorizontalPager(
        modifier = modifier,
        state = pagerState,
        key = { page -> updatedPages[page].key },
        pageContent = { page ->

            val timelineStateHolder = remember { updatedPages[page].value }
            val timelineState by timelineStateHolder.state.collectAsStateWithLifecycle()

            val gridState = rememberLazyStaggeredGridState()
            val items by rememberUpdatedState(timelineState.items)

            LazyVerticalStaggeredGrid(
                modifier = Modifier
                    .fillMaxSize(),
                state = gridState,
                columns = StaggeredGridCells.Adaptive(340.dp),
                verticalItemSpacing = 8.dp,
                contentPadding = PaddingValues(horizontal = 8.dp),
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
        }
    )
}


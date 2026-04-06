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

package com.tunjid.heron.standard.publication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tunjid.composables.collapsingheader.CollapsingHeaderLayout
import com.tunjid.composables.collapsingheader.rememberCollapsingHeaderState
import com.tunjid.heron.data.core.models.link
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.paneClip
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.timeline.ui.DismissableRefreshIndicator
import com.tunjid.heron.timeline.ui.standard.Document
import com.tunjid.heron.ui.UiTokens.bottomNavAndInsetPaddingValues
import com.tunjid.heron.ui.modifiers.gridColumnCount
import com.tunjid.tiler.compose.PivotedTilingEffect
import kotlin.math.roundToInt

@Composable
internal fun StandardPublicationScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val items by rememberUpdatedState(state.tilingData.items)
    val gridState = rememberLazyStaggeredGridState()
    val uriHandler = LocalUriHandler.current

    val collapsingHeaderState = rememberCollapsingHeaderState(
        collapsedHeight = 0f,
        initialExpandedHeight = with(density) { 200.dp.toPx() },
    )

    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        modifier = modifier
            .fillMaxSize()
            .paneClip(),
        isRefreshing = state.isRefreshing,
        state = pullToRefreshState,
        onRefresh = {
            actions(Action.Tile(TilingState.Action.Refresh))
        },
        indicator = {
            DismissableRefreshIndicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset {
                        IntOffset(x = 0, y = collapsingHeaderState.expandedHeight.roundToInt())
                    },
                state = pullToRefreshState,
                isRefreshing = state.isRefreshing,
                onDismissRequest = {},
            )
        },
    ) {
        CollapsingHeaderLayout(
            modifier = Modifier
                .fillMaxSize(),
            state = collapsingHeaderState,
            headerContent = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset {
                            IntOffset(
                                x = 0,
                                y = -collapsingHeaderState.translation.roundToInt(),
                            )
                        }
                        .padding(
                            horizontal = 16.dp,
                            vertical = 8.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.publication?.description
                        ?.takeIf(String::isNotBlank)
                        ?.let { description ->
                            Text(
                                text = description,
                            )
                        }
                }
            },
            body = {
                LazyVerticalStaggeredGrid(
                    modifier = Modifier
                        .fillMaxSize()
                        .paneClip()
                        .gridColumnCount(density) { numColumns ->
                            actions(
                                Action.Tile(TilingState.Action.GridSize(numColumns = numColumns)),
                            )
                        },
                    state = gridState,
                    columns = StaggeredGridCells.Adaptive(360.dp),
                    verticalItemSpacing = 8.dp,
                    contentPadding = bottomNavAndInsetPaddingValues(
                        isCompact = paneScaffoldState.prefersCompactBottomNav,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    userScrollEnabled = !paneScaffoldState.isTransitionActive,
                ) {
                    items(
                        items = items,
                        key = { it.uri.uri },
                        itemContent = { document ->
                            Document(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()
                                    .clickable {
                                        document.link?.let { link ->
                                            runCatching { uriHandler.openUri(link) }
                                        }
                                    },
                                document = document,
                            )
                        },
                    )
                }
            },
        )
    }

    gridState.PivotedTilingEffect(
        items = items,
        onQueryChanged = { query ->
            actions(
                Action.Tile(
                    TilingState.Action.LoadAround(
                        query = query ?: state.tilingData.currentQuery,
                    ),
                ),
            )
        },
    )
}

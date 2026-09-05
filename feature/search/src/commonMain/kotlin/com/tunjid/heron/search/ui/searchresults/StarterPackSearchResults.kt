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

package com.tunjid.heron.search.ui.searchresults

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.StarterPack
import com.tunjid.heron.search.SearchScreenStateHolders
import com.tunjid.heron.search.SearchState
import com.tunjid.heron.search.sharedElementPrefix
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.tiledItems
import com.tunjid.heron.timeline.ui.list.StarterPack
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.UiTokens.bottomNavAndInsetPaddingValues
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import com.tunjid.mutator.compose.produceStateWithLifecycle
import com.tunjid.tiler.compose.PivotedTilingEffect

@Composable
internal fun StarterPackSearchResults(
    stateHolder: SearchScreenStateHolders.StarterPacks,
    listState: LazyListState,
    modifier: Modifier,
    paneScaffoldState: PaneScaffoldState,
    onStarterPackClicked: (StarterPack, String) -> Unit,
) {
    val state = stateHolder.produceStateWithLifecycle() as SearchState.OfStarterPacks
    LazyColumn(
        modifier = modifier,
        state = listState,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = bottomNavAndInsetPaddingValues(
            top = UiTokens.statusBarHeight + UiTokens.toolbarHeight + UiTokens.tabsHeight,
            isCompact = paneScaffoldState.prefersCompactBottomNav,
        ),
    ) {
        items(
            items = state.tiledItems,
            key = { it.starterPack.cid.id },
            itemContent = { result ->
                StarterPack(
                    modifier = Modifier
                        .clip(StarterPackSearchResultShape)
                        .clickable {
                            onStarterPackClicked(
                                result.starterPack,
                                state.sharedElementPrefix,
                            )
                        }
                        .padding(8.dp)
                        .animateItem(),
                    paneTransitionScope = paneScaffoldState,
                    sharedElementPrefix = state.sharedElementPrefix,
                    starterPack = result.starterPack,
                )
            },
        )
    }
    listState.PivotedTilingEffect(
        items = state.tiledItems,
        onQueryChanged = { query ->
            stateHolder.accept(
                SearchState.Tile(
                    tilingAction = TilingState.Action.LoadAround(
                        query ?: state.tilingData.currentQuery,
                    ),
                ),
            )
        },
    )
}

private val StarterPackSearchResultShape = RoundedCornerShape(8.dp)

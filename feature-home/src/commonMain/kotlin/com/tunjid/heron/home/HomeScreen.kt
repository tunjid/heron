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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.FeedItem
import com.tunjid.tiler.compose.PivotedTilingEffect

@Composable
internal fun HomeScreen(
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyStaggeredGridState()
    val items by rememberUpdatedState(state.feed)

    LazyVerticalStaggeredGrid(
        state = gridState,
        modifier = modifier
            .fillMaxSize(),
        columns = StaggeredGridCells.Adaptive(400.dp)
    ) {
        items(
            items = items,
            key = FeedItem::id,
            itemContent = { item ->
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(item.post.author?.displayName ?: "hi")
                }
            }
        )
    }

    gridState.PivotedTilingEffect(
        items = items,
        onQueryChanged = { query ->
            actions(Action.LoadFeed.LoadAround(query ?: state.currentQuery))
        }
    )
}

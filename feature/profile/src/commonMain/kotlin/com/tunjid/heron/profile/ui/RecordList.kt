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

package com.tunjid.heron.profile.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.profile.ProfileScreenStateHolders
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.tiledItems
import com.tunjid.tiler.compose.PivotedTilingEffect

@Composable
internal fun <T : Record> RecordList(
    collectionStateHolder: ProfileScreenStateHolders.Records<T>,
    itemKey: (T) -> Any,
    itemContent: @Composable (LazyItemScope.(T) -> Unit),
) {
    val listState = rememberLazyListState()
    val collectionState by collectionStateHolder.state.collectAsStateWithLifecycle()
    val updatedItems by rememberUpdatedState(collectionState.tiledItems)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        state = listState,
    ) {
        items(
            items = updatedItems,
            key = itemKey,
            itemContent = itemContent,
        )
    }

    listState.PivotedTilingEffect(
        items = updatedItems,
        onQueryChanged = { query ->
            collectionStateHolder.accept(
                TilingState.Action.LoadAround(
                    query = query ?: collectionState.tilingData.currentQuery,
                ),
            )
        },
    )
}

internal const val ProfileCollectionSharedElementPrefix = "profile-collection"

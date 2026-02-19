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

package com.tunjid.heron.timeline.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.timeline.utilities.BottomSheetItemCard
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState

@Stable
class SelectListSheetState private constructor(lists: List<FeedList>, scope: BottomSheetScope) :
    BottomSheetState(scope) {
    internal var lists by mutableStateOf(lists)

    override fun onHidden() {
        // No-op
    }

    companion object {
        @Composable
        fun rememberSelectListSheetState(
            lists: List<FeedList>,
            onListSelected: (FeedList) -> Unit,
        ): SelectListSheetState {
            val state =
                rememberBottomSheetState { SelectListSheetState(lists = lists, scope = it) }
                    .also { it.lists = lists }

            SelectListBottomSheet(state = state, onListSelected = onListSelected)

            return state
        }
    }
}

@Composable
private fun SelectListBottomSheet(state: SelectListSheetState, onListSelected: (FeedList) -> Unit) {
    state.ModalBottomSheet {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items = state.lists, key = { it.uri.uri }) { list ->
                BottomSheetItemCard(
                    onClick = {
                        onListSelected(list)
                        state.hide()
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = list.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

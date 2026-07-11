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

package com.tunjid.heron.search.ui.filter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.tunjid.heron.data.repository.SearchQuery
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState

/**
 * Advanced-search-filter bottom sheet for post searches. It is a holder-less
 * [BottomSheetState] whose editable draft [SearchQuery.Filter] lives in the owning
 * screen's state holder; the sheet reads [filter] and emits edits through
 * [rememberUpdatedSearchFilterSheetState]'s callbacks. Profile pickers reuse the
 * app-level `ProfileSearchSheet`.
 */
@Stable
class SearchFilterSheetState internal constructor(
    scope: BottomSheetScope,
) : BottomSheetState(scope) {

    override fun onHidden() = Unit
}

@Composable
fun PaneScaffoldState.rememberUpdatedSearchFilterSheetState(
    queryText: String,
    filter: SearchQuery.Filter,
    onQueryTextChanged: (String) -> Unit,
    onFilterChanged: (SearchQuery.Filter) -> Unit,
    onApply: () -> Unit,
): SearchFilterSheetState {
    val state = rememberBottomSheetState(
        skipPartiallyExpanded = true,
        block = ::SearchFilterSheetState,
    )

    state.ModalBottomSheet {
        SearchFilterForm(
            paneScaffoldState = this@rememberUpdatedSearchFilterSheetState,
            queryText = queryText,
            filter = filter,
            onQueryTextChanged = onQueryTextChanged,
            onFilterChanged = onFilterChanged,
            onCancel = state::hide,
            onApply = {
                onApply()
                state.hide()
            },
        )
    }

    return state
}

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

package com.tunjid.heron.search.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusManager
import com.tunjid.heron.search.Action
import com.tunjid.heron.search.State
import com.tunjid.heron.search.ui.filter.rememberUpdatedSearchFilterSheetState
import com.tunjid.heron.ui.AppBarIconButton
import com.tunjid.heron.ui.icons.HeronIcons
import com.tunjid.heron.ui.icons.regular.Tune
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import heron.feature.search.generated.resources.Res
import heron.feature.search.generated.resources.filters_content_description
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PaneScaffoldState.PaneActions(
    state: State,
    focusManager: FocusManager,
    actions: (Action) -> Unit,
) {
    val searchFilterSheetState = rememberUpdatedSearchFilterSheetState(
        queryText = state.searchBarText,
        filter = state.draftFilter,
        onQueryTextChanged = { query ->
            actions(Action.Search.OnSearchQueryChanged(query))
        },
        onFilterChanged = { filter ->
            actions(Action.Filter.Edit(filter))
        },
        onClear = {
            actions(Action.Filter.Clear)
        },
        onApply = {
            focusManager.clearFocus()
            actions(Action.Filter.Apply)
        },
    )

    if (state.signedInProfile != null) SearchFilterAction(
        isActive = state.appliedFilter != null,
        onClick = {
            actions(Action.Filter.Begin)
            searchFilterSheetState.show()
        },
    )
}

@Composable
private fun SearchFilterAction(
    isActive: Boolean,
    onClick: () -> Unit,
) {
    AppBarIconButton(
        icon = HeronIcons.Regular.Tune,
        iconDescription = stringResource(Res.string.filters_content_description),
        tint = when {
            isActive -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        onClick = onClick,
    )
}

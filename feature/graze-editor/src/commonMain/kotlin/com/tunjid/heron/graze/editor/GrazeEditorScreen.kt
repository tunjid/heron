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

package com.tunjid.heron.graze.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.graze.Filter
import com.tunjid.heron.graze.editor.ui.AnalysisFilter
import com.tunjid.heron.graze.editor.ui.AttributeCompareFilter
import com.tunjid.heron.graze.editor.ui.AttributeEmbedFilter
import com.tunjid.heron.graze.editor.ui.EntityExcludesFilter
import com.tunjid.heron.graze.editor.ui.EntityMatchesFilter
import com.tunjid.heron.graze.editor.ui.MLModerationFilter
import com.tunjid.heron.graze.editor.ui.MLProbabilityFilter
import com.tunjid.heron.graze.editor.ui.MLSimilarityFilter
import com.tunjid.heron.graze.editor.ui.RegexAnyFilter
import com.tunjid.heron.graze.editor.ui.RegexMatchesFilter
import com.tunjid.heron.graze.editor.ui.RegexNegationFilter
import com.tunjid.heron.graze.editor.ui.RegexNoneFilter
import com.tunjid.heron.graze.editor.ui.SocialGraphFilter
import com.tunjid.heron.graze.editor.ui.SocialListMemberFilter
import com.tunjid.heron.graze.editor.ui.SocialMagicAudienceFilter
import com.tunjid.heron.graze.editor.ui.SocialStarterPackFilter
import com.tunjid.heron.graze.editor.ui.SocialUserListFilter
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import heron.feature.graze_editor.generated.resources.Res
import heron.feature.graze_editor.generated.resources.all_of_these_and
import heron.feature.graze_editor.generated.resources.any_of_these_or
import heron.feature.graze_editor.generated.resources.items_count
import heron.feature.graze_editor.generated.resources.unknown_filter
import org.jetbrains.compose.resources.stringResource

@Composable
fun GrazeEditorScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentFilter = state.currentFilter

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {

        currentFilter.filters.forEachIndexed { index, child ->
            Filter(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                filter = child,
                atTopLevel = true,
                enterFilter = { enteredIndex ->
                    actions(Action.EnterFilter(enteredIndex))
                },
                onUpdateFilter = { updatedFilter: Filter, path: List<Int>, updatedIndex: Int ->
                    actions(
                        Action.UpdateFilter(
                            filter = updatedFilter,
                            path = path,
                            index = updatedIndex,
                        ),
                    )
                },
                onRemoveFilter = { path: List<Int>, removedIndex: Int ->
                    actions(
                        Action.RemoveFilter(
                            path = path,
                            index = removedIndex,
                        ),
                    )
                },
                index = index,
                path = state.currentPath,
            )
        }
    }
}

@Composable
private fun Filter(
    modifier: Modifier = Modifier,
    filter: Filter,
    atTopLevel: Boolean,
    path: List<Int>,
    enterFilter: (Int) -> Unit,
    onUpdateFilter: (filter: Filter, path: List<Int>, index: Int) -> Unit,
    onRemoveFilter: (path: List<Int>, index: Int) -> Unit,
    index: Int,
) {

    if (filter is Filter.Root) FilterRow(
        atTopLevel = atTopLevel,
        modifier = modifier,
        filter = filter,
        index = index,
        path = path,
        enterFilter = enterFilter,
        onUpdateFilter = onUpdateFilter,
        onRemoveFilter = onRemoveFilter,
    )
    else FilterLeaf(
        modifier = modifier,
        filter = filter,
        onUpdate = { updatedFilter ->
            onUpdateFilter(
                updatedFilter,
                path,
                index,
            )
        },
        onRemove = {
            onRemoveFilter(
                path,
                index,
            )
        },
    )
}

@Composable
fun FilterRow(
    atTopLevel: Boolean,
    index: Int,
    filter: Filter.Root,
    path: List<Int>,
    enterFilter: (Int) -> Unit,
    onUpdateFilter: (filter: Filter, path: List<Int>, index: Int) -> Unit,
    onRemoveFilter: (path: List<Int>, index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .clickable {
                enterFilter(index)
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = when (filter) {
                    is Filter.And -> stringResource(Res.string.all_of_these_and)
                    is Filter.Or -> stringResource(Res.string.any_of_these_or)
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(Res.string.items_count, filter.filters.size),
                style = MaterialTheme.typography.bodyMedium,
            )

            if (atTopLevel) Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                filter.filters.forEachIndexed { childIndex, child ->
                    Filter(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        filter = child,
                        atTopLevel = false,
                        index = childIndex,
                        path = path + index,
                        enterFilter = enterFilter,
                        onUpdateFilter = onUpdateFilter,
                        onRemoveFilter = onRemoveFilter,
                    )
                }
            }
        }
    }
}

@Composable
fun FilterLeaf(
    filter: Filter,
    onUpdate: (Filter) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (filter) {
        is Filter.Attribute.Compare -> AttributeCompareFilter(
            filter = filter,
            onUpdate = onUpdate,
            onRemove = onRemove,
        )
        is Filter.Attribute.Embed -> AttributeEmbedFilter(
            filter = filter,
            onUpdate = onUpdate,
            onRemove = onRemove,
        )
        is Filter.Entity.Matches -> EntityMatchesFilter(
            filter = filter,
            onUpdate = onUpdate,
            onRemove = onRemove,
        )
        is Filter.Entity.Excludes -> EntityExcludesFilter(
            filter = filter,
            onUpdate = onUpdate,
            onRemove = onRemove,
        )
        is Filter.Regex.Matches -> RegexMatchesFilter(
            filter = filter,
            onUpdate = onUpdate,
            onRemove = onRemove,
        )
        is Filter.Regex.Negation -> RegexNegationFilter(
            filter = filter,
            onUpdate = onUpdate,
            onRemove = onRemove,
        )
        is Filter.Regex.Any -> RegexAnyFilter(
            filter = filter,
            onUpdate = onUpdate,
            onRemove = onRemove,
        )
        is Filter.Regex.None -> RegexNoneFilter(
            filter = filter,
            onUpdate = onUpdate,
            onRemove = onRemove,
        )
        is Filter.Social.Graph -> SocialGraphFilter(
            filter = filter,
            onUpdate = onUpdate,
            onRemove = onRemove,
        )
        is Filter.Social.UserList -> SocialUserListFilter(
            filter = filter,
            onUpdate = onUpdate,
            onRemove = onRemove,
        )
        is Filter.Social.StarterPack -> SocialStarterPackFilter(
            filter = filter,
            onUpdate = onUpdate,
            onRemove = onRemove,
        )
        is Filter.Social.ListMember -> SocialListMemberFilter(
            filter = filter,
            onUpdate = onUpdate,
            onRemove = onRemove,
        )
        is Filter.Social.MagicAudience -> SocialMagicAudienceFilter(
            filter = filter,
            onUpdate = onUpdate,
            onRemove = onRemove,
        )
        is Filter.ML.Similarity -> MLSimilarityFilter(
            filter = filter,
            onUpdate = onUpdate,
            onRemove = onRemove,
        )
        is Filter.ML.Probability -> MLProbabilityFilter(
            filter = filter,
            onUpdate = onUpdate,
            onRemove = onRemove,
        )
        is Filter.ML.Moderation -> MLModerationFilter(
            filter = filter,
            onUpdate = onUpdate,
            onRemove = onRemove,
        )
        is Filter.Analysis -> AnalysisFilter(
            filter = filter,
            onUpdate = onUpdate,
            onRemove = onRemove,
        )
        else -> {
            Card(modifier = modifier) {
                Column(
                    modifier = Modifier
                        .padding(16.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.unknown_filter),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

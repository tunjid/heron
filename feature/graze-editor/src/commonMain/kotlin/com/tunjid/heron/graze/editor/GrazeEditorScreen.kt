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

import androidx.compose.animation.animateBounds
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.graze.Filter
import com.tunjid.heron.graze.editor.ui.AnalysisFilter
import com.tunjid.heron.graze.editor.ui.AttributeCompareFilter
import com.tunjid.heron.graze.editor.ui.AttributeEmbedFilter
import com.tunjid.heron.graze.editor.ui.EntityFilter
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
            .animateBounds(paneScaffoldState)
            .fillMaxSize()
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RootFilterDescription(
            modifier = Modifier,
            isAnd = state.filter is Filter.And,
            size = state.filter.filters.size,
            onFlipClicked = {
                actions(
                    Action.EditFilter.FlipRootFilter(
                        path = state.currentPath,
                    ),
                )
            },
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(
                currentFilter.filters,
                key = { _, child -> child.id.value },
                itemContent = { index, child ->
                    Filter(
                        modifier = Modifier
                            .animateItem()
                            .animateBounds(paneScaffoldState)
                            .fillMaxWidth(),
                        filter = child,
                        atTopLevel = true,
                        enterFilter = { enteredIndex ->
                            actions(Action.EditorNavigation.EnterFilter(enteredIndex))
                        },
                        onFlipClicked = { flippedPath ->
                            actions(
                                Action.EditFilter.FlipRootFilter(
                                    path = flippedPath,
                                ),
                            )
                        },
                        onUpdateFilter = { updatedFilter: Filter, path: List<Int>, updatedIndex: Int ->
                            actions(
                                Action.EditFilter.UpdateFilter(
                                    filter = updatedFilter,
                                    path = path,
                                    index = updatedIndex,
                                ),
                            )
                        },
                        onRemoveFilter = { path: List<Int>, removedIndex: Int ->
                            actions(
                                Action.EditFilter.RemoveFilter(
                                    path = path,
                                    index = removedIndex,
                                ),
                            )
                        },
                        index = index,
                        path = state.currentPath,
                    )
                },
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
    onFlipClicked: (path: List<Int>) -> Unit,
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
        onFlipClicked = onFlipClicked,
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
    onFlipClicked: (path: List<Int>) -> Unit,
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
            RootFilterDescription(
                modifier = Modifier,
                isAnd = filter is Filter.And,
                size = filter.filters.size,
                onFlipClicked = {
                    onFlipClicked(path + index)
                },
            )

            if (atTopLevel) LazyRow(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(
                    items = filter.filters,
                    key = { _, child -> child.id },
                    itemContent = { childIndex, child ->
                        Filter(
                            modifier = Modifier
                                .animateItem()
                                .fillParentMaxWidth()
                                .padding(8.dp),
                            filter = child,
                            atTopLevel = false,
                            index = childIndex,
                            path = path + index,
                            enterFilter = enterFilter,
                            onFlipClicked = onFlipClicked,
                            onUpdateFilter = onUpdateFilter,
                            onRemoveFilter = onRemoveFilter,
                        )
                    },
                )
            }
        }
    }
}

@Composable
fun RootFilterDescription(
    modifier: Modifier,
    isAnd: Boolean,
    size: Int,
    onFlipClicked: () -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                modifier = Modifier
                    .weight(1f),
                text = stringResource(
                    if (isAnd) Res.string.all_of_these_and
                    else Res.string.any_of_these_or,
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            IconButton(
                onClick = {
                    onFlipClicked()
                },
                content = {
                    Icon(
                        imageVector = Icons.Rounded.SwapHoriz,
                        contentDescription = "Flip",
                    )
                },
            )
        }
        Text(
            text = stringResource(
                Res.string.items_count,
                size,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
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
        is Filter.Entity.Matches,
        is Filter.Entity.Excludes,
        -> EntityFilter(
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

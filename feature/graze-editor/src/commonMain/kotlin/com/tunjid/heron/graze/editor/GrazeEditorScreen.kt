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
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.graze.Filter
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState

@Composable
fun GrazeEditorScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentFilter = state.currentFilter
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .fillMaxSize(),
    ) {
        currentFilter.filters.forEachIndexed { index, child ->
            if (child is Filter.Root) {
                FilterRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    filter = child,
                    onClick = { actions(Action.EnterFilter(index)) },
                )
            } else {
                FilterLeaf(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    filter = child,
                )
            }
        }
    }
}

@Composable
fun FilterRow(
    filter: Filter.Root,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
        ) {
            Text(
                text = when (filter) {
                    is Filter.And -> "All of these (AND)"
                    is Filter.Or -> "Any of these (OR)"
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "${filter.filters.size} items",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun FilterLeaf(
    filter: Filter,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = when (filter) {
                    is Filter.Attribute.Compare -> "Attribute: ${filter.selector} ${filter.operator.value} ${filter.targetValue}"
                    is Filter.Attribute.Embed -> "Embed: ${filter.operator.value} ${filter.embedType}"
                    is Filter.Entity.Matches -> "Entity Matches: ${filter.entityType}"
                    is Filter.Entity.Excludes -> "Entity Excludes: ${filter.entityType}"
                    is Filter.Regex.Matches -> "Regex Matches: ${filter.variable}"
                    is Filter.Regex.Negation -> "Regex Negation: ${filter.variable}"
                    is Filter.Regex.Any -> "Regex Any: ${filter.variable}"
                    is Filter.Regex.None -> "Regex None: ${filter.variable}"
                    is Filter.Social.Graph -> "Social Graph: ${filter.username}"
                    is Filter.Social.UserList -> "User List: ${filter.dids.size} users"
                    is Filter.Social.StarterPack -> "Starter Pack: ${filter.url}"
                    is Filter.Social.ListMember -> "List Member: ${filter.url}"
                    is Filter.Social.MagicAudience -> "Magic Audience: ${filter.audienceId}"
                    is Filter.ML.Similarity -> "ML Similarity: ${filter.config.modelName}"
                    is Filter.ML.Probability -> "ML Probability: ${filter.config.modelName}"
                    is Filter.ML.Moderation -> "Content Moderation: ${filter.category}"
                    is Filter.Analysis -> "Analysis: ${filter.category}"
                    else -> filter::class.simpleName ?: "Unknown Filter"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

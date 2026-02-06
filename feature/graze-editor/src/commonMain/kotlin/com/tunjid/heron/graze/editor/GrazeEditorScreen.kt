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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.graze.Filter
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import heron.feature.graze_editor.generated.resources.Res
import heron.feature.graze_editor.generated.resources.all_of_these_and
import heron.feature.graze_editor.generated.resources.any_of_these_or
import heron.feature.graze_editor.generated.resources.desc_analysis
import heron.feature.graze_editor.generated.resources.desc_attribute_compare
import heron.feature.graze_editor.generated.resources.desc_embed_type
import heron.feature.graze_editor.generated.resources.desc_entity_excludes
import heron.feature.graze_editor.generated.resources.desc_entity_matches
import heron.feature.graze_editor.generated.resources.desc_ml_moderation
import heron.feature.graze_editor.generated.resources.desc_ml_probability
import heron.feature.graze_editor.generated.resources.desc_ml_similarity
import heron.feature.graze_editor.generated.resources.desc_regex_any
import heron.feature.graze_editor.generated.resources.desc_regex_matches
import heron.feature.graze_editor.generated.resources.desc_regex_negation
import heron.feature.graze_editor.generated.resources.desc_regex_none
import heron.feature.graze_editor.generated.resources.desc_social_graph
import heron.feature.graze_editor.generated.resources.desc_social_list_member
import heron.feature.graze_editor.generated.resources.desc_social_magic_audience
import heron.feature.graze_editor.generated.resources.desc_social_starter_pack
import heron.feature.graze_editor.generated.resources.desc_social_user_list
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

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(currentFilter.filters) { index, child ->
                if (child is Filter.Root) {
                    FilterRow(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        filter = child,
                        onClick = { actions(Action.EnterFilter(index)) },
                    )
                } else {
                    FilterLeaf(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        filter = child,
                    )
                }
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
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                    is Filter.Attribute.Compare -> stringResource(
                        Res.string.desc_attribute_compare,
                        filter.selector,
                        filter.operator.value,
                        filter.targetValue,
                    )
                    is Filter.Attribute.Embed -> stringResource(
                        Res.string.desc_embed_type,
                        filter.operator.value,
                        filter.embedType,
                    )
                    is Filter.Entity.Matches -> stringResource(
                        Res.string.desc_entity_matches,
                        filter.entityType,
                    )
                    is Filter.Entity.Excludes -> stringResource(
                        Res.string.desc_entity_excludes,
                        filter.entityType,
                    )
                    is Filter.Regex.Matches -> stringResource(
                        Res.string.desc_regex_matches,
                        filter.variable,
                    )
                    is Filter.Regex.Negation -> stringResource(
                        Res.string.desc_regex_negation,
                        filter.variable,
                    )
                    is Filter.Regex.Any -> stringResource(
                        Res.string.desc_regex_any,
                        filter.variable,
                    )
                    is Filter.Regex.None -> stringResource(
                        Res.string.desc_regex_none,
                        filter.variable,
                    )
                    is Filter.Social.Graph -> stringResource(
                        Res.string.desc_social_graph,
                        filter.username,
                    )
                    is Filter.Social.UserList -> stringResource(
                        Res.string.desc_social_user_list,
                        filter.dids.size,
                    )
                    is Filter.Social.StarterPack -> stringResource(
                        Res.string.desc_social_starter_pack,
                        filter.url,
                    )
                    is Filter.Social.ListMember -> stringResource(
                        Res.string.desc_social_list_member,
                        filter.url,
                    )
                    is Filter.Social.MagicAudience -> stringResource(
                        Res.string.desc_social_magic_audience,
                        filter.audienceId,
                    )
                    is Filter.ML.Similarity -> stringResource(
                        Res.string.desc_ml_similarity,
                        filter.config.modelName,
                    )
                    is Filter.ML.Probability -> stringResource(
                        Res.string.desc_ml_probability,
                        filter.config.modelName,
                    )
                    is Filter.ML.Moderation -> stringResource(
                        Res.string.desc_ml_moderation,
                        filter.category,
                    )
                    is Filter.Analysis -> stringResource(Res.string.desc_analysis, filter.category)
                    else -> stringResource(Res.string.unknown_filter)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

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

package com.tunjid.heron.graze.editor.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.graze.Filter
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState
import heron.feature.graze_editor.generated.resources.Res
import heron.feature.graze_editor.generated.resources.add_filter
import heron.feature.graze_editor.generated.resources.all_of_these_and
import heron.feature.graze_editor.generated.resources.any_of_these_or
import heron.feature.graze_editor.generated.resources.attribute_compare
import heron.feature.graze_editor.generated.resources.content_moderation
import heron.feature.graze_editor.generated.resources.embed_type
import heron.feature.graze_editor.generated.resources.emotion_analysis
import heron.feature.graze_editor.generated.resources.entity_excludes
import heron.feature.graze_editor.generated.resources.entity_matches
import heron.feature.graze_editor.generated.resources.filter_group_analysis
import heron.feature.graze_editor.generated.resources.filter_group_attribute
import heron.feature.graze_editor.generated.resources.filter_group_entity
import heron.feature.graze_editor.generated.resources.filter_group_logic
import heron.feature.graze_editor.generated.resources.filter_group_ml
import heron.feature.graze_editor.generated.resources.filter_group_regex
import heron.feature.graze_editor.generated.resources.filter_group_social
import heron.feature.graze_editor.generated.resources.financial_sentiment
import heron.feature.graze_editor.generated.resources.image_arbitrary
import heron.feature.graze_editor.generated.resources.image_nsfw
import heron.feature.graze_editor.generated.resources.language_analysis
import heron.feature.graze_editor.generated.resources.model_probability
import heron.feature.graze_editor.generated.resources.regex_any
import heron.feature.graze_editor.generated.resources.regex_matches
import heron.feature.graze_editor.generated.resources.regex_negation
import heron.feature.graze_editor.generated.resources.regex_none
import heron.feature.graze_editor.generated.resources.sentiment_analysis
import heron.feature.graze_editor.generated.resources.social_graph
import heron.feature.graze_editor.generated.resources.social_list_member
import heron.feature.graze_editor.generated.resources.social_magic_audience
import heron.feature.graze_editor.generated.resources.social_starter_pack
import heron.feature.graze_editor.generated.resources.social_user_list
import heron.feature.graze_editor.generated.resources.text_arbitrary
import heron.feature.graze_editor.generated.resources.text_similarity
import heron.feature.graze_editor.generated.resources.topic_analysis
import heron.feature.graze_editor.generated.resources.toxicity_analysis
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Stable
class AddFilterSheetState(
    scope: BottomSheetScope,
) : BottomSheetState(scope) {
    override fun onHidden() = Unit
}

@Composable
fun rememberAddFilterSheetState(
    onFilterSelected: (Filter) -> Unit,
): AddFilterSheetState {
    val state = rememberBottomSheetState {
        AddFilterSheetState(it)
    }
    AddFilterBottomSheet(
        state = state,
        onFilterSelected = onFilterSelected,
    )
    return state
}

@Composable
private fun AddFilterBottomSheet(
    state: AddFilterSheetState,
    onFilterSelected: (Filter) -> Unit,
) {
    state.ModalBottomSheet {
        val expandedGroupIndices = remember { mutableStateListOf<Int>() }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Text(
                text = stringResource(Res.string.add_filter),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(
                        horizontal = 16.dp,
                        vertical = 12.dp,
                    ),
            )
            AllFilterGroups.forEachIndexed { index, group ->
                val isExpanded = expandedGroupIndices.contains(index)
                FilterGroupItem(
                    group = group,
                    isExpanded = isExpanded,
                    onHeaderClick = {
                        if (isExpanded) expandedGroupIndices.remove(index)
                        else expandedGroupIndices.add(index)
                    },
                    onFilterSelected = {
                        onFilterSelected(it)
                        state.hide()
                    },
                )
            }
        }
    }
}

@Composable
private fun FilterGroupItem(
    group: FilterGroup,
    isExpanded: Boolean,
    onHeaderClick: () -> Unit,
    onFilterSelected: (Filter) -> Unit,
) {
    Column {
        ListItem(
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
            ),
            headlineContent = {
                Text(
                    text = stringResource(group.nameRes),
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            trailingContent = {
                Icon(
                    imageVector =
                    if (isExpanded) Icons.Rounded.ExpandLess
                    else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onHeaderClick),
        )
        AnimatedVisibility(visible = isExpanded) {
            Column {
                group.options.forEach { option ->
                    ListItem(
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent,
                        ),
                        headlineContent = {
                            Text(
                                text = stringResource(option.titleRes),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp)
                            .clickable { onFilterSelected(option.defaultInstance) },
                    )
                }
            }
        }
    }
}

private data class FilterGroup(
    val nameRes: StringResource,
    val options: List<FilterOption>,
)

private data class FilterOption(
    val titleRes: StringResource,
    val defaultInstance: Filter,
)

private val AllFilterGroups: List<FilterGroup> = listOf(
    FilterGroup(
        nameRes = Res.string.filter_group_logic,
        options = listOf(
            FilterOption(
                titleRes = Res.string.all_of_these_and,
                defaultInstance = Filter.And(
                    filters = emptyList(),
                ),
            ),
            FilterOption(
                titleRes = Res.string.any_of_these_or,
                defaultInstance = Filter.Or(
                    filters = emptyList(),
                ),
            ),
        ),
    ),
    FilterGroup(
        nameRes = Res.string.filter_group_attribute,
        options = listOf(
            FilterOption(
                titleRes = Res.string.attribute_compare,
                defaultInstance = Filter.Attribute.Compare(
                    selector = "",
                    operator = Filter.Comparator.Equality.Equal,
                    targetValue = "",
                ),
            ),
            FilterOption(
                titleRes = Res.string.embed_type,
                defaultInstance = Filter.Attribute.Embed(
                    operator = Filter.Comparator.Equality.Equal,
                    embedType = Filter.Attribute.Embed.Kind.Image,
                ),
            ),
        ),
    ),
    FilterGroup(
        nameRes = Res.string.filter_group_entity,
        options = listOf(
            FilterOption(
                titleRes = Res.string.entity_matches,
                defaultInstance = Filter.Entity.Matches(
                    entityType = Filter.Entity.Type.Hashtags,
                    values = emptyList(),
                ),
            ),
            FilterOption(
                titleRes = Res.string.entity_excludes,
                defaultInstance = Filter.Entity.Excludes(
                    entityType = Filter.Entity.Type.Hashtags,
                    values = emptyList(),
                ),
            ),
        ),
    ),
    FilterGroup(
        nameRes = Res.string.filter_group_regex,
        options = listOf(
            FilterOption(
                titleRes = Res.string.regex_matches,
                defaultInstance = Filter.Regex.Matches(
                    variable = "",
                    pattern = "",
                    isCaseInsensitive = false,
                ),
            ),
            FilterOption(
                titleRes = Res.string.regex_negation,
                defaultInstance = Filter.Regex.Negation(
                    variable = "",
                    pattern = "",
                    isCaseInsensitive = false,
                ),
            ),
            FilterOption(
                titleRes = Res.string.regex_any,
                defaultInstance = Filter.Regex.Any(
                    variable = "",
                    terms = emptyList(),
                    isCaseInsensitive = false,
                ),
            ),
            FilterOption(
                titleRes = Res.string.regex_none,
                defaultInstance = Filter.Regex.None(
                    variable = "",
                    terms = emptyList(),
                    isCaseInsensitive = false,
                ),
            ),
        ),
    ),
    FilterGroup(
        nameRes = Res.string.filter_group_social,
        options = listOf(
            FilterOption(
                titleRes = Res.string.social_graph,
                defaultInstance = Filter.Social.Graph(
                    username = "",
                    operator = Filter.Comparator.Set.In,
                    direction = "",
                ),
            ),
            FilterOption(
                titleRes = Res.string.social_user_list,
                defaultInstance = Filter.Social.UserList(
                    dids = emptyList(),
                    operator = Filter.Comparator.Set.In,
                ),
            ),
            FilterOption(
                titleRes = Res.string.social_starter_pack,
                defaultInstance = Filter.Social.StarterPack(
                    url = "",
                    operator = Filter.Comparator.Set.In,
                ),
            ),
            FilterOption(
                titleRes = Res.string.social_list_member,
                defaultInstance = Filter.Social.ListMember(
                    url = "",
                    operator = Filter.Comparator.Set.In,
                ),
            ),
            FilterOption(
                titleRes = Res.string.social_magic_audience,
                defaultInstance = Filter.Social.MagicAudience(
                    audienceId = "",
                    operator = Filter.Comparator.Set.In,
                ),
            ),
        ),
    ),
    FilterGroup(
        nameRes = Res.string.filter_group_ml,
        options = listOf(
            FilterOption(
                titleRes = Res.string.text_similarity,
                defaultInstance = Filter.ML.Similarity(
                    path = "",
                    config = Filter.ML.Similarity.Config(
                        anchorText = "",
                        modelName = "",
                    ),
                    operator = Filter.Comparator.Range.GreaterThan,
                    threshold = 0.8,
                ),
            ),
            FilterOption(
                titleRes = Res.string.model_probability,
                defaultInstance = Filter.ML.Probability(
                    config = Filter.ML.Probability.Config(
                        modelName = "",
                    ),
                    operator = Filter.Comparator.Range.GreaterThan,
                    threshold = 0.8,
                ),
            ),
            FilterOption(
                titleRes = Res.string.content_moderation,
                defaultInstance = Filter.ML.Moderation(
                    category = "",
                    operator = Filter.Comparator.Equality.Equal,
                    threshold = 0.8,
                ),
            ),
        ),
    ),
    FilterGroup(
        nameRes = Res.string.filter_group_analysis,
        options = listOf(
            FilterOption(
                titleRes = Res.string.language_analysis,
                defaultInstance = Filter.Analysis.Language(
                    category = "",
                    operator = Filter.Comparator.Range.GreaterThan,
                    threshold = 0.8,
                ),
            ),
            FilterOption(
                titleRes = Res.string.sentiment_analysis,
                defaultInstance = Filter.Analysis.Sentiment(
                    category = "",
                    operator = Filter.Comparator.Range.GreaterThan,
                    threshold = 0.8,
                ),
            ),
            FilterOption(
                titleRes = Res.string.financial_sentiment,
                defaultInstance = Filter.Analysis.FinancialSentiment(
                    category = "",
                    operator = Filter.Comparator.Range.GreaterThan,
                    threshold = 0.8,
                ),
            ),
            FilterOption(
                titleRes = Res.string.emotion_analysis,
                defaultInstance = Filter.Analysis.Emotion(
                    category = "",
                    operator = Filter.Comparator.Range.GreaterThan,
                    threshold = 0.8,
                ),
            ),
            FilterOption(
                titleRes = Res.string.toxicity_analysis,
                defaultInstance = Filter.Analysis.Toxicity(
                    category = "",
                    operator = Filter.Comparator.Range.GreaterThan,
                    threshold = 0.8,
                ),
            ),
            FilterOption(
                titleRes = Res.string.topic_analysis,
                defaultInstance = Filter.Analysis.Topic(
                    category = "",
                    operator = Filter.Comparator.Range.GreaterThan,
                    threshold = 0.8,
                ),
            ),
            FilterOption(
                titleRes = Res.string.text_arbitrary,
                defaultInstance = Filter.Analysis.TextArbitrary(
                    category = "",
                    operator = Filter.Comparator.Range.GreaterThan,
                    threshold = 0.8,
                ),
            ),
            FilterOption(
                titleRes = Res.string.image_nsfw,
                defaultInstance = Filter.Analysis.ImageNsfw(
                    category = "",
                    operator = Filter.Comparator.Range.GreaterThan,
                    threshold = 0.8,
                ),
            ),
            FilterOption(
                titleRes = Res.string.image_arbitrary,
                defaultInstance = Filter.Analysis.ImageArbitrary(
                    category = "",
                    operator = Filter.Comparator.Range.GreaterThan,
                    threshold = 0.8,
                ),
            ),
        ),
    ),
)

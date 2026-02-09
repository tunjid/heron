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
                                style = MaterialTheme.typography.titleSmall,
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp)
                            .clickable { onFilterSelected(option.factory()) },
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

@Stable
private class FilterOption(
    val titleRes: StringResource,
    val factory: () -> Filter,
)

private val AllFilterGroups: List<FilterGroup> = listOf(
    FilterGroup(
        nameRes = Res.string.filter_group_logic,
        options = listOf(
            FilterOption(
                titleRes = Res.string.all_of_these_and,
                factory = Filter.And::empty,
            ),
            FilterOption(
                titleRes = Res.string.any_of_these_or,
                factory = Filter.Or::empty,
            ),
        ),
    ),
    FilterGroup(
        nameRes = Res.string.filter_group_attribute,
        options = listOf(
            FilterOption(
                titleRes = Res.string.attribute_compare,
                factory = Filter.Attribute.Compare::empty,
            ),
            FilterOption(
                titleRes = Res.string.embed_type,
                factory = Filter.Attribute.Embed::empty,
            ),
        ),
    ),
    FilterGroup(
        nameRes = Res.string.filter_group_entity,
        options = listOf(
            FilterOption(
                titleRes = Res.string.entity_matches,
                factory = Filter.Entity.Matches::empty,
            ),
            FilterOption(
                titleRes = Res.string.entity_excludes,
                factory = Filter.Entity.Excludes::empty,
            ),
        ),
    ),
    // No regular expression support for now
//    FilterGroup(
//        nameRes = Res.string.filter_group_regex,
//        options = listOf(
//            FilterOption(
//                titleRes = Res.string.regex_matches,
//                factory = Filter.Regex.Matches::empty,
//            ),
//            FilterOption(
//                titleRes = Res.string.regex_negation,
//                factory = Filter.Regex.Negation::empty,
//            ),
//            FilterOption(
//                titleRes = Res.string.regex_any,
//                factory = Filter.Regex.Any::empty,
//            ),
//            FilterOption(
//                titleRes = Res.string.regex_none,
//                factory = Filter.Regex.None::empty,
//            ),
//        ),
//    ),
    FilterGroup(
        nameRes = Res.string.filter_group_social,
        options = listOf(
            FilterOption(
                titleRes = Res.string.social_graph,
                factory = Filter.Social.Graph::empty,
            ),
            FilterOption(
                titleRes = Res.string.social_user_list,
                factory = Filter.Social.UserList::empty,
            ),
            FilterOption(
                titleRes = Res.string.social_starter_pack,
                factory = Filter.Social.StarterPack::empty,
            ),
            FilterOption(
                titleRes = Res.string.social_list_member,
                factory = Filter.Social.ListMember::empty,
            ),
            FilterOption(
                titleRes = Res.string.social_magic_audience,
                factory = Filter.Social.MagicAudience::empty,
            ),
        ),
    ),
    FilterGroup(
        nameRes = Res.string.filter_group_ml,
        options = listOf(
            // Unsupported for now
//            FilterOption(
//                titleRes = Res.string.text_similarity,
//                factory = Filter.ML.Similarity::empty,
//            ),
//            FilterOption(
//                titleRes = Res.string.model_probability,
//                factory = Filter.ML.Probability::empty,
//            ),
            FilterOption(
                titleRes = Res.string.content_moderation,
                factory = Filter.ML.Moderation::empty,
            ),
        ),
    ),
    FilterGroup(
        nameRes = Res.string.filter_group_analysis,
        options = listOf(
            FilterOption(
                titleRes = Res.string.language_analysis,
                factory = Filter.Analysis.Language::empty,
            ),
            FilterOption(
                titleRes = Res.string.sentiment_analysis,
                factory = Filter.Analysis.Sentiment::empty,
            ),
            FilterOption(
                titleRes = Res.string.financial_sentiment,
                factory = Filter.Analysis.FinancialSentiment::empty,
            ),
            FilterOption(
                titleRes = Res.string.emotion_analysis,
                factory = Filter.Analysis.Emotion::empty,
            ),
            FilterOption(
                titleRes = Res.string.toxicity_analysis,
                factory = Filter.Analysis.Toxicity::empty,
            ),
            FilterOption(
                titleRes = Res.string.topic_analysis,
                factory = Filter.Analysis.Topic::empty,
            ),
            // Unsupported for now
//            FilterOption(
//                titleRes = Res.string.text_arbitrary,
//                factory = Filter.Analysis.TextArbitrary::empty,
//            ),
            FilterOption(
                titleRes = Res.string.image_nsfw,
                factory = Filter.Analysis.ImageNsfw::empty,
            ),
            // Unsupported for now
//            FilterOption(
//                titleRes = Res.string.image_arbitrary,
//                factory = Filter.Analysis.ImageArbitrary::empty,
//            ),
        ),
    ),
)

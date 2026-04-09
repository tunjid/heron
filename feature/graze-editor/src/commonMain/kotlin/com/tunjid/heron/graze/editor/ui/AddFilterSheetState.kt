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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.graze.Filter
import com.tunjid.heron.ui.Tab
import com.tunjid.heron.ui.Tabs
import com.tunjid.heron.ui.TabsState.Companion.rememberTabsState
import com.tunjid.heron.ui.icons.AccountTree
import com.tunjid.heron.ui.icons.Analytics
import com.tunjid.heron.ui.icons.AttachMoney
import com.tunjid.heron.ui.icons.Block
import com.tunjid.heron.ui.icons.CallSplit
import com.tunjid.heron.ui.icons.CompareArrows
import com.tunjid.heron.ui.icons.Diversity3
import com.tunjid.heron.ui.icons.ExpandLess
import com.tunjid.heron.ui.icons.ExpandMore
import com.tunjid.heron.ui.icons.Face
import com.tunjid.heron.ui.icons.Gavel
import com.tunjid.heron.ui.icons.Group
import com.tunjid.heron.ui.icons.Groups
import com.tunjid.heron.ui.icons.HeronIcons
import com.tunjid.heron.ui.icons.Image
import com.tunjid.heron.ui.icons.Language
import com.tunjid.heron.ui.icons.ManageSearch
import com.tunjid.heron.ui.icons.Mood
import com.tunjid.heron.ui.icons.PermMedia
import com.tunjid.heron.ui.icons.Person
import com.tunjid.heron.ui.icons.Psychology
import com.tunjid.heron.ui.icons.Search
import com.tunjid.heron.ui.icons.SelectAll
import com.tunjid.heron.ui.icons.Share
import com.tunjid.heron.ui.icons.Tag
import com.tunjid.heron.ui.icons.Topic
import com.tunjid.heron.ui.icons.Tune
import com.tunjid.heron.ui.icons.VisibilityOff
import com.tunjid.heron.ui.icons.Warning
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState
import com.tunjid.heron.ui.tabIndex
import heron.feature.graze_editor.generated.resources.Res
import heron.feature.graze_editor.generated.resources.add_filter
import heron.feature.graze_editor.generated.resources.advanced_filters
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
import heron.feature.graze_editor.generated.resources.filter_group_social
import heron.feature.graze_editor.generated.resources.financial_sentiment
import heron.feature.graze_editor.generated.resources.image_nsfw
import heron.feature.graze_editor.generated.resources.images_and_videos_only
import heron.feature.graze_editor.generated.resources.images_only
import heron.feature.graze_editor.generated.resources.language_analysis
import heron.feature.graze_editor.generated.resources.posts_from_profiles
import heron.feature.graze_editor.generated.resources.posts_with_hashtags
import heron.feature.graze_editor.generated.resources.sentiment_analysis
import heron.feature.graze_editor.generated.resources.simple_filters
import heron.feature.graze_editor.generated.resources.social_graph
import heron.feature.graze_editor.generated.resources.social_list_member
import heron.feature.graze_editor.generated.resources.social_user_list
import heron.feature.graze_editor.generated.resources.topic_analysis
import heron.feature.graze_editor.generated.resources.toxicity_analysis
import kotlinx.coroutines.launch
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(SheetHeightFraction)
                .padding(bottom = 16.dp),
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
            val pagerState = rememberPagerState { 2 }
            val coroutineScope = rememberCoroutineScope()
            val onTabSelected: (Int) -> Unit = { page: Int ->
                coroutineScope.launch { pagerState.animateScrollToPage(page) }
            }
            Tabs(
                modifier = Modifier
                    .padding(
                        horizontal = 16.dp,
                    )
                    .fillMaxWidth(),
                tabsState = rememberTabsState(
                    tabs = filterTabs(),
                    selectedTabIndex = pagerState::tabIndex,
                    onTabSelected = onTabSelected,
                    onTabReselected = onTabSelected,
                ),
            )
            val selectFilter: (Filter) -> Unit = {
                onFilterSelected(it)
                state.hide()
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) { page ->
                when (page) {
                    0 -> SimpleFilterList(
                        onFilterSelected = selectFilter,
                    )

                    1 -> AdvancedFilterList(
                        onFilterSelected = selectFilter,
                    )
                }
            }
        }
    }
}

@Composable
private fun filterTabs(): List<Tab> {
    val simpleFilters = stringResource(Res.string.simple_filters)
    val advancedFilters = stringResource(Res.string.advanced_filters)
    return remember(simpleFilters, advancedFilters) {
        listOf(
            Tab(
                title = simpleFilters,
                hasUpdate = false,
            ),
            Tab(
                title = advancedFilters,
                hasUpdate = false,
            ),
        )
    }
}

@Composable
private fun SimpleFilterList(
    onFilterSelected: (Filter) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        SimpleFilterOptions.forEach { option ->
            ListItem(
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                ),
                leadingContent = {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = null,
                    )
                },
                headlineContent = {
                    Text(
                        text = stringResource(option.titleRes),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFilterSelected(option.factory()) },
            )
        }
    }
}

@Composable
private fun AdvancedFilterList(
    onFilterSelected: (Filter) -> Unit,
) {
    val expandedGroupIndices = remember { mutableStateListOf<Int>() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        AllFilterGroups.forEachIndexed { index, group ->
            val isExpanded = expandedGroupIndices.contains(index)
            FilterGroupItem(
                group = group,
                isExpanded = isExpanded,
                onHeaderClick = {
                    if (isExpanded) expandedGroupIndices.remove(index)
                    else expandedGroupIndices.add(index)
                },
                onFilterSelected = onFilterSelected,
            )
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
            leadingContent = {
                Icon(
                    imageVector = group.icon,
                    contentDescription = null,
                )
            },
            headlineContent = {
                Text(
                    text = stringResource(group.nameRes),
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            trailingContent = {
                Icon(
                    imageVector =
                    if (isExpanded) HeronIcons.ExpandLess
                    else HeronIcons.ExpandMore,
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
                        leadingContent = {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                            )
                        },
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
    val icon: ImageVector,
    val options: List<FilterOption>,
)

@Stable
private class FilterOption(
    val titleRes: StringResource,
    val icon: ImageVector,
    val factory: () -> Filter,
)

private val SimpleFilterOptions: List<FilterOption> = listOf(
    FilterOption(
        titleRes = Res.string.images_only,
        icon = HeronIcons.Image,
        factory = {
            Filter.Attribute.Embed(
                embedType = Filter.Attribute.Embed.Kind.Image,
                operator = Filter.Comparator.Equality.Equal,
            )
        },
    ),
    FilterOption(
        titleRes = Res.string.images_and_videos_only,
        icon = HeronIcons.PermMedia,
        factory = {
            Filter.Or(
                filters = listOf(
                    Filter.Attribute.Embed(
                        embedType = Filter.Attribute.Embed.Kind.Image,
                        operator = Filter.Comparator.Equality.Equal,
                    ),
                    Filter.Attribute.Embed(
                        embedType = Filter.Attribute.Embed.Kind.Video,
                        operator = Filter.Comparator.Equality.Equal,
                    ),
                ),
            )
        },
    ),
    FilterOption(
        titleRes = Res.string.posts_from_profiles,
        icon = HeronIcons.Person,
        factory = Filter.Social.UserList::empty,
    ),
    FilterOption(
        titleRes = Res.string.posts_with_hashtags,
        icon = HeronIcons.Tag,
        factory = Filter.Entity.Matches::empty,
    ),
)

private val AllFilterGroups: List<FilterGroup> = listOf(
    FilterGroup(
        nameRes = Res.string.filter_group_logic,
        icon = HeronIcons.AccountTree,
        options = listOf(
            FilterOption(
                titleRes = Res.string.all_of_these_and,
                icon = HeronIcons.SelectAll,
                factory = Filter.And::empty,
            ),
            FilterOption(
                titleRes = Res.string.any_of_these_or,
                icon = HeronIcons.CallSplit,
                factory = Filter.Or::empty,
            ),
        ),
    ),
    FilterGroup(
        nameRes = Res.string.filter_group_entity,
        icon = HeronIcons.ManageSearch,
        options = listOf(
            FilterOption(
                titleRes = Res.string.entity_matches,
                icon = HeronIcons.Search,
                factory = Filter.Entity.Matches::empty,
            ),
            FilterOption(
                titleRes = Res.string.entity_excludes,
                icon = HeronIcons.Block,
                factory = Filter.Entity.Excludes::empty,
            ),
        ),
    ),
    FilterGroup(
        nameRes = Res.string.filter_group_attribute,
        icon = HeronIcons.Tune,
        options = listOf(
            FilterOption(
                titleRes = Res.string.attribute_compare,
                icon = HeronIcons.CompareArrows,
                factory = Filter.Attribute.Compare::empty,
            ),
            FilterOption(
                titleRes = Res.string.embed_type,
                icon = HeronIcons.PermMedia,
                factory = Filter.Attribute.Embed::empty,
            ),
        ),
    ),
    // No regular expression support for now
//    FilterGroup(
//        nameRes = Res.string.filter_group_regex,
//        icon = HeronIcons.RegularExpression,
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
        icon = HeronIcons.Diversity3,
        options = listOf(
            FilterOption(
                titleRes = Res.string.social_graph,
                icon = HeronIcons.Share,
                factory = Filter.Social.Graph::empty,
            ),
            FilterOption(
                titleRes = Res.string.social_user_list,
                icon = HeronIcons.Group,
                factory = Filter.Social.UserList::empty,
            ),
//            FilterOption(
//                titleRes = Res.string.social_starter_pack,
//                factory = Filter.Social.StarterPack::empty,
//            ),
            FilterOption(
                titleRes = Res.string.social_list_member,
                icon = HeronIcons.Groups,
                factory = Filter.Social.ListMember::empty,
            ),
//            FilterOption(
//                titleRes = Res.string.social_magic_audience,
//                factory = Filter.Social.MagicAudience::empty,
//            ),
        ),
    ),
    FilterGroup(
        nameRes = Res.string.filter_group_ml,
        icon = HeronIcons.Psychology,
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
                icon = HeronIcons.Gavel,
                factory = Filter.ML.Moderation::empty,
            ),
        ),
    ),
    FilterGroup(
        nameRes = Res.string.filter_group_analysis,
        icon = HeronIcons.Analytics,
        options = listOf(
            FilterOption(
                titleRes = Res.string.language_analysis,
                icon = HeronIcons.Language,
                factory = Filter.Analysis.Language::empty,
            ),
            FilterOption(
                titleRes = Res.string.sentiment_analysis,
                icon = HeronIcons.Mood,
                factory = Filter.Analysis.Sentiment::empty,
            ),
            FilterOption(
                titleRes = Res.string.financial_sentiment,
                icon = HeronIcons.AttachMoney,
                factory = Filter.Analysis.FinancialSentiment::empty,
            ),
            FilterOption(
                titleRes = Res.string.emotion_analysis,
                icon = HeronIcons.Face,
                factory = Filter.Analysis.Emotion::empty,
            ),
            FilterOption(
                titleRes = Res.string.toxicity_analysis,
                icon = HeronIcons.Warning,
                factory = Filter.Analysis.Toxicity::empty,
            ),
            FilterOption(
                titleRes = Res.string.topic_analysis,
                icon = HeronIcons.Topic,
                factory = Filter.Analysis.Topic::empty,
            ),
            // Unsupported for now
//            FilterOption(
//                titleRes = Res.string.text_arbitrary,
//                factory = Filter.Analysis.TextArbitrary::empty,
//            ),
            FilterOption(
                titleRes = Res.string.image_nsfw,
                icon = HeronIcons.VisibilityOff,
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

private const val SheetHeightFraction = 0.8f

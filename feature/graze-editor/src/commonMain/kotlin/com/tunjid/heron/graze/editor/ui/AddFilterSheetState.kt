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
import com.tunjid.heron.ui.icons.HeronIcons
import com.tunjid.heron.ui.icons.automirrored.CallSplit
import com.tunjid.heron.ui.icons.automirrored.CompareArrows
import com.tunjid.heron.ui.icons.automirrored.ManageSearch
import com.tunjid.heron.ui.icons.automirrored.Rule
import com.tunjid.heron.ui.icons.regular.AccountTree
import com.tunjid.heron.ui.icons.regular.Analytics
import com.tunjid.heron.ui.icons.regular.AttachMoney
import com.tunjid.heron.ui.icons.regular.AutoAwesome
import com.tunjid.heron.ui.icons.regular.Backpack
import com.tunjid.heron.ui.icons.regular.Block
import com.tunjid.heron.ui.icons.regular.Code
import com.tunjid.heron.ui.icons.regular.Diversity3
import com.tunjid.heron.ui.icons.regular.ExpandLess
import com.tunjid.heron.ui.icons.regular.ExpandMore
import com.tunjid.heron.ui.icons.regular.Face
import com.tunjid.heron.ui.icons.regular.Gavel
import com.tunjid.heron.ui.icons.regular.Group
import com.tunjid.heron.ui.icons.regular.Groups
import com.tunjid.heron.ui.icons.regular.Image
import com.tunjid.heron.ui.icons.regular.Language
import com.tunjid.heron.ui.icons.regular.Mood
import com.tunjid.heron.ui.icons.regular.PermMedia
import com.tunjid.heron.ui.icons.regular.Person
import com.tunjid.heron.ui.icons.regular.Psychology
import com.tunjid.heron.ui.icons.regular.Search
import com.tunjid.heron.ui.icons.regular.SelectAll
import com.tunjid.heron.ui.icons.regular.Share
import com.tunjid.heron.ui.icons.regular.Tag
import com.tunjid.heron.ui.icons.regular.TextFields
import com.tunjid.heron.ui.icons.regular.Topic
import com.tunjid.heron.ui.icons.regular.Tune
import com.tunjid.heron.ui.icons.regular.VisibilityOff
import com.tunjid.heron.ui.icons.regular.Warning
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
import heron.feature.graze_editor.generated.resources.filter_group_regex
import heron.feature.graze_editor.generated.resources.filter_group_social
import heron.feature.graze_editor.generated.resources.financial_sentiment
import heron.feature.graze_editor.generated.resources.image_arbitrary
import heron.feature.graze_editor.generated.resources.image_nsfw
import heron.feature.graze_editor.generated.resources.images_and_videos_only
import heron.feature.graze_editor.generated.resources.images_only
import heron.feature.graze_editor.generated.resources.language_analysis
import heron.feature.graze_editor.generated.resources.posts_from_profiles
import heron.feature.graze_editor.generated.resources.posts_with_hashtags
import heron.feature.graze_editor.generated.resources.regex_any
import heron.feature.graze_editor.generated.resources.regex_matches
import heron.feature.graze_editor.generated.resources.regex_negation
import heron.feature.graze_editor.generated.resources.regex_none
import heron.feature.graze_editor.generated.resources.sentiment_analysis
import heron.feature.graze_editor.generated.resources.simple_filters
import heron.feature.graze_editor.generated.resources.social_graph
import heron.feature.graze_editor.generated.resources.social_list_member
import heron.feature.graze_editor.generated.resources.social_magic_audience
import heron.feature.graze_editor.generated.resources.social_starter_pack
import heron.feature.graze_editor.generated.resources.social_user_list
import heron.feature.graze_editor.generated.resources.text_arbitrary
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
                    if (isExpanded) HeronIcons.Regular.ExpandLess
                    else HeronIcons.Regular.ExpandMore,
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
        icon = HeronIcons.Regular.Image,
        factory = {
            Filter.Attribute.Embed(
                embedType = Filter.Attribute.Embed.Kind.Image,
                operator = Filter.Comparator.Equality.Equal,
            )
        },
    ),
    FilterOption(
        titleRes = Res.string.images_and_videos_only,
        icon = HeronIcons.Regular.PermMedia,
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
        icon = HeronIcons.Regular.Person,
        factory = Filter.Social.UserList::empty,
    ),
    FilterOption(
        titleRes = Res.string.posts_with_hashtags,
        icon = HeronIcons.Regular.Tag,
        factory = Filter.Entity.Matches::empty,
    ),
)

private val AllFilterGroups: List<FilterGroup> = listOf(
    FilterGroup(
        nameRes = Res.string.filter_group_logic,
        icon = HeronIcons.Regular.AccountTree,
        options = listOf(
            FilterOption(
                titleRes = Res.string.all_of_these_and,
                icon = HeronIcons.Regular.SelectAll,
                factory = Filter.And::empty,
            ),
            FilterOption(
                titleRes = Res.string.any_of_these_or,
                icon = HeronIcons.AutoMirrored.CallSplit,
                factory = Filter.Or::empty,
            ),
        ),
    ),
    FilterGroup(
        nameRes = Res.string.filter_group_entity,
        icon = HeronIcons.AutoMirrored.ManageSearch,
        options = listOf(
            FilterOption(
                titleRes = Res.string.entity_matches,
                icon = HeronIcons.Regular.Search,
                factory = Filter.Entity.Matches::empty,
            ),
            FilterOption(
                titleRes = Res.string.entity_excludes,
                icon = HeronIcons.Regular.Block,
                factory = Filter.Entity.Excludes::empty,
            ),
        ),
    ),
    FilterGroup(
        nameRes = Res.string.filter_group_attribute,
        icon = HeronIcons.Regular.Tune,
        options = listOf(
            FilterOption(
                titleRes = Res.string.attribute_compare,
                icon = HeronIcons.AutoMirrored.CompareArrows,
                factory = Filter.Attribute.Compare::empty,
            ),
            FilterOption(
                titleRes = Res.string.embed_type,
                icon = HeronIcons.Regular.PermMedia,
                factory = Filter.Attribute.Embed::empty,
            ),
        ),
    ),
    FilterGroup(
        nameRes = Res.string.filter_group_regex,
        icon = HeronIcons.Regular.Code,
        options = listOf(
            FilterOption(
                titleRes = Res.string.regex_matches,
                icon = HeronIcons.AutoMirrored.Rule,
                factory = Filter.Regex.Matches::empty,
            ),
            FilterOption(
                titleRes = Res.string.regex_negation,
                icon = HeronIcons.Regular.Block,
                factory = Filter.Regex.Negation::empty,
            ),
            FilterOption(
                titleRes = Res.string.regex_any,
                icon = HeronIcons.AutoMirrored.Rule,
                factory = Filter.Regex.Any::empty,
            ),
            FilterOption(
                titleRes = Res.string.regex_none,
                icon = HeronIcons.Regular.Block,
                factory = Filter.Regex.None::empty,
            ),
        ),
    ),
    FilterGroup(
        nameRes = Res.string.filter_group_social,
        icon = HeronIcons.Regular.Diversity3,
        options = listOf(
            FilterOption(
                titleRes = Res.string.social_graph,
                icon = HeronIcons.Regular.Share,
                factory = Filter.Social.Graph::empty,
            ),
            FilterOption(
                titleRes = Res.string.social_user_list,
                icon = HeronIcons.Regular.Group,
                factory = Filter.Social.UserList::empty,
            ),
            FilterOption(
                titleRes = Res.string.social_starter_pack,
                icon = HeronIcons.Regular.Backpack,
                factory = Filter.Social.StarterPack::empty,
            ),
            FilterOption(
                titleRes = Res.string.social_list_member,
                icon = HeronIcons.Regular.Groups,
                factory = Filter.Social.ListMember::empty,
            ),
            FilterOption(
                titleRes = Res.string.social_magic_audience,
                icon = HeronIcons.Regular.AutoAwesome,
                factory = Filter.Social.MagicAudience::empty,
            ),
        ),
    ),
    FilterGroup(
        nameRes = Res.string.filter_group_ml,
        icon = HeronIcons.Regular.Psychology,
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
                icon = HeronIcons.Regular.Gavel,
                factory = Filter.ML.Moderation::empty,
            ),
        ),
    ),
    FilterGroup(
        nameRes = Res.string.filter_group_analysis,
        icon = HeronIcons.Regular.Analytics,
        options = listOf(
            FilterOption(
                titleRes = Res.string.language_analysis,
                icon = HeronIcons.Regular.Language,
                factory = Filter.Analysis.Language::empty,
            ),
            FilterOption(
                titleRes = Res.string.sentiment_analysis,
                icon = HeronIcons.Regular.Mood,
                factory = Filter.Analysis.Sentiment::empty,
            ),
            FilterOption(
                titleRes = Res.string.financial_sentiment,
                icon = HeronIcons.Regular.AttachMoney,
                factory = Filter.Analysis.FinancialSentiment::empty,
            ),
            FilterOption(
                titleRes = Res.string.emotion_analysis,
                icon = HeronIcons.Regular.Face,
                factory = Filter.Analysis.Emotion::empty,
            ),
            FilterOption(
                titleRes = Res.string.toxicity_analysis,
                icon = HeronIcons.Regular.Warning,
                factory = Filter.Analysis.Toxicity::empty,
            ),
            FilterOption(
                titleRes = Res.string.topic_analysis,
                icon = HeronIcons.Regular.Topic,
                factory = Filter.Analysis.Topic::empty,
            ),
            FilterOption(
                titleRes = Res.string.text_arbitrary,
                icon = HeronIcons.Regular.TextFields,
                factory = Filter.Analysis.TextArbitrary::empty,
            ),
            FilterOption(
                titleRes = Res.string.image_nsfw,
                icon = HeronIcons.Regular.VisibilityOff,
                factory = Filter.Analysis.ImageNsfw::empty,
            ),
            FilterOption(
                titleRes = Res.string.image_arbitrary,
                icon = HeronIcons.Regular.Image,
                factory = Filter.Analysis.ImageArbitrary::empty,
            ),
        ),
    ),
)

private const val SheetHeightFraction = 0.8f

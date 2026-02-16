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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.graze.Filter
import com.tunjid.heron.graze.editor.ui.filter.AnalysisFilter
import com.tunjid.heron.graze.editor.ui.filter.AttributeCompareFilter
import com.tunjid.heron.graze.editor.ui.filter.AttributeEmbedFilter
import com.tunjid.heron.graze.editor.ui.filter.EntityFilter
import com.tunjid.heron.graze.editor.ui.filter.MLModerationFilter
import com.tunjid.heron.graze.editor.ui.filter.RegexFilter
import com.tunjid.heron.graze.editor.ui.filter.SocialGraphFilter
import com.tunjid.heron.graze.editor.ui.filter.SocialListMemberFilter
import com.tunjid.heron.graze.editor.ui.filter.SocialMagicAudienceFilter
import com.tunjid.heron.graze.editor.ui.filter.SocialStarterPackFilter
import com.tunjid.heron.graze.editor.ui.filter.SocialUserListFilter
import com.tunjid.heron.graze.editor.ui.filter.UnsupportedFilter
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.Indicator
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.modifiers.blockClickEvents
import com.tunjid.heron.ui.modifiers.ifTrue
import com.tunjid.heron.ui.text.CommonStrings
import heron.feature.graze_editor.generated.resources.Res
import heron.feature.graze_editor.generated.resources.all_of_these_and
import heron.feature.graze_editor.generated.resources.any_of_these_or
import heron.feature.graze_editor.generated.resources.items_count
import heron.feature.graze_editor.generated.resources.model_probability
import heron.feature.graze_editor.generated.resources.remove_filter
import heron.feature.graze_editor.generated.resources.text_similarity
import heron.feature.graze_editor.generated.resources.unknown_filter
import heron.ui.core.generated.resources.go_back
import org.jetbrains.compose.resources.stringResource

@Composable
fun GrazeEditorScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) = with(paneScaffoldState) {
    AnimatedContent(
        modifier = modifier
            .ifTrue(
                predicate = state.isLoading,
                block = Modifier::blockClickEvents,
            ),
        targetState = state.currentFilter to state.currentPath,
        contentKey = { (currentFilter) -> currentFilter.id },
        transitionSpec = {
            FilterTransitionSpec
        },
    ) { (currentFilter, currentPath) ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 16.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RootFilterDescription(
                modifier = Modifier,
                isAnd = currentFilter is Filter.And,
                size = currentFilter.filters.size,
                animatedVisibilityScope = this@AnimatedContent,
                paneScaffoldState = paneScaffoldState,
                id = currentFilter.id,
                onRemove = null,
                onFlipClicked = {
                    actions(
                        Action.EditFilter.FlipRootFilter(
                            path = currentPath,
                        ),
                    )
                },
            )
            LazyColumn(
                modifier = Modifier
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(currentFilter.backgroundSharedElementKey()),
                        animatedVisibilityScope = this@AnimatedContent,
                    )
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = UiTokens.bottomNavAndInsetPaddingValues(
                    isCompact = paneScaffoldState.prefersCompactBottomNav,
                ),
            ) {
                itemsIndexed(
                    currentFilter.filters,
                    key = { _, child -> child.id.value },
                    itemContent = { index, child ->
                        Filter(
                            modifier = Modifier
                                .animateItem()
                                .fillMaxWidth(),
                            animatedVisibilityScope = this@AnimatedContent,
                            paneScaffoldState = paneScaffoldState,
                            profileSearchResults = state.suggestedProfiles,
                            filter = child,
                            atTopLevel = true,
                            onProfileQueryChanged = { query ->
                                actions(Action.SearchProfiles(query))
                            },
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
                            path = currentPath,
                        )
                    },
                )
            }
        }
    }
    if (state.isLoading) Dialog(
        onDismissRequest = {},
        properties = remember {
            DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            )
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()

                FilledTonalButton(
                    onClick = {
                        actions(Action.Navigate.Pop)
                    },
                    content = {
                        Text(stringResource(CommonStrings.go_back))
                    },
                )
            }
        }
    }
}

@Composable
private fun Filter(
    modifier: Modifier = Modifier,
    animatedVisibilityScope: AnimatedVisibilityScope,
    paneScaffoldState: PaneScaffoldState,
    filter: Filter,
    profileSearchResults: List<Profile>,
    atTopLevel: Boolean,
    path: List<Int>,
    enterFilter: (Int) -> Unit,
    onFlipClicked: (path: List<Int>) -> Unit,
    onProfileQueryChanged: (String) -> Unit,
    onUpdateFilter: (filter: Filter, path: List<Int>, index: Int) -> Unit,
    onRemoveFilter: (path: List<Int>, index: Int) -> Unit,
    index: Int,
) {
    if (filter is Filter.Root) FilterRow(
        animatedVisibilityScope = animatedVisibilityScope,
        paneScaffoldState = paneScaffoldState,
        atTopLevel = atTopLevel,
        modifier = modifier,
        filter = filter,
        profileSearchResults = profileSearchResults,
        index = index,
        path = path,
        onProfileQueryChanged = onProfileQueryChanged,
        enterFilter = enterFilter,
        onFlipClicked = onFlipClicked,
        onUpdateFilter = onUpdateFilter,
        onRemoveFilter = onRemoveFilter,
    )
    else FilterLeaf(
        modifier = modifier,
        filter = filter,
        profileSearchResults = profileSearchResults,
        onProfileQueryChanged = onProfileQueryChanged,
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
    animatedVisibilityScope: AnimatedVisibilityScope,
    paneScaffoldState: PaneScaffoldState,
    atTopLevel: Boolean,
    index: Int,
    filter: Filter.Root,
    path: List<Int>,
    profileSearchResults: List<Profile>,
    onProfileQueryChanged: (String) -> Unit,
    enterFilter: (Int) -> Unit,
    onFlipClicked: (path: List<Int>) -> Unit,
    onUpdateFilter: (filter: Filter, path: List<Int>, index: Int) -> Unit,
    onRemoveFilter: (path: List<Int>, index: Int) -> Unit,
    modifier: Modifier = Modifier,
) = with(paneScaffoldState) {
    Card(
        modifier = modifier
            .sharedBounds(
                sharedContentState = rememberSharedContentState(filter.backgroundSharedElementKey()),
                animatedVisibilityScope = animatedVisibilityScope,
            ),
        onClick = {
            enterFilter(index)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RootFilterDescription(
                animatedVisibilityScope = animatedVisibilityScope,
                paneScaffoldState = paneScaffoldState,
                id = filter.id,
                modifier = Modifier
                    .fillMaxWidth(),
                isAnd = filter is Filter.And,
                size = filter.filters.size,
                onFlipClicked = {
                    onFlipClicked(path + index)
                },
                onRemove = {
                    onRemoveFilter(path, index)
                },
            )

            if (atTopLevel) {
                val lazyListState = rememberLazyListState()
                LazyRow(
                    modifier = Modifier
                        .border(
                            width = Dp.Hairline,
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = FilterRowShape,
                        )
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    flingBehavior = rememberSnapFlingBehavior(
                        lazyListState = lazyListState,
                    ),
                    state = lazyListState,
                ) {
                    itemsIndexed(
                        items = filter.filters,
                        key = { _, child -> child.id.value },
                        itemContent = { childIndex, child ->
                            Filter(
                                modifier = Modifier
                                    .animateItem()
                                    .fillParentMaxWidth()
                                    .padding(8.dp),
                                animatedVisibilityScope = animatedVisibilityScope,
                                paneScaffoldState = paneScaffoldState,
                                profileSearchResults = profileSearchResults,
                                filter = child,
                                atTopLevel = false,
                                index = childIndex,
                                path = path + index,
                                enterFilter = enterFilter,
                                onProfileQueryChanged = onProfileQueryChanged,
                                onFlipClicked = onFlipClicked,
                                onUpdateFilter = onUpdateFilter,
                                onRemoveFilter = onRemoveFilter,
                            )
                        },
                    )
                }
                Indicator(
                    lazyListState = lazyListState,
                    indicatorSize = 4.dp,
                )
            }
        }
    }
}

@Composable
private fun RootFilterDescription(
    modifier: Modifier,
    animatedVisibilityScope: AnimatedVisibilityScope,
    paneScaffoldState: PaneScaffoldState,
    id: Filter.Id,
    isAnd: Boolean,
    size: Int,
    onFlipClicked: () -> Unit,
    onRemove: (() -> Unit)?,
) = with(paneScaffoldState) {
    Column(
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .sharedElement(
                        sharedContentState = rememberSharedContentState("$id-title"),
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                    .weight(1f),
                text = stringResource(
                    if (isAnd) Res.string.all_of_these_and
                    else Res.string.any_of_these_or,
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            IconButton(
                modifier = Modifier
                    .sharedElement(
                        sharedContentState = rememberSharedContentState("$id-icon"),
                        animatedVisibilityScope = animatedVisibilityScope,
                    ),
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
            if (onRemove != null) IconButton(
                onClick = onRemove,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(Res.string.remove_filter),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            modifier = Modifier
                .sharedElement(
                    sharedContentState = rememberSharedContentState("$id-description"),
                    animatedVisibilityScope = animatedVisibilityScope,
                ),
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
    profileSearchResults: List<Profile>,
    onProfileQueryChanged: (String) -> Unit,
    onUpdate: (Filter) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (filter) {
        is Filter.Attribute.Compare -> AttributeCompareFilter(
            modifier = modifier,
            filter = filter,
            onUpdate = onUpdate,
            onRemove = onRemove,
        )
        is Filter.Attribute.Embed -> AttributeEmbedFilter(
            modifier = modifier,
            filter = filter,
            onUpdate = onUpdate,
            onRemove = onRemove,
        )
        is Filter.Entity.Matches,
        is Filter.Entity.Excludes,
        -> EntityFilter(
            modifier = modifier,
            filter = filter,
            onUpdate = onUpdate,
            onRemove = onRemove,
        )
        is Filter.Regex.Matches,
        is Filter.Regex.Negation,
        is Filter.Regex.Any,
        is Filter.Regex.None,
        ->
            RegexFilter(
                modifier = modifier,
                filter = filter,
                onRemove = onRemove,
            )

        is Filter.Social.Graph -> SocialGraphFilter(
            modifier = modifier,
            filter = filter,
            results = profileSearchResults,
            onProfileQueryChanged = onProfileQueryChanged,
            onUpdate = onUpdate,
            onRemove = onRemove,
        )
        is Filter.Social.UserList -> SocialUserListFilter(
            modifier = modifier,
            filter = filter,
            results = profileSearchResults,
            onProfileQueryChanged = onProfileQueryChanged,
            onUpdate = onUpdate,
            onRemove = onRemove,
        )
        is Filter.Social.StarterPack -> SocialStarterPackFilter(
            modifier = modifier,
            filter = filter,
            onUpdate = onUpdate,
            onRemove = onRemove,
        )
        is Filter.Social.ListMember -> SocialListMemberFilter(
            modifier = modifier,
            filter = filter,
            onUpdate = onUpdate,
            onRemove = onRemove,
        )
        is Filter.Social.MagicAudience -> SocialMagicAudienceFilter(
            modifier = modifier,
            filter = filter,
            onUpdate = onUpdate,
            onRemove = onRemove,
        )
        is Filter.ML.Similarity -> UnsupportedFilter(
            modifier = modifier,
            title = stringResource(Res.string.text_similarity),
            onRemove = onRemove,
        )
        is Filter.ML.Probability -> UnsupportedFilter(
            modifier = modifier,
            title = stringResource(Res.string.model_probability),
            onRemove = onRemove,
        )
        is Filter.ML.Moderation -> MLModerationFilter(
            modifier = modifier,
            filter = filter,
            onUpdate = onUpdate,
            onRemove = onRemove,
        )
        is Filter.Analysis -> AnalysisFilter(
            modifier = modifier,
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

private fun Filter.Root.backgroundSharedElementKey(): String = "$id-background"

private val FilterRowShape = RoundedCornerShape(8.dp)

private val FilterTransitionSpec = fadeIn() togetherWith fadeOut()

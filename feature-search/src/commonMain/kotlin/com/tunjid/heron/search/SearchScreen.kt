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

package com.tunjid.heron.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.SearchResult
import com.tunjid.heron.data.core.models.Trend
import com.tunjid.heron.data.core.models.Trends
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.scaffold.BottomNavHeight
import com.tunjid.heron.scaffold.scaffold.StatusBarHeight
import com.tunjid.heron.scaffold.scaffold.ToolbarHeight
import com.tunjid.heron.search.ui.PostSearchResult
import com.tunjid.heron.search.ui.ProfileSearchResult
import com.tunjid.heron.search.ui.avatarSharedElementKey
import com.tunjid.heron.search.ui.sharedElementPrefix
import com.tunjid.heron.timeline.ui.avatarSharedElementKey
import com.tunjid.heron.ui.PanedSharedElementScope
import com.tunjid.heron.ui.Tab
import com.tunjid.heron.ui.Tabs
import com.tunjid.heron.ui.tabIndex
import com.tunjid.tiler.compose.PivotedTilingEffect
import heron.feature_search.generated.resources.Res
import heron.feature_search.generated.resources.latest
import heron.feature_search.generated.resources.people
import heron.feature_search.generated.resources.recommended_description
import heron.feature_search.generated.resources.recommended_title
import heron.feature_search.generated.resources.top
import heron.feature_search.generated.resources.trending_description
import heron.feature_search.generated.resources.trending_title
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SearchScreen(
    panedSharedElementScope: PanedSharedElementScope,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
    ) {
        Spacer(Modifier.height(ToolbarHeight + StatusBarHeight))
        val pagerState = rememberPagerState {
            3
        }
        val onProfileSearchResultClicked: (SearchResult.Profile) -> Unit = remember {
            { profileSearchResult ->
                actions(
                    Action.Navigate.DelegateTo(
                        NavigationAction.Common.ToProfile(
                            profile = profileSearchResult.profileWithRelationship.profile,
                            avatarSharedElementKey = profileSearchResult.avatarSharedElementKey(),
                            referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent
                        )
                    )
                )
            }
        }
        val onPostSearchResultProfileClicked = remember {
            { result: SearchResult.Post ->
                actions(
                    Action.Navigate.DelegateTo(
                        NavigationAction.Common.ToProfile(
                            referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                            profile = result.post.author,
                            avatarSharedElementKey = result.post.avatarSharedElementKey(
                                result.sharedElementPrefix()
                            )
                        )
                    )
                )
            }
        }
        val onPostSearchResultClicked = remember {
            { result: SearchResult.Post ->
                actions(
                    Action.Navigate.DelegateTo(
                        NavigationAction.Common.ToPost(
                            referringRouteOption = NavigationAction.ReferringRouteOption.ParentOrCurrent,
                            sharedElementPrefix = result.sharedElementPrefix(),
                            post = result.post,
                        )
                    )
                )
            }
        }
        val onPostInteraction = remember {
            { interaction: Post.Interaction ->
                actions(
                    Action.SendPostInteraction(interaction)
                )
            }
        }
        AnimatedContent(
            targetState = state.layout
        ) { targetLayout ->
            when (targetLayout) {
                ScreenLayout.Trends -> Trends(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp),
                    trends = state.trends,
                    onTrendClicked = { trend ->
                        actions(
                            Action.Navigate.DelegateTo(
                                NavigationAction.Common.ToRawUrl(trend.link)
                            )
                        )
                    },
                )

                ScreenLayout.AutoCompleteProfiles -> AutoCompleteProfileSearchResults(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp),
                    panedSharedElementScope = panedSharedElementScope,
                    results = state.autoCompletedProfiles,
                    onProfileClicked = onProfileSearchResultClicked,
                )

                ScreenLayout.GeneralSearchResults -> TabbedSearchResults(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    pagerState = pagerState,
                    state = state,
                    panedSharedElementScope = panedSharedElementScope,
                    onProfileClicked = onProfileSearchResultClicked,
                    onPostSearchResultProfileClicked = onPostSearchResultProfileClicked,
                    onPostSearchResultClicked = onPostSearchResultClicked,
                    onPostInteraction = onPostInteraction,
                )
            }
        }
    }
}

@Composable
private fun Trends(
    modifier: Modifier = Modifier,
    trends: Trends,
    onTrendClicked: (Trend) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(
            horizontal = 24.dp,
        )
    ) {
        item {
            TrendTitle(
                icon = Icons.AutoMirrored.Rounded.ShowChart,
                title = stringResource(Res.string.trending_title),
                description = stringResource(Res.string.trending_description),
            )
        }
        item {
            TrendChips(
                trendList = trends.topics,
                onTrendClicked = onTrendClicked,
            )
        }
        item {
            TrendTitle(
                icon = Icons.Rounded.Tag,
                title = stringResource(Res.string.recommended_title),
                description = stringResource(Res.string.recommended_description),
            )
        }
        item {
            TrendChips(
                trendList = trends.suggested,
                onTrendClicked = onTrendClicked,
            )
        }
        item {
            Spacer(
                Modifier
                    .padding(WindowInsets.navigationBars.asPaddingValues())
                    .height(BottomNavHeight)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TrendTitle(
    icon: ImageVector,
    title: String,
    description: String?,
) {
    Column(
        modifier = Modifier.padding(
            vertical = 4.dp,
        )
    ) {
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Icon(
                modifier = Modifier
                    .size(24.dp),
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMediumEmphasized
            )
        }
        Spacer(Modifier.height(8.dp))
        if (description != null) Text(
            text = description,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrendChips(
    trendList: List<Trend>,
    onTrendClicked: (Trend) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        trendList.forEach { trend ->
            SuggestionChip(
                shape = CircleShape,
                onClick = { onTrendClicked(trend) },
                label = {
                    Text(text = trend.topic)
                },
            )
        }
    }
}

@Composable
private fun AutoCompleteProfileSearchResults(
    panedSharedElementScope: PanedSharedElementScope,
    modifier: Modifier = Modifier,
    results: List<SearchResult.Profile>,
    onProfileClicked: (SearchResult.Profile) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
        )
    ) {
        items(
            items = results,
            key = { it.profileWithRelationship.profile.did.id },
            itemContent = { result ->
                ProfileSearchResult(
                    panedSharedElementScope = panedSharedElementScope,
                    result = result,
                    onProfileClicked = onProfileClicked
                )
            }
        )
    }
}

@Composable
private fun TabbedSearchResults(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    state: State,
    panedSharedElementScope: PanedSharedElementScope,
    onProfileClicked: (SearchResult.Profile) -> Unit,
    onPostSearchResultProfileClicked: (SearchResult.Post) -> Unit,
    onPostSearchResultClicked: (SearchResult.Post) -> Unit,
    onPostInteraction: (Post.Interaction) -> Unit,
) {
    Column(
        modifier = modifier
    ) {
        val scope = rememberCoroutineScope()
        Tabs(
            tabs = listOf(
                Tab(
                    title = stringResource(resource = Res.string.top),
                    hasUpdate = false
                ),
                Tab(
                    title = stringResource(resource = Res.string.latest),
                    hasUpdate = false
                ),
                Tab(
                    title = stringResource(resource = Res.string.people),
                    hasUpdate = false
                ),
            ),
            modifier = Modifier.fillMaxWidth(),
            selectedTabIndex = pagerState.tabIndex,
            onTabSelected = {
                scope.launch {
                    pagerState.animateScrollToPage(it)
                }
            },
            onTabReselected = { },
        )
        HorizontalPager(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                    )
                ),
            state = pagerState,
            key = { page -> page },
            pageContent = { page ->
                val searchResultStateHolder = remember { state.searchStateHolders[page] }
                SearchResults(
                    panedSharedElementScope = panedSharedElementScope,
                    searchResultStateHolder = searchResultStateHolder,
                    onProfileClicked = onProfileClicked,
                    onPostSearchResultProfileClicked = onPostSearchResultProfileClicked,
                    onPostSearchResultClicked = onPostSearchResultClicked,
                    onPostInteraction = onPostInteraction,
                )
            }
        )
    }
}

@Composable
private fun SearchResults(
    modifier: Modifier = Modifier,
    panedSharedElementScope: PanedSharedElementScope,
    searchResultStateHolder: SearchResultStateHolder,
    onProfileClicked: (SearchResult.Profile) -> Unit,
    onPostSearchResultProfileClicked: (SearchResult.Post) -> Unit,
    onPostSearchResultClicked: (SearchResult.Post) -> Unit,
    onPostInteraction: (Post.Interaction) -> Unit,
) {
    val searchState = searchResultStateHolder.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    when (val state = searchState.value) {
        is SearchState.Post -> {
            val now = remember { Clock.System.now() }
            val results by rememberUpdatedState(state.results)
            LazyColumn(
                modifier = modifier,
                state = listState,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(
                    items = results,
                    key = { it.post.cid.id },
                    itemContent = { result ->
                        PostSearchResult(
                            panedSharedElementScope = panedSharedElementScope,
                            now = now,
                            result = result,
                            onProfileClicked = onPostSearchResultProfileClicked,
                            onPostClicked = onPostSearchResultClicked,
                            onPostInteraction = onPostInteraction,
                        )
                    }
                )
            }
            listState.PivotedTilingEffect(
                items = results,
                onQueryChanged = { query ->
                    searchResultStateHolder.accept(
                        SearchState.LoadAround(query = query ?: state.currentQuery)
                    )
                }
            )
        }

        is SearchState.Profile -> {
            val results by rememberUpdatedState(state.results)
            LazyColumn(
                modifier = modifier,
                state = listState,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(
                    items = results,
                    key = { it.profileWithRelationship.profile.did.id },
                    itemContent = { result ->
                        ProfileSearchResult(
                            panedSharedElementScope = panedSharedElementScope,
                            result = result,
                            onProfileClicked = onProfileClicked
                        )
                    }
                )
            }
            listState.PivotedTilingEffect(
                items = results,
                onQueryChanged = { query ->
                    searchResultStateHolder.accept(
                        SearchState.LoadAround(query = query ?: state.currentQuery)
                    )
                }
            )
        }
    }
}
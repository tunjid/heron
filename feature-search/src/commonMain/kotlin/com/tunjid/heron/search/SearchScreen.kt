/*
 * Copyright 2024 Adetunji Dahunsi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.heron.search

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.SearchResult
import com.tunjid.heron.data.core.models.contentDescription
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.ui.AttributionLayout
import com.tunjid.heron.ui.SharedElementScope
import com.tunjid.heron.ui.Tab
import com.tunjid.heron.ui.Tabs
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.tabIndex
import heron.feature_search.generated.resources.Res
import heron.feature_search.generated.resources.latest
import heron.feature_search.generated.resources.people
import heron.feature_search.generated.resources.top
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SearchScreen(
    sharedElementScope: SharedElementScope,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
    ) {
        val pagerState = rememberPagerState {
            3
        }
        when(state.layout) {
            ScreenLayout.Trends -> Unit
            ScreenLayout.AutoCompleteProfiles -> AutoCompleteProfileSearchResults(
                modifier = Modifier.fillMaxSize(),
                results = state.autoCompletedProfiles,
            )
            ScreenLayout.GeneralSearchResults -> TabbedSearchResults(
                modifier = Modifier.fillMaxSize(),
                pagerState = pagerState,
                state = state,
                sharedElementScope = sharedElementScope,
            )
        }
    }
}

@Composable
private fun AutoCompleteProfileSearchResults(
    modifier: Modifier = Modifier,
    results: List<SearchResult.Profile>,
) {
    LazyColumn(
        modifier = modifier
    ) {
        items(
            items = results,
            key = { it.profile.did.id },
            itemContent = { result ->
                AttributionLayout(
                    avatar = {
                        AsyncImage(
                            modifier = Modifier
                                .size(48.dp),
                            args = remember {
                                ImageArgs(
                                    url = result.profile.avatar?.uri,
                                    contentScale = ContentScale.Crop,
                                    contentDescription = result.profile.contentDescription,
                                    shape = RoundedPolygonShape.Circle,
                                )
                            },
                        )
                    },
                    label = {},
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
    sharedElementScope: SharedElementScope,
) {
    Column {
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
            modifier = modifier,
            selectedTabIndex = pagerState.tabIndex,
            onTabSelected = { },
            onTabReselected = { },
        )
        HorizontalPager(
            modifier = Modifier
                .padding(horizontal = 8.dp)
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
                    sharedElementScope = sharedElementScope,
                    searchResultStateHolder = searchResultStateHolder,
                )
            }
        )
    }
}

@Composable
private fun SearchResults(
    sharedElementScope: SharedElementScope,
    searchResultStateHolder: SearchResultStateHolder,
) {

}
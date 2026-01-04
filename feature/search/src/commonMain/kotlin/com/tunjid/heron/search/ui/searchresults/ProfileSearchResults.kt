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

package com.tunjid.heron.search.ui.searchresults

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.models.contentDescription
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.search.SearchResult
import com.tunjid.heron.search.SearchState
import com.tunjid.heron.search.sharedElementPrefix
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.tiledItems
import com.tunjid.heron.timeline.ui.profile.ProfileHandle
import com.tunjid.heron.timeline.ui.profile.ProfileName
import com.tunjid.heron.timeline.ui.profile.ProfileViewerState
import com.tunjid.heron.ui.AttributionLayout
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.UiTokens.bottomNavAndInsetPaddingValues
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.tiler.compose.PivotedTilingEffect
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import com.tunjid.treenav.compose.UpdatedMovableStickySharedElementOf

@Composable
internal fun AutoCompleteProfileSearchResults(
    paneMovableElementSharedTransitionScope: PaneScaffoldState,
    modifier: Modifier = Modifier,
    results: List<SearchResult.OfProfile>,
    onProfileClicked: (Profile, String) -> Unit,
    onViewerStateClicked: (ProfileWithViewerState) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = bottomNavAndInsetPaddingValues(
            top = UiTokens.statusBarHeight + UiTokens.toolbarHeight,
            horizontal = 16.dp,
            isCompact = paneMovableElementSharedTransitionScope.usesCompactBottomNavigation,
        ),
    ) {
        items(
            items = results,
            key = { it.profileWithViewerState.profile.did.id },
            itemContent = { result ->
                ProfileSearchResult(
                    paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                    result = result,
                    sharedElementPrefix = AutoCompleteProfilesSharedElementPrefix,
                    onProfileClicked = onProfileClicked,
                    onViewerStateClicked = { onViewerStateClicked(it.profileWithViewerState) },
                )
            },
        )
    }
}

@Composable
internal fun ProfileSearchResults(
    state: SearchState.OfProfiles,
    listState: LazyListState,
    modifier: Modifier,
    paneScaffoldState: PaneScaffoldState,
    onProfileClicked: (Profile, String) -> Unit,
    onViewerStateClicked: (SearchResult.OfProfile) -> Unit,
    searchResultActions: (SearchState.Tile) -> Unit,
) {
    val results by rememberUpdatedState(state.tiledItems)
    LazyColumn(
        modifier = modifier,
        state = listState,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = bottomNavAndInsetPaddingValues(
            top = UiTokens.statusBarHeight + UiTokens.toolbarHeight + UiTokens.tabsHeight,
            isCompact = paneScaffoldState.usesCompactBottomNavigation,
        ),
    ) {
        items(
            items = results,
            key = { it.profileWithViewerState.profile.did.id },
            itemContent = { result ->
                ProfileSearchResult(
                    paneMovableElementSharedTransitionScope = paneScaffoldState,
                    result = result,
                    sharedElementPrefix = state.sharedElementPrefix,
                    onProfileClicked = onProfileClicked,
                    onViewerStateClicked = onViewerStateClicked,
                )
            },
        )
    }
    listState.PivotedTilingEffect(
        items = results,
        onQueryChanged = { query ->
            searchResultActions(
                SearchState.Tile(
                    tilingAction = TilingState.Action.LoadAround(
                        query ?: state.tilingData.currentQuery,
                    ),
                ),
            )
        },
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ProfileSearchResult(
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    result: SearchResult.OfProfile,
    sharedElementPrefix: String,
    onProfileClicked: (Profile, String) -> Unit,
    onViewerStateClicked: (SearchResult.OfProfile) -> Unit,
) = with(paneMovableElementSharedTransitionScope) {
    AttributionLayout(
        modifier = Modifier
            .clickable {
                onProfileClicked(
                    result.profileWithViewerState.profile,
                    sharedElementPrefix,
                )
            },
        avatar = {
            UpdatedMovableStickySharedElementOf(
                modifier = Modifier
                    .size(UiTokens.avatarSize)
                    .clickable {
                        onProfileClicked(
                            result.profileWithViewerState.profile,
                            sharedElementPrefix,
                        )
                    },
                sharedContentState = with(paneMovableElementSharedTransitionScope) {
                    rememberSharedContentState(
                        key = result.profileWithViewerState
                            .profile
                            .avatarSharedElementKey(sharedElementPrefix),
                    )
                },
                state = remember(result.profileWithViewerState.profile.avatar) {
                    ImageArgs(
                        url = result.profileWithViewerState.profile.avatar?.uri,
                        contentScale = ContentScale.Crop,
                        contentDescription = result.profileWithViewerState.profile.contentDescription,
                        shape = RoundedPolygonShape.Circle,
                    )
                },
                sharedElement = { state, modifier ->
                    AsyncImage(state, modifier)
                },
            )
        },
        label = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ProfileName(
                    profile = result.profileWithViewerState.profile,
                )
                ProfileHandle(
                    profile = result.profileWithViewerState.profile,
                )
            }
        },
        action = {
            ProfileViewerState(
                viewerState = result.profileWithViewerState.viewerState,
                isSignedInProfile = false,
                onClick = { onViewerStateClicked(result) },
            )
        },
    )
}

internal fun Profile.avatarSharedElementKey(
    prefix: String,
): String =
    "$prefix-${did.id}"

private const val AutoCompleteProfilesSharedElementPrefix = "search-profile-auto-complete-results"

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

package com.tunjid.heron.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.profileDestination
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.scaffold.scaffold.paneClip
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.tiledItems
import com.tunjid.heron.timeline.ui.profile.ProfileWithViewerState
import com.tunjid.heron.ui.UiTokens.bottomNavAndInsetPaddingValues
import com.tunjid.tiler.compose.PivotedTilingEffect

@Composable
internal fun ProfilesScreen(
    paneScaffoldState: PaneScaffoldState,
    modifier: Modifier = Modifier,
    state: State,
    actions: (Action) -> Unit,
) {
    val listState = rememberLazyListState()
    val items by rememberUpdatedState(state.tiledItems)
    val signedInProfileId by rememberUpdatedState(state.signedInProfileId)
    LazyColumn(
        modifier = modifier.padding(horizontal = 8.dp).fillMaxSize().paneClip(),
        state = listState,
        contentPadding =
            bottomNavAndInsetPaddingValues(
                horizontal = 8.dp,
                isCompact = paneScaffoldState.prefersCompactBottomNav,
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = !paneScaffoldState.isTransitionActive,
    ) {
        items(
            items = items,
            key = { it.profile.did.id },
            itemContent = { item ->
                ProfileWithViewerState(
                    modifier = Modifier.fillMaxWidth().animateItem(),
                    movableElementSharedTransitionScope = paneScaffoldState,
                    signedInProfileId = signedInProfileId,
                    profile = item.profile,
                    viewerState = item.viewerState,
                    profileSharedElementKey =
                        Profile::profileWithRelationshipAvatarSharedElementKey,
                    onProfileClicked = { profile ->
                        actions(
                            Action.Navigate.To(
                                profileDestination(
                                    referringRouteOption =
                                        NavigationAction.ReferringRouteOption.Current,
                                    profile = profile,
                                    avatarSharedElementKey =
                                        item.profile.profileWithRelationshipAvatarSharedElementKey(),
                                )
                            )
                        )
                    },
                    onViewerStateClicked = { viewerState ->
                        state.signedInProfileId?.let {
                            actions(
                                Action.ToggleViewerState(
                                    signedInProfileId = it,
                                    viewedProfileId = item.profile.did,
                                    following = viewerState?.following,
                                    followedBy = viewerState?.followedBy,
                                )
                            )
                        }
                    },
                )
            },
        )
    }

    listState.PivotedTilingEffect(
        items = items,
        onQueryChanged = { query ->
            actions(
                Action.Tile(TilingState.Action.LoadAround(query ?: state.tilingData.currentQuery))
            )
        },
    )
}

private fun Profile.profileWithRelationshipAvatarSharedElementKey() = "profiles-$did"

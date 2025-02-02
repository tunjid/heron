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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tunjid.heron.profiles.ui.ProfileWithRelationship
import com.tunjid.heron.profiles.ui.sharedElementKey
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.tiler.compose.PivotedTilingEffect

@Composable
internal fun ProfilesScreen(
    paneScaffoldState: PaneScaffoldState,
    modifier: Modifier = Modifier,
    state: State,
    actions: (Action) -> Unit,
) {
    val listState = rememberLazyListState()
    val items by rememberUpdatedState(state.profiles)
    val signedInProfileId by rememberUpdatedState(state.signedInProfileId)
    LazyColumn(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .fillMaxSize()
            .clip(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                )
            ),
        state = listState,
        contentPadding = PaddingValues(
            start = 8.dp,
            end = 8.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = !paneScaffoldState.isTransitionActive,
    ) {
        items(
            items = items,
            key = { it.profile.did.id },
            itemContent = { item ->
                ProfileWithRelationship(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                    panedSharedElementScope = paneScaffoldState,
                    profileWithViewerState = item,
                    signedInProfileId = signedInProfileId,
                    onProfileClicked = { profile ->
                        actions(
                            Action.Navigate.DelegateTo(
                                NavigationAction.Common.ToProfile(
                                    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                    profile = profile,
                                    avatarSharedElementKey = item.sharedElementKey()
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
            }
        )
    }

    listState.PivotedTilingEffect(
        items = items,
        onQueryChanged = { query ->
            actions(
                Action.LoadAround(query ?: state.currentQuery)
            )
        }
    )
}


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

package com.tunjid.heron.search.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.contentDescription
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.search.SearchResult
import com.tunjid.heron.timeline.ui.profile.ProfileHandle
import com.tunjid.heron.timeline.ui.profile.ProfileName
import com.tunjid.heron.timeline.ui.profile.ProfileViewerState
import com.tunjid.heron.ui.AttributionLayout
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableStickySharedElementOf

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileSearchResult(
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    result: SearchResult.OfProfile,
    onProfileClicked: (SearchResult.OfProfile) -> Unit,
    onViewerStateClicked: (SearchResult.OfProfile) -> Unit,
) = with(paneMovableElementSharedTransitionScope) {
    AttributionLayout(
        modifier = Modifier
            .clickable { onProfileClicked(result) },
        avatar = {
            updatedMovableStickySharedElementOf(
                modifier = Modifier
                    .size(UiTokens.avatarSize)
                    .clickable { onProfileClicked(result) },
                sharedContentState = with(paneMovableElementSharedTransitionScope) {
                    rememberSharedContentState(
                        key = result.avatarSharedElementKey(),
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

internal fun SearchResult.OfProfile.avatarSharedElementKey(): String =
    "$sharedElementPrefix-${profileWithViewerState.profile.did.id}"

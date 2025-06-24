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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.models.contentDescription
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.ui.profile.ProfileHandle
import com.tunjid.heron.timeline.ui.profile.ProfileName
import com.tunjid.heron.timeline.ui.profile.ProfileViewerState
import com.tunjid.heron.ui.AttributionLayout
import com.tunjid.heron.ui.UiTokens
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableSharedElementOf

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SuggestedProfile(
    modifier: Modifier = Modifier,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    profileWithViewerState: ProfileWithViewerState,
    onProfileClicked: (ProfileWithViewerState) -> Unit,
    onViewerStateClicked: (ProfileWithViewerState) -> Unit
) = with(paneMovableElementSharedTransitionScope) {
    AttributionLayout(
        modifier = modifier
            .clickable { onProfileClicked(profileWithViewerState) }
            .padding(horizontal = 24.dp),
        avatar = {
            updatedMovableSharedElementOf(
                modifier = Modifier
                    .size(UiTokens.avatarSize)
                    .clickable { onProfileClicked(profileWithViewerState) },
                sharedContentState = with(paneMovableElementSharedTransitionScope) {
                    rememberSharedContentState(
                        key = profileWithViewerState.avatarSharedElementKey(),
                    )
                },
                state = remember(profileWithViewerState.profile.avatar) {
                    ImageArgs(
                        url = profileWithViewerState.profile.avatar?.uri,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        contentDescription = profileWithViewerState.profile.contentDescription,
                        shape = com.tunjid.heron.ui.shapes.RoundedPolygonShape.Circle,
                    )
                },
                sharedElement = { state, modifier ->
                    AsyncImage(state, modifier)
                }
            )
        },
        label = {
            Column(
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
            ) {
                ProfileName(
                    profile = profileWithViewerState.profile
                )
                ProfileHandle(
                    profile = profileWithViewerState.profile
                )
            }
        },
        action = {
            ProfileViewerState(
                viewerState = profileWithViewerState.viewerState,
                isSignedInProfile = false,
                onClick = {
                    onViewerStateClicked(profileWithViewerState)
                }
            )
        }
    )
}

internal fun ProfileWithViewerState.avatarSharedElementKey(): String =
    "suggested-profile-${profile.did.id}"
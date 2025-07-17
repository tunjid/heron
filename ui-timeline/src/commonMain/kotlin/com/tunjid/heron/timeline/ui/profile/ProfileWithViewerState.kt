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

package com.tunjid.heron.timeline.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileViewerState
import com.tunjid.heron.data.core.models.contentDescription
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.ui.AttributionLayout
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableStickySharedElementOf

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileWithViewerState(
    modifier: Modifier,
    movableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    signedInProfileId: ProfileId?,
    profile: Profile,
    viewerState: ProfileViewerState?,
    profileSharedElementKey: (Profile) -> Any,
    onProfileClicked: (Profile) -> Unit,
    onViewerStateClicked: (ProfileViewerState?) -> Unit,
) = with(movableElementSharedTransitionScope) {
    AttributionLayout(
        modifier = modifier,
        avatar = {
            updatedMovableStickySharedElementOf(
                modifier = Modifier
                    .size(UiTokens.avatarSize)
                    .clickable { onProfileClicked(profile) },
                sharedContentState = with(movableElementSharedTransitionScope) {
                    rememberSharedContentState(
                        key = profileSharedElementKey(profile),
                    )
                },
                state = remember(profile.avatar) {
                    ImageArgs(
                        url = profile.avatar?.uri,
                        contentScale = ContentScale.Crop,
                        contentDescription = profile.contentDescription,
                        shape = RoundedPolygonShape.Circle,
                    )
                },
                sharedElement = { state, modifier ->
                    AsyncImage(state, modifier)
                }
            )
        },
        label = {
            Column {
                ProfileName(
                    modifier = Modifier,
                    profile = profile,
                    ellipsize = false,
                )
                Spacer(Modifier.height(4.dp))
                ProfileHandle(
                    modifier = Modifier,
                    profile = profile,
                )
            }
        },
        action = {
            val isSignedInProfile = signedInProfileId == profile.did
            AnimatedVisibility(
                visible = viewerState != null || isSignedInProfile,
                content = {
                    ProfileViewerState(
                        viewerState = viewerState,
                        isSignedInProfile = isSignedInProfile,
                        onClick = {
                            onViewerStateClicked(viewerState)
                        }
                    )
                },
            )
        },
    )
}
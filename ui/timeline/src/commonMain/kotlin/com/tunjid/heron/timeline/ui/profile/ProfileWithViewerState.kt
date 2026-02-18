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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileViewerState
import com.tunjid.heron.data.core.models.contentDescription
import com.tunjid.heron.data.core.models.followsYou
import com.tunjid.heron.data.core.models.isBlocked
import com.tunjid.heron.data.core.models.isMuted
import com.tunjid.heron.data.core.models.isRestricted
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.utilities.Label
import com.tunjid.heron.timeline.utilities.LabelFlowRow
import com.tunjid.heron.timeline.utilities.LabelIconSize
import com.tunjid.heron.timeline.utilities.LabelText
import com.tunjid.heron.ui.AttributionLayout
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.modifiers.blur
import com.tunjid.heron.ui.modifiers.ifTrue
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.text.CommonStrings
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import com.tunjid.treenav.compose.UpdatedMovableStickySharedElementOf
import heron.ui.core.generated.resources.viewer_state_blocked
import heron.ui.core.generated.resources.viewer_state_follows_you
import heron.ui.core.generated.resources.viewer_state_muted
import org.jetbrains.compose.resources.stringResource

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
) =
    with(movableElementSharedTransitionScope) {
        val profileClicked = { onProfileClicked(profile) }
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            AttributionLayout(
                modifier = Modifier,
                avatar = {
                    UpdatedMovableStickySharedElementOf(
                        modifier =
                            Modifier.size(UiTokens.avatarSize)
                                .ifTrue(viewerState.isBlocked) {
                                    blur(
                                        shape = CircleShape,
                                        radius = ::BlockedContentBlurRadius,
                                        clip = ::BlockedContentBlurClip,
                                        progress = ::BlockedContentBlurProgress,
                                    )
                                }
                                .clickable(onClick = profileClicked),
                        sharedContentState =
                            with(movableElementSharedTransitionScope) {
                                rememberSharedContentState(key = profileSharedElementKey(profile))
                            },
                        state =
                            remember(profile.avatar) {
                                ImageArgs(
                                    url = profile.avatar?.uri,
                                    contentScale = ContentScale.Crop,
                                    contentDescription = profile.contentDescription,
                                    shape = RoundedPolygonShape.Circle,
                                )
                            },
                        sharedElement = { state, modifier -> AsyncImage(state, modifier) },
                    )
                },
                label = {
                    Column {
                        ProfileName(modifier = Modifier, profile = profile, ellipsize = false)
                        Spacer(Modifier.height(4.dp))
                        ProfileHandle(modifier = Modifier, profile = profile)
                    }
                },
                action = {
                    val isSignedInProfile = signedInProfileId == profile.did
                    AnimatedVisibility(
                        visible = !isSignedInProfile && !viewerState.isRestricted,
                        content = {
                            ProfileViewerState(
                                viewerState = viewerState,
                                isSignedInProfile = isSignedInProfile,
                                onClick = { onViewerStateClicked(viewerState) },
                            )
                        },
                    )
                },
            )
            LabelFlowRow {
                if (viewerState.followsYou)
                    IconLabel(
                        icon = null,
                        contentDescription = stringResource(CommonStrings.viewer_state_follows_you),
                        onClick = profileClicked,
                    )
                if (viewerState.isBlocked)
                    IconLabel(
                        icon = Icons.Rounded.Block,
                        contentDescription = stringResource(CommonStrings.viewer_state_blocked),
                        onClick = profileClicked,
                    )
                if (viewerState.isMuted)
                    IconLabel(
                        icon = Icons.AutoMirrored.Rounded.VolumeOff,
                        contentDescription = stringResource(CommonStrings.viewer_state_muted),
                        onClick = profileClicked,
                    )
            }
        }
    }

@Composable
private fun IconLabel(icon: ImageVector?, contentDescription: String, onClick: () -> Unit) {
    Label(
        isElevated = true,
        contentDescription = contentDescription,
        icon = {
            if (icon != null)
                Icon(
                    modifier = Modifier.size(LabelIconSize),
                    imageVector = icon,
                    contentDescription = null,
                )
        },
        description = { LabelText(contentDescription) },
        onClick = onClick,
    )
}

private val BlockedContentBlurRadius = 20.dp
private const val BlockedContentBlurClip = true
private const val BlockedContentBlurProgress = 1f

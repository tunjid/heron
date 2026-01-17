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

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.ProfileViewerState
import com.tunjid.heron.data.core.models.isBlocked
import com.tunjid.heron.data.core.models.isBlockedBy
import com.tunjid.heron.data.core.models.isFollowing
import com.tunjid.heron.ui.rememberLatchedState
import com.tunjid.heron.ui.text.CommonStrings
import heron.ui.core.generated.resources.viewer_state_follow
import heron.ui.core.generated.resources.viewer_state_following
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.edit
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProfileViewerState(
    viewerState: ProfileViewerState?,
    isSignedInProfile: Boolean,
    onClick: () -> Unit,
) {
    val follows = viewerState.isFollowing
    val latchedStatus = rememberLatchedState(
        if (isSignedInProfile) FollowStatus.Edit
        else if (follows) FollowStatus.Following
        else FollowStatus.NotFollowing,
    )
    val followStatusText = stringResource(
        when (latchedStatus.value) {
            FollowStatus.Edit -> Res.string.edit
            FollowStatus.Following -> CommonStrings.viewer_state_following
            FollowStatus.NotFollowing -> CommonStrings.viewer_state_follow
        },
    )
    if (!viewerState.isBlocked && !viewerState.isBlockedBy) FilterChip(
        modifier = Modifier
            .animateContentSize(),
        selected = latchedStatus.value == FollowStatus.Following,
        onClick = {
            // Try to show feedback as quickly as possible
            if (!isSignedInProfile && viewerState != null) latchedStatus.latch(
                if (follows) FollowStatus.NotFollowing
                else FollowStatus.Following,
            )
            onClick()
        },
        shape = FollowChipShape,
        leadingIcon = {
            Icon(
                imageVector = when (latchedStatus.value) {
                    FollowStatus.Edit -> Icons.Rounded.Edit
                    FollowStatus.Following -> Icons.Rounded.Check
                    FollowStatus.NotFollowing -> Icons.Rounded.Add
                },
                contentDescription = followStatusText,
            )
        },
        label = {
            Text(followStatusText)
        },
    )
}

private enum class FollowStatus {
    Edit,
    NotFollowing,
    Following,
}

private val FollowChipShape = RoundedCornerShape(16.dp)

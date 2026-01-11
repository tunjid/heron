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
import com.tunjid.heron.data.core.models.isFollowing
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.edit
import heron.ui.timeline.generated.resources.follow
import heron.ui.timeline.generated.resources.follow_back
import heron.ui.timeline.generated.resources.following
import heron.ui.timeline.generated.resources.mutuals
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProfileViewerState(
    viewerState: ProfileViewerState?,
    isSignedInProfile: Boolean,
    onClick: () -> Unit,
) {
    val follows = viewerState.isFollowing
    val followsYou = viewerState?.followedBy != null
    val followStatusText = stringResource(
        when {
            isSignedInProfile -> Res.string.edit
            follows && followsYou -> Res.string.mutuals
            follows -> Res.string.following
            followsYou -> Res.string.follow_back
            else -> Res.string.follow
        },
    )
    FilterChip(
        modifier = Modifier
            .animateContentSize(),
        selected = follows,
        onClick = onClick,
        shape = FollowChipShape,
        leadingIcon = {
            Icon(
                imageVector =
                if (isSignedInProfile) Icons.Rounded.Edit
                else if (follows) Icons.Rounded.Check
                else Icons.Rounded.Add,
                contentDescription = followStatusText,
            )
        },
        label = {
            Text(followStatusText)
        },
    )
}

private val FollowChipShape = RoundedCornerShape(16.dp)

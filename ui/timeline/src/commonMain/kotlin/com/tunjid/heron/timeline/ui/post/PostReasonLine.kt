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

package com.tunjid.heron.timeline.ui.post

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.TimelineItem
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.repost
import heron.ui.timeline.generated.resources.repost_by
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PostReasonLine(
    modifier: Modifier = Modifier,
    item: TimelineItem,
    onProfileClicked: (Post, Profile) -> Unit,
) {
    when (item) {
        is TimelineItem.Pinned -> PostPinnedReasonLine(
            modifier = modifier,
        )

        is TimelineItem.Repost -> PostRepostReasonLine(
            modifier = modifier,
            repostBy = item.by,
            onProfileClicked = {
                onProfileClicked(item.post, it)
            },
        )

        is TimelineItem.Thread,
        is TimelineItem.Single,
        is TimelineItem.Loading,
        -> Unit
    }
}

@Composable
private fun PostRepostReasonLine(
    modifier: Modifier = Modifier,
    repostBy: Profile,
    onProfileClicked: (Profile) -> Unit,
) {
    PostReasonLine(
        modifier = modifier.clickable { onProfileClicked(repostBy) },
        imageVector = Icons.Rounded.Repeat,
        iconContentDescription = stringResource(Res.string.repost),
        text = stringResource(
            Res.string.repost_by,
            repostBy.displayName ?: repostBy.handle,
        ),
    )
}

@Composable
private fun PostPinnedReasonLine(
    modifier: Modifier = Modifier,
) {
    PostReasonLine(
        modifier = modifier,
        imageVector = Icons.Rounded.Star,
        iconContentDescription = "Pinned",
        text = "Pinned",
    )
}

@Composable
private fun PostReasonLine(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    iconContentDescription: String,
    text: String,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = spacedBy(4.dp),
    ) {
        Icon(
            modifier = Modifier.size(12.dp),
            imageVector = imageVector,
            contentDescription = iconContentDescription,
        )

        Text(
            text = text,
            maxLines = 1,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

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
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Profile
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.reply
import heron.ui.timeline.generated.resources.reply_to
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PostReplyLine(
    replyingTo: Profile,
    onReplyTapped: (Profile) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.clickable { onReplyTapped(replyingTo) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = spacedBy(4.dp),
    ) {
        Icon(
            modifier = Modifier.size(12.dp),
            imageVector = Icons.AutoMirrored.Rounded.Reply,
            contentDescription = stringResource(Res.string.reply),
            tint = MaterialTheme.typography.bodySmall.color,
        )

        Text(
            text = stringResource(
                Res.string.reply_to,
                replyingTo.displayName ?: replyingTo.handle.id,
            ),
            maxLines = 1,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

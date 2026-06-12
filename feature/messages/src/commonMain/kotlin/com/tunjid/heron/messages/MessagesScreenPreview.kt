/*
 *    Copyright 2026 Adetunji Dahunsi
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

package com.tunjid.heron.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tunjid.heron.ui.modifiers.shapedClickable

/**
 * Mirrors the `Conversation` row from [MessagesScreen] — same fixed
 * `Row` height, `shapedClickable` clip and [ConversationDetails] column — with a
 * static avatar placeholder in place of the shared-element member avatars.
 *
 * @param rowHeight pass the production fixed height to reproduce the clipped
 * summary, or `null` to use `heightIn(min = …)` and see the row breathe.
 */
@Composable
private fun PreviewConversationRow(
    row: PreviewFixtures.ConversationRow,
    rowHeight: Dp?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (rowHeight != null) it.height(rowHeight) else it.heightIn(min = 68.dp) }
            .shapedClickable {}
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier.width(64.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
            )
        }
        ConversationDetails(
            participants = listOf(row.profile),
            signedInProfileId = null,
            conversationSummary = row.summary,
        )
    }
}

@Composable
private fun PreviewMessageList(rowHeight: Dp?) {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                PreviewFixtures.conversations.forEach { row ->
                    PreviewConversationRow(row = row, rowHeight = rowHeight)
                }
            }
        }
    }
}

/**
 * Multi-preview that fans the message list across device widths. Width is the
 * dimension the Desktop renderer varies, and narrower widths stress the summary
 * wrapping / ellipsis where the clipping shows up.
 */
@Preview(name = "Compact width 320dp", widthDp = 320)
@Preview(name = "Phone width 360dp", widthDp = 360)
@Preview(name = "Expanded width 480dp", widthDp = 480)
annotation class MessageListPreviews

/** Reproduces the reported bug: the fixed 68dp row clips the summary line. */
@MessageListPreviews
@Composable
internal fun MessageListFixedHeightPreview() {
    PreviewMessageList(rowHeight = 68.dp)
}

/** Same rows with heightIn(min = 68dp) — the summary is no longer clipped. */
@MessageListPreviews
@Composable
internal fun MessageListMinHeightPreview() {
    PreviewMessageList(rowHeight = null)
}

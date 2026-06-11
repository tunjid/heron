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

package com.tunjid.heron.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Renders the real [MessageRow] from [ConversationScreen] with a static avatar
 * placeholder in place of the shared element, so the production layout (not a
 * copy of it) is previewed without a `PaneScaffoldState`.
 */
@Composable
private fun PreviewMessageRow(
    item: MessageItem,
    side: Side,
) {
    MessageRow(
        item = item,
        side = side,
        isFirstMessageByAuthor = true,
        isLastMessageByAuthor = true,
        avatar = {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.secondaryContainer),
            )
        },
    )
}

@Composable
private fun PreviewConversation() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                PreviewMessageRow(
                    item = PreviewFixtures.designReviewQuestion,
                    side = Side.Receiver,
                )
                PreviewMessageRow(
                    item = PreviewFixtures.confirmation,
                    side = Side.Sender,
                )
                PreviewMessageRow(
                    item = PreviewFixtures.figmaFollowUp,
                    side = Side.Receiver,
                )
            }
        }
    }
}

@Preview(name = "Direct messages - font 1.0x", fontScale = 1f, widthDp = 360)
@Preview(name = "Direct messages - font 1.5x", fontScale = 1.5f, widthDp = 360)
@Preview(name = "Direct messages - font 2.0x", fontScale = 2f, widthDp = 360)
annotation class FontScalePreviews

@FontScalePreviews
@Composable
internal fun ConversationPreview() {
    PreviewConversation()
}

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

package com.tunjid.heron.conversation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.ui.AppBarIconButton
import heron.feature.conversation.generated.resources.Res
import heron.feature.conversation.generated.resources.conversation_accept
import heron.feature.conversation.generated.resources.conversation_leave
import heron.feature.conversation.generated.resources.conversation_mute
import heron.feature.conversation.generated.resources.conversation_options
import heron.feature.conversation.generated.resources.conversation_unmute
import org.jetbrains.compose.resources.stringResource

/**
 * Overflow menu exposing the member-level conversation actions: accepting a
 * conversation request, muting/unmuting, and leaving.
 */
@Composable
internal fun ConversationOverflowMenu(
    modifier: Modifier = Modifier,
    conversation: Conversation?,
    onAccept: () -> Unit,
    onLeave: () -> Unit,
    onToggleMute: (Boolean) -> Unit,
) {
    conversation ?: return
    Box(
        modifier = modifier,
    ) {
        var expanded by remember { mutableStateOf(false) }
        AppBarIconButton(
            icon = Icons.Rounded.MoreVert,
            iconDescription = stringResource(Res.string.conversation_options),
            onClick = { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            if (conversation.status == Conversation.Status.Request) {
                DropdownMenuItem(
                    text = {
                        Text(stringResource(Res.string.conversation_accept))
                    },
                    onClick = {
                        expanded = false
                        onAccept()
                    },
                )
            }
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (conversation.muted) Res.string.conversation_unmute
                            else Res.string.conversation_mute,
                        ),
                    )
                },
                onClick = {
                    expanded = false
                    onToggleMute(!conversation.muted)
                },
            )
            DropdownMenuItem(
                text = {
                    Text(stringResource(Res.string.conversation_leave))
                },
                onClick = {
                    expanded = false
                    onLeave()
                },
            )
        }
    }
}

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

package com.tunjid.heron.timeline.utilities

import androidx.compose.runtime.Composable
import com.tunjid.heron.data.core.models.Message
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.system_message_group_renamed
import heron.ui.timeline.generated.resources.system_message_group_renamed_to
import heron.ui.timeline.generated.resources.system_message_join_link_updated
import heron.ui.timeline.generated.resources.system_message_locked
import heron.ui.timeline.generated.resources.system_message_locked_permanently
import heron.ui.timeline.generated.resources.system_message_member_added
import heron.ui.timeline.generated.resources.system_message_member_joined
import heron.ui.timeline.generated.resources.system_message_member_left
import heron.ui.timeline.generated.resources.system_message_member_removed
import heron.ui.timeline.generated.resources.system_message_unknown_actor
import heron.ui.timeline.generated.resources.system_message_unlocked
import org.jetbrains.compose.resources.stringResource

/**
 * Localized, human readable summary for a group conversation [Message.SystemContent]
 * (member joins/leaves, group renames, lock changes). Shared across the messages inbox
 * and the conversation detail screen. Returns an empty string for events with no display
 * text (e.g. [Message.SystemContent.Unknown]).
 */
@Composable
fun Message.SystemContent.summary(): String {
    val someone = stringResource(Res.string.system_message_unknown_actor)
    return when (this) {
        is Message.SystemContent.MemberAdded -> stringResource(
            Res.string.system_message_member_added,
            addedBy.name(someone),
            member.name(someone),
        )
        is Message.SystemContent.MemberRemoved -> stringResource(
            Res.string.system_message_member_removed,
            removedBy.name(someone),
            member.name(someone),
        )
        is Message.SystemContent.MemberJoined -> stringResource(
            Res.string.system_message_member_joined,
            member.name(someone),
        )
        is Message.SystemContent.MemberLeft -> stringResource(
            Res.string.system_message_member_left,
            member.name(someone),
        )
        is Message.SystemContent.GroupRenamed -> {
            val newName = newName
            if (newName.isNullOrBlank()) stringResource(Res.string.system_message_group_renamed)
            else stringResource(Res.string.system_message_group_renamed_to, newName)
        }
        is Message.SystemContent.Locked -> stringResource(
            Res.string.system_message_locked,
            by.name(someone),
        )
        is Message.SystemContent.Unlocked -> stringResource(
            Res.string.system_message_unlocked,
            by.name(someone),
        )
        is Message.SystemContent.LockedPermanently -> stringResource(
            Res.string.system_message_locked_permanently,
            by.name(someone),
        )
        Message.SystemContent.JoinLinkChanged ->
            stringResource(Res.string.system_message_join_link_updated)
        Message.SystemContent.Unknown -> ""
    }
}

private fun Message.SystemContent.Actor.name(fallback: String): String =
    displayName?.takeIf(String::isNotBlank) ?: handle ?: fallback

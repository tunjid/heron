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

import com.tunjid.heron.data.core.models.Message
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.ui.text.Memo
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.writable_block
import heron.ui.timeline.generated.resources.writable_bookmark
import heron.ui.timeline.generated.resources.writable_bookmark_removal
import heron.ui.timeline.generated.resources.writable_duplicate
import heron.ui.timeline.generated.resources.writable_duplicate_post_interaction
import heron.ui.timeline.generated.resources.writable_failed
import heron.ui.timeline.generated.resources.writable_failed_post_interaction
import heron.ui.timeline.generated.resources.writable_follow
import heron.ui.timeline.generated.resources.writable_like
import heron.ui.timeline.generated.resources.writable_message
import heron.ui.timeline.generated.resources.writable_mute
import heron.ui.timeline.generated.resources.writable_notification_update
import heron.ui.timeline.generated.resources.writable_post
import heron.ui.timeline.generated.resources.writable_profile_update
import heron.ui.timeline.generated.resources.writable_reaction
import heron.ui.timeline.generated.resources.writable_reaction_removal
import heron.ui.timeline.generated.resources.writable_record_deletion
import heron.ui.timeline.generated.resources.writable_repost
import heron.ui.timeline.generated.resources.writable_repost_removal
import heron.ui.timeline.generated.resources.writable_thread_gate_update
import heron.ui.timeline.generated.resources.writable_timeline_update
import heron.ui.timeline.generated.resources.writable_unblock
import heron.ui.timeline.generated.resources.writable_unfollow
import heron.ui.timeline.generated.resources.writable_unlike
import heron.ui.timeline.generated.resources.writable_unmute
import org.jetbrains.compose.resources.StringResource

fun Writable.writeStatusMessage(
    status: WriteQueue.Status,
) = when (status) {
    WriteQueue.Status.Enqueued -> null
    else -> {
        val isDropped = status == WriteQueue.Status.Dropped
        when (this) {
            is Writable.Connection -> Memo.Resource(
                stringResource = genericDroppedOrDuplicateResource(isDropped),
                args = listOf(
                    when (connection) {
                        is Profile.Connection.Follow -> Res.string.writable_follow
                        is Profile.Connection.Unfollow -> Res.string.writable_unfollow
                    },
                ),
            )

            is Writable.Create -> Memo.Resource(
                stringResource = genericDroppedOrDuplicateResource(isDropped),
                args = listOf(Res.string.writable_post),
            )

            is Writable.Interaction -> when {
                isDropped -> Memo.Resource(
                    stringResource = Res.string.writable_failed_post_interaction,
                )
                else -> Memo.Resource(
                    stringResource = Res.string.writable_duplicate_post_interaction,
                    args = listOf(
                        when (interaction) {
                            is Post.Interaction.Create.Bookmark -> Res.string.writable_bookmark
                            is Post.Interaction.Create.Like -> Res.string.writable_like
                            is Post.Interaction.Create.Repost -> Res.string.writable_repost
                            is Post.Interaction.Delete.RemoveBookmark -> Res.string.writable_bookmark_removal
                            is Post.Interaction.Delete.RemoveRepost -> Res.string.writable_repost_removal
                            is Post.Interaction.Delete.Unlike -> Res.string.writable_unlike
                            is Post.Interaction.Upsert.Gate -> Res.string.writable_thread_gate_update
                        },
                    ),
                )
            }

            is Writable.ProfileUpdate -> Memo.Resource(
                stringResource = genericDroppedOrDuplicateResource(isDropped),
                args = listOf(Res.string.writable_profile_update),
            )

            is Writable.Reaction -> Memo.Resource(
                stringResource = genericDroppedOrDuplicateResource(isDropped),
                args = listOf(
                    when (update) {
                        is Message.UpdateReaction.Add -> Res.string.writable_reaction
                        is Message.UpdateReaction.Remove -> Res.string.writable_reaction_removal
                    },
                ),
            )

            is Writable.Restriction -> Memo.Resource(
                stringResource = genericDroppedOrDuplicateResource(isDropped),
                args = listOf(
                    when (restriction) {
                        is Profile.Restriction.Block.Add -> Res.string.writable_block
                        is Profile.Restriction.Block.Remove -> Res.string.writable_unblock
                        is Profile.Restriction.Mute.Add -> Res.string.writable_mute
                        is Profile.Restriction.Mute.Remove -> Res.string.writable_unmute
                    },
                ),
            )

            is Writable.Send -> Memo.Resource(
                stringResource = genericDroppedOrDuplicateResource(isDropped),
                args = listOf(Res.string.writable_message),
            )

            is Writable.TimelineUpdate -> Memo.Resource(
                stringResource = genericDroppedOrDuplicateResource(isDropped),
                args = listOf(Res.string.writable_timeline_update),
            )
            is Writable.NotificationUpdate -> Memo.Resource(
                stringResource = genericDroppedOrDuplicateResource(isDropped),
                args = listOf(Res.string.writable_notification_update),
            )
            is Writable.RecordDeletion -> Memo.Resource(
                stringResource = genericDroppedOrDuplicateResource(isDropped),
                args = listOf(Res.string.writable_record_deletion),
            )
        }
    }
}

private fun genericDroppedOrDuplicateResource(
    isDropped: Boolean,
): StringResource =
    if (isDropped) Res.string.writable_failed
    else Res.string.writable_duplicate

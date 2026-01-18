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
import heron.ui.timeline.generated.resources.writable_post
import heron.ui.timeline.generated.resources.writable_profile_update
import heron.ui.timeline.generated.resources.writable_reaction
import heron.ui.timeline.generated.resources.writable_reaction_removal
import heron.ui.timeline.generated.resources.writable_repost
import heron.ui.timeline.generated.resources.writable_repost_removal
import heron.ui.timeline.generated.resources.writable_thread_gate_update
import heron.ui.timeline.generated.resources.writable_timeline_update
import heron.ui.timeline.generated.resources.writable_unblock
import heron.ui.timeline.generated.resources.writable_unfollow
import heron.ui.timeline.generated.resources.writable_unlike
import heron.ui.timeline.generated.resources.writable_unmute

fun Writable.writeStatusMessage(
    status: WriteQueue.Status,
) = when (this) {
    is Writable.Connection -> when (status) {
        WriteQueue.Status.Dropped -> Memo.Resource(
            stringResource = Res.string.writable_failed,
            args = listOf(
                when (this.connection) {
                    is Profile.Connection.Follow -> Res.string.writable_follow
                    is Profile.Connection.Unfollow -> Res.string.writable_unfollow
                },
            ),
        )
        WriteQueue.Status.Duplicate -> Memo.Resource(
            stringResource = Res.string.writable_duplicate,
            args = listOf(
                when (this.connection) {
                    is Profile.Connection.Follow -> Res.string.writable_follow
                    is Profile.Connection.Unfollow -> Res.string.writable_unfollow
                },
            ),
        )
        WriteQueue.Status.Enqueued -> null
    }
    is Writable.Create -> when (status) {
        WriteQueue.Status.Dropped -> Memo.Resource(
            stringResource = Res.string.writable_failed,
            args = listOf(Res.string.writable_post),
        )
        WriteQueue.Status.Duplicate -> Memo.Resource(
            stringResource = Res.string.writable_duplicate,
            args = listOf(Res.string.writable_post),
        )
        WriteQueue.Status.Enqueued -> null
    }
    is Writable.Interaction -> when (status) {
        WriteQueue.Status.Dropped -> Memo.Resource(
            stringResource = Res.string.writable_failed_post_interaction,
        )
        WriteQueue.Status.Duplicate -> Memo.Resource(
            stringResource = Res.string.writable_duplicate_post_interaction,
            args = listOf(
                when (this.interaction) {
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
        WriteQueue.Status.Enqueued -> null
    }
    is Writable.ProfileUpdate -> when (status) {
        WriteQueue.Status.Dropped -> Memo.Resource(
            stringResource = Res.string.writable_failed,
            args = listOf(Res.string.writable_profile_update),
        )
        WriteQueue.Status.Duplicate -> Memo.Resource(
            stringResource = Res.string.writable_duplicate,
            args = listOf(Res.string.writable_profile_update),
        )
        WriteQueue.Status.Enqueued -> null
    }
    is Writable.Reaction -> when (status) {
        WriteQueue.Status.Dropped -> Memo.Resource(
            stringResource = Res.string.writable_failed,
            args = listOf(
                when (this.update) {
                    is Message.UpdateReaction.Add -> Res.string.writable_reaction
                    is Message.UpdateReaction.Remove -> Res.string.writable_reaction_removal
                },
            ),
        )
        WriteQueue.Status.Duplicate -> Memo.Resource(
            stringResource = Res.string.writable_duplicate,
            args = listOf(
                when (this.update) {
                    is Message.UpdateReaction.Add -> Res.string.writable_reaction
                    is Message.UpdateReaction.Remove -> Res.string.writable_reaction_removal
                },
            ),
        )
        WriteQueue.Status.Enqueued -> null
    }
    is Writable.Restriction -> when (status) {
        WriteQueue.Status.Dropped -> Memo.Resource(
            stringResource = Res.string.writable_failed,
            args = listOf(
                when (this.restriction) {
                    is Profile.Restriction.Block.Add -> Res.string.writable_block
                    is Profile.Restriction.Block.Remove -> Res.string.writable_unblock
                    is Profile.Restriction.Mute.Add -> Res.string.writable_mute
                    is Profile.Restriction.Mute.Remove -> Res.string.writable_unmute
                },
            ),
        )
        WriteQueue.Status.Duplicate -> Memo.Resource(
            stringResource = Res.string.writable_duplicate,
            args = listOf(
                when (this.restriction) {
                    is Profile.Restriction.Block.Add -> Res.string.writable_block
                    is Profile.Restriction.Block.Remove -> Res.string.writable_unblock
                    is Profile.Restriction.Mute.Add -> Res.string.writable_mute
                    is Profile.Restriction.Mute.Remove -> Res.string.writable_unmute
                },
            ),
        )
        WriteQueue.Status.Enqueued -> null
    }
    is Writable.Send -> when (status) {
        WriteQueue.Status.Dropped -> Memo.Resource(
            stringResource = Res.string.writable_failed,
            args = listOf(Res.string.writable_message),
        )
        WriteQueue.Status.Duplicate -> Memo.Resource(
            stringResource = Res.string.writable_duplicate,
            args = listOf(Res.string.writable_message),
        )
        WriteQueue.Status.Enqueued -> null
    }
    is Writable.TimelineUpdate -> when (status) {
        WriteQueue.Status.Dropped -> Memo.Resource(
            stringResource = Res.string.writable_failed,
            args = listOf(Res.string.writable_timeline_update),
        )
        WriteQueue.Status.Duplicate -> Memo.Resource(
            stringResource = Res.string.writable_duplicate,
            args = listOf(Res.string.writable_timeline_update),
        )
        WriteQueue.Status.Enqueued -> null
    }
}

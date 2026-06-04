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

package com.tunjid.heron.tasks.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkRemove
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.HeartBroken
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.MoodBad
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import com.tunjid.heron.data.core.models.Message
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.utilities.File
import com.tunjid.heron.data.utilities.writequeue.FailedWrite
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.ui.text.Memo
import heron.feature.tasks.generated.resources.Res
import heron.feature.tasks.generated.resources.adding_to_list
import heron.feature.tasks.generated.resources.blocking_profile
import heron.feature.tasks.generated.resources.bookmarking_post
import heron.feature.tasks.generated.resources.creating_post
import heron.feature.tasks.generated.resources.deleting_record
import heron.feature.tasks.generated.resources.following_profile
import heron.feature.tasks.generated.resources.liking_post
import heron.feature.tasks.generated.resources.muting_profile
import heron.feature.tasks.generated.resources.post_photo
import heron.feature.tasks.generated.resources.post_video
import heron.feature.tasks.generated.resources.reacting_with
import heron.feature.tasks.generated.resources.removing_bookmark
import heron.feature.tasks.generated.resources.removing_like
import heron.feature.tasks.generated.resources.removing_reaction
import heron.feature.tasks.generated.resources.removing_repost
import heron.feature.tasks.generated.resources.reposting
import heron.feature.tasks.generated.resources.sending_message
import heron.feature.tasks.generated.resources.subscribing_publication
import heron.feature.tasks.generated.resources.unblocking_profile
import heron.feature.tasks.generated.resources.unfollowing_profile
import heron.feature.tasks.generated.resources.unmuting_profile
import heron.feature.tasks.generated.resources.updating_feed_settings
import heron.feature.tasks.generated.resources.updating_notification_preferences
import heron.feature.tasks.generated.resources.updating_profile
import heron.feature.tasks.generated.resources.updating_reply_settings
import heron.feature.tasks.generated.resources.updating_status

sealed class TaskItem {
    abstract val writable: Writable
    abstract val description: Description
    abstract val associatedRecord: Record.Embeddable?

    data class InFlight(
        override val writable: Writable,
        override val description: Description,
        override val associatedRecord: Record.Embeddable?,
    ) : TaskItem()

    data class Failed(
        val failedWrite: FailedWrite,
        override val description: Description,
        override val associatedRecord: Record.Embeddable?,
    ) : TaskItem() {
        override val writable: Writable get() = failedWrite.writable
    }

    data class Description(
        val icon: ImageVector,
        val title: Memo,
        val summary: Memo?,
        val photoCount: Int = 0,
        val videoCount: Int = 0,
    )
}

internal fun Writable.inFlightTaskItem(
    associatedRecord: Record.Embeddable?,
): TaskItem.InFlight = TaskItem.InFlight(
    writable = this,
    description = describe(),
    associatedRecord = associatedRecord,
)

internal fun FailedWrite.failedTaskItem(
    associatedRecord: Record.Embeddable?,
): TaskItem.Failed = TaskItem.Failed(
    failedWrite = this,
    description = writable.describe(),
    associatedRecord = associatedRecord,
)

private fun Writable.describe(): TaskItem.Description =
    when (this) {
        is Writable.Create -> request.describe()
        is Writable.Interaction -> interaction.describe()
        is Writable.Send -> TaskItem.Description(
            icon = Icons.AutoMirrored.Rounded.Send,
            title = Memo.Resource(Res.string.sending_message),
            summary = request.text.snippet()?.let(Memo::Text),
        )
        is Writable.Reaction -> when (update) {
            is Message.UpdateReaction.Add -> TaskItem.Description(
                icon = Icons.Rounded.Mood,
                title = Memo.Resource(Res.string.reacting_with, listOf(update.value)),
                summary = null,
            )
            is Message.UpdateReaction.Remove -> TaskItem.Description(
                icon = Icons.Rounded.MoodBad,
                title = Memo.Resource(Res.string.removing_reaction, listOf(update.value)),
                summary = null,
            )
        }
        is Writable.Connection -> when (connection) {
            is Profile.Connection.Follow -> TaskItem.Description(
                icon = Icons.Rounded.PersonAdd,
                title = Memo.Resource(Res.string.following_profile),
                summary = null,
            )
            is Profile.Connection.Unfollow -> TaskItem.Description(
                icon = Icons.Rounded.PersonRemove,
                title = Memo.Resource(Res.string.unfollowing_profile),
                summary = null,
            )
        }
        is Writable.FeedList.AddMember -> TaskItem.Description(
            icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
            title = Memo.Resource(Res.string.adding_to_list),
            summary = null,
        )
        is Writable.StandardSite.Subscribe -> TaskItem.Description(
            icon = Icons.Rounded.Subscriptions,
            title = Memo.Resource(Res.string.subscribing_publication),
            summary = null,
        )
        is Writable.Restriction -> when (restriction) {
            is Profile.Restriction.Block.Add -> TaskItem.Description(
                icon = Icons.Rounded.Block,
                title = Memo.Resource(Res.string.blocking_profile),
                summary = null,
            )
            is Profile.Restriction.Block.Remove -> TaskItem.Description(
                icon = Icons.Rounded.Block,
                title = Memo.Resource(Res.string.unblocking_profile),
                summary = null,
            )
            is Profile.Restriction.Mute.Add -> TaskItem.Description(
                icon = Icons.AutoMirrored.Rounded.VolumeOff,
                title = Memo.Resource(Res.string.muting_profile),
                summary = null,
            )
            is Profile.Restriction.Mute.Remove -> TaskItem.Description(
                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                title = Memo.Resource(Res.string.unmuting_profile),
                summary = null,
            )
        }
        is Writable.StatusUpdate -> TaskItem.Description(
            icon = Icons.Rounded.Sensors,
            title = Memo.Resource(Res.string.updating_status),
            summary = null,
        )
        is Writable.TimelineUpdate -> TaskItem.Description(
            icon = Icons.Rounded.Tune,
            title = Memo.Resource(Res.string.updating_feed_settings),
            summary = null,
        )
        is Writable.ProfileUpdate -> TaskItem.Description(
            icon = Icons.Rounded.ManageAccounts,
            title = Memo.Resource(Res.string.updating_profile),
            summary = null,
        )
        is Writable.NotificationUpdate -> TaskItem.Description(
            icon = Icons.Rounded.Notifications,
            title = Memo.Resource(Res.string.updating_notification_preferences),
            summary = null,
        )
        is Writable.RecordDeletion -> TaskItem.Description(
            icon = Icons.Rounded.Delete,
            title = Memo.Resource(Res.string.deleting_record),
            summary = null,
        )
    }

private fun Post.Create.Request.describe(): TaskItem.Description {
    val photoCount = metadata.embeddedMedia.count { it is File.Media.Photo }
    val videoCount = metadata.embeddedMedia.count { it is File.Media.Video }
    val text = text.snippet()
    return TaskItem.Description(
        icon = Icons.Rounded.Edit,
        title = Memo.Resource(Res.string.creating_post),
        summary = when {
            text != null -> Memo.Text(text)
            videoCount > 0 -> Memo.Resource(Res.string.post_video)
            photoCount > 0 -> Memo.Resource(Res.string.post_photo)
            else -> null
        },
        photoCount = photoCount,
        videoCount = videoCount,
    )
}

private fun Post.Interaction.describe(): TaskItem.Description =
    when (this) {
        is Post.Interaction.Create.Like -> TaskItem.Description(
            icon = Icons.Rounded.Favorite,
            title = Memo.Resource(Res.string.liking_post),
            summary = null,
        )
        is Post.Interaction.Create.Repost -> TaskItem.Description(
            icon = Icons.Rounded.Repeat,
            title = Memo.Resource(Res.string.reposting),
            summary = null,
        )
        is Post.Interaction.Create.Bookmark -> TaskItem.Description(
            icon = Icons.Rounded.Bookmark,
            title = Memo.Resource(Res.string.bookmarking_post),
            summary = null,
        )
        is Post.Interaction.Delete.Unlike -> TaskItem.Description(
            icon = Icons.Rounded.HeartBroken,
            title = Memo.Resource(Res.string.removing_like),
            summary = null,
        )
        is Post.Interaction.Delete.RemoveRepost -> TaskItem.Description(
            icon = Icons.Rounded.Repeat,
            title = Memo.Resource(Res.string.removing_repost),
            summary = null,
        )
        is Post.Interaction.Delete.RemoveBookmark -> TaskItem.Description(
            icon = Icons.Rounded.BookmarkRemove,
            title = Memo.Resource(Res.string.removing_bookmark),
            summary = null,
        )
        is Post.Interaction.Upsert.Gate -> TaskItem.Description(
            icon = Icons.Rounded.Shield,
            title = Memo.Resource(Res.string.updating_reply_settings),
            summary = null,
        )
    }

private fun String.snippet(): String? =
    trim().takeIf(String::isNotEmpty)?.let {
        if (it.length > 140) it.take(140).trimEnd() + "…" else it
    }

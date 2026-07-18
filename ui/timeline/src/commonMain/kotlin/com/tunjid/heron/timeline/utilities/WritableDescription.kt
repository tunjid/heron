package com.tunjid.heron.timeline.utilities

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkRemove
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.HeartBroken
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
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
import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.models.Message
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.StandardDocument
import com.tunjid.heron.data.core.utilities.File
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.ui.text.Memo
import com.tunjid.heron.ui.text.Memo.Resource
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.writable_description_accepting_conversation
import heron.ui.timeline.generated.resources.writable_description_adding_to_list
import heron.ui.timeline.generated.resources.writable_description_blocking_profile
import heron.ui.timeline.generated.resources.writable_description_bookmarking_post
import heron.ui.timeline.generated.resources.writable_description_creating_post
import heron.ui.timeline.generated.resources.writable_description_deleting_record
import heron.ui.timeline.generated.resources.writable_description_following_profile
import heron.ui.timeline.generated.resources.writable_description_leaving_conversation
import heron.ui.timeline.generated.resources.writable_description_liking_post
import heron.ui.timeline.generated.resources.writable_description_linking_document
import heron.ui.timeline.generated.resources.writable_description_muting_conversation
import heron.ui.timeline.generated.resources.writable_description_muting_profile
import heron.ui.timeline.generated.resources.writable_description_post_photo
import heron.ui.timeline.generated.resources.writable_description_post_video
import heron.ui.timeline.generated.resources.writable_description_reacting_with
import heron.ui.timeline.generated.resources.writable_description_removing_bookmark
import heron.ui.timeline.generated.resources.writable_description_removing_like
import heron.ui.timeline.generated.resources.writable_description_removing_reaction
import heron.ui.timeline.generated.resources.writable_description_removing_repost
import heron.ui.timeline.generated.resources.writable_description_reposting
import heron.ui.timeline.generated.resources.writable_description_sending_feed_feedback
import heron.ui.timeline.generated.resources.writable_description_sending_message
import heron.ui.timeline.generated.resources.writable_description_subscribing_publication
import heron.ui.timeline.generated.resources.writable_description_unblocking_profile
import heron.ui.timeline.generated.resources.writable_description_unfollowing_profile
import heron.ui.timeline.generated.resources.writable_description_unlinking_document
import heron.ui.timeline.generated.resources.writable_description_unmuting_conversation
import heron.ui.timeline.generated.resources.writable_description_unmuting_profile
import heron.ui.timeline.generated.resources.writable_description_updating_feed_settings
import heron.ui.timeline.generated.resources.writable_description_updating_notification_preferences
import heron.ui.timeline.generated.resources.writable_description_updating_profile
import heron.ui.timeline.generated.resources.writable_description_updating_reply_settings
import heron.ui.timeline.generated.resources.writable_description_updating_status

data class WritableDescription(
    val icon: ImageVector,
    val title: Memo,
    val summary: Memo?,
    val photoCount: Int = 0,
    val videoCount: Int = 0,
)

fun Writable.describe(): WritableDescription =
    when (this) {
        is Writable.Create -> request.describe()
        is Writable.Interaction -> interaction.describe()
        is Writable.FeedInteraction -> WritableDescription(
            icon = Icons.Rounded.Tune,
            title = Resource(Res.string.writable_description_sending_feed_feedback),
            summary = null,
        )
        is Writable.Send -> WritableDescription(
            icon = Icons.AutoMirrored.Rounded.Send,
            title = Resource(Res.string.writable_description_sending_message),
            summary = request.text.snippet()?.let(Memo::Text),
        )
        is Writable.Reaction -> when (update) {
            is Message.UpdateReaction.Add -> WritableDescription(
                icon = Icons.Rounded.Mood,
                title = Resource(Res.string.writable_description_reacting_with, listOf(update.value)),
                summary = null,
            )
            is Message.UpdateReaction.Remove -> WritableDescription(
                icon = Icons.Rounded.MoodBad,
                title = Resource(Res.string.writable_description_removing_reaction, listOf(update.value)),
                summary = null,
            )
        }
        is Writable.Connection -> when (connection) {
            is Profile.Connection.Follow -> WritableDescription(
                icon = Icons.Rounded.PersonAdd,
                title = Resource(Res.string.writable_description_following_profile),
                summary = null,
            )
            is Profile.Connection.Unfollow -> WritableDescription(
                icon = Icons.Rounded.PersonRemove,
                title = Resource(Res.string.writable_description_unfollowing_profile),
                summary = null,
            )
        }
        is Writable.FeedList.AddMember -> WritableDescription(
            icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
            title = Resource(Res.string.writable_description_adding_to_list),
            summary = null,
        )
        is Writable.StandardSite.Subscribe -> WritableDescription(
            icon = Icons.Rounded.Subscriptions,
            title = Resource(Res.string.writable_description_subscribing_publication),
            summary = null,
        )
        is Writable.StandardSite.UpdatePostReference -> when (reference) {
            is StandardDocument.PostReference.Link -> WritableDescription(
                icon = Icons.Rounded.Link,
                title = Resource(Res.string.writable_description_linking_document),
                summary = null,
            )
            is StandardDocument.PostReference.Unlink -> WritableDescription(
                icon = Icons.Rounded.LinkOff,
                title = Resource(Res.string.writable_description_unlinking_document),
                summary = null,
            )
        }
        is Writable.Restriction -> when (restriction) {
            is Profile.Restriction.Block.Add -> WritableDescription(
                icon = Icons.Rounded.Block,
                title = Resource(Res.string.writable_description_blocking_profile),
                summary = null,
            )
            is Profile.Restriction.Block.Remove -> WritableDescription(
                icon = Icons.Rounded.Block,
                title = Resource(Res.string.writable_description_unblocking_profile),
                summary = null,
            )
            is Profile.Restriction.Mute.Add -> WritableDescription(
                icon = Icons.AutoMirrored.Rounded.VolumeOff,
                title = Resource(Res.string.writable_description_muting_profile),
                summary = null,
            )
            is Profile.Restriction.Mute.Remove -> WritableDescription(
                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                title = Resource(Res.string.writable_description_unmuting_profile),
                summary = null,
            )
        }
        is Writable.StatusUpdate -> WritableDescription(
            icon = Icons.Rounded.Sensors,
            title = Resource(Res.string.writable_description_updating_status),
            summary = null,
        )
        is Writable.TimelineUpdate -> WritableDescription(
            icon = Icons.Rounded.Tune,
            title = Resource(Res.string.writable_description_updating_feed_settings),
            summary = null,
        )
        is Writable.ProfileUpdate -> WritableDescription(
            icon = Icons.Rounded.ManageAccounts,
            title = Resource(Res.string.writable_description_updating_profile),
            summary = null,
        )
        is Writable.NotificationUpdate -> WritableDescription(
            icon = Icons.Rounded.Notifications,
            title = Resource(Res.string.writable_description_updating_notification_preferences),
            summary = null,
        )
        is Writable.RecordDeletion -> WritableDescription(
            icon = Icons.Rounded.Delete,
            title = Resource(Res.string.writable_description_deleting_record),
            summary = null,
        )
        is Writable.ConversationUpdate -> when (val update = update) {
            is Conversation.Update.Accept -> WritableDescription(
                icon = Icons.Rounded.Check,
                title = Resource(Res.string.writable_description_accepting_conversation),
                summary = null,
            )
            is Conversation.Update.Leave -> WritableDescription(
                icon = Icons.AutoMirrored.Rounded.Logout,
                title = Resource(Res.string.writable_description_leaving_conversation),
                summary = null,
            )
            is Conversation.Update.Mute -> WritableDescription(
                icon = if (update.muted) Icons.AutoMirrored.Rounded.VolumeOff
                else Icons.AutoMirrored.Rounded.VolumeUp,
                title = Resource(
                    if (update.muted) Res.string.writable_description_muting_conversation
                    else Res.string.writable_description_unmuting_conversation,
                ),
                summary = null,
            )
        }
    }

private fun Post.Create.Request.describe(): WritableDescription {
    val photoCount = metadata.embeddedMedia.count { it is File.Media.Photo }
    val videoCount = metadata.embeddedMedia.count { it is File.Media.Video }
    val text = text.snippet()
    return WritableDescription(
        icon = Icons.Rounded.Edit,
        title = Resource(Res.string.writable_description_creating_post),
        summary = when {
            text != null -> Memo.Text(text)
            videoCount > 0 -> Resource(Res.string.writable_description_post_video)
            photoCount > 0 -> Resource(Res.string.writable_description_post_photo)
            else -> null
        },
        photoCount = photoCount,
        videoCount = videoCount,
    )
}

private fun Post.Interaction.describe(): WritableDescription =
    when (this) {
        is Post.Interaction.Create.Like -> WritableDescription(
            icon = Icons.Rounded.Favorite,
            title = Resource(Res.string.writable_description_liking_post),
            summary = null,
        )
        is Post.Interaction.Create.Repost -> WritableDescription(
            icon = Icons.Rounded.Repeat,
            title = Resource(Res.string.writable_description_reposting),
            summary = null,
        )
        is Post.Interaction.Create.Bookmark -> WritableDescription(
            icon = Icons.Rounded.Bookmark,
            title = Resource(Res.string.writable_description_bookmarking_post),
            summary = null,
        )
        is Post.Interaction.Delete.Unlike -> WritableDescription(
            icon = Icons.Rounded.HeartBroken,
            title = Resource(Res.string.writable_description_removing_like),
            summary = null,
        )
        is Post.Interaction.Delete.RemoveRepost -> WritableDescription(
            icon = Icons.Rounded.Repeat,
            title = Resource(Res.string.writable_description_removing_repost),
            summary = null,
        )
        is Post.Interaction.Delete.RemoveBookmark -> WritableDescription(
            icon = Icons.Rounded.BookmarkRemove,
            title = Resource(Res.string.writable_description_removing_bookmark),
            summary = null,
        )
        is Post.Interaction.Upsert.Gate -> WritableDescription(
            icon = Icons.Rounded.Shield,
            title = Resource(Res.string.writable_description_updating_reply_settings),
            summary = null,
        )
    }

private fun String.snippet(): String? =
    trim().takeIf(String::isNotEmpty)?.let {
        if (it.length > 140) it.take(140).trimEnd() + "…" else it
    }

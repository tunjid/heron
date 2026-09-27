package com.tunjid.heron.timeline.utilities

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.models.Message
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.StandardDocument
import com.tunjid.heron.data.core.utilities.File
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.ui.icons.HeronIcons
import com.tunjid.heron.ui.icons.automirrored.Logout
import com.tunjid.heron.ui.icons.automirrored.PlaylistAdd
import com.tunjid.heron.ui.icons.automirrored.Send
import com.tunjid.heron.ui.icons.automirrored.VolumeOff
import com.tunjid.heron.ui.icons.automirrored.VolumeUp
import com.tunjid.heron.ui.icons.regular.Block
import com.tunjid.heron.ui.icons.regular.Bookmark
import com.tunjid.heron.ui.icons.regular.BookmarkRemove
import com.tunjid.heron.ui.icons.regular.Check
import com.tunjid.heron.ui.icons.regular.Delete
import com.tunjid.heron.ui.icons.regular.Drafts
import com.tunjid.heron.ui.icons.regular.Edit
import com.tunjid.heron.ui.icons.regular.Favorite
import com.tunjid.heron.ui.icons.regular.HeartBroken
import com.tunjid.heron.ui.icons.regular.Link
import com.tunjid.heron.ui.icons.regular.LinkOff
import com.tunjid.heron.ui.icons.regular.ManageAccounts
import com.tunjid.heron.ui.icons.regular.Mood
import com.tunjid.heron.ui.icons.regular.MoodBad
import com.tunjid.heron.ui.icons.regular.Notifications
import com.tunjid.heron.ui.icons.regular.PersonAdd
import com.tunjid.heron.ui.icons.regular.PersonRemove
import com.tunjid.heron.ui.icons.regular.Repeat
import com.tunjid.heron.ui.icons.regular.Sensors
import com.tunjid.heron.ui.icons.regular.Shield
import com.tunjid.heron.ui.icons.regular.Subscriptions
import com.tunjid.heron.ui.icons.regular.Tune
import com.tunjid.heron.ui.text.Memo
import com.tunjid.heron.ui.text.Memo.Resource
import com.tunjid.heron.ui.text.message
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.writable_description_accepting_conversation
import heron.ui.timeline.generated.resources.writable_description_adding_to_list
import heron.ui.timeline.generated.resources.writable_description_blocking_profile
import heron.ui.timeline.generated.resources.writable_description_bookmarking_post
import heron.ui.timeline.generated.resources.writable_description_creating_post
import heron.ui.timeline.generated.resources.writable_description_deleting_draft
import heron.ui.timeline.generated.resources.writable_description_deleting_record
import heron.ui.timeline.generated.resources.writable_description_following_profile
import heron.ui.timeline.generated.resources.writable_description_following_starter_pack
import heron.ui.timeline.generated.resources.writable_description_leaving_conversation
import heron.ui.timeline.generated.resources.writable_description_liking_post
import heron.ui.timeline.generated.resources.writable_description_linking_document
import heron.ui.timeline.generated.resources.writable_description_media_photos_multiple
import heron.ui.timeline.generated.resources.writable_description_media_photos_single
import heron.ui.timeline.generated.resources.writable_description_media_videos_multiple
import heron.ui.timeline.generated.resources.writable_description_media_videos_single
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
import heron.ui.timeline.generated.resources.writable_description_saving_draft
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

val WritableDescription.mediaSummary: List<Memo>
    get() = buildList {
        if (photoCount > 0) add(
            Resource(
                stringResource = if (photoCount == 1) Res.string.writable_description_media_photos_single
                else Res.string.writable_description_media_photos_multiple,
                args = listOf(photoCount),
            ),
        )
        if (videoCount > 0) add(
            Resource(
                stringResource = if (videoCount == 1) Res.string.writable_description_media_videos_single
                else Res.string.writable_description_media_videos_multiple,
                args = listOf(videoCount),
            ),
        )
    }

val WritableDescription.mediaSummaryMessage: String?
    @Composable get() = mediaSummary
        .map { it.message }
        .joinMediaSummary()

suspend fun WritableDescription.mediaSummaryMessage(): String? =
    mediaSummary
        .map { it.message() }
        .joinMediaSummary()

private fun List<String>.joinMediaSummary(): String? =
    takeIf(List<String>::isNotEmpty)?.joinToString(separator = " · ")

fun Writable.describe(): WritableDescription =
    when (this) {
        is Writable.Create -> request.describe()
        is Writable.PostDraft.Save -> WritableDescription(
            icon = HeronIcons.Regular.Drafts,
            title = Resource(Res.string.writable_description_saving_draft),
            summary = null,
        )
        is Writable.PostDraft.Delete -> WritableDescription(
            icon = HeronIcons.Regular.Delete,
            title = Resource(Res.string.writable_description_deleting_draft),
            summary = null,
        )
        is Writable.Interaction -> interaction.describe()
        is Writable.FeedInteraction -> WritableDescription(
            icon = HeronIcons.Regular.Tune,
            title = Resource(Res.string.writable_description_sending_feed_feedback),
            summary = null,
        )
        is Writable.Send -> WritableDescription(
            icon = HeronIcons.AutoMirrored.Send,
            title = Resource(Res.string.writable_description_sending_message),
            summary = request.text.snippet()?.let(Memo::Text),
        )
        is Writable.Reaction -> when (update) {
            is Message.UpdateReaction.Add -> WritableDescription(
                icon = HeronIcons.Regular.Mood,
                title = Resource(Res.string.writable_description_reacting_with, listOf(update.value)),
                summary = null,
            )
            is Message.UpdateReaction.Remove -> WritableDescription(
                icon = HeronIcons.Regular.MoodBad,
                title = Resource(Res.string.writable_description_removing_reaction, listOf(update.value)),
                summary = null,
            )
        }
        is Writable.Connection -> when (connection) {
            is Profile.Connection.Follow -> WritableDescription(
                icon = HeronIcons.Regular.PersonAdd,
                title = Resource(Res.string.writable_description_following_profile),
                summary = null,
            )
            is Profile.Connection.Unfollow -> WritableDescription(
                icon = HeronIcons.Regular.PersonRemove,
                title = Resource(Res.string.writable_description_unfollowing_profile),
                summary = null,
            )
            is Profile.Connection.FollowStarterPack -> WritableDescription(
                icon = HeronIcons.Regular.PersonAdd,
                title = Resource(Res.string.writable_description_following_starter_pack),
                summary = null,
            )
        }
        is Writable.FeedList.AddMember -> WritableDescription(
            icon = HeronIcons.AutoMirrored.PlaylistAdd,
            title = Resource(Res.string.writable_description_adding_to_list),
            summary = null,
        )
        is Writable.StandardSite.Subscribe -> WritableDescription(
            icon = HeronIcons.Regular.Subscriptions,
            title = Resource(Res.string.writable_description_subscribing_publication),
            summary = null,
        )
        is Writable.StandardSite.UpdatePostReference -> when (reference) {
            is StandardDocument.PostReference.Link -> WritableDescription(
                icon = HeronIcons.Regular.Link,
                title = Resource(Res.string.writable_description_linking_document),
                summary = null,
            )
            is StandardDocument.PostReference.Unlink -> WritableDescription(
                icon = HeronIcons.Regular.LinkOff,
                title = Resource(Res.string.writable_description_unlinking_document),
                summary = null,
            )
        }
        is Writable.Restriction -> when (restriction) {
            is Profile.Restriction.Block.Add -> WritableDescription(
                icon = HeronIcons.Regular.Block,
                title = Resource(Res.string.writable_description_blocking_profile),
                summary = null,
            )
            is Profile.Restriction.Block.Remove -> WritableDescription(
                icon = HeronIcons.Regular.Block,
                title = Resource(Res.string.writable_description_unblocking_profile),
                summary = null,
            )
            is Profile.Restriction.Mute.Add -> WritableDescription(
                icon = HeronIcons.AutoMirrored.VolumeOff,
                title = Resource(Res.string.writable_description_muting_profile),
                summary = null,
            )
            is Profile.Restriction.Mute.Remove -> WritableDescription(
                icon = HeronIcons.AutoMirrored.VolumeUp,
                title = Resource(Res.string.writable_description_unmuting_profile),
                summary = null,
            )
        }
        is Writable.StatusUpdate -> WritableDescription(
            icon = HeronIcons.Regular.Sensors,
            title = Resource(Res.string.writable_description_updating_status),
            summary = null,
        )
        is Writable.TimelineUpdate -> WritableDescription(
            icon = HeronIcons.Regular.Tune,
            title = Resource(Res.string.writable_description_updating_feed_settings),
            summary = null,
        )
        is Writable.ProfileUpdate -> WritableDescription(
            icon = HeronIcons.Regular.ManageAccounts,
            title = Resource(Res.string.writable_description_updating_profile),
            summary = null,
        )
        is Writable.NotificationUpdate -> WritableDescription(
            icon = HeronIcons.Regular.Notifications,
            title = Resource(Res.string.writable_description_updating_notification_preferences),
            summary = null,
        )
        is Writable.RecordDeletion -> WritableDescription(
            icon = HeronIcons.Regular.Delete,
            title = Resource(Res.string.writable_description_deleting_record),
            summary = null,
        )
        is Writable.ConversationUpdate -> when (val update = update) {
            is Conversation.Update.Accept -> WritableDescription(
                icon = HeronIcons.Regular.Check,
                title = Resource(Res.string.writable_description_accepting_conversation),
                summary = null,
            )
            is Conversation.Update.Leave -> WritableDescription(
                icon = HeronIcons.AutoMirrored.Logout,
                title = Resource(Res.string.writable_description_leaving_conversation),
                summary = null,
            )
            is Conversation.Update.Mute -> WritableDescription(
                icon = if (update.muted) HeronIcons.AutoMirrored.VolumeOff
                else HeronIcons.AutoMirrored.VolumeUp,
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
        icon = HeronIcons.Regular.Edit,
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
            icon = HeronIcons.Regular.Favorite,
            title = Resource(Res.string.writable_description_liking_post),
            summary = null,
        )
        is Post.Interaction.Create.Repost -> WritableDescription(
            icon = HeronIcons.Regular.Repeat,
            title = Resource(Res.string.writable_description_reposting),
            summary = null,
        )
        is Post.Interaction.Create.Bookmark -> WritableDescription(
            icon = HeronIcons.Regular.Bookmark,
            title = Resource(Res.string.writable_description_bookmarking_post),
            summary = null,
        )
        is Post.Interaction.Delete.Unlike -> WritableDescription(
            icon = HeronIcons.Regular.HeartBroken,
            title = Resource(Res.string.writable_description_removing_like),
            summary = null,
        )
        is Post.Interaction.Delete.RemoveRepost -> WritableDescription(
            icon = HeronIcons.Regular.Repeat,
            title = Resource(Res.string.writable_description_removing_repost),
            summary = null,
        )
        is Post.Interaction.Delete.RemoveBookmark -> WritableDescription(
            icon = HeronIcons.Regular.BookmarkRemove,
            title = Resource(Res.string.writable_description_removing_bookmark),
            summary = null,
        )
        is Post.Interaction.Upsert.Gate -> WritableDescription(
            icon = HeronIcons.Regular.Shield,
            title = Resource(Res.string.writable_description_updating_reply_settings),
            summary = null,
        )
    }

private fun String.snippet(): String? =
    trim().takeIf(String::isNotEmpty)?.let {
        if (it.length > 140) it.take(140).trimEnd() + "…" else it
    }

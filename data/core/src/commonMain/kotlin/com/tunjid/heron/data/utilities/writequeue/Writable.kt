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

package com.tunjid.heron.data.utilities.writequeue

import com.tunjid.heron.data.core.models.Message
import com.tunjid.heron.data.core.models.NotificationPreferences
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.utilities.Outcome
import kotlin.time.Instant
import kotlinx.serialization.Serializable

/**
 * Interface definition for data that is written to disk or over the wire.
 * If written to disk, it is using protobufs and all the rules about constructor argument
 * order apply.
 */
@Serializable
sealed interface Writable {
    /**
     * An id to identify if a [Writable] is in the [WriteQueue]
     */
    val queueId: String

    suspend fun WriteQueue.write(): Outcome

    @Serializable
    data class Interaction(
        val interaction: Post.Interaction,
    ) : Writable {

        override val queueId: String
            get() = when (interaction) {
                is Post.Interaction.Create.Like -> "like-${interaction.postUri}"
                is Post.Interaction.Create.Repost -> "repost-${interaction.postUri}"
                is Post.Interaction.Delete.RemoveRepost -> "remove-repost-${interaction.postUri}"
                is Post.Interaction.Delete.Unlike -> "remove-like-${interaction.postUri}"
                is Post.Interaction.Create.Bookmark -> "bookmark-${interaction.postUri}"
                is Post.Interaction.Delete.RemoveBookmark -> "remove-bookmark-${interaction.postUri}"
                is Post.Interaction.Upsert.Gate -> "update-thread-gate-$interaction"
            }

        override suspend fun WriteQueue.write(): Outcome =
            postRepository.sendInteraction(interaction)
    }

    @Serializable
    data class Create(
        val request: Post.Create.Request,
    ) : Writable {

        override val queueId: String
            get() = "create-post-$request"

        override suspend fun WriteQueue.write(): Outcome =
            postRepository.createPost(request)
    }

    @Serializable
    data class Send(
        val request: Message.Create,
    ) : Writable {

        override val queueId: String
            get() = "send-message-$request"

        override suspend fun WriteQueue.write(): Outcome =
            messageRepository.sendMessage(request)
    }

    @Serializable
    data class Reaction(
        val update: Message.UpdateReaction,
    ) : Writable {

        override val queueId: String
            get() = "update-reaction-$update"

        override suspend fun WriteQueue.write(): Outcome =
            messageRepository.updateReaction(update)
    }

    @Serializable
    data class Connection(
        val connection: Profile.Connection,
    ) : Writable {

        override val queueId: String
            get() = when (connection) {
                is Profile.Connection.Follow -> "follow-$connection"
                is Profile.Connection.Unfollow -> "unfollow-$connection"
            }

        override suspend fun WriteQueue.write(): Outcome =
            profileRepository.sendConnection(connection)
    }

    @Serializable
    data class Restriction(
        val restriction: Profile.Restriction,
    ) : Writable {

        override val queueId: String
            get() = when (restriction) {
                is Profile.Restriction.Block.Add -> "block-$restriction"
                is Profile.Restriction.Block.Remove -> "unblock-$restriction"
                is Profile.Restriction.Mute.Add -> "mute-$restriction"
                is Profile.Restriction.Mute.Remove -> "unmute-$restriction"
            }

        override suspend fun WriteQueue.write(): Outcome =
            profileRepository.updateRestriction(restriction)
    }

    @Serializable
    data class TimelineUpdate(
        val update: Timeline.Update,
    ) : Writable {
        override val queueId: String
            get() = when (update) {
                is Timeline.Update.Bulk -> update.timelines.joinToString(
                    separator = "-",
                    transform = Timeline.Home::sourceId,
                )
                is Timeline.Update.HomeFeed.Pin -> "pin-${update.uri}"
                is Timeline.Update.HomeFeed.Remove -> "remove-${update.uri}"
                is Timeline.Update.HomeFeed.Save -> "save-${update.uri}"
                is Timeline.Update.OfContentLabel -> "visibility-change-$update"
                is Timeline.Update.OfLabeler.Subscription -> "labeler-subscription-$update"
                is Timeline.Update.OfAdultContent -> "adult-content-change-$update"
                is Timeline.Update.OfMutedWord -> "muted-words-change-$update"
                is Timeline.Update.OfInteractionSettings -> "interaction-settings-$update"
            }

        override suspend fun WriteQueue.write(): Outcome =
            timelineRepository.updateHomeTimelines(update)
    }

    @Serializable
    data class ProfileUpdate(
        val update: Profile.Update,
    ) : Writable {

        override val queueId: String
            get() = "update-profile-${update.profileId}-$update"

        override suspend fun WriteQueue.write(): Outcome =
            profileRepository.updateProfile(update)
    }

    @Serializable
    data class NotificationUpdate(
        val updates: List<NotificationPreferences.Update>,
    ) : Writable {

        override val queueId: String
            get() = "notification-pref-$updates"

        override suspend fun WriteQueue.write(): Outcome =
            notificationRepository.updateNotificationPreferences(updates)
    }
}

@Serializable
data class FailedWrite(
    val writable: Writable,
    val failedAt: Instant,
    val reason: Reason?,
) {
    @Serializable
    enum class Reason {
        IO,
    }
}

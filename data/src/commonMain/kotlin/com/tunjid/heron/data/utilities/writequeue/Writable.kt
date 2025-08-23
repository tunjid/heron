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
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Timeline
import kotlinx.serialization.Serializable

sealed interface Writable {
    /**
     * An id to identify if a [Writable] is in the [WriteQueue]
     */
    val queueId: String

    suspend fun WriteQueue.write()

    @Serializable
    data class Interaction(
        val interaction: Post.Interaction,
    ) : Writable {

        override val queueId: String
            get() = when (interaction) {
                is Post.Interaction.Create.Like -> "like-${interaction.postUri}"
                is Post.Interaction.Create.Repost -> "repost-${interaction.postUri}"
                is Post.Interaction.Delete.RemoveRepost -> "repost-${interaction.postUri}"
                is Post.Interaction.Delete.Unlike -> "like-${interaction.postUri}"
            }

        override suspend fun WriteQueue.write() {
            postRepository.sendInteraction(interaction)
        }
    }

    @Serializable
    data class Create(
        val request: Post.Create.Request,
    ) : Writable {

        override val queueId: String
            get() = "create-post-$request"

        override suspend fun WriteQueue.write() {
            postRepository.createPost(request)
        }
    }

    @Serializable
    data class Send(
        val request: Message.Create,
    ) : Writable {

        override val queueId: String
            get() = "send-message-$request"

        override suspend fun WriteQueue.write() {
            messageRepository.sendMessage(request)
        }
    }

    @Serializable
    data class Reaction(
        val update: Message.UpdateReaction,
    ) : Writable {

        override val queueId: String
            get() = "update-reaction-$update"

        override suspend fun WriteQueue.write() {
            messageRepository.updateReaction(update)
        }
    }

    @Serializable
    data class Connection(
        val connection: Profile.Connection,
    ) : Writable {

        override val queueId: String
            get() = when (connection) {
                is Profile.Connection.Follow -> "follow-${connection.profileId}"
                is Profile.Connection.Unfollow -> "unfollow-${connection.profileId}"
            }

        override suspend fun WriteQueue.write() {
            profileRepository.sendConnection(connection)
        }
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

                is Timeline.Update.OfFeedGenerator.Pin -> "pin-${update.uri}"
                is Timeline.Update.OfFeedGenerator.Remove -> "remove-${update.uri}"
                is Timeline.Update.OfFeedGenerator.Save -> "save-${update.uri}"
            }

        override suspend fun WriteQueue.write() {
            timelineRepository.updateHomeTimelines(update)
        }
    }
}
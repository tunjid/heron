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

import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
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
                is Post.Interaction.Create.Like -> "like-${interaction.postId}"
                is Post.Interaction.Create.Repost -> "repost-${interaction.postId}"
                is Post.Interaction.Delete.RemoveRepost -> "repost-${interaction.postId}"
                is Post.Interaction.Delete.Unlike -> "like-${interaction.postId}"
            }

        override suspend fun WriteQueue.write() {
            postRepository.sendInteraction(interaction)
        }
    }

    @Serializable
    data class Create(
        val request: Post.Create.Request,
        val replyTo: Post.Create.Reply?,
    ) : Writable {

        override val queueId: String
            get() = "create-post-$request-$replyTo"

        override suspend fun WriteQueue.write() {
            postRepository.createPost(request, replyTo)
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
}
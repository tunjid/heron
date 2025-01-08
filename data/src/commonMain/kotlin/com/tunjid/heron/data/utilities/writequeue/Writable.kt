package com.tunjid.heron.data.utilities.writequeue

import com.tunjid.heron.data.core.models.Post
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
            timelineRepository.sendInteraction(interaction)
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
            timelineRepository.createPost(request, replyTo)
        }
    }
}
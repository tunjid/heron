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

package com.tunjid.heron.data.core.models

import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val cid: Id,
    val uri: Uri,
    val author: Profile,
    val replyCount: Long,
    val repostCount: Long,
    val likeCount: Long,
    val quoteCount: Long,
    val indexedAt: Instant,
    val embed: Embed?,
    val quote: Post?,
    val record: Record?,
    val viewerStats: ViewerStats?,
//    public val viewer: ViewerState? = null,
//    public val labels: List<Label> = emptyList(),
//    public val threadgate: ThreadgateView? = null,
) : ByteSerializable {
    @Serializable
    data class Record(
        val text: String,
        val createdAt: Instant,
        val links: List<Link> = emptyList(),
        val replyRef: ReplyRef? = null,
    ) : ByteSerializable

    @Serializable
    data class ViewerStats(
        val likeUri: Uri?,
        val repostUri: Uri?,
        val threadMuted: Boolean,
        val replyDisabled: Boolean,
        val embeddingDisabled: Boolean,
        val pinned: Boolean,
    )

    @Serializable
    data class Link(
        val start: Int,
        val end: Int,
        val target: LinkTarget,
    )

    @Serializable
    data class ReplyRef(
        val rootCid: Id,
        val rootUri: Uri,
        val parentCid: Id,
        val parentUri: Uri,
    )

    @Serializable
    sealed interface LinkTarget {
        @Serializable
        data class UserHandleMention(
            val handle: Id,
        ) : LinkTarget

        @Serializable
        data class UserDidMention(
            val did: Id,
        ) : LinkTarget

        @Serializable
        data class ExternalLink(
            val uri: Uri,
        ) : LinkTarget

        @Serializable
        data class Hashtag(
            val tag: String,
        ) : LinkTarget
    }

    @Serializable
    sealed class Create : ByteSerializable {

        @Serializable
        data class Metadata(
            val quote: Quote? = null,
            val reply: Reply? = null,
            val imageList: ImageList? = null,
            val video: Video? = null,
        )

        @Serializable
        data class Reply(
            val parent: Post,
        ) : Create()

        @Serializable
        data class Mention(
            val profile: Profile,
        ) : Create()

        @Serializable
        data class Quote(
            val interaction: Interaction.Create.Repost,
        ) : Create()

        @Serializable
        data object Timeline : Create()

        @Serializable
        data class Request(
            val authorId: Id,
            val text: String,
            val links: List<Link>,
            val metadata: Metadata,
        )
    }

    @Serializable
    sealed class Interaction {

        abstract val postId: Id

        @Serializable
        sealed class Create : Interaction() {
            @Serializable
            data class Like(
                override val postId: Id,
                val postUri: Uri,
            ) : Create()

            @Serializable
            data class Repost(
                override val postId: Id,
                val postUri: Uri,
            ) : Create()
        }

        @Serializable
        sealed class Delete : Interaction() {

            @Serializable
            data class Unlike(
                override val postId: Id,
                val likeUri: Uri,
            ) : Delete()

            @Serializable
            data class RemoveRepost(
                override val postId: Id,
                val repostUri: Uri,
            ) : Delete()
        }
    }

    @Serializable
    sealed class Metadata {
        @Serializable
        data class Likes(
            val profileId: Id,
            val postId: Id,
        ) : Metadata()

        @Serializable
        data class Reposts(
            val profileId: Id,
            val postId: Id,
        ) : Metadata()

        @Serializable
        data class Quotes(
            val profileId: Id,
            val postId: Id,
        ) : Metadata()
    }
}

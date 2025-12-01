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

import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.ThreadGateUri
import com.tunjid.heron.data.core.utilities.File
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class Post(
    val cid: PostId,
    val uri: PostUri,
    val author: Profile,
    val replyCount: Long,
    val repostCount: Long,
    val likeCount: Long,
    val quoteCount: Long,
    // Default value needed in serialization
    val bookmarkCount: Long = 0,
    val indexedAt: Instant,
    val embed: Embed?,
    @Deprecated(
        message = "Use the quotedPost extension instead",
        replaceWith = ReplaceWith(
            expression = "quotedPost",
            "com.tunjid.heron.data.core.models.quotedPost",
        ),
    )
    val quote: Post?,
    val record: Record?,
    val viewerStats: ViewerStats?,
//    public val viewer: ViewerState? = null,
    val labels: List<Label>,
//    public val threadgate: ThreadgateView? = null,
    val embeddedRecord: com.tunjid.heron.data.core.models.Record?,
) : UrlEncodableModel,
    Record {

    override val reference: com.tunjid.heron.data.core.models.Record.Reference =
        com.tunjid.heron.data.core.models.Record.Reference(
            id = cid,
            uri = uri,
        )

    @Serializable
    data class Record(
        val text: String,
        val createdAt: Instant,
        val links: List<Link> = emptyList(),
        val replyRef: ReplyRef? = null,
    ) : UrlEncodableModel

    @Serializable
    data class ViewerStats(
        val likeUri: GenericUri?,
        val repostUri: GenericUri?,
        val threadMuted: Boolean,
        val replyDisabled: Boolean,
        val embeddingDisabled: Boolean,
        val pinned: Boolean,
        // Default value needed for migration
        val bookmarked: Boolean = false,
    )

    @Serializable
    data class ReplyRef(
        val rootCid: PostId,
        val rootUri: PostUri,
        val parentCid: PostId,
        val parentUri: PostUri,
    )

    @Serializable
    sealed class Create : UrlEncodableModel {

        @Serializable
        data class Metadata(
            @Deprecated(
                message = "Use the embeddedRecordReference field instead",
            )
            @ProtoNumber(1)
            val quote: Quote? = null,
            @ProtoNumber(2)
            val reply: Reply? = null,
            @ProtoNumber(3)
            @Deprecated(
                message = "Media data should be read separately",
                replaceWith = ReplaceWith(
                    "embeddedMedia",
                    "com.tunjid.heron.data.core.utilities.File",
                ),
            )
            val mediaFiles: List<MediaFile> = emptyList(),
            @ProtoNumber(4)
            val embeddedMedia: List<File.Media> = emptyList(),
            @ProtoNumber(5)
            val embeddedRecordReference: com.tunjid.heron.data.core.models.Record.Reference? = null,
        ) {
            init {
                @Suppress("DEPRECATION")
                require(!(mediaFiles.isNotEmpty() && embeddedMedia.isNotEmpty())) {
                    "Using both the deprecated mediaFiles and embeddedMedia fields is not allowed"
                }
            }
        }

        @Serializable
        data class Reply(
            val parent: Post,
        ) : Create(),
            UrlEncodableModel

        @Serializable
        data class Mention(
            val profile: Profile,
        ) : Create(),
            UrlEncodableModel

        @Serializable
        data class Quote(
            val interaction: Interaction.Create.Repost,
        ) : Create(),
            UrlEncodableModel

        @Serializable
        data object Timeline : Create(), UrlEncodableModel

        @Serializable
        data class Request(
            val authorId: ProfileId,
            val text: String,
            val links: List<Link>,
            val metadata: Metadata,
        )
    }

    @Serializable
    sealed class Interaction {

        abstract val postUri: PostUri

        @Serializable
        sealed class Create : Interaction() {
            @Serializable
            data class Like(
                val postId: PostId,
                override val postUri: PostUri,
            ) : Create()

            @Serializable
            data class Repost(
                val postId: PostId,
                override val postUri: PostUri,
            ) : Create()

            @Serializable
            data class Bookmark(
                val postId: PostId,
                override val postUri: PostUri,
            ) : Create()
        }

        @Serializable
        sealed class Delete : Interaction() {

            @Serializable
            data class Unlike(
                override val postUri: PostUri,
                val likeUri: GenericUri,
            ) : Delete()

            @Serializable
            data class RemoveRepost(
                override val postUri: PostUri,
                val repostUri: GenericUri,
            ) : Delete()

            @Serializable
            data class RemoveBookmark(
                override val postUri: PostUri,
            ) : Delete()
        }

        @Serializable
        sealed class Upsert : Interaction() {
            @Serializable
            data class Gate(
                override val postUri: PostUri,
                val threadGateUri: ThreadGateUri?,
                val allowsFollowing: Boolean,
                val allowsFollowers: Boolean,
                val allowsMentioned: Boolean,
                val allowedListUris: List<ListUri>,
            ) : Upsert()
        }
    }
}

fun Post.appliedLabels(
    adultContentEnabled: Boolean,
    labelers: List<Labeler>,
    labelPreferences: ContentLabelPreferences,
) = AppliedLabels(
    adultContentEnabled = adultContentEnabled,
    labels = labels + author.labels,
    labelers = labelers,
    contentLabelPreferences = labelPreferences,
)

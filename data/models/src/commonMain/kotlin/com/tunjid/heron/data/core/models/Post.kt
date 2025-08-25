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
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordKey
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val cid: PostId,
    val uri: PostUri,
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
    val labels: List<Label>,
//    public val threadgate: ThreadgateView? = null,
) : UrlEncodableModel {
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
            val quote: Quote? = null,
            val reply: Reply? = null,
            val mediaFiles: List<MediaFile> = emptyList(),
        )

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
        }
    }

    @Serializable
    sealed class Metadata {
        @Serializable
        data class Likes(
            val profileId: ProfileId,
            val postRecordKey: RecordKey,
        ) : Metadata()

        @Serializable
        data class Reposts(
            val profileId: ProfileId,
            val postRecordKey: RecordKey,
        ) : Metadata()

        @Serializable
        data class Quotes(
            val profileId: ProfileId,
            val postRecordKey: RecordKey,
        ) : Metadata()
    }
}

fun Post.labelVisibilitiesToDefinitions(
    labelers: List<Labeler>,
    labelPreferences: ContentLabelPreferences,
): Map<Label.Visibility, List<Label.Definition>> = labelVisibilitiesToDefinitions(
    postLabels = when {
        labels.isEmpty() -> emptySet()
        else -> labels.mapTo(
            destination = mutableSetOf(),
            transform = Label::value,
        )
    },
    labelers = labelers,
    labelsVisibilityMap = labelPreferences.associateBy(
        keySelector = ContentLabelPreference::label,
        valueTransform = ContentLabelPreference::visibility,
    ),
)

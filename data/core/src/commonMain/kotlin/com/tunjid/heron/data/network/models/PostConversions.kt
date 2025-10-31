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

package com.tunjid.heron.data.network.models

import app.bsky.embed.RecordViewRecordUnion
import app.bsky.embed.RecordWithMediaViewMediaUnion
import app.bsky.feed.GeneratorView
import app.bsky.feed.Post as BskyPost
import app.bsky.feed.PostEmbedUnion
import app.bsky.feed.PostView
import app.bsky.feed.PostViewEmbedUnion
import app.bsky.feed.ReplyRefParentUnion
import app.bsky.feed.ReplyRefRootUnion
import app.bsky.feed.ViewerState
import app.bsky.graph.ListView
import app.bsky.graph.StarterPackViewBasic
import app.bsky.richtext.Facet
import app.bsky.richtext.FacetFeatureUnion
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.ImageList
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.data.core.models.Link
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.StarterPack
import com.tunjid.heron.data.core.models.toUrlEncodedBase64
import com.tunjid.heron.data.core.types.FeedGeneratorId
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.GenericId
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ListId
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.core.types.StarterPackId
import com.tunjid.heron.data.core.types.StarterPackUri
import com.tunjid.heron.data.database.entities.PostEntity
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.database.entities.postembeds.ExternalEmbedEntity
import com.tunjid.heron.data.database.entities.postembeds.ImageEntity
import com.tunjid.heron.data.database.entities.postembeds.PostEmbed
import com.tunjid.heron.data.database.entities.postembeds.PostExternalEmbedEntity
import com.tunjid.heron.data.database.entities.postembeds.PostImageEntity
import com.tunjid.heron.data.database.entities.postembeds.PostVideoEntity
import com.tunjid.heron.data.database.entities.postembeds.VideoEntity
import com.tunjid.heron.data.database.entities.postembeds.asExternalModel
import com.tunjid.heron.data.database.entities.profile.PostViewerStatisticsEntity
import com.tunjid.heron.data.utilities.asRecordUri
import sh.christian.ozone.api.model.JsonContent

internal fun PostEntity.postVideoEntity(
    embedEntity: VideoEntity,
) = PostVideoEntity(
    postUri = uri,
    videoId = embedEntity.cid,
)

internal fun PostEntity.postImageEntity(
    embedEntity: ImageEntity,
) = PostImageEntity(
    postUri = uri,
    imageUri = embedEntity.fullSize,
)

internal fun PostEntity.postExternalEmbedEntity(
    embedEntity: ExternalEmbedEntity,
) = PostExternalEmbedEntity(
    postUri = uri,
    externalEmbedUri = embedEntity.uri,
)

internal fun PostView.post(
    viewingProfileId: ProfileId?,
): Post {
    val postEntity = postEntity()
    val quotedPostEntity = quotedPostEntity()
    val quotedPostProfileEntity = quotedPostProfileEntity()

    val embeddedRecord = embeddedRecord()

    val quotedPost = if (quotedPostEntity != null && quotedPostProfileEntity != null) {
        post(
            postEntity = quotedPostEntity,
            profileEntity = quotedPostProfileEntity,
            embeds = quotedPostEmbedEntities(),
            viewerStatisticsEntity = null,
            labels = emptyList(),
            embeddedRecord = null,
            quote = null,
        )
    } else {
        null
    }
    return post(
        postEntity = postEntity,
        profileEntity = profileEntity(),
        embeds = embedEntities(),
        viewerStatisticsEntity = viewer
            ?.postViewerStatisticsEntity(
                postUri = postEntity.uri,
                viewingProfileId = viewingProfileId,
            ),
        quote = quotedPost,
        labels = labels.map(com.atproto.label.Label::asExternalModel),
        embeddedRecord = embeddedRecord,
    )
}

private fun post(
    postEntity: PostEntity,
    profileEntity: ProfileEntity,
    embeds: List<PostEmbed>,
    viewerStatisticsEntity: PostViewerStatisticsEntity?,
    quote: Post?,
    labels: List<Label>,
    embeddedRecord: Record?,
) = Post(
    cid = postEntity.cid,
    uri = postEntity.uri,
    author = profileEntity.asExternalModel(),
    replyCount = postEntity.replyCount.orZero(),
    repostCount = postEntity.repostCount.orZero(),
    likeCount = postEntity.likeCount.orZero(),
    quoteCount = postEntity.quoteCount.orZero(),
    bookmarkCount = postEntity.bookmarkCount.orZero(),
    indexedAt = postEntity.indexedAt,
    record = postEntity.record?.asExternalModel(),
    embed = when (val embedEntity = embeds.firstOrNull()) {
        is ExternalEmbedEntity -> embedEntity.asExternalModel()
        is ImageEntity -> ImageList(
            images = embeds.filterIsInstance<ImageEntity>()
                .map(ImageEntity::asExternalModel),
        )

        is VideoEntity -> embedEntity.asExternalModel()
        null -> null
    },
    quote = quote,
    viewerStats = viewerStatisticsEntity?.asExternalModel(),
    labels = labels,
    embeddedRecord = embeddedRecord,
)

internal fun PostView.postEntity() =
    PostEntity(
        cid = PostId(cid.cid),
        uri = PostUri(uri.atUri),
        authorId = ProfileId(author.did.did),
        replyCount = replyCount,
        repostCount = repostCount,
        likeCount = likeCount,
        quoteCount = quoteCount,
        bookmarkCount = bookmarkCount,
        indexedAt = indexedAt,
        record = record.asPostEntityRecordData(),
    )

internal fun PostView.profileEntity(): ProfileEntity =
    author.profileEntity()

internal fun PostView.embedEntities(): List<PostEmbed> =
    when (val embed = embed) {
        is PostViewEmbedUnion.ExternalView -> listOf(
            ExternalEmbedEntity(
                uri = GenericUri(embed.value.external.uri.uri),
                title = embed.value.external.title,
                description = embed.value.external.description,
                thumb = embed.value.external.thumb?.uri?.let(::ImageUri),
            ),
        )

        is PostViewEmbedUnion.ImagesView -> embed.value.images.map {
            ImageEntity(
                fullSize = ImageUri(it.fullsize.uri),
                thumb = ImageUri(it.thumb.uri),
                alt = it.alt,
                width = it.aspectRatio?.width,
                height = it.aspectRatio?.height,
            )
        }

        is PostViewEmbedUnion.RecordView -> emptyList()
        is PostViewEmbedUnion.RecordWithMediaView -> when (val mediaEmbed = embed.value.media) {
            is RecordWithMediaViewMediaUnion.ExternalView -> listOf(
                ExternalEmbedEntity(
                    uri = GenericUri(mediaEmbed.value.external.uri.uri),
                    title = mediaEmbed.value.external.title,
                    description = mediaEmbed.value.external.description,
                    thumb = mediaEmbed.value.external.thumb?.uri?.let(::ImageUri),
                ),
            )

            is RecordWithMediaViewMediaUnion.ImagesView -> mediaEmbed.value.images.map {
                ImageEntity(
                    fullSize = ImageUri(it.fullsize.uri),
                    thumb = ImageUri(it.thumb.uri),
                    alt = it.alt,
                    width = it.aspectRatio?.width,
                    height = it.aspectRatio?.height,
                )
            }

            is RecordWithMediaViewMediaUnion.Unknown -> emptyList()
            is RecordWithMediaViewMediaUnion.VideoView -> listOf(
                VideoEntity(
                    cid = GenericId(mediaEmbed.value.cid.cid),
                    playlist = GenericUri(mediaEmbed.value.playlist.uri),
                    thumbnail = mediaEmbed.value.thumbnail?.uri?.let(::ImageUri),
                    alt = mediaEmbed.value.alt,
                    width = mediaEmbed.value.aspectRatio?.width,
                    height = mediaEmbed.value.aspectRatio?.height,
                ),
            )
        }

        is PostViewEmbedUnion.Unknown -> emptyList()
        is PostViewEmbedUnion.VideoView -> listOf(
            VideoEntity(
                cid = GenericId(embed.value.cid.cid),
                playlist = GenericUri(embed.value.playlist.uri),
                thumbnail = embed.value.thumbnail?.uri?.let(::ImageUri),
                alt = embed.value.alt,
                width = embed.value.aspectRatio?.width,
                height = embed.value.aspectRatio?.height,
            ),
        )

        null -> emptyList()
    }

internal fun ViewerState.postViewerStatisticsEntity(
    postUri: PostUri,
    viewingProfileId: ProfileId?,
) =
    if (viewingProfileId == null) null
    else PostViewerStatisticsEntity(
        postUri = postUri,
        viewingProfileId = viewingProfileId,
        likeUri = like?.atUri?.let(::GenericUri),
        repostUri = repost?.atUri?.let(::GenericUri),
        threadMuted = threadMuted ?: false,
        replyDisabled = replyDisabled ?: false,
        embeddingDisabled = embeddingDisabled ?: false,
        pinned = pinned ?: false,
        bookmarked = bookmarked ?: false,
    )

internal fun ReplyRefRootUnion.postViewerStatisticsEntity(
    viewingProfileId: ProfileId?,
) = when (this) {
    is ReplyRefRootUnion.PostView -> value.viewer?.postViewerStatisticsEntity(
        postUri = PostUri(value.uri.atUri),
        viewingProfileId = viewingProfileId,
    )

    is ReplyRefRootUnion.BlockedPost,
    is ReplyRefRootUnion.NotFoundPost,
    is ReplyRefRootUnion.Unknown,
    -> null
}

internal fun ReplyRefParentUnion.postViewerStatisticsEntity(
    viewingProfileId: ProfileId?,
) = when (this) {
    is ReplyRefParentUnion.PostView -> value.viewer?.postViewerStatisticsEntity(
        postUri = PostUri(value.uri.atUri),
        viewingProfileId = viewingProfileId,
    )

    is ReplyRefParentUnion.BlockedPost,
    is ReplyRefParentUnion.NotFoundPost,
    is ReplyRefParentUnion.Unknown,
    -> null
}

internal fun JsonContent.asPostEntityRecordData(): PostEntity.RecordData? =
    try {
        val bskyPost = decodeAs<BskyPost>()

        val embeddedUri: RecordUri? = when (val embed = bskyPost.embed) {
            is PostEmbedUnion.Record ->
                embed.value.record.uri.atUri.asRecordUri()

            is PostEmbedUnion.RecordWithMedia ->
                embed.value.record.record.uri.atUri.asRecordUri()

            else -> null
        }

        PostEntity.RecordData(
            text = bskyPost.text,
            base64EncodedRecord = bskyPost.toPostRecord().toUrlEncodedBase64(),
            createdAt = bskyPost.createdAt,
            embeddedRecordUri = embeddedUri,
        )
    } catch (_: Exception) {
        null
    }
private fun BskyPost.toPostRecord() =
    Post.Record(
        text = text,
        createdAt = createdAt,
        links = facets.mapNotNull(Facet::toLinkOrNull),
        replyRef = reply?.let {
            Post.ReplyRef(
                rootCid = it.root.cid.cid.let(::PostId),
                rootUri = it.root.uri.atUri.let(::PostUri),
                parentCid = it.parent.cid.cid.let(::PostId),
                parentUri = it.parent.uri.atUri.let(::PostUri),
            )
        },
    )

private fun Facet.toLinkOrNull(): Link? {
    return if (features.isEmpty()) null else Link(
        start = index.byteStart.toInt(),
        end = index.byteEnd.toInt(),
        target = when (val feature = features.first()) {
            is FacetFeatureUnion.Link -> LinkTarget.ExternalLink(feature.value.uri.uri.let(::GenericUri))
            is FacetFeatureUnion.Mention -> LinkTarget.UserDidMention(
                feature.value.did.did.let(::ProfileId),
            )

            is FacetFeatureUnion.Tag -> LinkTarget.Hashtag(feature.value.tag)
            is FacetFeatureUnion.Unknown -> return null
        },
    )
}
private fun PostView.embeddedRecord(): Record? =
    when (val embed = embed) {
        is PostViewEmbedUnion.RecordView -> when (val recordUnion = embed.value.record) {
            is RecordViewRecordUnion.FeedGeneratorView ->
                recordUnion.value.asExternalModel()
            is RecordViewRecordUnion.GraphListView ->
                recordUnion.value.asExternalModel()
            is RecordViewRecordUnion.GraphStarterPackViewBasic ->
                recordUnion.value.asExternalModel()
            // This is a regular post, but we already handled the quotedPostEntity case above
            is RecordViewRecordUnion.ViewRecord -> null
            else -> null
        }
        is PostViewEmbedUnion.RecordWithMediaView -> {
            when (val recordUnion = embed.value.record.record) {
                is RecordViewRecordUnion.FeedGeneratorView ->
                    recordUnion.value.asExternalModel()
                is RecordViewRecordUnion.GraphListView ->
                    recordUnion.value.asExternalModel()
                is RecordViewRecordUnion.GraphStarterPackViewBasic ->
                    recordUnion.value.asExternalModel()
                else -> null
            }
        }
        else -> null
    }

private fun StarterPackViewBasic.asExternalModel() = StarterPack(
    cid = StarterPackId(cid.cid),
    uri = StarterPackUri(uri.atUri),
    name = "", // You might need to handle this if available
    description = "", // You might need to handle this if available
    creator = creator.profileEntity().asExternalModel(),
    list = null, // You might need to handle this if available
    joinedWeekCount = joinedWeekCount,
    joinedAllTimeCount = joinedAllTimeCount,
    indexedAt = indexedAt,
    labels = labels.map(com.atproto.label.Label::asExternalModel),
)

private fun ListView.asExternalModel() = FeedList(
    cid = ListId(cid.cid),
    uri = ListUri(uri.atUri),
    creator = creator.profileEntity().asExternalModel(),
    name = name,
    description = description,
    avatar = avatar?.uri?.let(::ImageUri),
    listItemCount = listItemCount,
    purpose = purpose.toString(),
    indexedAt = indexedAt,
    labels = labels.map(com.atproto.label.Label::asExternalModel),
)

private fun GeneratorView.asExternalModel() = FeedGenerator(
    cid = FeedGeneratorId(cid.cid),
    uri = FeedGeneratorUri(uri.atUri),
    did = FeedGeneratorId(did.did),
    avatar = avatar?.uri?.let(::ImageUri),
    likeCount = likeCount,
    creator = creator.profileEntity().asExternalModel(),
    displayName = displayName,
    description = description,
    contentMode = contentMode,
    acceptsInteractions = acceptsInteractions,
    indexedAt = indexedAt,
    labels = labels.map(com.atproto.label.Label::asExternalModel),
)

private fun com.atproto.label.Label.asExternalModel() = Label(
    uri = uri.uri.let(::GenericUri),
    creatorId = src.did.let(::ProfileId),
    value = Label.Value(`val`),
    version = ver,
    createdAt = cts,
)

private fun Long?.orZero() = this ?: 0L

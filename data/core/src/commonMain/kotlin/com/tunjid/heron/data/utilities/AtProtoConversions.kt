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

package com.tunjid.heron.data.utilities

import app.bsky.embed.AspectRatio
import app.bsky.embed.Images as BskyImages
import app.bsky.embed.ImagesImage
import app.bsky.embed.Record
import app.bsky.embed.RecordWithMedia
import app.bsky.embed.RecordWithMediaMediaUnion
import app.bsky.embed.Video as BskyVideo
import app.bsky.feed.PostEmbedUnion
import app.bsky.richtext.Facet
import app.bsky.richtext.FacetByteSlice
import app.bsky.richtext.FacetFeatureUnion.Link as FacetFeatureLink
import app.bsky.richtext.FacetFeatureUnion.Mention as FacetFeatureMention
import app.bsky.richtext.FacetFeatureUnion.Tag as FacetFeatureTag
import app.bsky.richtext.FacetLink
import app.bsky.richtext.FacetMention
import app.bsky.richtext.FacetTag
import com.atproto.repo.StrongRef
import com.tunjid.heron.data.core.models.Link
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.MediaFile
import com.tunjid.heron.data.core.models.Post
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Cid
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Uri as BskyUri
import sh.christian.ozone.api.model.Blob

internal sealed class MediaBlob {
    data class Image(
        val file: MediaFile.Photo,
        val blob: Blob,
    ) : MediaBlob()

    data class Video(
        val file: MediaFile.Video,
        val blob: Blob,
    ) : MediaBlob()
}

internal fun MediaFile.with(blob: Blob) = when (this) {
    is MediaFile.Photo -> MediaBlob.Image(
        file = this,
        blob = blob,
    )

    is MediaFile.Video -> MediaBlob.Video(
        file = this,
        blob = blob,
    )
}

internal fun postEmbedUnion(
    repost: Post.Interaction.Create.Repost?,
    mediaBlobs: List<MediaBlob>,
): PostEmbedUnion? {
    val record = repost?.toRecord()
    val video = mediaBlobs.video()
    val images = mediaBlobs.images()

    return when {
        record != null && (video != null || images != null) -> PostEmbedUnion.RecordWithMedia(
            value = RecordWithMedia(
                record = record,
                media = video
                    ?.let { RecordWithMediaMediaUnion.Video(it) }
                    ?: images?.let { RecordWithMediaMediaUnion.Images(it) }
                    ?: throw IllegalArgumentException("Media should exist"),
            ),
        )

        record != null -> PostEmbedUnion.Record(
            value = record,
        )

        video != null -> PostEmbedUnion.Video(video)
        images != null -> PostEmbedUnion.Images(images)
        else -> null
    }
}

internal fun List<Link>.facet(): List<Facet> = map { link ->
    Facet(
        index = FacetByteSlice(
            byteStart = link.start.toLong(),
            byteEnd = link.end.toLong(),
        ),
        features = when (val target = link.target) {
            is LinkTarget.ExternalLink -> listOf(
                FacetFeatureLink(FacetLink(target.uri.uri.let(::BskyUri))),
            )

            is LinkTarget.UserDidMention -> listOf(
                FacetFeatureMention(FacetMention(target.did.id.let(::Did))),
            )

            is LinkTarget.Hashtag -> listOf(
                FacetFeatureTag(FacetTag(target.tag)),
            )

            is LinkTarget.UserHandleMention -> emptyList()
        },
    )
}

private fun Post.Interaction.Create.Repost.toRecord(): Record = Record(
    record = StrongRef(
        uri = AtUri(postUri.uri),
        cid = Cid(postId.id),
    ),
)

private fun List<MediaBlob>.video(): BskyVideo? = filterIsInstance<MediaBlob.Video>()
    .firstOrNull()
    ?.let { videoFile ->
        BskyVideo(
            video = videoFile.blob,
            alt = videoFile.file.altText,
            aspectRatio = AspectRatio(
                videoFile.file.width,
                videoFile.file.height,
            ),
        )
    }

private fun List<MediaBlob>.images(): BskyImages? = filterIsInstance<MediaBlob.Image>()
    .map { photoFile ->
        ImagesImage(
            image = photoFile.blob,
            alt = photoFile.file.altText,
            aspectRatio = AspectRatio(
                photoFile.file.width,
                photoFile.file.height,
            ),
        )
    }
    .takeUnless(List<ImagesImage>::isEmpty)
    ?.let(::BskyImages)

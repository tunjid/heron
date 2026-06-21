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
import app.bsky.embed.External as BskyExternal
import app.bsky.embed.ExternalExternal
import app.bsky.embed.Gallery
import app.bsky.embed.GalleryImage
import app.bsky.embed.GalleryItemUnion
import app.bsky.embed.Images as BskyImages
import app.bsky.embed.ImagesImage
import app.bsky.embed.Record as BskyRecord
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
import com.tunjid.heron.data.core.models.LinkPreview
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.MediaList
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.utilities.File
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Cid
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Uri as BskyUri
import sh.christian.ozone.api.model.Blob

internal sealed class MediaBlob {
    sealed class Image : MediaBlob() {
        data class Local(
            val file: File.Media.Photo,
            val blob: Blob,
        ) : Image()
    }

    sealed class Video : MediaBlob() {
        data class Local(
            val file: File.Media.Video,
            val blob: Blob,
        ) : Video()
    }
}

internal fun File.Media.with(blob: Blob) = when (this) {
    is File.Media.Photo -> MediaBlob.Image.Local(
        file = this,
        blob = blob,
    )

    is File.Media.Video -> MediaBlob.Video.Local(
        file = this,
        blob = blob,
    )
}

internal fun postEmbedUnion(
    embeddedRecordReference: Record.Reference?,
    mediaBlobs: List<MediaBlob>,
    linkPreview: LinkPreview?,
    externalThumbBlob: Blob?,
): PostEmbedUnion? {
    val record = embeddedRecordReference?.toStrongReferencedRecord()
    val video = mediaBlobs.video()
    val images = mediaBlobs.images()
    val hasMedia = video != null || images != null || linkPreview != null

    return when {
        record != null && hasMedia -> PostEmbedUnion.RecordWithMedia(
            value = RecordWithMedia(
                record = record,
                media = video
                    ?.let { RecordWithMediaMediaUnion.Video(it) }
                    ?: images?.let { RecordWithMediaMediaUnion.Images(it) }
                    ?: linkPreview?.let {
                        RecordWithMediaMediaUnion.External(
                            it.bskyExternalMedia(externalThumbBlob),
                        )
                    }
                    ?: throw IllegalArgumentException("Media should exist"),
            ),
        )

        record != null -> PostEmbedUnion.Record(
            value = record,
        )

        video != null -> PostEmbedUnion.Video(video)
        images != null ->
            images.images
                .mapNotNull(ImagesImage::galleryItemImage)
                .let {
                    when {
                        it.size < MediaList.MinGallerySize -> PostEmbedUnion.Images(images)
                        else -> PostEmbedUnion.Gallery(Gallery(items = it))
                    }
                }

        // An external link card only applies when the post has no quote and no media.
        linkPreview != null -> PostEmbedUnion.External(
            value = linkPreview.bskyExternalMedia(externalThumbBlob),
        )

        else -> null
    }
}

private fun LinkPreview.bskyExternalMedia(
    externalThumbBlob: Blob?,
): BskyExternal = BskyExternal(
    external = ExternalExternal(
        uri = BskyUri(embed.uri.uri),
        title = embed.title,
        description = embed.description,
        thumb = externalThumbBlob,
        associatedRefs = records.associatedStrongRefs(),
    ),
)

/**
 * The strong refs (uri + cid) of the `site.standard.*` records backing a link preview, written into
 * `app.bsky.embed.external#associatedRefs`. Records without a cid are skipped; `null` when none remain.
 */
private fun List<Record.Embeddable.External>.associatedStrongRefs(): List<StrongRef>? =
    mapNotNull { record ->
        val cid = record.reference.id ?: return@mapNotNull null
        StrongRef(
            uri = AtUri(record.reference.uri.uri),
            cid = Cid(cid.id),
        )
    }.takeIf(List<StrongRef>::isNotEmpty)

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

internal fun Record.Reference.toStrongReferencedRecord(): BskyRecord? =
    when (val id = id) {
        null -> null
        else -> BskyRecord(
            record = StrongRef(
                uri = AtUri(uri.uri),
                cid = Cid(id.id),
            ),
        )
    }

private fun List<MediaBlob>.video(): BskyVideo? =
    filterIsInstance<MediaBlob.Video>()
        .firstOrNull()
        ?.let { videoFile ->
            when (videoFile) {
                is MediaBlob.Video.Local -> BskyVideo(
                    video = videoFile.blob,
                    alt = videoFile.file.altText,
                    aspectRatio = AspectRatio(
                        videoFile.file.width.toLong(),
                        videoFile.file.height.toLong(),
                    ),
                )
            }
        }

private fun List<MediaBlob>.images(): BskyImages? =
    filterIsInstance<MediaBlob.Image>()
        .map { photoFile ->
            when (photoFile) {
                is MediaBlob.Image.Local -> ImagesImage(
                    image = photoFile.blob,
                    alt = photoFile.file.altText ?: "",
                    aspectRatio = AspectRatio(
                        photoFile.file.width.toLong(),
                        photoFile.file.height.toLong(),
                    ),
                )
            }
        }
        .takeUnless(List<ImagesImage>::isEmpty)
        ?.let(::BskyImages)

private fun ImagesImage.galleryItemImage(): GalleryItemUnion.Image? =
    aspectRatio?.let { aspectRatio ->
        GalleryItemUnion.Image(
            GalleryImage(
                image = image,
                alt = alt,
                aspectRatio = aspectRatio,
            ),
        )
    }

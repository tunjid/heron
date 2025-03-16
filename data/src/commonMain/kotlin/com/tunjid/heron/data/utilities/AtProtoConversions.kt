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
import app.bsky.embed.ImagesImage
import app.bsky.embed.Record
import app.bsky.embed.RecordWithMedia
import app.bsky.embed.RecordWithMediaMediaUnion
import app.bsky.feed.PostEmbedUnion
import com.atproto.repo.StrongRef
import com.tunjid.heron.data.core.models.MediaFile
import com.tunjid.heron.data.core.models.Post
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Cid
import sh.christian.ozone.api.model.Blob
import app.bsky.embed.Images as BskyImages
import app.bsky.embed.Video as BskyVideo

internal data class MediaBlob(
    val file: MediaFile,
    val blob: Blob,
)

internal fun MediaFile.with(blob: Blob) = MediaBlob(
    file = this,
    blob = blob,
)

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
                    ?: throw IllegalArgumentException("Media should exist")
            )
        )

        record != null -> PostEmbedUnion.Record(
            value = record
        )

        video != null -> PostEmbedUnion.Video(video)
        images != null -> PostEmbedUnion.Images(images)
        else -> null
    }
}

private fun Post.Interaction.Create.Repost.toRecord(): Record =
    Record(
        record = StrongRef(
            uri = AtUri(postUri.uri),
            cid = Cid(postId.id),
        )
    )

private fun List<MediaBlob>.video(): BskyVideo? =
    firstOrNull { it.file is MediaFile.Video }
        ?.let { videoFile ->
            BskyVideo(
                video = videoFile.blob,
                aspectRatio = AspectRatio(
                    videoFile.file.width,
                    videoFile.file.height
                )
            )
        }

private fun List<MediaBlob>.images(): BskyImages? =
    filter { it.file is MediaFile.Photo }
        .map { photoFile ->
            ImagesImage(
                image = photoFile.blob,
                alt = "ball",
                aspectRatio = AspectRatio(
                    photoFile.file.width,
                    photoFile.file.height
                )
            )
        }
        .takeUnless(List<ImagesImage>::isEmpty)
        ?.let(::BskyImages)

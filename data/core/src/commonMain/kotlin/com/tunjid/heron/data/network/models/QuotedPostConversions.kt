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

import app.bsky.actor.ProfileViewBasic
import app.bsky.embed.RecordViewRecordEmbedUnion
import app.bsky.embed.RecordViewRecordUnion
import app.bsky.feed.PostView
import app.bsky.feed.PostViewEmbedUnion
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.PostEntity
import com.tunjid.heron.data.database.entities.postembeds.PostEmbed

internal fun PostView.quotedPostEntity(): PostEntity? =
    when (val embed = embed) {
        is PostViewEmbedUnion.ExternalView -> null
        is PostViewEmbedUnion.ImagesView -> null
        is PostViewEmbedUnion.RecordWithMediaView -> embed.value.record.record.postEntity()
        is PostViewEmbedUnion.Unknown -> null
        is PostViewEmbedUnion.VideoView -> null
        is PostViewEmbedUnion.GalleryView -> null
        is PostViewEmbedUnion.RecordView -> embed.value.record.postEntity()

        null -> null
    }

internal fun PostView.quotedPostProfileView(): ProfileViewBasic? =
    when (val embed = embed) {
        is PostViewEmbedUnion.ExternalView -> null
        is PostViewEmbedUnion.ImagesView -> null
        is PostViewEmbedUnion.RecordWithMediaView -> embed.value.record.record.profileView()

        is PostViewEmbedUnion.Unknown -> null
        is PostViewEmbedUnion.VideoView -> null
        is PostViewEmbedUnion.GalleryView -> null
        is PostViewEmbedUnion.RecordView -> embed.value.record.profileView()

        null -> null
    }

private fun RecordViewRecordUnion.profileView() =
    when (this) {
        is RecordViewRecordUnion.FeedGeneratorView,
        is RecordViewRecordUnion.GraphListView,
        is RecordViewRecordUnion.GraphStarterPackViewBasic,
        is RecordViewRecordUnion.LabelerLabelerView,
        is RecordViewRecordUnion.Unknown,
        is RecordViewRecordUnion.ViewBlocked,
        is RecordViewRecordUnion.ViewDetached,
        is RecordViewRecordUnion.ViewNotFound,
        -> null

        is RecordViewRecordUnion.ViewRecord -> value.author
    }

internal fun PostView.quotedPostEmbedEntities(): List<PostEmbed> =
    when (val embed = embed) {
        is PostViewEmbedUnion.ExternalView -> emptyList()
        is PostViewEmbedUnion.ImagesView -> emptyList()
        is PostViewEmbedUnion.RecordWithMediaView ->
            embed
                .value
                .record
                .record
                .embedEntities()

        is PostViewEmbedUnion.Unknown -> emptyList()
        is PostViewEmbedUnion.VideoView -> emptyList()
        is PostViewEmbedUnion.GalleryView -> emptyList()
        is PostViewEmbedUnion.RecordView ->
            embed
                .value
                .record
                .embedEntities()

        null -> emptyList()
    }

private fun RecordViewRecordUnion.embedEntities(): List<PostEmbed> =
    when (this) {
        is RecordViewRecordUnion.FeedGeneratorView,
        is RecordViewRecordUnion.GraphListView,
        is RecordViewRecordUnion.GraphStarterPackViewBasic,
        is RecordViewRecordUnion.LabelerLabelerView,
        is RecordViewRecordUnion.Unknown,
        is RecordViewRecordUnion.ViewDetached,
        is RecordViewRecordUnion.ViewBlocked,
        is RecordViewRecordUnion.ViewNotFound,
        -> emptyList()

        is RecordViewRecordUnion.ViewRecord ->
            value.embeds?.map { innerRecord ->
                when (innerRecord) {
                    is RecordViewRecordEmbedUnion.ExternalView -> listOf(
                        innerRecord.value.external.asExternalEmbedEntity(),
                    )

                    is RecordViewRecordEmbedUnion.ImagesView ->
                        innerRecord.value
                            .images
                            .mapIndexed(::imageEntity)

                    is RecordViewRecordEmbedUnion.RecordView -> emptyList()
                    is RecordViewRecordEmbedUnion.RecordWithMediaView -> emptyList()
                    is RecordViewRecordEmbedUnion.Unknown -> emptyList()
                    is RecordViewRecordEmbedUnion.VideoView -> listOf(
                        videoEntity(
                            index = 0,
                            videoView = innerRecord.value,
                        ),
                    )
                    is RecordViewRecordEmbedUnion.GalleryView ->
                        innerRecord.value
                            .items
                            .mapIndexedNotNull(::postEmbed)
                }
            } ?: emptyList()
    }.flatten()

private fun RecordViewRecordUnion.postEntity() =
    when (this) {
        is RecordViewRecordUnion.FeedGeneratorView,
        is RecordViewRecordUnion.GraphListView,
        is RecordViewRecordUnion.GraphStarterPackViewBasic,
        is RecordViewRecordUnion.LabelerLabelerView,
        is RecordViewRecordUnion.Unknown,
        is RecordViewRecordUnion.ViewDetached,
        is RecordViewRecordUnion.ViewBlocked,
        is RecordViewRecordUnion.ViewNotFound,
        -> null

        is RecordViewRecordUnion.ViewRecord ->
            PostEntity(
                cid = PostId(value.cid.cid),
                uri = PostUri(value.uri.atUri),
                authorId = ProfileId(value.author.did.did),
                replyCount = value.replyCount,
                repostCount = value.repostCount,
                likeCount = value.likeCount,
                quoteCount = value.quoteCount,
                indexedAt = value.indexedAt,
                hasThreadGate = null,
                record = value.value.asPostEntityRecordData(),
            )
    }

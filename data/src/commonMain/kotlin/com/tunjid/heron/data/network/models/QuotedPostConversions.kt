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

import app.bsky.embed.RecordViewRecordEmbedUnion
import app.bsky.embed.RecordViewRecordUnion
import app.bsky.embed.RecordWithMediaViewMediaUnion
import app.bsky.feed.PostView
import app.bsky.feed.PostViewEmbedUnion
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.entities.PostEntity
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.postembeds.ExternalEmbedEntity
import com.tunjid.heron.data.database.entities.postembeds.ImageEntity
import com.tunjid.heron.data.database.entities.postembeds.PostEmbed
import com.tunjid.heron.data.database.entities.postembeds.VideoEntity

internal fun PostView.quotedPostEntity(): PostEntity? =
    when (val embed = embed) {
        is PostViewEmbedUnion.ExternalView -> null
        is PostViewEmbedUnion.ImagesView -> null
        is PostViewEmbedUnion.RecordWithMediaView -> null
        is PostViewEmbedUnion.Unknown -> null
        is PostViewEmbedUnion.VideoView -> null
        is PostViewEmbedUnion.RecordView -> when (val innerEmbed = embed.value.record) {
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
                    cid = Id(innerEmbed.value.cid.cid),
                    uri = Uri(innerEmbed.value.uri.atUri),
                    authorId = Id(innerEmbed.value.author.did.did),
                    replyCount = innerEmbed.value.replyCount,
                    repostCount = innerEmbed.value.repostCount,
                    likeCount = innerEmbed.value.likeCount,
                    quoteCount = innerEmbed.value.quoteCount,
                    indexedAt = innerEmbed.value.indexedAt,
                    record = innerEmbed.value.value.asPostEntityRecordData(),
                )
        }

        null -> null
    }

internal fun PostView.quotedPostProfileEntity(): ProfileEntity? =
    when (val embed = embed) {
        is PostViewEmbedUnion.ExternalView -> null
        is PostViewEmbedUnion.ImagesView -> null
        is PostViewEmbedUnion.RecordWithMediaView -> when (val innerEmbed =
            embed.value.record.record) {
            is RecordViewRecordUnion.FeedGeneratorView,
            is RecordViewRecordUnion.GraphListView,
            is RecordViewRecordUnion.GraphStarterPackViewBasic,
            is RecordViewRecordUnion.LabelerLabelerView,
            is RecordViewRecordUnion.Unknown,
            is RecordViewRecordUnion.ViewBlocked,
            is RecordViewRecordUnion.ViewDetached,
            is RecordViewRecordUnion.ViewNotFound,
                -> null

            is RecordViewRecordUnion.ViewRecord ->
                innerEmbed.profileEntity()
        }

        is PostViewEmbedUnion.Unknown -> null
        is PostViewEmbedUnion.VideoView -> null
        is PostViewEmbedUnion.RecordView -> when (val innerEmbed = embed.value.record) {
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
                innerEmbed.profileEntity()
        }

        null -> null
    }

private fun RecordViewRecordUnion.ViewRecord.profileEntity() =
    ProfileEntity(
        did = Id(value.author.did.did),
        handle = Id(value.author.handle.handle),
        displayName = value.author.displayName,
        description = null,
        avatar = value.author.avatar?.uri?.let(::Uri),
        banner = null,
        followersCount = 0,
        followsCount = 0,
        postsCount = 0,
        joinedViaStarterPack = null,
        indexedAt = null,
        createdAt = value.author.createdAt,
    )

internal fun PostView.quotedPostEmbedEntities(): List<PostEmbed> =
    when (val embed = embed) {
        is PostViewEmbedUnion.ExternalView -> emptyList()
        is PostViewEmbedUnion.ImagesView -> emptyList()
        is PostViewEmbedUnion.RecordWithMediaView -> embed.embedEntities()
        is PostViewEmbedUnion.Unknown -> emptyList()
        is PostViewEmbedUnion.VideoView -> emptyList()
        is PostViewEmbedUnion.RecordView -> embed.embedEntities()

        null -> emptyList()
    }

internal fun PostViewEmbedUnion.RecordWithMediaView.embedEntities() =
    when (val innerEmbed = value.media) {
        is RecordWithMediaViewMediaUnion.ExternalView -> listOf(
            ExternalEmbedEntity(
                uri = Uri(innerEmbed.value.external.uri.uri),
                title = innerEmbed.value.external.title,
                description = innerEmbed.value.external.description,
                thumb = innerEmbed.value.external.thumb?.uri?.let(::Uri),
            )
        )

        is RecordWithMediaViewMediaUnion.ImagesView -> innerEmbed.value.images.map {
            ImageEntity(
                fullSize = Uri(it.fullsize.uri),
                thumb = Uri(it.thumb.uri),
                alt = it.alt,
                width = it.aspectRatio?.width,
                height = it.aspectRatio?.height,
            )
        }

        is RecordWithMediaViewMediaUnion.Unknown -> emptyList()
        is RecordWithMediaViewMediaUnion.VideoView -> listOf(
            VideoEntity(
                cid = Id(innerEmbed.value.cid.cid),
                playlist = Uri(innerEmbed.value.playlist.uri),
                thumbnail = innerEmbed.value.thumbnail?.uri?.let(::Uri),
                alt = innerEmbed.value.alt,
                width = innerEmbed.value.aspectRatio?.width,
                height = innerEmbed.value.aspectRatio?.height,
            )
        )
    }

internal fun PostViewEmbedUnion.RecordView.embedEntities() =
    when (val innerEmbed = value.record) {
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
            innerEmbed.value.embeds.map<RecordViewRecordEmbedUnion, List<PostEmbed>> { innerRecord ->
                when (innerRecord) {
                    is RecordViewRecordEmbedUnion.ExternalView -> listOf(
                        ExternalEmbedEntity(
                            uri = Uri(innerRecord.value.external.uri.uri),
                            title = innerRecord.value.external.title,
                            description = innerRecord.value.external.description,
                            thumb = innerRecord.value.external.thumb?.uri?.let(::Uri),
                        )
                    )

                    is RecordViewRecordEmbedUnion.ImagesView -> innerRecord.value.images.map {
                        ImageEntity(
                            fullSize = Uri(it.fullsize.uri),
                            thumb = Uri(it.thumb.uri),
                            alt = it.alt,
                            width = it.aspectRatio?.width,
                            height = it.aspectRatio?.height,
                        )
                    }

                    is RecordViewRecordEmbedUnion.RecordView -> emptyList()
                    is RecordViewRecordEmbedUnion.RecordWithMediaView -> emptyList()
                    is RecordViewRecordEmbedUnion.Unknown -> emptyList()
                    is RecordViewRecordEmbedUnion.VideoView -> listOf(
                        VideoEntity(
                            cid = Id(innerRecord.value.cid.cid),
                            playlist = Uri(innerRecord.value.playlist.uri),
                            thumbnail = innerRecord.value.thumbnail?.uri?.let(::Uri),
                            alt = innerRecord.value.alt,
                            width = innerRecord.value.aspectRatio?.width,
                            height = innerRecord.value.aspectRatio?.height,
                        )
                    )
                }
            }
    }.flatten()

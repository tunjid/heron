package com.tunjid.heron.data.network.models

import app.bsky.embed.RecordViewRecordEmbedUnion
import app.bsky.embed.RecordViewRecordUnion
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
            is RecordViewRecordUnion.ViewNotFound -> null

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
            is RecordViewRecordUnion.ViewNotFound -> null

            is RecordViewRecordUnion.ViewRecord ->
                ProfileEntity(
                    did = Id(innerEmbed.value.author.did.did),
                    handle = Id(innerEmbed.value.author.handle.handle),
                    displayName = innerEmbed.value.author.displayName,
                    description = null,
                    avatar = innerEmbed.value.author.avatar?.uri?.let(::Uri),
                    banner = null,
                    followersCount = 0,
                    followsCount = 0,
                    postsCount = 0,
                    joinedViaStarterPack = null,
                    indexedAt = null,
                    createdAt = innerEmbed.value.author.createdAt,
                )
        }

        null -> null
    }

internal fun PostView.quotedPostEmbedEntities(): List<PostEmbed> =
    when (val embed = embed) {
        is PostViewEmbedUnion.ExternalView -> emptyList()
        is PostViewEmbedUnion.ImagesView -> emptyList()
        is PostViewEmbedUnion.RecordWithMediaView -> emptyList()
        is PostViewEmbedUnion.Unknown -> emptyList()
        is PostViewEmbedUnion.VideoView -> emptyList()
        is PostViewEmbedUnion.RecordView -> when (val innerEmbed = embed.value.record) {
            is RecordViewRecordUnion.FeedGeneratorView,
            is RecordViewRecordUnion.GraphListView,
            is RecordViewRecordUnion.GraphStarterPackViewBasic,
            is RecordViewRecordUnion.LabelerLabelerView,
            is RecordViewRecordUnion.Unknown,
            is RecordViewRecordUnion.ViewDetached,
            is RecordViewRecordUnion.ViewBlocked,
            is RecordViewRecordUnion.ViewNotFound -> emptyList()

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

        null -> emptyList()
    }
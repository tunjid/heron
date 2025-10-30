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

import app.bsky.embed.RecordViewRecord
import app.bsky.embed.RecordViewRecordEmbedUnion
import app.bsky.embed.RecordViewRecordUnion
import app.bsky.feed.GeneratorView
import app.bsky.feed.PostView
import app.bsky.feed.PostViewEmbedUnion
import app.bsky.graph.ListView
import app.bsky.graph.StarterPackViewBasic
import com.tunjid.heron.data.core.types.FeedGeneratorId
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.GenericId
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ListId
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.core.types.StarterPackId
import com.tunjid.heron.data.core.types.StarterPackUri
import com.tunjid.heron.data.database.entities.FeedGeneratorEntity
import com.tunjid.heron.data.database.entities.ListEntity
import com.tunjid.heron.data.database.entities.PostEntity
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.StarterPackEntity
import com.tunjid.heron.data.database.entities.postembeds.ExternalEmbedEntity
import com.tunjid.heron.data.database.entities.postembeds.ImageEntity
import com.tunjid.heron.data.database.entities.postembeds.PostEmbed
import com.tunjid.heron.data.database.entities.postembeds.VideoEntity
import kotlinx.datetime.Clock

internal fun PostView.quotedPostEntity(): PostEntity? =
    when (val embed = embed) {
        is PostViewEmbedUnion.ExternalView -> null
        is PostViewEmbedUnion.ImagesView -> null
        is PostViewEmbedUnion.RecordWithMediaView -> embed.value.record.record.postEntity()
        is PostViewEmbedUnion.Unknown -> null
        is PostViewEmbedUnion.VideoView -> null
        is PostViewEmbedUnion.RecordView -> embed.value.record.postEntity()

        null -> null
    }

internal fun PostView.quotedPostProfileEntity(): ProfileEntity? =
    when (val embed = embed) {
        is PostViewEmbedUnion.ExternalView -> null
        is PostViewEmbedUnion.ImagesView -> null
        is PostViewEmbedUnion.RecordWithMediaView -> embed.value.record.record.profileEntity()

        is PostViewEmbedUnion.Unknown -> null
        is PostViewEmbedUnion.VideoView -> null
        is PostViewEmbedUnion.RecordView -> embed.value.record.profileEntity()

        null -> null
    }

private fun RecordViewRecordUnion.profileEntity() =
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

        is RecordViewRecordUnion.ViewRecord -> ProfileEntity(
            did = ProfileId(value.author.did.did),
            handle = ProfileHandle(value.author.handle.handle),
            displayName = value.author.displayName,
            description = null,
            avatar = value.author.avatar?.uri?.let(::ImageUri),
            banner = null,
            followersCount = null,
            followsCount = null,
            postsCount = null,
            joinedViaStarterPack = null,
            indexedAt = null,
            createdAt = value.author.createdAt,
            associated = ProfileEntity.Associated(
                createdListCount = value.author.associated?.lists,
                createdFeedGeneratorCount = value.author.associated?.feedgens,
                createdStarterPackCount = value.author.associated?.starterPacks,
                labeler = value.author.associated?.labeler,
                allowDms = value.author.associated?.chat?.allowIncoming?.value,
            ),
        )
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
        is PostViewEmbedUnion.RecordView ->
            embed
                .value
                .record
                .embedEntities()

        null -> emptyList()
    }

private fun RecordViewRecordUnion.embedEntities() =
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
            value.embeds.map<RecordViewRecordEmbedUnion, List<PostEmbed>> { innerRecord ->
                when (innerRecord) {
                    is RecordViewRecordEmbedUnion.ExternalView -> listOf(
                        ExternalEmbedEntity(
                            uri = GenericUri(innerRecord.value.external.uri.uri),
                            title = innerRecord.value.external.title,
                            description = innerRecord.value.external.description,
                            thumb = innerRecord.value.external.thumb?.uri?.let(::ImageUri),
                        ),
                    )

                    is RecordViewRecordEmbedUnion.ImagesView -> innerRecord.value.images.map {
                        ImageEntity(
                            fullSize = ImageUri(it.fullsize.uri),
                            thumb = ImageUri(it.thumb.uri),
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
                            cid = GenericId(innerRecord.value.cid.cid),
                            playlist = GenericUri(innerRecord.value.playlist.uri),
                            thumbnail = innerRecord.value.thumbnail?.uri?.let(::ImageUri),
                            alt = innerRecord.value.alt,
                            width = innerRecord.value.aspectRatio?.width,
                            height = innerRecord.value.aspectRatio?.height,
                        ),
                    )
                }
            }
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
                bookmarkCount = value.bookmarkCount,
                indexedAt = value.indexedAt,
                record = value.value.asPostEntityRecordData(),
            )
    }

fun PostView.getEmbeddedFeedGenerator(): GeneratorView? = when (val embed = embed) {
    is PostViewEmbedUnion.RecordView -> when (val recordUnion = embed.value.record) {
        is RecordViewRecordUnion.FeedGeneratorView -> recordUnion.value
        else -> null
    }
    is PostViewEmbedUnion.RecordWithMediaView -> when (val recordUnion = embed.value.record.record) {
        is RecordViewRecordUnion.FeedGeneratorView -> recordUnion.value
        else -> null
    }
    else -> null
}

fun PostView.getEmbeddedList(): ListView? = when (val embed = embed) {
    is PostViewEmbedUnion.RecordView -> when (val recordUnion = embed.value.record) {
        is RecordViewRecordUnion.GraphListView -> recordUnion.value
        else -> null
    }
    is PostViewEmbedUnion.RecordWithMediaView -> when (val recordUnion = embed.value.record.record) {
        is RecordViewRecordUnion.GraphListView -> recordUnion.value
        else -> null
    }
    else -> null
}

fun PostView.getEmbeddedStarterPack(): StarterPackViewBasic? = when (val embed = embed) {
    is PostViewEmbedUnion.RecordView -> when (val recordUnion = embed.value.record) {
        is RecordViewRecordUnion.GraphStarterPackViewBasic -> recordUnion.value
        else -> null
    }
    is PostViewEmbedUnion.RecordWithMediaView -> when (val recordUnion = embed.value.record.record) {
        is RecordViewRecordUnion.GraphStarterPackViewBasic -> recordUnion.value
        else -> null
    }
    else -> null
}

internal fun GeneratorView.feedGeneratorEntity() = FeedGeneratorEntity(
    cid = FeedGeneratorId(cid.cid),
    did = FeedGeneratorId(did.did),
    uri = FeedGeneratorUri(uri.atUri),
    avatar = avatar?.uri?.let(::ImageUri),
    likeCount = likeCount,
    creatorId = ProfileId(creator.did.did),
    displayName = displayName,
    description = description,
    acceptsInteractions = acceptsInteractions,
    contentMode = contentMode,
    indexedAt = indexedAt,
    createdAt = Clock.System.now(),
)

internal fun ListView.listEntity() = ListEntity(
    cid = ListId(cid.cid),
    uri = ListUri(uri.atUri),
    creatorId = ProfileId(creator.did.did),
    name = name,
    description = description,
    avatar = avatar?.uri?.let(::ImageUri),
    listItemCount = listItemCount,
    purpose = purpose.toString(),
    indexedAt = indexedAt,
    createdAt = Clock.System.now(),
)

internal fun StarterPackViewBasic.starterPackEntity() = StarterPackEntity(
    cid = StarterPackId(cid.cid),
    uri = StarterPackUri(uri.atUri),
    creatorId = ProfileId(creator.did.did),
    listUri = null, // You might need to handle this if available
    name = "", // You might need to handle this if available
    description = "", // You might need to handle this if available
    joinedWeekCount = joinedWeekCount,
    joinedAllTimeCount = joinedAllTimeCount,
    indexedAt = indexedAt,
    createdAt = Clock.System.now(),
)

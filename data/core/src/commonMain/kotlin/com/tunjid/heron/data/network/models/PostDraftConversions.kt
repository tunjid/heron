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

import app.bsky.draft.Draft as BskyDraft
import app.bsky.draft.DraftEmbedExternal
import app.bsky.draft.DraftEmbedImage
import app.bsky.draft.DraftEmbedLocalRef
import app.bsky.draft.DraftEmbedRecord
import app.bsky.draft.DraftEmbedVideo
import app.bsky.draft.DraftPost
import app.bsky.draft.DraftThreadgateAllowUnion
import app.bsky.draft.DraftView
import app.bsky.feed.ThreadgateFollowerRule
import app.bsky.feed.ThreadgateFollowingRule
import app.bsky.feed.ThreadgateListRule
import app.bsky.feed.ThreadgateMentionRule
import com.atproto.repo.StrongRef
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.ThreadGate
import com.tunjid.heron.data.core.models.fromBase64EncodedUrl
import com.tunjid.heron.data.core.models.toUrlEncodedBase64
import com.tunjid.heron.data.core.types.DraftId
import com.tunjid.heron.data.core.types.FileUri
import com.tunjid.heron.data.core.types.GenericId
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.asRecordUriOrNull
import com.tunjid.heron.data.core.utilities.File
import com.tunjid.heron.data.database.entities.PostDraftEntity
import kotlin.time.Instant
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Cid
import sh.christian.ozone.api.Language
import sh.christian.ozone.api.Uri as BskyUri

// region domain -> network

internal fun Post.Draft.toNetworkDraft(): BskyDraft = BskyDraft(
    posts = posts.map(Post.Create.Request::toDraftPost),
    langs = langs.map(::Language).takeIf(List<Language>::isNotEmpty),
    // A draft has a single, thread level threadgate. Use the root post's interaction settings.
    threadgateAllow = posts.firstOrNull()
        ?.metadata
        ?.allowed
        ?.toDraftThreadgateAllow(),
)

private fun Post.Create.Request.toDraftPost(): DraftPost = DraftPost(
    text = text,
    embedImages = metadata.embeddedMedia
        .filterIsInstance<File.Media.Photo>()
        .map { photo ->
            DraftEmbedImage(
                localRef = DraftEmbedLocalRef(path = photo.uri.uri),
                alt = photo.altText,
            )
        }
        .takeIf(List<DraftEmbedImage>::isNotEmpty),
    embedVideos = metadata.embeddedMedia
        .filterIsInstance<File.Media.Video>()
        .map { video ->
            DraftEmbedVideo(
                localRef = DraftEmbedLocalRef(path = video.uri.uri),
                alt = video.altText,
            )
        }
        .takeIf(List<DraftEmbedVideo>::isNotEmpty),
    // StrongRef requires a cid, so only include a quoted record when the reference carries one.
    embedRecords = metadata.embeddedRecordReference
        ?.let { reference ->
            val cid = reference.id?.id ?: return@let null
            listOf(
                DraftEmbedRecord(
                    record = StrongRef(
                        uri = reference.uri.uri.let(::AtUri),
                        cid = cid.let(::Cid),
                    ),
                ),
            )
        },
    embedExternals = metadata.linkPreview
        ?.let { linkPreview ->
            listOf(
                DraftEmbedExternal(uri = linkPreview.embed.uri.uri.let(::BskyUri)),
            )
        },
)

private fun ThreadGate.Allowed.toDraftThreadgateAllow(): List<DraftThreadgateAllowUnion>? = buildList {
    if (allowsFollowers) add(
        DraftThreadgateAllowUnion.FollowerRule(ThreadgateFollowerRule),
    )
    if (allowsFollowing) add(
        DraftThreadgateAllowUnion.FollowingRule(ThreadgateFollowingRule),
    )
    if (allowsMentioned) add(
        DraftThreadgateAllowUnion.MentionRule(ThreadgateMentionRule),
    )
    allowedListUris.forEach { listUri ->
        add(
            DraftThreadgateAllowUnion.ListRule(
                ThreadgateListRule(listUri.uri.let(::AtUri)),
            ),
        )
    }
}.takeIf(List<DraftThreadgateAllowUnion>::isNotEmpty)

// endregion

// region network -> domain

internal fun DraftView.asExternalModel(
    authorId: ProfileId,
): Post.Draft = Post.Draft(
    id = id.tid.let(::DraftId),
    authorId = authorId,
    posts = draft.asRequests(authorId),
    langs = draft.langs.orEmpty().map(Language::tag),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun BskyDraft.asRequests(
    authorId: ProfileId,
): List<Post.Create.Request> = posts.map { it.toRequest(authorId) }

private fun DraftPost.toRequest(
    authorId: ProfileId,
): Post.Create.Request = Post.Create.Request(
    authorId = authorId,
    text = text,
    // Facets are not stored on a draft; they are re-resolved from the text at publish time.
    links = emptyList(),
    metadata = Post.Create.Metadata(
        // Media dimensions are not stored on a draft (only the local path + alt), so they must be
        // re-probed from the file before the composer uses them.
        embeddedMedia = buildList {
            embedImages?.forEach { image ->
                add(
                    File.Media.Photo(
                        uri = FileUri(image.localRef.path),
                        width = 0,
                        height = 0,
                        altText = image.alt,
                    ),
                )
            }
            embedVideos?.forEach { video ->
                add(
                    File.Media.Video(
                        uri = FileUri(video.localRef.path),
                        width = 0,
                        height = 0,
                        altText = video.alt,
                    ),
                )
            }
        },
        embeddedRecordReference = embedRecords
            ?.firstOrNull()
            ?.record
            ?.let { strongRef ->
                strongRef.uri.atUri.asRecordUriOrNull()?.let { recordUri ->
                    Record.Reference(
                        id = strongRef.cid.cid.let(::GenericId),
                        uri = recordUri,
                    )
                }
            },
        // Only the external URI is stored on a draft; the full link card must be re-fetched on load.
        linkPreview = null,
    ),
)

// endregion

// region entity <-> domain

internal fun DraftView.asEntity(
    authorId: ProfileId,
): PostDraftEntity = PostDraftEntity(
    id = id.tid.let(::DraftId),
    authorId = authorId,
    content = asExternalModel(authorId).toUrlEncodedBase64(),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun Post.Draft.asEntity(
    fallback: Instant,
): PostDraftEntity = PostDraftEntity(
    id = requireNotNull(id) { "Cannot cache a draft without an id" },
    authorId = authorId,
    content = toUrlEncodedBase64(),
    createdAt = createdAt ?: fallback,
    updatedAt = updatedAt ?: fallback,
)

internal fun PostDraftEntity.asExternalModel(): Post.Draft =
    content.fromBase64EncodedUrl<Post.Draft>().copy(
        id = id,
        authorId = authorId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

// endregion

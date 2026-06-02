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

package com.tunjid.heron.data.utilities.multipleEntitysaver

import app.bsky.actor.ProfileViewBasic
import app.bsky.embed.ExternalColorRGB
import app.bsky.embed.ExternalViewExternal
import app.bsky.embed.ExternalViewExternalSource
import app.bsky.embed.ExternalViewExternalSourceTheme
import com.tunjid.heron.data.core.types.EmbeddableRecordUri
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.StandardDocumentId
import com.tunjid.heron.data.core.types.StandardDocumentUri
import com.tunjid.heron.data.core.types.StandardPublicationId
import com.tunjid.heron.data.core.types.StandardPublicationUri
import com.tunjid.heron.data.core.types.asEmbeddableRecordUriOrNull
import com.tunjid.heron.data.core.types.profileId
import com.tunjid.heron.data.core.types.recordKey
import com.tunjid.heron.data.database.entities.StandardDocumentEntity
import com.tunjid.heron.data.database.entities.StandardPublicationEntity
import com.tunjid.heron.data.database.entities.postembeds.PostExternalAssociatedProfilesEntity
import com.tunjid.heron.data.database.entities.postembeds.PostExternalAssociatedRecordEntity
import com.tunjid.heron.data.network.models.profileEntity
import com.tunjid.heron.data.network.models.stubProfileEntity
import com.tunjid.heron.data.utilities.Collections
import com.tunjid.heron.data.utilities.tidInstant
import kotlin.time.Instant

/**
 * Persists the `site.standard.*` records that back a post's `app.bsky.embed.external`
 * (via `associatedRefs` + the hydrated `viewExternalSource`) so they can be resolved into the
 * post's `embeddedRecords` offline-first, without a second network call.
 *
 * The rows are written as STUBS ([StandardDocumentEntity.cid] / [StandardPublicationEntity.cid] are
 * `null`) so [MultipleEntitySaver.flushPendingOperations] routes them through `insertOrIgnore` and
 * they never clobber a fully-resolved record. The strong-ref cid is preserved on the
 * [PostExternalAssociatedRecordEntity] linkage instead.
 */
internal fun MultipleEntitySaver.addExternalAssociatedRecords(
    externalView: ExternalViewExternal,
    postUri: PostUri,
) {
    val refs = externalView.associatedRefs?.takeIf(List<*>::isNotEmpty) ?: return
    val externalEmbedUri = GenericUri(externalView.uri.uri)
    val associatedProfiles = externalView.associatedProfiles
        .orEmpty()
        .associateBy { it.did.did }

    var documentUri: StandardDocumentUri? = null
    var documentCid: StandardDocumentId? = null
    var publicationUri: StandardPublicationUri? = null
    var publicationCid: StandardPublicationId? = null

    refs.forEach { ref ->
        when (val recordUri = ref.uri.atUri.asEmbeddableRecordUriOrNull()) {
            is StandardDocumentUri -> {
                documentUri = recordUri
                documentCid = StandardDocumentId(ref.cid.cid)
            }
            is StandardPublicationUri -> {
                publicationUri = recordUri
                publicationCid = StandardPublicationId(ref.cid.cid)
            }
            else -> Unit
        }
    }

    val source = externalView.source

    publicationUri?.let { pubUri ->
        addAssociatedProfile(
            profileId = pubUri.profileId(),
            associatedProfiles = associatedProfiles,
        )
        add(
            stubStandardPublicationEntity(
                uri = pubUri,
                source = source,
            ),
        )
        add(
            associatedRecord(
                postUri = postUri,
                externalEmbedUri = externalEmbedUri,
                recordUri = pubUri,
                recordCid = publicationCid,
            ),
        )
    }

    documentUri?.let { docUri ->
        addAssociatedProfile(
            profileId = docUri.profileId(),
            associatedProfiles = associatedProfiles,
        )
        add(
            stubStandardDocumentEntity(
                uri = docUri,
                view = externalView,
                publicationUri = publicationUri,
            ),
        )
        add(
            associatedRecord(
                postUri = postUri,
                externalEmbedUri = externalEmbedUri,
                recordUri = docUri,
                recordCid = documentCid,
            ),
        )
    }

    // The owners of the backing records (viewExternal.associatedProfiles).
    externalView.associatedProfiles?.forEachIndexed { index, profile ->
        add(profile.profileEntity())
        add(
            PostExternalAssociatedProfilesEntity(
                postUri = postUri,
                externalEmbedUri = externalEmbedUri,
                profileId = ProfileId(profile.did.did),
                ordinal = index,
            ),
        )
    }
}

private fun MultipleEntitySaver.addAssociatedProfile(
    profileId: ProfileId,
    associatedProfiles: Map<String, ProfileViewBasic>,
) {
    add(
        associatedProfiles[profileId.id]?.profileEntity()
            ?: stubProfileEntity(profileId),
    )
}

private fun associatedRecord(
    postUri: PostUri,
    externalEmbedUri: GenericUri,
    recordUri: EmbeddableRecordUri,
    recordCid: Id?,
) = PostExternalAssociatedRecordEntity(
    postUri = postUri,
    externalEmbedUri = externalEmbedUri,
    recordUri = recordUri,
    recordCid = recordCid,
)

private fun stubStandardPublicationEntity(
    uri: StandardPublicationUri,
    source: ExternalViewExternalSource?,
) = StandardPublicationEntity(
    uri = uri,
    // Null cid forces insertOrIgnore so a stub never clobbers a resolved publication.
    cid = null,
    publisherId = uri.profileId(),
    name = source?.title.orEmpty(),
    description = source?.description,
    url = source?.uri?.uri ?: Collections.PLACEHOLDER_URL,
    icon = source?.icon?.uri?.let(::ImageUri),
    preferences = null,
    basicTheme = source?.theme?.toBasicTheme(),
    sortedAt = uri.recordKey.tidInstant ?: Instant.DISTANT_PAST,
)

private fun stubStandardDocumentEntity(
    uri: StandardDocumentUri,
    view: ExternalViewExternal,
    publicationUri: StandardPublicationUri?,
) = StandardDocumentEntity(
    uri = uri,
    // Null cid forces insertOrIgnore so a stub never clobbers a resolved document.
    cid = null,
    authorId = uri.profileId(),
    title = view.title,
    description = view.description,
    textContent = null,
    path = null,
    site = view.source?.uri?.uri ?: view.uri.uri,
    publishedAt = view.createdAt ?: uri.recordKey.tidInstant ?: Instant.DISTANT_PAST,
    updatedAt = view.updatedAt,
    coverImage = view.thumb?.uri?.let(::ImageUri),
    bskyPostRefUri = null,
    bskyPostRefCid = null,
    tags = null,
    publicationUri = publicationUri,
    markdownContent = null,
)

private fun ExternalViewExternalSourceTheme.toBasicTheme() = StandardPublicationEntity.BasicTheme(
    accent = accentRGB?.toColor(),
    accentForeground = accentForegroundRGB?.toColor(),
    background = backgroundRGB?.toColor(),
    foreground = foregroundRGB?.toColor(),
)

private fun ExternalColorRGB.toColor() = StandardPublicationEntity.Color(
    r = r.toInt(),
    g = g.toInt(),
    b = b.toInt(),
    a = 100,
)

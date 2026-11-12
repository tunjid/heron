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

import app.bsky.feed.Post as BskyPost
import app.bsky.feed.PostView
import app.bsky.richtext.FacetFeatureUnion
import com.tunjid.heron.data.core.types.EmbeddableRecordUri
import com.tunjid.heron.data.core.types.EventUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.core.types.profileId
import com.tunjid.heron.data.core.types.recordKey
import com.tunjid.heron.data.core.types.recordUriOrNull
import com.tunjid.heron.data.core.types.requireCollection
import com.tunjid.heron.data.database.entities.PopulatedPostEntity
import com.tunjid.heron.data.database.entities.postembeds.ExternalEmbedEntity
import com.tunjid.heron.data.utilities.safeDecodeAs

internal fun String.externalRecordUriOrNull(): EmbeddableRecordUri? {
    val path = removePrefix(Uri.Host.Https.prefix)
        .removePrefix("www.")
        .substringBefore('?')
        .substringBefore('#')
    return when {
        path.startsWith(AtmoRsvpProfilePrefix) -> path.atmoRsvpEventUriOrNull()
        else -> null
    }
}

internal fun PostView.externalRecordUris(): List<EmbeddableRecordUri> =
    listOfNotNull(
        externalAssociatedView()
            ?.uri
            ?.uri
            ?.externalRecordUriOrNull(),
    ).mapNotNull { uri ->
        val profileId = profileIdFromPostOrNull(
            actor = uri.profileId().id,
        ) ?: return@mapNotNull null
        recordUriOrNull(
            profileId = profileId,
            namespace = uri.requireCollection(),
            recordKey = uri.recordKey,
        ) as? EmbeddableRecordUri
    }

internal fun PopulatedPostEntity.unresolvedExternalRecordLinks(): List<Pair<ExternalEmbedEntity, EmbeddableRecordUri>> {
    if (externalEmbeds.isEmpty()) return emptyList()
    return externalEmbeds.mapNotNull { embed ->
        val recordUri = embed.uri.uri.externalRecordUriOrNull() ?: return@mapNotNull null
        val isResolved = associatedExternalRecords.any { it.recordUri.isSameRecordAs(recordUri) }
        if (isResolved) null else embed to recordUri
    }
}

internal fun EmbeddableRecordUri.isSameRecordAs(
    other: EmbeddableRecordUri,
) = requireCollection() == other.requireCollection() && recordKey == other.recordKey

private fun String.atmoRsvpEventUriOrNull(): EventUri? {
    val segments = removePrefix(AtmoRsvpProfilePrefix).split('/')
    if (segments.size !in 3..4 || segments[1] != "e") return null
    if (segments.size == 4 && segments[3].isNotEmpty()) return null

    val actor = segments[0].takeIf(String::isNotEmpty) ?: return null
    val recordKey = segments[2].takeIf(String::isNotEmpty) ?: return null

    return EventUri("${Uri.Host.AtProto.prefix}$actor/${EventUri.NAMESPACE}/$recordKey")
}

private fun PostView.profileIdFromPostOrNull(
    actor: String,
): ProfileId? {
    if (
        actor.matchesProfile(
            did = author.did.did,
            handle = author.handle.handle,
        )
    ) return ProfileId(author.did.did)

    quotedPostProfileView()?.let { quotedAuthor ->
        if (
            actor.matchesProfile(
                did = quotedAuthor.did.did,
                handle = quotedAuthor.handle.handle,
            )
        ) return ProfileId(quotedAuthor.did.did)
    }
    val bskyPost = record.safeDecodeAs<BskyPost>() ?: return null
    val textBytes = bskyPost.text.encodeToByteArray()
    return bskyPost.facets
        ?.firstNotNullOfOrNull { facet ->
            val mention = facet.features
                .firstNotNullOfOrNull { it as? FacetFeatureUnion.Mention }
                ?: return@firstNotNullOfOrNull null
            val start = facet.index.byteStart.toInt()
            val end = facet.index.byteEnd.toInt()
            if (start !in 0..end || end > textBytes.size) return@firstNotNullOfOrNull null
            val handle = textBytes.decodeToString(
                startIndex = start,
                endIndex = end,
            ).removePrefix("@")
            mention.value.did.did.takeIf { did ->
                actor.matchesProfile(did = did, handle = handle)
            }
        }
        ?.let(::ProfileId)
}

private fun String.matchesProfile(
    did: String,
    handle: String,
) = this == did || equals(handle, ignoreCase = true)

private const val AtmoRsvpProfilePrefix = "atmo.rsvp/p/"

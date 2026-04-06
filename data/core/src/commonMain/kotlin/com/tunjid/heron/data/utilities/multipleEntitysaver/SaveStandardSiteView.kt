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

import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.StandardDocument
import com.tunjid.heron.data.core.models.StandardPublication
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.StandardDocumentId
import com.tunjid.heron.data.core.types.StandardDocumentUri
import com.tunjid.heron.data.core.types.StandardPublicationId
import com.tunjid.heron.data.core.types.StandardPublicationUri
import com.tunjid.heron.data.core.types.StandardSubscriptionId
import com.tunjid.heron.data.core.types.StandardSubscriptionUri
import com.tunjid.heron.data.core.types.profileId
import com.tunjid.heron.data.core.types.recordKey
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.StandardDocumentEntity
import com.tunjid.heron.data.database.entities.StandardPublicationEntity
import com.tunjid.heron.data.database.entities.StandardSubscriptionEntity
import com.tunjid.heron.data.utilities.Collections
import com.tunjid.heron.data.utilities.safeDecodeAs
import com.tunjid.heron.data.utilities.tidInstant
import kotlin.time.Instant
import sh.christian.ozone.api.model.Blob
import site.standard.Document
import site.standard.Publication
import site.standard.graph.Subscription
import site.standard.heron.DocumentView
import site.standard.heron.ProfileView
import site.standard.heron.PublicationView
import site.standard.theme.BasicAccentForegroundUnion
import site.standard.theme.BasicAccentUnion
import site.standard.theme.BasicBackgroundUnion
import site.standard.theme.BasicForegroundUnion
import site.standard.theme.ColorRgb
import site.standard.theme.ColorRgba

internal fun MultipleEntitySaver.add(
    documentView: DocumentView,
    viewingProfileId: ProfileId?,
) {
    val document = documentView.record.safeDecodeAs<Document>() ?: return

    add(documentView.author)
    documentView.publication?.let {
        add(
            publicationView = it,
            viewingProfileId = viewingProfileId,
        )
    }
    add(
        StandardDocumentEntity(
            uri = documentView.uri.atUri.let(::StandardDocumentUri),
            cid = documentView.cid.cid.let(::StandardDocumentId),
            authorId = documentView.author.did.did.let(::ProfileId),
            title = document.title,
            description = document.description,
            textContent = document.textContent,
            path = document.path,
            site = document.site.uri,
            publishedAt = document.publishedAt,
            updatedAt = document.updatedAt,
            coverImage = documentView.coverImageUrl?.uri?.let(::ImageUri),
            bskyPostRefUri = document.bskyPostRef?.uri?.atUri?.let(::PostUri),
            bskyPostRefCid = document.bskyPostRef?.cid?.cid?.let(::PostId),
            tags = document.tags?.joinToString(separator = ","),
            publicationUri = documentView.publication?.uri?.atUri?.let(::StandardPublicationUri),
            // Markdown content is always null for now.
            // To be added in later as support for different standard doc
            // types are added.
            markdownContent = null,
        ),
    )
}

internal fun MultipleEntitySaver.add(
    publicationView: PublicationView,
    viewingProfileId: ProfileId?,
) {
    val publication = publicationView.record.safeDecodeAs<Publication>() ?: return
    val publicationUri = publicationView.uri.atUri.let(::StandardPublicationUri)

    viewingProfileId?.let { profileId ->
        publicationView.viewerSubscription?.let {
            add(
                StandardSubscriptionEntity(
                    uri = it.uri.atUri.let(::StandardSubscriptionUri),
                    cid = it.cid.cid.let(::StandardSubscriptionId),
                    sortedAt = it.sortedAt,
                    publicationUri = publicationUri,
                    viewingProfileId = profileId,
                ),
            )
        } ?: remove(
            StandardSubscriptionEntity.Deletion(
                publicationUri = publicationUri,
                viewingProfileId = profileId,
            ),
        )
    }

    add(publicationView.author)
    add(
        StandardPublicationEntity(
            uri = publicationUri,
            cid = publicationView.cid.cid.let(::StandardPublicationId),
            publisherId = publicationView.author.did.did.let(::ProfileId),
            name = publication.name,
            description = publication.description,
            url = publication.url.uri,
            icon = publicationView.iconUrl?.uri?.let(::ImageUri),
            sortedAt = publicationView.sortedAt,
            preferences = publication.preferences?.let { prefs ->
                StandardPublicationEntity.Preferences(
                    showInDiscover = prefs.showInDiscover == true,
                )
            },
            basicTheme = publication.basicTheme?.let { theme ->
                StandardPublicationEntity.BasicTheme(
                    accent = theme.accent.toColor(),
                    accentForeground = theme.accentForeground.toColor(),
                    background = theme.background.toColor(),
                    foreground = theme.foreground.toColor(),
                )
            },
        ),
    )
}

private fun MultipleEntitySaver.add(
    author: ProfileView,
) {
    add(
        ProfileEntity(
            did = ProfileId(author.did.did),
            handle = ProfileHandle(author.handle.handle),
            displayName = author.displayName,
            description = null,
            avatar = author.avatar?.uri?.let(::ImageUri),
            banner = null,
            followersCount = null,
            followsCount = null,
            postsCount = null,
            joinedViaStarterPack = null,
            indexedAt = null,
            createdAt = null,
            associated = ProfileEntity.Associated(
                createdListCount = null,
                createdFeedGeneratorCount = null,
                createdStarterPackCount = null,
                labeler = null,
                allowDms = null,
            ),
            status = null,
        ),
    )
}

internal fun MultipleEntitySaver.add(
    subscriptionUri: StandardSubscriptionUri,
    subscriptionCid: StandardSubscriptionId?,
    subscription: Subscription,
    sortedAt: Instant,
    viewingProfileId: ProfileId,
) {
    val publicationUri = StandardPublicationUri(subscription.publication.atUri)
    add(
        stubPublicationEntity(
            publicationUri = publicationUri,
        ),
    )
    add(
        StandardSubscriptionEntity(
            uri = subscriptionUri,
            cid = subscriptionCid,
            sortedAt = sortedAt,
            publicationUri = publicationUri,
            viewingProfileId = viewingProfileId,
        ),
    )
}

private fun Blob?.imageUri(
    profileId: ProfileId,
    pdsUrl: String?,
): ImageUri? {
    if (pdsUrl == null) return null
    return when (val icon = this) {
        is Blob.LegacyBlob -> null
        is Blob.StandardBlob -> ImageUri(
            "$pdsUrl/xrpc/com.atproto.sync.getBlob?did=${profileId.id}&cid=${icon.ref.link.cid}",
        )
        null -> null
    }
}

private fun stubPublicationEntity(
    publicationUri: StandardPublicationUri,
): StandardPublicationEntity =
    StandardPublicationEntity(
        uri = publicationUri,
        cid = null,
        sortedAt = publicationUri.recordKey.tidInstant ?: Instant.DISTANT_PAST,
        publisherId = publicationUri.profileId(),
        name = "",
        description = null,
        url = Collections.PLACEHOLDER_URL,
        icon = null,
        preferences = null,
        basicTheme = null,
    )

private fun BasicAccentUnion.toColor(): StandardPublicationEntity.Color? = when (this) {
    is BasicAccentUnion.Rgb -> value.toColor()
    is BasicAccentUnion.Rgba -> value.toColor()
    is BasicAccentUnion.Unknown -> null
}

private fun BasicAccentForegroundUnion.toColor(): StandardPublicationEntity.Color? = when (this) {
    is BasicAccentForegroundUnion.Rgb -> value.toColor()
    is BasicAccentForegroundUnion.Rgba -> value.toColor()
    is BasicAccentForegroundUnion.Unknown -> null
}

private fun BasicBackgroundUnion.toColor(): StandardPublicationEntity.Color? = when (this) {
    is BasicBackgroundUnion.Rgb -> value.toColor()
    is BasicBackgroundUnion.Rgba -> value.toColor()
    is BasicBackgroundUnion.Unknown -> null
}

private fun BasicForegroundUnion.toColor(): StandardPublicationEntity.Color? = when (this) {
    is BasicForegroundUnion.Rgb -> value.toColor()
    is BasicForegroundUnion.Rgba -> value.toColor()
    is BasicForegroundUnion.Unknown -> null
}

private fun ColorRgb.toColor() = StandardPublicationEntity.Color(
    r = r.toInt(),
    g = g.toInt(),
    b = b.toInt(),
    a = 100,
)

private fun ColorRgba.toColor() = StandardPublicationEntity.Color(
    r = r.toInt(),
    g = g.toInt(),
    b = b.toInt(),
    a = a.toInt(),
)

internal fun Document.asExternalModel(
    uri: StandardDocumentUri,
    cid: StandardDocumentId?,
    publication: StandardPublication?,
    pdsUrl: String,
) = StandardDocument(
    uri = uri,
    cid = cid,
    authorId = uri.profileId(),
    title = title,
    description = description,
    textContent = textContent,
    path = path,
    site = site.uri,
    publishedAt = publishedAt,
    updatedAt = updatedAt,
    coverImage = coverImage?.imageUri(
        profileId = uri.profileId(),
        pdsUrl = pdsUrl,
    ),
    bskyPostRef = bskyPostRef?.let { ref ->
        Record.Reference(
            id = ref.cid.cid.let(::PostId),
            uri = PostUri(ref.uri.atUri),
        )
    },
    tags = tags ?: emptyList(),
    publication = publication,
)

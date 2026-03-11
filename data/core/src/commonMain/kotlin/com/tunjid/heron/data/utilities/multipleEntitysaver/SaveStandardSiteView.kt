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
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.StandardDocumentId
import com.tunjid.heron.data.core.types.StandardDocumentUri
import com.tunjid.heron.data.core.types.StandardPublicationId
import com.tunjid.heron.data.core.types.StandardPublicationUri
import com.tunjid.heron.data.core.types.StandardSubscriptionUri
import com.tunjid.heron.data.core.types.asRecordUriOrNull
import com.tunjid.heron.data.core.types.profileId
import com.tunjid.heron.data.database.entities.StandardDocumentEntity
import com.tunjid.heron.data.database.entities.StandardPublicationEntity
import com.tunjid.heron.data.database.entities.StandardSubscriptionEntity
import com.tunjid.heron.data.utilities.Collections
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.model.Blob
import site.standard.Document
import site.standard.Publication
import site.standard.graph.Subscription
import site.standard.theme.BasicAccentForegroundUnion
import site.standard.theme.BasicAccentUnion
import site.standard.theme.BasicBackgroundUnion
import site.standard.theme.BasicForegroundUnion
import site.standard.theme.ColorRgb
import site.standard.theme.ColorRgba

internal fun MultipleEntitySaver.add(
    publicationUri: StandardPublicationUri,
    publicationCid: StandardPublicationId?,
    publication: Publication,
    pdsUrl: String,
) {
    val publisherId = publicationUri.profileId()
    add(
        stubProfileEntity(did = Did(publisherId.id)),
    )
    add(
        StandardPublicationEntity(
            uri = publicationUri,
            cid = publicationCid,
            publisherId = publisherId,
            name = publication.name,
            description = publication.description,
            url = publication.url.uri,
            icon = publication.icon.imageUri(
                profileId = publisherId,
                pdsUrl = pdsUrl,
            ),
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

internal fun MultipleEntitySaver.add(
    documentUri: StandardDocumentUri,
    documentCid: StandardDocumentId?,
    document: Document,
    pdsUrl: String,
) {
    val authorId = documentUri.profileId()
    val publicationUri = document.site.uri.asRecordUriOrNull() as? StandardPublicationUri
    add(
        stubProfileEntity(did = Did(authorId.id)),
    )

    if (publicationUri != null) add(
        stubPublicationEntity(publicationUri),
    )
    add(
        StandardDocumentEntity(
            uri = documentUri,
            cid = documentCid,
            authorId = authorId,
            title = document.title,
            description = document.description,
            textContent = document.textContent,
            path = document.path,
            site = document.site.uri,
            publishedAt = document.publishedAt,
            updatedAt = document.updatedAt,
            coverImage = document.coverImage?.imageUri(
                profileId = authorId,
                pdsUrl = pdsUrl,
            ),
            bskyPostRefUri = document.bskyPostRef?.uri?.atUri?.let(::PostUri),
            bskyPostRefCid = document.bskyPostRef?.cid?.cid?.let(::PostId),
            tags = document.tags?.joinToString(separator = ","),
            publicationUri = publicationUri,
            // Markdown content is always null for now.
            // To be added in later as support for different standard doc
            // types are added.
            markdownContent = null,
        ),
    )
}

internal fun MultipleEntitySaver.add(
    subscriptionUri: StandardSubscriptionUri,
    subscription: Subscription,
    viewingProfileId: ProfileId,
) {
    add(
        StandardSubscriptionEntity(
            uri = subscriptionUri,
            publicationUri = StandardPublicationUri(subscription.publication.atUri),
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

private fun stubPublicationEntity(publicationUri: StandardPublicationUri): StandardPublicationEntity =
    StandardPublicationEntity(
        uri = publicationUri,
        cid = null,
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

internal fun Publication.asExternalModel(
    uri: StandardPublicationUri,
    cid: StandardPublicationId?,
    pdsUrl: String,
) = StandardPublication(
    uri = uri,
    cid = cid,
    publisherId = uri.profileId(),
    name = name,
    description = description,
    url = url.uri,
    icon = icon?.imageUri(
        profileId = uri.profileId(),
        pdsUrl = pdsUrl,
    ),
    showInDiscover = preferences?.showInDiscover ?: true,
    basicTheme = basicTheme?.let { theme ->
        StandardPublication.BasicTheme(
            accent = theme.accent.toThemeColor() ?: return@let null,
            accentForeground = theme.accentForeground.toThemeColor() ?: return@let null,
            background = theme.background.toThemeColor() ?: return@let null,
            foreground = theme.foreground.toThemeColor() ?: return@let null,
        )
    },
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

private fun BasicAccentUnion.toThemeColor(): StandardPublication.ThemeColor? = when (this) {
    is BasicAccentUnion.Rgb -> value.toThemeColor()
    is BasicAccentUnion.Rgba -> value.toThemeColor()
    is BasicAccentUnion.Unknown -> null
}

private fun BasicAccentForegroundUnion.toThemeColor(): StandardPublication.ThemeColor? =
    when (this) {
        is BasicAccentForegroundUnion.Rgb -> value.toThemeColor()
        is BasicAccentForegroundUnion.Rgba -> value.toThemeColor()
        is BasicAccentForegroundUnion.Unknown -> null
    }

private fun BasicBackgroundUnion.toThemeColor(): StandardPublication.ThemeColor? = when (this) {
    is BasicBackgroundUnion.Rgb -> value.toThemeColor()
    is BasicBackgroundUnion.Rgba -> value.toThemeColor()
    is BasicBackgroundUnion.Unknown -> null
}

private fun BasicForegroundUnion.toThemeColor(): StandardPublication.ThemeColor? = when (this) {
    is BasicForegroundUnion.Rgb -> value.toThemeColor()
    is BasicForegroundUnion.Rgba -> value.toThemeColor()
    is BasicForegroundUnion.Unknown -> null
}

private fun ColorRgb.toThemeColor() = StandardPublication.ThemeColor(
    r = r.toInt(),
    g = g.toInt(),
    b = b.toInt(),
    a = 100,
)

private fun ColorRgba.toThemeColor() = StandardPublication.ThemeColor(
    r = r.toInt(),
    g = g.toInt(),
    b = b.toInt(),
    a = a.toInt(),
)

package com.tunjid.heron.data.network.models

import app.bsky.embed.ExternalColorRGB
import app.bsky.embed.ExternalView
import app.bsky.embed.ExternalViewExternal
import app.bsky.embed.ExternalViewExternalSource
import app.bsky.embed.ExternalViewExternalSourceTheme
import app.bsky.embed.GalleryViewItemUnion
import app.bsky.embed.ImagesViewImage
import app.bsky.embed.RecordWithMediaViewMediaUnion
import app.bsky.embed.VideoView
import app.bsky.feed.PostView
import app.bsky.feed.PostViewEmbedUnion
import com.tunjid.heron.data.core.models.ExternalEmbed
import com.tunjid.heron.data.core.models.LinkPreview
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.StandardDocument
import com.tunjid.heron.data.core.models.StandardPublication
import com.tunjid.heron.data.core.models.stubProfile
import com.tunjid.heron.data.core.types.GenericId
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.StandardDocumentId
import com.tunjid.heron.data.core.types.StandardDocumentUri
import com.tunjid.heron.data.core.types.StandardPublicationId
import com.tunjid.heron.data.core.types.StandardPublicationUri
import com.tunjid.heron.data.core.types.asEmbeddableRecordUriOrNull
import com.tunjid.heron.data.core.types.profileId
import com.tunjid.heron.data.core.types.recordKey
import com.tunjid.heron.data.database.entities.postembeds.ExternalEmbedEntity
import com.tunjid.heron.data.database.entities.postembeds.ImageEntity
import com.tunjid.heron.data.database.entities.postembeds.PostEmbed
import com.tunjid.heron.data.database.entities.postembeds.VideoEntity
import com.tunjid.heron.data.database.entities.postembeds.asExternalModel
import com.tunjid.heron.data.utilities.Collections
import com.tunjid.heron.data.utilities.recordResolver.CardyExtractResponse
import com.tunjid.heron.data.utilities.recordResolver.ExternalStandardRefs
import com.tunjid.heron.data.utilities.tidInstant
import kotlin.time.Instant

/**
 * The hydrated `app.bsky.embed.external#viewExternal` of this post's embed, if any — used to
 * surface `associatedRefs` / `viewExternalSource` for standard-site record stubbing.
 */
internal fun PostView.externalAssociatedView(): ExternalViewExternal? =
    when (val embed = embed) {
        is PostViewEmbedUnion.ExternalView -> embed.value.external
        is PostViewEmbedUnion.RecordWithMediaView -> when (val media = embed.value.media) {
            is RecordWithMediaViewMediaUnion.ExternalView -> media.value.external
            else -> null
        }
        else -> null
    }

internal fun ExternalViewExternal.asExternalEmbedEntity() = ExternalEmbedEntity(
    uri = GenericUri(uri.uri),
    title = title,
    description = description,
    thumb = thumb?.uri?.let(::ImageUri),
    readingTime = readingTime,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun imageEntity(
    index: Int,
    imagesViewImage: ImagesViewImage,
): ImageEntity = ImageEntity(
    fullSize = ImageUri(imagesViewImage.fullsize.uri),
    thumb = ImageUri(imagesViewImage.thumb.uri),
    alt = imagesViewImage.alt,
    width = imagesViewImage.aspectRatio?.width,
    height = imagesViewImage.aspectRatio?.height,
    index = index,
)

internal fun videoEntity(
    index: Int,
    videoView: VideoView,
): VideoEntity =
    VideoEntity(
        cid = GenericId(videoView.cid.cid),
        playlist = GenericUri(videoView.playlist.uri),
        thumbnail = videoView.thumbnail?.uri?.let(::ImageUri),
        alt = videoView.alt,
        width = videoView.aspectRatio?.width,
        height = videoView.aspectRatio?.height,
        index = index,
    )

internal fun postEmbed(
    index: Int,
    galleryViewItemUnion: GalleryViewItemUnion,
): PostEmbed? =
    when (galleryViewItemUnion) {
        // At some point this will contain video.
        // USe the parent type to accommodate this
        is GalleryViewItemUnion.Unknown -> null
        is GalleryViewItemUnion.ViewImage -> ImageEntity(
            fullSize = ImageUri(galleryViewItemUnion.value.fullsize.uri),
            thumb = ImageUri(galleryViewItemUnion.value.thumbnail.uri),
            alt = galleryViewItemUnion.value.alt,
            width = galleryViewItemUnion.value.aspectRatio.width,
            height = galleryViewItemUnion.value.aspectRatio.height,
            index = index,
        )
    }

// region External link preview (cardyb `extract`)

/**
 * Maps a cardyb `extract` response to a [LinkPreview]. Prefers the hydrated [ExternalView] (which may
 * carry standard-site backing records); otherwise falls back to the legacy top-level card fields.
 * Returns `null` when extraction failed or yielded nothing renderable.
 */
internal fun CardyExtractResponse.asLinkPreview(
    requestedUrl: GenericUri,
): LinkPreview? {
    if (!error.isNullOrBlank()) return null
    view?.external?.let { return it.asLinkPreview() }

    val resolvedTitle = title.orEmpty()
    val thumb = image?.takeIf(String::isNotBlank)?.let(::ImageUri)
    if (resolvedTitle.isBlank() && thumb == null) return null

    return LinkPreview(
        embed = ExternalEmbed(
            uri = requestedUrl,
            title = resolvedTitle,
            description = description.orEmpty(),
            thumb = thumb,
        ),
    )
}

/**
 * Resolves [ExternalViewExternal.associatedRefs] into the typed `site.standard.*` document /
 * publication strong refs they point at. Shared by the offline saver
 * ([com.tunjid.heron.data.utilities.multipleEntitysaver.addExternalAssociatedRecords]) and the
 * in-memory link preview mapper.
 */
internal fun ExternalViewExternal.associatedStandardRefs(): ExternalStandardRefs? {
    val refs = associatedRefs?.takeIf(List<*>::isNotEmpty) ?: return null

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

    return ExternalStandardRefs(
        documentUri = documentUri,
        documentCid = documentCid,
        publicationUri = publicationUri,
        publicationCid = publicationCid,
    )
}

/**
 * Maps a hydrated `app.bsky.embed.external#viewExternal` to a [LinkPreview], stubbing the
 * `site.standard.*` records referenced by [ExternalViewExternal.associatedRefs] directly into domain
 * models (no DB round-trip — this is a transient pre-publish preview). Mirrors the entity derivations
 * in `MultipleEntitySaver.addExternalAssociatedRecords`.
 */
internal fun ExternalViewExternal.asLinkPreview(): LinkPreview {
    val embed = asExternalEmbedEntity().asExternalModel()
    val refs = associatedStandardRefs() ?: return LinkPreview(embed = embed)

    val associatedProfilesByDid = associatedProfiles
        .orEmpty()
        .associateBy { it.did.did }

    val publication = refs.publicationUri?.let { pubUri ->
        standardPublication(
            uri = pubUri,
            cid = refs.publicationCid,
            source = source,
            publisher = associatedProfilesByDid[pubUri.profileId().id]?.profile()
                ?: stubProfile(
                    did = pubUri.profileId(),
                    handle = ProfileHandle(pubUri.profileId().id),
                ),
        )
    }

    val document = refs.documentUri?.let { docUri ->
        standardDocument(
            uri = docUri,
            cid = refs.documentCid,
            view = this,
            publication = publication,
        )
    }

    return LinkPreview(
        embed = embed,
        records = listOfNotNull(document, publication),
    )
}

private fun standardPublication(
    uri: StandardPublicationUri,
    cid: StandardPublicationId?,
    source: ExternalViewExternalSource?,
    publisher: Profile,
) = StandardPublication(
    uri = uri,
    cid = cid,
    publisher = publisher,
    name = source?.title.orEmpty(),
    description = source?.description,
    url = source?.uri?.uri ?: Collections.PLACEHOLDER_URL,
    icon = source?.icon?.uri?.let(::ImageUri),
    showInDiscover = true,
    basicTheme = source?.theme?.asBasicTheme(),
    subscription = null,
)

private fun standardDocument(
    uri: StandardDocumentUri,
    cid: StandardDocumentId?,
    view: ExternalViewExternal,
    publication: StandardPublication?,
) = StandardDocument(
    uri = uri,
    cid = cid,
    authorId = uri.profileId(),
    title = view.title,
    description = view.description,
    textContent = null,
    path = null,
    site = view.source?.uri?.uri ?: view.uri.uri,
    publishedAt = view.createdAt ?: uri.recordKey.tidInstant ?: Instant.DISTANT_PAST,
    updatedAt = view.updatedAt,
    coverImage = view.thumb?.uri?.let(::ImageUri),
    bskyPostRef = null,
    tags = emptyList(),
    publication = publication,
)

private fun ExternalViewExternalSourceTheme.asBasicTheme(): StandardPublication.BasicTheme? =
    StandardPublication.BasicTheme(
        accent = accentRGB?.asThemeColor() ?: return null,
        accentForeground = accentForegroundRGB?.asThemeColor() ?: return null,
        background = backgroundRGB?.asThemeColor() ?: return null,
        foreground = foregroundRGB?.asThemeColor() ?: return null,
    )

private fun ExternalColorRGB.asThemeColor() = StandardPublication.ThemeColor(
    r = r.toInt(),
    g = g.toInt(),
    b = b.toInt(),
    a = 100,
)

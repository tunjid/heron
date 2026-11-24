package com.tunjid.heron.data.network.models

import app.bsky.embed.ExternalColorRGB
import app.bsky.embed.ExternalView
import app.bsky.embed.ExternalViewArticle
import app.bsky.embed.ExternalViewArticlePublication
import app.bsky.embed.ExternalViewDataUnion
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
import com.tunjid.heron.data.core.models.StandardRecommendation
import com.tunjid.heron.data.core.models.StandardSubscription
import com.tunjid.heron.data.core.models.stubProfile
import com.tunjid.heron.data.core.types.GenericId
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.StandardDocumentId
import com.tunjid.heron.data.core.types.StandardDocumentUri
import com.tunjid.heron.data.core.types.StandardPublicationId
import com.tunjid.heron.data.core.types.StandardPublicationUri
import com.tunjid.heron.data.core.types.StandardRecommendationUri
import com.tunjid.heron.data.core.types.StandardSubscriptionUri
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
 * The hydrated `app.bsky.embed.external#view` of this post's embed, if any — used to surface
 * `associatedRefs` / `viewExternalSource` and the article [ExternalView.data] for standard-site
 * record stubbing.
 */
internal fun PostView.externalAssociatedView(): ExternalView? =
    when (val embed = embed) {
        is PostViewEmbedUnion.ExternalView -> embed.value
        is PostViewEmbedUnion.RecordWithMediaView -> when (val media = embed.value.media) {
            is RecordWithMediaViewMediaUnion.ExternalView -> media.value
            else -> null
        }
        else -> null
    }

/**
 * The hydrated `app.bsky.embed.external#viewArticle`, when the appview resolved one for this embed.
 */
internal val ExternalView.article: ExternalViewArticle?
    get() = (data as? ExternalViewDataUnion.ViewArticle)?.value

/**
 * The hydrated `app.bsky.embed.external#viewArticlePublication`, either as the publisher of
 * [article] or as the embed's own data when it links to the publication itself.
 */
internal val ExternalView.articlePublication: ExternalViewArticlePublication?
    get() = article?.publisher
        ?: (data as? ExternalViewDataUnion.ViewArticlePublication)?.value

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
    view?.let { return it.asLinkPreview() }

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
 * ([com.tunjid.heron.data.utilities.multipleEntitysaver.addBlueskyIndexedExternalAssociatedRecords]) and the
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

    if (documentUri == null && publicationUri == null) return null

    return ExternalStandardRefs(
        documentUri = documentUri,
        documentCid = documentCid,
        publicationUri = publicationUri,
        publicationCid = publicationCid,
    )
}

/**
 * Maps a hydrated `app.bsky.embed.external#view` to a [LinkPreview], stubbing the
 * `site.standard.*` records referenced by [ExternalViewExternal.associatedRefs] directly into domain
 * models (no DB round-trip — this is a transient pre-publish preview). Mirrors the entity derivations
 * in `MultipleEntitySaver.addBlueskyIndexedExternalAssociatedRecords`.
 *
 * When the appview also hydrates [ExternalView.data] as an article or publication, its
 * recommendation count and viewer state (the viewer's recommendation / subscription) are carried
 * onto the stubbed records.
 */
internal fun ExternalView.asLinkPreview(): LinkPreview {
    val embed = external.asExternalEmbedEntity().asExternalModel()
    val refs = external.associatedStandardRefs() ?: return LinkPreview(embed = embed)

    val associatedProfilesByDid = external.associatedProfiles
        .orEmpty()
        .associateBy { it.did.did }

    val publication = refs.publicationUri?.let { pubUri ->
        standardPublication(
            uri = pubUri,
            cid = refs.publicationCid,
            source = external.source,
            publisher = associatedProfilesByDid[pubUri.profileId().id]?.profile()
                ?: stubProfile(
                    did = pubUri.profileId(),
                    handle = ProfileHandle(pubUri.profileId().id),
                ),
            subscription = articlePublication?.viewer?.subscription?.let { subscriptionUri ->
                StandardSubscription(
                    uri = StandardSubscriptionUri(subscriptionUri.atUri),
                    cid = null,
                    publicationUri = pubUri,
                    sortedAt = subscriptionUri.tidInstant ?: Instant.DISTANT_PAST,
                )
            },
        )
    }

    val document = refs.documentUri?.let { docUri ->
        standardDocument(
            uri = docUri,
            cid = refs.documentCid,
            view = external,
            publication = publication,
            recommendationCount = article?.likeCount ?: 0,
            recommendation = article?.viewer?.like?.let { recommendationUri ->
                StandardRecommendation(
                    uri = StandardRecommendationUri(recommendationUri.atUri),
                    cid = null,
                    documentUri = docUri,
                    sortedAt = recommendationUri.tidInstant ?: Instant.DISTANT_PAST,
                )
            },
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
    subscription: StandardSubscription?,
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
    subscription = subscription,
)

private fun standardDocument(
    uri: StandardDocumentUri,
    cid: StandardDocumentId?,
    view: ExternalViewExternal,
    publication: StandardPublication?,
    recommendationCount: Long,
    recommendation: StandardRecommendation?,
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
    recommendationCount = recommendationCount,
    recommendation = recommendation,
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

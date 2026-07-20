package com.tunjid.heron.data.utilities.recordResolver

import app.bsky.embed.ExternalView
import app.bsky.embed.ExternalViewExternal
import com.tunjid.heron.data.core.models.LinkPreview
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.StandardDocumentId
import com.tunjid.heron.data.core.types.StandardDocumentUri
import com.tunjid.heron.data.core.types.StandardPublicationId
import com.tunjid.heron.data.core.types.StandardPublicationUri
import com.tunjid.heron.data.network.models.asLinkPreview
import com.tunjid.heron.data.utilities.runCatchingUnlessCancelled
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

@Serializable
internal data class CardyExtractResponse(
    val view: ExternalView? = null,
    val error: String? = null,
    val title: String? = null,
    val description: String? = null,
    val image: String? = null,
    val url: String? = null,
)

internal data class ExternalStandardRefs(
    val documentUri: StandardDocumentUri? = null,
    val documentCid: StandardDocumentId? = null,
    val publicationUri: StandardPublicationUri? = null,
    val publicationCid: StandardPublicationId? = null,
)

internal suspend fun HttpClient.linkPreviewOrNull(
    url: GenericUri,
): LinkPreview? = runCatchingUnlessCancelled {
    get(CardyExtractUrl) {
        parameter("url", url.uri)
    }
        .body<CardyExtractResponse>()
        .asLinkPreview(url)
}.getOrNull()

private const val CardyExtractUrl = "https://cardyb.bsky.app/v1/extract"

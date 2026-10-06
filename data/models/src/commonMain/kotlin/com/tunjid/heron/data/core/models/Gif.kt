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

package com.tunjid.heron.data.core.models

import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ImageUri
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import kotlinx.serialization.Serializable

@Serializable
data class Gif(
    val id: String,
    val title: String,
    val description: String,
    val gif: Media,
    val preview: Media,
    val tinyGif: Media?,
    val mp4: Media?,
    val webm: Media?,
) {
    @Serializable
    data class Media(
        val uri: GenericUri,
        val width: Long,
        val height: Long,
    ) {
        val aspectRatio: Float
            get() = if (width > 0 && height > 0) width.toFloat() / height else 1f
    }

    // A GIF embedded in a post as an `app.bsky.embed.external`, recognized the same way Bluesky's
    // `embed-player.ts` does.
    data class Embedded(
        val provider: Provider,
        val playbackUri: ImageUri,
        val width: Long?,
        val height: Long?,
        val mp4: GenericUri?,
        val webm: GenericUri?,
        val altText: String,
        val isUserAltText: Boolean,
    ) {
        enum class Provider {
            Klipy,
            Tenor,
            Giphy,
            Unknown,
        }

        val aspectRatio: Float?
            get() = if (width != null && height != null && width > 0 && height > 0) {
                width.toFloat() / height
            } else {
                null
            }
    }
}

val Gif.Media.displayUri: ImageUri
    get() = ImageUri(gifProxied(uri.uri))

fun ExternalEmbed.asEmbeddedGifOrNull(): Gif.Embedded? {
    val url = try {
        Url(uri.uri)
    } catch (_: Exception) {
        return null
    }
    val segments = url.encodedPath
        .split('/')
        .filter(String::isNotEmpty)

    return url.klipyGif(
        embed = this,
        segments = segments,
    ) ?: url.tenorGif(
        embed = this,
        segments = segments,
    ) ?: url.giphyGif(
        embed = this,
        segments = segments,
    ) ?: url.unknownGif(
        embed = this,
        segments = segments,
    )
}

fun Gif.asLinkPreview(
    altText: String? = null,
): LinkPreview {
    val userAltText = altText?.trim().orEmpty()
    val alt = userAltText.ifEmpty { description.ifBlank { title } }
    return LinkPreview(
        embed = ExternalEmbed(
            uri = GenericUri(embedUri()),
            title = alt,
            description = when {
                userAltText.isNotEmpty() -> UserGifAltTextPrefix + alt
                else -> DefaultGifAltTextPrefix + alt
            },
            thumb = ImageUri(preview.uri.uri),
        ),
    )
}

private fun Gif.embedUri(): String = URLBuilder(gif.uri.uri)
    .apply {
        parameters.append(
            name = GifHeightParam,
            value = gif.height.toString(),
        )
        parameters.append(
            name = GifWidthParam,
            value = gif.width.toString(),
        )
        if (host == KlipyStaticHost) {
            mp4?.uri?.fileSlug()?.let { slug ->
                parameters.append(
                    name = GifMp4SlugParam,
                    value = slug,
                )
            }
            webm?.uri?.fileSlug()?.let { slug ->
                parameters.append(
                    name = GifWebmSlugParam,
                    value = slug,
                )
            }
        }
    }
    .buildString()

private fun Url.klipyGif(
    embed: ExternalEmbed,
    segments: List<String>,
): Gif.Embedded? {
    if (host != KlipyStaticHost) return null
    if (segments.firstOrNull() != KlipyPathPrefix) return null
    val height = positiveLongParameter(GifHeightParam) ?: return null
    val width = positiveLongParameter(GifWidthParam) ?: return null

    val playbackUrl = URLBuilder(this).apply {
        host = KlipyProxyHost
        parameters.remove(GifHeightParam)
        parameters.remove(GifWidthParam)
        parameters.remove(GifMp4SlugParam)
        parameters.remove(GifWebmSlugParam)
    }
        .build()

    return embed.asEmbeddedGifOrNull(
        provider = Gif.Embedded.Provider.Klipy,
        playbackUri = playbackUrl.toString(),
        width = width,
        height = height,
        mp4 = parameters[GifMp4SlugParam]?.let { slug ->
            playbackUrl.withFileName("$slug.mp4")
        },
        webm = parameters[GifWebmSlugParam]?.let { slug ->
            playbackUrl.withFileName("$slug.webm")
        },
    )
}

private fun Url.tenorGif(
    embed: ExternalEmbed,
    segments: List<String>,
): Gif.Embedded? {
    if (host != TenorMediaHost) return null
    val id = segments.getOrNull(0) ?: return null
    val fileName = segments.getOrNull(1) ?: return null
    if (!id.contains(TenorGifIdMarker)) return null
    val height = positiveLongParameter(GifHeightParam) ?: return null
    val width = positiveLongParameter(GifWidthParam) ?: return null

    return embed.asEmbeddedGifOrNull(
        provider = Gif.Embedded.Provider.Tenor,
        playbackUri = "https://$TenorProxyHost/${id.replace(TenorGifIdMarker, TenorTinyGifIdMarker)}/$fileName",
        width = width,
        height = height,
        mp4 = GenericUri(
            "https://$TenorProxyHost/${id.replace(TenorGifIdMarker, TenorMp4IdMarker)}/${fileName.replace(GifExtension, ".mp4")}",
        ),
        webm = GenericUri(
            "https://$TenorProxyHost/${id.replace(TenorGifIdMarker, TenorWebmIdMarker)}/${fileName.replace(GifExtension, ".webm")}",
        ),
    )
}

private fun Url.giphyGif(
    embed: ExternalEmbed,
    segments: List<String>,
): Gif.Embedded? {
    val gifId = when {
        host == GiphyHost || host == "www.$GiphyHost" ->
            segments
                .takeIf { it.getOrNull(0) == "gifs" }
                ?.getOrNull(1)
                ?.substringAfterLast('-')

        GiphyMediaHostRegex.containsMatchIn(host) ->
            segments
                .takeIf { it.getOrNull(0) == "media" }
                ?.let { mediaSegments ->
                    val trackingOrId = mediaSegments.getOrNull(1)
                    val idOrFileName = mediaSegments.getOrNull(2)
                    val fileName = mediaSegments.getOrNull(3)
                    when {
                        idOrFileName != null && GiphyFileNameRegex.matches(idOrFileName) -> trackingOrId
                        fileName != null && GiphyFileNameRegex.matches(fileName) -> idOrFileName
                        else -> null
                    }
                }

        host == GiphyImageHost || host == "www.$GiphyImageHost" -> when {
            segments.getOrNull(0) == "media" -> segments.getOrNull(1)
            else -> segments.getOrNull(0)
        }?.substringBefore('.')

        else -> null
    }
        ?.takeIf(String::isNotEmpty)
        ?: return null

    return embed.asEmbeddedGifOrNull(
        provider = Gif.Embedded.Provider.Giphy,
        playbackUri = "https://$GiphyImageHost/media/$gifId/200.webp",
        width = null,
        height = null,
        mp4 = null,
        webm = null,
    )
}

private fun Url.unknownGif(
    embed: ExternalEmbed,
    segments: List<String>,
): Gif.Embedded? {
    val fileName = segments.lastOrNull() ?: return null
    if (!fileName.endsWith(GifExtension, ignoreCase = true)) return null

    return embed.asEmbeddedGifOrNull(
        provider = Gif.Embedded.Provider.Unknown,
        playbackUri = embed.uri.uri,
        width = positiveLongParameter(GifWidthParam),
        height = positiveLongParameter(GifHeightParam),
        mp4 = null,
        webm = null,
    )
}

private fun ExternalEmbed.asEmbeddedGifOrNull(
    provider: Gif.Embedded.Provider,
    playbackUri: String,
    width: Long?,
    height: Long?,
    mp4: GenericUri?,
    webm: GenericUri?,
): Gif.Embedded {
    val isUserAltText = description.startsWith(UserGifAltTextPrefix)
    val altText = when {
        isUserAltText -> description.removePrefix(UserGifAltTextPrefix)
        description.startsWith(DefaultGifAltTextPrefix) -> description.removePrefix(DefaultGifAltTextPrefix)
        else -> title
    }
    return Gif.Embedded(
        provider = provider,
        playbackUri = ImageUri(playbackUri),
        width = width,
        height = height,
        mp4 = mp4,
        webm = webm,
        altText = altText,
        isUserAltText = isUserAltText,
    )
}

private fun Url.positiveLongParameter(
    name: String,
): Long? = parameters[name]
    ?.toLongOrNull()
    ?.takeIf { it > 0 }

private fun Url.withFileName(
    fileName: String,
): GenericUri = GenericUri(
    URLBuilder(this)
        .apply {
            pathSegments = pathSegments.dropLast(1) + fileName
        }
        .buildString(),
)

private fun gifProxied(
    uri: String,
): String {
    val url = try {
        Url(uri)
    } catch (_: Exception) {
        return uri
    }
    val proxyHost = when (url.host) {
        KlipyStaticHost -> KlipyProxyHost
        TenorMediaHost -> TenorProxyHost
        else -> return uri
    }
    return URLBuilder(url)
        .apply { host = proxyHost }
        .buildString()
}

private fun GenericUri.fileSlug(): String? {
    val fileName = URLBuilder(uri).pathSegments.lastOrNull() ?: return null
    val extensionIndex = fileName.lastIndexOf('.')
    return if (extensionIndex > 0) fileName.substring(0, extensionIndex) else null
}

private const val KlipyStaticHost = "static.klipy.com"
private const val KlipyProxyHost = "k.gifs.bsky.app"
private const val KlipyPathPrefix = "ii"
private const val TenorMediaHost = "media.tenor.com"
private const val TenorProxyHost = "t.gifs.bsky.app"
private const val TenorGifIdMarker = "AAAAC"
private const val TenorTinyGifIdMarker = "AAAAM"
private const val TenorMp4IdMarker = "AAAP1"
private const val TenorWebmIdMarker = "AAAP3"
private const val GiphyHost = "giphy.com"
private const val GiphyImageHost = "i.giphy.com"
private val GiphyMediaHostRegex = Regex("media(?:[0-4]\\.giphy\\.com|\\.giphy\\.com)", RegexOption.IGNORE_CASE)
private val GiphyFileNameRegex = Regex("^(\\S+)\\.(webp|gif|mp4)$", RegexOption.IGNORE_CASE)
private const val GifExtension = ".gif"
private const val GifHeightParam = "hh"
private const val GifWidthParam = "ww"
private const val GifMp4SlugParam = "mp4"
private const val GifWebmSlugParam = "webm"

private const val UserGifAltTextPrefix = "Alt: "
private const val DefaultGifAltTextPrefix = "ALT: "

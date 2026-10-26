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
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.parseUrl

object ProfileLinks {
    const val MAX_LINKS = 10
    const val MAX_TITLE_LENGTH = 30
}

sealed class ProfileLinkValidation {
    data class Valid(
        val url: GenericUri,
    ) : ProfileLinkValidation()

    data object Empty : ProfileLinkValidation()
    data object Invalid : ProfileLinkValidation()
    data object Insecure : ProfileLinkValidation()
    data object Shortener : ProfileLinkValidation()
    data object Duplicate : ProfileLinkValidation()
}

fun validateProfileLinkInput(
    input: String,
    existingUrls: Collection<GenericUri>,
): ProfileLinkValidation {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return ProfileLinkValidation.Empty
    // Typing more can't fix an http scheme, so flag it before the host is done
    if (trimmed.startsWith(prefix = "http:", ignoreCase = true)) {
        return ProfileLinkValidation.Insecure
    }
    val url = normalizeProfileLinkUrl(trimmed)
        ?: return ProfileLinkValidation.Invalid
    val parsed = parseUrl(url.uri)
        ?: return ProfileLinkValidation.Invalid
    return when {
        parsed.isInsecure() -> ProfileLinkValidation.Insecure
        parsed.isShortener() -> ProfileLinkValidation.Shortener
        existingUrls.any { normalizeProfileLinkUrl(it.uri) == url } -> ProfileLinkValidation.Duplicate
        else -> ProfileLinkValidation.Valid(url)
    }
}

/**
 * Normalizes input like `patreon.com/name` into a web URL, or returns null if it
 * isn't a usable web link. The scheme is kept, so an http link still needs
 * [isShowableProfileLink] or [validateProfileLinkInput] to be rejected.
 */
fun normalizeProfileLinkUrl(
    input: String,
): GenericUri? {
    val trimmed = input.trim()
    if (trimmed.isEmpty() || trimmed.endsWith('.')) return null

    val hasWebScheme = trimmed.startsWith(prefix = "https://", ignoreCase = true) ||
        trimmed.startsWith(prefix = "http://", ignoreCase = true)
    val withScheme =
        if (hasWebScheme) trimmed
        else "https://$trimmed"

    val parsed = parseUrl(withScheme) ?: return null
    if (!parsed.isWebUrl()) return null
    // Stricter than the Bluesky app: userinfo can disguise the destination,
    // as in https://paypal.com@example.com
    if (parsed.user != null || parsed.password != null) return null

    val host = parsed.host.lowercase()
    if (host.any { it.isWhitespace() || it in InvalidHostChars }) return null

    // At least two labels (e.g. 'example' and 'com'), and a TLD of letters only
    val labels = host.split('.')
    if (labels.size < 2) return null
    if (!TldRegex.matches(labels.last())) return null

    return URLBuilder(parsed)
        .apply { this.host = host }
        .buildString()
        .let(::GenericUri)
}

fun GenericUri.isShowableProfileLink(): Boolean {
    val normalized = normalizeProfileLinkUrl(uri) ?: return false
    val parsed = parseUrl(normalized.uri) ?: return false
    return !parsed.isInsecure() && !parsed.isShortener()
}

private fun Url.isWebUrl() =
    protocol == URLProtocol.HTTPS || protocol == URLProtocol.HTTP

private fun Url.isInsecure() =
    protocol == URLProtocol.HTTP

private fun Url.isShortener() =
    ShortenerDomains.any(host.lowercase()::matchesDomain)

private fun String.matchesDomain(
    domain: String,
) = this == domain || endsWith(".$domain")

private val TldRegex = Regex("^[a-z]{2,}$", RegexOption.IGNORE_CASE)

private val InvalidHostChars = setOf('/', '\\', '@', ':', '?', '#', '%', '<', '>', '"', '\'')

/**
 * General-purpose URL shorteners, plus file and link hosts used the same way.
 * Brand specific short links (youtu.be, amzn.to) are allowed.
 */
private val ShortenerDomains = listOf(
    "aceimg.in",
    "acel.me",
    "adf.ly",
    "bit.ly",
    "bitly.com",
    "bl.ink",
    "buff.ly",
    "clck.ru",
    "cutt.ly",
    "fc-lc.xyz",
    "goo.gl",
    "goto.now",
    "is.gd",
    "link2.fm",
    "ln.run",
    "lnk.ua",
    "lnkd.in",
    "n9.cl",
    "onlylinks.cc",
    "ow.ly",
    "psee.io",
    "rb.gy",
    "rebrand.ly",
    "rg.by",
    "s.id",
    "short.do",
    "short.gy",
    "shorten.as",
    "shorten.ee",
    "shorten.is",
    "shorten.so",
    "shorten.tv",
    "shorten.world",
    "shorter.me",
    "shortn.icu",
    "shorturl.at",
    "shorturl.lt",
    "shorturl.ly",
    "slicedrve.in",
    "slnt.to",
    "smarturl.lt",
    "smarturl.my",
    "sprl.me",
    "sw.run",
    "t.co",
    "t.ly",
    "tiny.cc",
    "tinyurl.com",
    "urlr.me",
    "v.gd",
    "vdey.in",
    "videy.at",
    "videy.lt",
    "videy.my",
    "x.gd",
    "zig.ht",
)

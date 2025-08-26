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

package com.tunjid.heron.data.utilities

import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.data.core.models.Labeler
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.core.types.recordKey
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import sh.christian.ozone.BlueskyJson
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.RKey
import sh.christian.ozone.api.model.JsonContent

internal object Collections {
    const val Post = "app.bsky.feed.post"
    const val Repost = "app.bsky.feed.repost"
    const val Like = "app.bsky.feed.like"
    const val Follow = "app.bsky.graph.follow"
    const val List = "app.bsky.graph.list"
    const val StarterPack = "app.bsky.graph.list"
    const val FeedGenerator = "app.bsky.feed.generator"

    // TODO: This should be more specific
    fun rKey(uri: GenericUri) = RKey(
        rkey = uri.recordKey.value,
    )

    val DefaultLabelers = BlueSkuLabelersStub
}

// TODO: This should be more specific
val GenericUri.tidInstant: Instant?
    get() = try {
        Instant.fromEpochMilliseconds(tidTimestampFromBase32(recordKey.value))
    } catch (e: IllegalArgumentException) {
        null
    }

internal val AtUri.tidInstant: Instant?
    get() = try {
        Instant.fromEpochMilliseconds(tidTimestampFromBase32(atUri.split("/").last()))
    } catch (e: IllegalArgumentException) {
        null
    }

/**
 * Extracts the path component from a given [Uri].
 */
val Uri.path: String
    get() = LeadingSlash + uri.split(SchemeSeparator)
        .last()
        .split(QueryDelimiter)
        .first()

fun String.getAsRawUri(host: Uri.Host): String = host.prefix + split(LeadingSlash)
    .drop(1)
    .joinToString(separator = LeadingSlash)
    .split(QueryDelimiter)
    .first()

internal fun <T> T.asJsonContent(
    serializer: KSerializer<T>,
): JsonContent = BlueskyJson.decodeFromString(
    BlueskyJson.encodeToString(serializer, this),
)

private fun tidTimestampFromBase32(base32Tid: String): Long {
    val tidLong = base32ToLong(base32Tid)
    return extractTimestampFromTid(tidLong)
}

private fun base32ToLong(base32String: String): Long {
    if (base32String.length != 13) {
        throw IllegalArgumentException("Base32 string must be 13 characters long.")
    }

    var result: Long = 0
    for (char in base32String) {
        val value = Alphabet.indexOf(char)
        if (value == -1) {
            throw IllegalArgumentException("Invalid base32 character: $char")
        }
        result = (result shl 5) or value.toLong()
    }

    return result
}

private fun extractTimestampFromTid(tid: Long): Long {
    // Shift to remove the clock identifier (10 bits)
    return tid shr 10
}

private const val SchemeSeparator = "://"
private const val QueryDelimiter = "?"
private const val LeadingSlash = "/"

private const val Alphabet = "234567abcdefghijklmnopqrstuvwxyz"

private val BlueSkuLabelersStub = listOf(
    Labeler(
        uri = GenericUri(uri = "at://did:plc:ar7c4by46qjdydhdevvrndac/app.bsky.labeler.service/self"),
        creatorId = ProfileId(id = "did:plc:ar7c4by46qjdydhdevvrndac"),
        definitions = listOf(
            Label.Definition(
                adultOnly = false,
                blurs = Label.BlurTarget.Content,
                defaultSetting = Label.Visibility.Hide,
                identifier = Label.Value("spam"),
                severity = Label.Severity.Inform,
            ),
            Label.Definition(
                adultOnly = false,
                blurs = Label.BlurTarget.None,
                defaultSetting = Label.Visibility.Hide,
                identifier = Label.Value("impersonation"),
                severity = Label.Severity.Inform,
            ),
            Label.Definition(
                adultOnly = false,
                blurs = Label.BlurTarget.Content,
                defaultSetting = Label.Visibility.Hide,
                identifier = Label.Value("scam"),
                severity = Label.Severity.Alert,
            ),
            Label.Definition(
                adultOnly = false,
                blurs = Label.BlurTarget.Content,
                defaultSetting = Label.Visibility.Warn,
                identifier = Label.Value("intolerant"),
                severity = Label.Severity.Alert,
            ),
            Label.Definition(
                adultOnly = true,
                blurs = Label.BlurTarget.Content,
                defaultSetting = Label.Visibility.Warn,
                identifier = Label.Value("self-harm"),
                severity = Label.Severity.Alert,
            ),
            Label.Definition(
                adultOnly = false,
                blurs = Label.BlurTarget.Content,
                defaultSetting = Label.Visibility.Hide,
                identifier = Label.Value("security"),
                severity = Label.Severity.Alert,
            ),
            Label.Definition(
                adultOnly = false,
                blurs = Label.BlurTarget.Content,
                defaultSetting = Label.Visibility.Warn,
                identifier = Label.Value("misleading"),
                severity = Label.Severity.Alert,
            ),
            Label.Definition(
                adultOnly = false,
                blurs = Label.BlurTarget.Content,
                defaultSetting = Label.Visibility.Hide,
                identifier = Label.Value("threat"),
                severity = Label.Severity.Inform,
            ),
            Label.Definition(
                adultOnly = false,
                blurs = Label.BlurTarget.Content,
                defaultSetting = Label.Visibility.Hide,
                identifier = Label.Value("unsafe-link"),
                severity = Label.Severity.Alert,
            ),
            Label.Definition(

                adultOnly = false,
                blurs = Label.BlurTarget.Content,
                defaultSetting = Label.Visibility.Hide,
                identifier = Label.Value("illicit"),
                severity = Label.Severity.Alert,
            ),
            Label.Definition(
                adultOnly = false,
                blurs = Label.BlurTarget.Content,
                defaultSetting = Label.Visibility.Warn,
                identifier = Label.Value("misinformation"),
                severity = Label.Severity.Inform,
            ),
            Label.Definition(

                adultOnly = false,
                blurs = Label.BlurTarget.None,
                defaultSetting = Label.Visibility.Warn,
                identifier = Label.Value("rumor"),
                severity = Label.Severity.Inform,
            ),
            Label.Definition(
                adultOnly = false,
                blurs = Label.BlurTarget.Content,
                defaultSetting = Label.Visibility.Hide,
                identifier = Label.Value("rude"),
                severity = Label.Severity.Inform,
            ),
            Label.Definition(
                adultOnly = false,
                blurs = Label.BlurTarget.Content,
                defaultSetting = Label.Visibility.Hide,
                identifier = Label.Value("extremist"),
                severity = Label.Severity.Alert,
            ),
            Label.Definition(
                adultOnly = true,
                blurs = Label.BlurTarget.Content,
                defaultSetting = Label.Visibility.Warn,
                identifier = Label.Value("sensitive"),
                severity = Label.Severity.Alert,
            ),
            Label.Definition(
                adultOnly = false,
                blurs = Label.BlurTarget.Content,
                defaultSetting = Label.Visibility.Hide,
                identifier = Label.Value("engagement-farming"),
                severity = Label.Severity.Alert,
            ),
            Label.Definition(
                adultOnly = false,
                blurs = Label.BlurTarget.Content,
                defaultSetting = Label.Visibility.Hide,
                identifier = Label.Value("inauthentic"),
                severity = Label.Severity.Alert,
            ),
            Label.Definition(
                adultOnly = true,
                blurs = Label.BlurTarget.Media,
                defaultSetting = Label.Visibility.Hide,
                identifier = Label.Value("sexual-figurative"),
                severity = Label.Severity.Inform,
            ),
            Label.Definition(
                adultOnly = true,
                blurs = Label.BlurTarget.Media,
                defaultSetting = Label.Visibility.Hide,
                identifier = Label.Value("porn"),
                severity = Label.Severity.Inform,
            ),
            Label.Definition(
                adultOnly = true,
                blurs = Label.BlurTarget.Media,
                defaultSetting = Label.Visibility.Hide,
                identifier = Label.Value("nudity"),
                severity = Label.Severity.Inform,
            ),
            Label.Definition(
                adultOnly = true,
                blurs = Label.BlurTarget.Media,
                defaultSetting = Label.Visibility.Hide,
                identifier = Label.Value("sexual"),
                severity = Label.Severity.Inform,
            ),
        ),
        values = listOf(
            Label.Value(value = "!hide"),
            Label.Value(value = "!warn"),
            Label.Value(value = "porn"),
            Label.Value(value = "sexual"),
            Label.Value(value = "nudity"),
            Label.Value(value = "sexual-figurative"),
            Label.Value(value = "graphic-media"),
            Label.Value(value = "self-harm"),
            Label.Value(value = "sensitive"),
            Label.Value(value = "extremist"),
            Label.Value(value = "intolerant"),
            Label.Value(value = "threat"),
            Label.Value(value = "rude"),
            Label.Value(value = "illicit"),
            Label.Value(value = "security"),
            Label.Value(value = "unsafe-link"),
            Label.Value(value = "impersonation"),
            Label.Value(value = "misinformation"),
            Label.Value(value = "scam"),
            Label.Value(value = "engagement-farming"),
            Label.Value(value = "spam"),
            Label.Value(value = "rumor"),
            Label.Value(value = "misleading"),
            Label.Value(value = "inauthentic"),
        ),
    ),
)

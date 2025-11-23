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

import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.core.types.recordKeyOrNull
import com.tunjid.heron.data.network.BlueskyJson
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.RKey
import sh.christian.ozone.api.model.JsonContent

internal object Collections {
    const val Post = "app.bsky.feed.post"
    const val Profile = "app.bsky.actor.profile"
    const val Repost = "app.bsky.feed.repost"
    const val Like = "app.bsky.feed.like"
    const val Follow = "app.bsky.graph.follow"
    const val List = "app.bsky.graph.list"
    const val StarterPack = "app.bsky.graph.starterpack"
    const val FeedGenerator = "app.bsky.feed.generator"
    const val UploadVideo = "com.atproto.repo.uploadBlob"

    val SelfRecordKey = RKey("self")

    // Internal method
    fun requireRKey(uri: GenericUri) = RKey(
        rkey = requireNotNull(uri.recordKeyOrNull()) {
            "This uri does not have a record key"
        }.value,
    )

    val DefaultLabelerProfileId = ProfileId(id = "did:plc:ar7c4by46qjdydhdevvrndac")
}

fun Uri.asGenericUri(): GenericUri = GenericUri(uri)

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

internal inline fun <reified T : Any> JsonContent.safeDecodeAs(): T? =
    try {
        decodeAs<T>()
    } catch (_: kotlinx.serialization.SerializationException) {
        null
    }

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

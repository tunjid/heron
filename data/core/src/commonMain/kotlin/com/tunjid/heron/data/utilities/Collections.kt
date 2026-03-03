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

import com.tunjid.heron.data.core.types.GenericId
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.core.types.asRecordUriOrNull
import com.tunjid.heron.data.network.BlueskyJson
import kotlin.time.Instant
import kotlinx.serialization.KSerializer
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.RKey
import sh.christian.ozone.api.model.JsonContent

internal object Collections {
    const val Profile = "app.bsky.actor.profile"
    const val ProfileStatus = "app.bsky.actor.status"
    const val UploadVideo = "com.atproto.repo.uploadBlob"

    val SelfRecordKey = RKey("self")

    val DefaultLabelerProfileId = ProfileId(id = "did:plc:ar7c4by46qjdydhdevvrndac")

    fun Id.isStubbedId() = id == StubbedId

    fun AtUri.requireRecordUri() = requireNotNull(atUri.asRecordUriOrNull())

    inline fun <reified T : Id> stubbedId(
        constructor: (String) -> T,
    ) = constructor(StubbedId)
}

fun Uri.asGenericUri(): GenericUri = GenericUri(uri)

fun Id.asGenericId(): GenericId = GenericId(id)

/**
 * Attempts to fetch the timestamp from the record key if it is a valid TID.
 */
val RecordKey.tidInstant: Instant?
    get() = try {
        Instant.fromEpochMilliseconds(tidTimestampFromBase32(value))
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

internal val AtUri.tidInstant: Instant?
    get() = RecordKey(atUri.split("/").last()).tidInstant

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

/**
 * @see [TID definition](https://atproto.com/specs/tid)
 */
fun tidTimestampFromBase32(tid: String): Long {
    require(tid.length == 13) { "TID must be 13 characters long" }

    var result = 0L

    // 1. Decode the Base32-sortable string into the original 64-bit integer
    for (char in tid) {
        val value = decodeBase32Char(char)
        // Shift left by 5 bits (base32) and add the new value
        result = (result shl 5) or value.toLong()
    }

    // 2. Extract the timestamp
    // The layout is: [0 (1 bit)] [Micros (53 bits)] [ClockID (10 bits)]
    // We strictly need the 53 bits representing microseconds.
    // Right shifting by 10 discards the ClockID and moves the micros to the least significant position.
    // The top bit is guaranteed to be 0, so unsigned vs signed shift doesn't matter here,
    // but 'ushr' is semantically safer for bitwise operations.
    val micros = result ushr 10

    // 3. Convert microseconds to milliseconds
    return micros / 1000
}

// Helper to map Base32-sortable characters to their 5-bit integer values (0-31)
// Alphabet: 234567abcdefghijklmnopqrstuvwxyz
private fun decodeBase32Char(char: Char): Int =
    when (char) {
        in '2'..'7' -> char - '2' // '2' is 0, '7' is 5
        in 'a'..'z' -> char - 'a' + 6 // 'a' is 6, 'z' is 31
        else -> throw IllegalArgumentException("Invalid TID character: $char")
    }

private const val SchemeSeparator = "://"
private const val QueryDelimiter = "?"
private const val LeadingSlash = "/"

internal const val StubbedId = "bafyreipendingunknownstub2222222222222222222222222222222222"

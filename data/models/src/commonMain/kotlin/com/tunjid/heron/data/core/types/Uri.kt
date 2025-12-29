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

package com.tunjid.heron.data.core.types

import io.ktor.http.Url
import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

@Serializable
sealed interface Uri {
    val uri: String

    enum class Host(val prefix: String) {
        AtProto("at://"),
        Http("http://"),
    }
}

@Serializable
sealed interface RecordUri : Uri

/**
 * Extracts the [ProfileId] component from an AT URI string.
 */
fun RecordUri.profileId(): ProfileId =
    requireNotNull(
        uri.atUriComponents { authorityRange, _, _ ->
            ProfileId(uri.substring(authorityRange.start, authorityRange.endExclusive))
        },
    )

val RecordUri.recordKey: RecordKey
    get() = requireNotNull(
        uri.atUriComponents { _, _, rKeyRange ->
            RecordKey(uri.substring(rKeyRange.start, rKeyRange.endExclusive))
        },
    )

fun GenericUri.recordKeyOrNull(): RecordKey? =
    uri.atUriComponents { _, _, rKeyRange ->
        RecordKey(uri.substring(rKeyRange.start, rKeyRange.endExclusive))
    }

val Uri.domain get() = Url(uri).host.removePrefix("www.")

@Serializable
@JvmInline
value class PostUri(
    override val uri: String,
) : Uri,
    RecordUri {
    override fun toString(): String = uri

    companion object {
        const val NAMESPACE = "app.bsky.feed.post"
    }
}

@Serializable
@JvmInline
value class ProfileUri(
    override val uri: String,
) : Uri {
    override fun toString(): String = uri

    companion object {
        fun ProfileId.asSelfLabelerUri() =
            LabelerUri("${Uri.Host.AtProto.prefix}$id/${LabelerUri.NAMESPACE}/self")
    }
}

@Serializable
@JvmInline
value class FeedGeneratorUri(
    override val uri: String,
) : Uri,
    RecordUri {
    override fun toString(): String = uri

    companion object {
        const val NAMESPACE = "app.bsky.feed.generator"
    }
}

@Serializable
@JvmInline
value class ListUri(
    override val uri: String,
) : Uri,
    RecordUri {
    override fun toString(): String = uri

    companion object {
        const val NAMESPACE = "app.bsky.graph.list"
    }
}

@Serializable
@JvmInline
value class StarterPackUri(
    override val uri: String,
) : Uri,
    RecordUri {
    override fun toString(): String = uri

    companion object {
        const val NAMESPACE = "app.bsky.graph.starterpack"
    }
}

@Serializable
@JvmInline
value class LabelerUri(
    override val uri: String,
) : Uri,
    RecordUri {
    override fun toString(): String = uri

    companion object {
        const val NAMESPACE = "app.bsky.labeler.service"
    }
}

@Serializable
@JvmInline
value class ListMemberUri(
    override val uri: String,
) : Uri {
    override fun toString(): String = uri
}

@Serializable
@JvmInline
value class ThreadGateUri(
    override val uri: String,
) : Uri {
    override fun toString(): String = uri

    companion object {
        const val NAMESPACE = "app.bsky.feed.threadgate"
    }
}

@Serializable
@JvmInline
value class ImageUri(
    override val uri: String,
) : Uri {
    override fun toString(): String = uri
}

@Serializable
@JvmInline
value class GenericUri(
    override val uri: String,
) : Uri {
    override fun toString(): String = uri
}

@Serializable
@JvmInline
value class FileUri(
    override val uri: String,
) : Uri {
    override fun toString(): String = uri
}

/**
 * Returns this [Uri] as a [RecordUri] if applicable.
 */
fun Uri.asRecordUriOrNull(): RecordUri? = uri.asRecordUriOrNull()

/**
 * Parses a string into a specific [RecordUri] object based on its collection.
 *
 * @receiver The string to parse.
 * @return A specific [RecordUri] (e.g., [FeedGeneratorUri], [ListUri], e.t.c)
 * if parsing is successful, or `null` if the string is not a valid AT URI.
 */
fun String.asRecordUriOrNull(): RecordUri? = atUriComponents { _, collectionRange, _ ->
    when (substring(collectionRange.start, collectionRange.endExclusive)) {
        PostUri.NAMESPACE -> PostUri(this)
        FeedGeneratorUri.NAMESPACE -> FeedGeneratorUri(this)
        ListUri.NAMESPACE -> ListUri(this)
        StarterPackUri.NAMESPACE -> StarterPackUri(this)
        LabelerUri.NAMESPACE -> LabelerUri(this)
        else -> null
    }
}

/**
 * Parses an AT URI string into its components without using Regex or intermediate data classes.
 *
 * Format expected: at://{authority}/{collection}/{rkey}
 * Example: at://did:plc:12345/app.bsky.feed.post/98765
 *
 * @param action A lambda to consume the parsed parts.
 * @return The result of [action] if parsing is successful, or `null` if the format is invalid.
 */
private inline fun <T> String.atUriComponents(
    action: (authorityRange: StringRange, collectionRange: StringRange, rKeyRange: StringRange) -> T,
): T? {
    // 1. Validate Prefix "at://"
    if (!this.startsWith(Uri.Host.AtProto.prefix)) return null

    val authorityStart = 5
    val length = this.length

    // 2. Find the first separator '/' after authority
    // returns -1 if not found
    val firstSlashIndex = this.indexOf('/', startIndex = authorityStart)

    // Validation:
    // - Must have a slash
    // - Slash cannot be immediately after prefix (would mean empty authority)
    if (firstSlashIndex <= authorityStart) return null

    // 3. Find the second separator '/' after collection
    val secondSlashIndex = this.indexOf('/', startIndex = firstSlashIndex + 1)

    // Validation:
    // - Must have a second slash
    // - Second slash cannot be immediately after first slash (would mean empty collection)
    if (secondSlashIndex <= firstSlashIndex + 1) return null

    // 4. Validate rKey (must not be empty)
    if (secondSlashIndex + 1 >= length) return null

    // 5. Extract components
    return action(
        StringRange(authorityStart, firstSlashIndex),
        StringRange(firstSlashIndex + 1, secondSlashIndex),
        StringRange(secondSlashIndex + 1, length),
    )
}

@JvmInline
private value class StringRange(
    val packed: Long,
)

private fun StringRange(
    start: Int,
    endExclusive: Int,
) = StringRange(
    packed = packInts(start, endExclusive),
)

private val StringRange.start
    get() = unpackInt1(packed)

private val StringRange.endExclusive
    get() = unpackInt2(packed)

private fun packInts(val1: Int, val2: Int): Long {
    return (val1.toLong() shl 32) or (val2.toLong() and 0xFFFFFFFF)
}

private fun unpackInt1(value: Long): Int {
    return (value shr 32).toInt()
}

private fun unpackInt2(value: Long): Int {
    return (value and 0xFFFFFFFF).toInt()
}

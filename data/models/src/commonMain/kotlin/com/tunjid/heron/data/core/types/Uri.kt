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
        Https("https://"),
    }
}

fun String.takeIfIs(
    host: Uri.Host,
) = takeIf { it.startsWith(host.prefix) }

@Serializable
sealed interface RecordUri : Uri

@Serializable
sealed interface EmbeddableRecordUri : RecordUri

/**
 * Extracts the [ProfileId] component from an AT URI string.
 */
fun RecordUri.profileId(): ProfileId =
    requireNotNull(
        uri.atUriComponents { authorityRange, _, _ ->
            ProfileId(uri.substring(authorityRange.start, authorityRange.endExclusive))
        },
    )

fun RecordUri.requireCollection(): String =
    when (this) {
        is FeedGeneratorUri -> FeedGeneratorUri.NAMESPACE
        is LabelerUri -> LabelerUri.NAMESPACE
        is ListUri -> ListUri.NAMESPACE
        is ListMemberUri -> ListMemberUri.NAMESPACE
        is PostUri -> PostUri.NAMESPACE
        is StarterPackUri -> StarterPackUri.NAMESPACE
        is FollowUri -> FollowUri.NAMESPACE
        is LikeUri -> LikeUri.NAMESPACE
        is RepostUri -> RepostUri.NAMESPACE
        is BlockUri -> BlockUri.NAMESPACE
        is StandardPublicationUri -> StandardPublicationUri.NAMESPACE
        is StandardDocumentUri -> StandardDocumentUri.NAMESPACE
        is StandardSubscriptionUri -> StandardSubscriptionUri.NAMESPACE
        is UnknownRecordUri -> throw UnresolvableRecordException(this)
    }

fun recordUriOrNull(
    profileId: ProfileId,
    namespace: String,
    recordKey: RecordKey,
): RecordUri? = "${Uri.Host.AtProto.prefix}${profileId.id}/$namespace/${recordKey.value}"
    .asRecordUriOrNull()

val RecordUri.recordKey: RecordKey
    get() = requireNotNull(
        uri.atUriComponents { _, _, rKeyRange ->
            RecordKey(uri.substring(rKeyRange.start, rKeyRange.endExclusive))
        },
    )

val Uri.domain get() = Url(uri).host.removePrefix("www.")

@Serializable
@JvmInline
value class PostUri(
    override val uri: String,
) : Uri,
    RecordUri,
    EmbeddableRecordUri {
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
    RecordUri,
    EmbeddableRecordUri {
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
    RecordUri,
    EmbeddableRecordUri {
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
    RecordUri,
    EmbeddableRecordUri {
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
    RecordUri,
    EmbeddableRecordUri {
    override fun toString(): String = uri

    companion object {
        const val NAMESPACE = "app.bsky.labeler.service"
    }
}

@Serializable
@JvmInline
value class LikeUri(
    override val uri: String,
) : Uri,
    RecordUri {
    override fun toString(): String = uri

    companion object {
        const val NAMESPACE = "app.bsky.feed.like"
    }
}

@Serializable
@JvmInline
value class RepostUri(
    override val uri: String,
) : Uri,
    RecordUri {
    override fun toString(): String = uri

    companion object {
        const val NAMESPACE = "app.bsky.feed.repost"
    }
}

@Serializable
@JvmInline
value class FollowUri(
    override val uri: String,
) : Uri,
    RecordUri {
    override fun toString(): String = uri

    companion object {
        const val NAMESPACE = "app.bsky.graph.follow"
    }
}

@Serializable
@JvmInline
value class BlockUri(
    override val uri: String,
) : Uri,
    RecordUri {
    override fun toString(): String = uri

    companion object {
        const val NAMESPACE = "app.bsky.graph.block"
    }
}

@Serializable
@JvmInline
value class StandardPublicationUri(
    override val uri: String,
) : Uri,
    RecordUri {
    override fun toString(): String = uri

    companion object {
        const val NAMESPACE = "site.standard.publication"
    }
}

@Serializable
@JvmInline
value class StandardDocumentUri(
    override val uri: String,
) : Uri,
    RecordUri {
    override fun toString(): String = uri

    companion object {
        const val NAMESPACE = "site.standard.document"
    }
}

@Serializable
@JvmInline
value class StandardSubscriptionUri(
    override val uri: String,
) : Uri,
    RecordUri {
    override fun toString(): String = uri

    companion object {
        const val NAMESPACE = "site.standard.graph.subscription"
    }
}

@Serializable
@JvmInline
value class UnknownRecordUri(
    override val uri: String,
) : Uri,
    RecordUri {
    override fun toString(): String = uri
}

@Serializable
@JvmInline
value class ListMemberUri(
    override val uri: String,
) : Uri,
    RecordUri {
    override fun toString(): String = uri

    companion object {
        const val NAMESPACE = "app.bsky.graph.listitem"
    }
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
value class PostGateUri(
    override val uri: String,
) : Uri {
    override fun toString(): String = uri

    companion object {
        const val NAMESPACE = "app.bsky.feed.postgate"
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
 * Returns this [Uri] as a [RecordUri] if applicable.
 */
fun Uri.asEmbeddableRecordUriOrNull(): EmbeddableRecordUri? = uri.asEmbeddableRecordUriOrNull()

/**
 * Parses a string into a specific [RecordUri] object based on its collection.
 *
 * @receiver The string to parse.
 * @return A specific [RecordUri] (e.g., [FeedGeneratorUri], [ListUri], e.t.c)
 * if parsing is successful, or `null` if the string is not a valid AT URI.
 */
fun String.asRecordUriOrNull(): RecordUri? = atUriComponents { _, collectionRange, _ ->
    val normalized = withAtProtoPrefix()
    when (substring(collectionRange.start, collectionRange.endExclusive)) {
        PostUri.NAMESPACE -> PostUri(normalized)
        FeedGeneratorUri.NAMESPACE -> FeedGeneratorUri(normalized)
        ListUri.NAMESPACE -> ListUri(normalized)
        StarterPackUri.NAMESPACE -> StarterPackUri(normalized)
        LabelerUri.NAMESPACE -> LabelerUri(normalized)
        LikeUri.NAMESPACE -> LikeUri(normalized)
        RepostUri.NAMESPACE -> RepostUri(normalized)
        FollowUri.NAMESPACE -> FollowUri(normalized)
        ListMemberUri.NAMESPACE -> ListMemberUri(normalized)
        BlockUri.NAMESPACE -> BlockUri(normalized)
        StandardPublicationUri.NAMESPACE -> StandardPublicationUri(normalized)
        StandardDocumentUri.NAMESPACE -> StandardDocumentUri(normalized)
        StandardSubscriptionUri.NAMESPACE -> StandardSubscriptionUri(normalized)
        else -> UnknownRecordUri(normalized)
    }
}

fun String.asEmbeddableRecordUriOrNull(): EmbeddableRecordUri? {
    val atUri = when {
        startsWith(Uri.Host.Https.prefix) -> bskyHttpsUrlToAtUri() ?: return null
        else -> this
    }
    return atUri.atUriComponents { _, collectionRange, _ ->
        val normalized = atUri.withAtProtoPrefix()
        when (atUri.substring(collectionRange.start, collectionRange.endExclusive)) {
            PostUri.NAMESPACE -> PostUri(normalized)
            FeedGeneratorUri.NAMESPACE -> FeedGeneratorUri(normalized)
            ListUri.NAMESPACE -> ListUri(normalized)
            StarterPackUri.NAMESPACE -> StarterPackUri(normalized)
            LabelerUri.NAMESPACE -> LabelerUri(normalized)
            else -> null
        }
    }
}

private fun String.withAtProtoPrefix(): String =
    if (startsWith(Uri.Host.AtProto.prefix)) this
    else "${Uri.Host.AtProto.prefix}$this"

private val bskyPathSegmentToNamespace = mapOf(
    "post" to PostUri.NAMESPACE,
    "feed" to FeedGeneratorUri.NAMESPACE,
    "lists" to ListUri.NAMESPACE,
    "starter-pack" to StarterPackUri.NAMESPACE,
    "labeler" to LabelerUri.NAMESPACE,
)

fun String.bskyHttpsUrlToAtUri(): String? {
    if (!startsWith(Uri.Host.Https.prefix)) return null

    // Skip past "https://" and optional "www."
    var cursor = Uri.Host.Https.prefix.length
    if (startsWith("www.", cursor)) cursor += 4

    // Must start with "bsky.app/"
    if (!startsWith("bsky.app/", cursor)) return null
    cursor += "bsky.app/".length

    // Find first segment
    val seg0End = indexOf('/', cursor).takeIf { it != -1 } ?: return null
    val seg0 = substring(cursor, seg0End)

    // Find second segment
    val seg1Start = seg0End + 1
    val seg1End = indexOf('/', seg1Start).takeIf { it != -1 } ?: return null
    val seg1 = substring(seg1Start, seg1End)

    // Find third segment — stop at '?', '#' or end of string
    val seg2Start = seg1End + 1
    val seg2End = indexOfFirst(seg2Start) { it == '/' || it == '?' || it == '#' }
        .takeIf { it != -1 } ?: length
    val seg2 = substring(seg2Start, seg2End)

    // Find optional fourth segment
    val seg3End = if (seg2End < length && this[seg2End] == '/') {
        indexOfFirst(seg2End + 1) { it == '?' || it == '#' }
            .takeIf { it != -1 } ?: length
    } else -1
    val seg3 = if (seg3End != -1) substring(seg2End + 1, seg3End) else null

    val (handle, type, rkey) = when {
        seg0 == "profile" && seg3 != null -> Triple(seg1, seg2, seg3)
        seg0 == "starter-pack" -> Triple(seg1, seg0, seg2)
        seg0 == "profile" -> Triple(seg1, seg2, "self") // labeler
        else -> return null
    }

    val namespace = bskyPathSegmentToNamespace[type] ?: return null
    return "${Uri.Host.AtProto.prefix}$handle/$namespace/$rkey"
}

private inline fun String.indexOfFirst(startIndex: Int, predicate: (Char) -> Boolean): Int {
    for (i in startIndex until length) if (predicate(this[i])) return i
    return -1
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
    // 1. Prefix "at://" is optional; start parsing after it if present.
    val authorityStart =
        if (this.startsWith(Uri.Host.AtProto.prefix)) Uri.Host.AtProto.prefix.length
        else 0
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

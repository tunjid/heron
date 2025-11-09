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
fun String.asRecordUriOrNull(): RecordUri? {
    // We must validate the string and parse components first.
    // If components are null, the string was invalid, so return null.
    val components = this.toAtUriComponentsOrNull() ?: return null

    // We know 'this' is not null here because toAtUriComponentsOrNull would have returned null.
    return when (components.collection) {
        PostUri.NAMESPACE -> PostUri(this)
        FeedGeneratorUri.NAMESPACE -> FeedGeneratorUri(this)
        ListUri.NAMESPACE -> ListUri(this)
        StarterPackUri.NAMESPACE -> StarterPackUri(this)
        LabelerUri.NAMESPACE -> LabelerUri(this)
        else -> null
    }
}

/**
 * Extracts the [ProfileId] component from an AT URI string.
 */
fun RecordUri.profileId(): ProfileId =
    ProfileId(requireNotNull(uri.toAtUriComponentsOrNull()).authority)

/**
 * Internal function to parse a string into its raw components.
 *
 * @receiver The string to parse.
 * @return An [AtUriComponents] data class if parsing is successful, or `null`.
 */
private fun String?.toAtUriComponentsOrNull(): AtUriComponents? {
    // Check for null or blank string
    if (this.isNullOrBlank()) {
        return null
    }

    // Use matchEntire to ensure the whole string matches the regex
    val match = AT_URI_REGEX.matchEntire(this) ?: return null

    // Regex groups are 1-indexed. Group 0 is the full match.
    // We expect 4 groups total: [full_match, authority, collection, rkey]
    if (match.groupValues.size != 4) {
        return null
    }

    return AtUriComponents(
        authority = match.groupValues[1],
        collection = match.groupValues[2],
        rkey = match.groupValues[3],
    )
}

/**
 * An internal data class to hold the structured components of a parsed AT URI.
 * This is used by the extension functions to avoid parsing multiple times.
 */
private data class AtUriComponents(
    val authority: String,
    val collection: String,
    val rkey: String,
)

// Regex to validate and capture the three main parts of an AT URI.
// at://[authority]/[collection]/[rkey]
private val AT_URI_REGEX = "^at://([^/]+)/([^/]+)/(.+)$".toRegex()

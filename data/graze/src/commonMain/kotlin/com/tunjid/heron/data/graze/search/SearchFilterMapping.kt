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

package com.tunjid.heron.data.graze.search

import com.tunjid.heron.data.core.models.SearchFilter
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.graze.Filter
import com.tunjid.heron.data.graze.isValid
import com.tunjid.heron.data.graze.search.Graze.FeedFromSearch
import com.tunjid.heron.data.graze.search.Graze.FeedFromSearch.MappingNote
import com.tunjid.heron.data.graze.search.Graze.SearchFromFeed
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

object Graze {

    data class FeedFromSearch(
        val filter: Filter.Root,
        val notes: List<MappingNote>,
    ) {
        enum class MappingNote {
            FreeTextApproximated,
            MediaApproximated,
            RepliesApproximated,
        }
    }

    data class SearchFromFeed(
        val query: String,
        val filter: SearchFilter,
        val droppedLeaves: List<Filter.Leaf>,
    )
}

suspend fun SearchFilter?.toFeedFilter(
    query: String,
    viewerHandle: ProfileHandle? = null,
    resolveHandle: suspend (ProfileId) -> ProfileHandle?,
): FeedFromSearch {
    val leaves = mutableListOf<Filter>()
    val notes = mutableSetOf<MappingNote>()

    query.toTermList().takeIf(List<String>::isNotEmpty)?.let { terms ->
        leaves += Filter.Regex.Any(
            variable = TextVariable,
            terms = terms,
            isCaseInsensitive = true,
        )
        notes += MappingNote.FreeTextApproximated
    }

    this?.exactPhrase.nonBlankTrimmed()?.let { phrase ->
        leaves += Filter.Regex.Matches(
            variable = TextVariable,
            pattern = phrase.escapeForRegex(),
            isCaseInsensitive = true,
        )
        notes += MappingNote.FreeTextApproximated
    }

    this?.noneOfWords.toTermList().takeIf(List<String>::isNotEmpty)?.let { terms ->
        leaves += Filter.Regex.None(
            variable = TextVariable,
            terms = terms,
            isCaseInsensitive = true,
        )
    }

    this?.language.nonBlankTrimmed()?.let { code ->
        leaves += Filter.Entity.Matches(
            entityType = Filter.Entity.Type.Languages,
            values = listOf(code),
        )
    }

    when (this?.media) {
        null -> Unit
        SearchFilter.Media.All -> Unit
        SearchFilter.Media.VideosOnly -> leaves += Filter.Attribute.Embed(
            operator = Filter.Comparator.Equality.Equal,
            embedType = Filter.Attribute.Embed.Kind.Video,
        )
        SearchFilter.Media.WithMedia -> {
            leaves += Filter.Or(
                filters = MediaEmbedKinds.map { kind ->
                    Filter.Attribute.Embed(
                        operator = Filter.Comparator.Equality.Equal,
                        embedType = kind,
                    )
                },
            )
            notes += MappingNote.MediaApproximated
        }
    }

    when (this?.replies) {
        null -> Unit
        SearchFilter.Replies.PostsAndReplies -> Unit
        SearchFilter.Replies.PostsOnly -> {
            leaves += replyCompare(isReply = false)
            notes += MappingNote.RepliesApproximated
        }
        SearchFilter.Replies.RepliesOnly -> {
            leaves += replyCompare(isReply = true)
            notes += MappingNote.RepliesApproximated
        }
    }

    // Authors match by DID via social_list; aggregate across groups the way searchPostsV2 does.
    this?.people
        ?.didsFor(
            mode = SearchFilter.PersonGroup.Mode.Include,
            kind = SearchFilter.PersonGroup.Kind.Authors,
        )
        ?.takeIf(List<String>::isNotEmpty)?.let { dids ->
            leaves += Filter.Social.UserList(
                dids = dids,
                operator = Filter.Comparator.Set.In,
            )
        }
    this?.people
        ?.didsFor(
            mode = SearchFilter.PersonGroup.Mode.Exclude,
            kind = SearchFilter.PersonGroup.Kind.Authors,
        )
        ?.takeIf(List<String>::isNotEmpty)?.let { dids ->
            leaves += Filter.Social.UserList(
                dids = dids,
                operator = Filter.Comparator.Set.NotIn,
            )
        }

    // Mentions match by handle; resolve concurrently and drop DIDs with no known handle.
    this?.people?.mentionDids(mode = SearchFilter.PersonGroup.Mode.Include)
        ?.awaitMapNotNull { resolveHandle(it) }
        ?.map(ProfileHandle::id)
        ?.distinct()
        ?.takeIf(List<String>::isNotEmpty)
        ?.let { handles ->
            leaves += Filter.Entity.Matches(
                entityType = Filter.Entity.Type.Mentions,
                values = handles,
            )
        }
    this?.people
        ?.mentionDids(mode = SearchFilter.PersonGroup.Mode.Exclude)
        ?.awaitMapNotNull { resolveHandle(it) }
        ?.map(ProfileHandle::id)
        ?.distinct()
        ?.takeIf(List<String>::isNotEmpty)
        ?.let { handles ->
            leaves += Filter.Entity.Excludes(
                entityType = Filter.Entity.Type.Mentions,
                values = handles,
            )
        }

    if (this?.from == SearchFilter.From.Following) {
        viewerHandle?.id.nonBlankTrimmed()?.let { handle ->
            leaves += Filter.Social.Graph(
                username = handle,
                operator = Filter.Comparator.Set.In,
                direction = Filter.Social.Graph.Direction.Following,
            )
        }
    }

    // SearchFilter.since / .until are intentionally not mapped: Graze has no creation-date operator.

    return FeedFromSearch(
        filter = Filter.And(filters = leaves.filter { it.isValid }),
        notes = notes.toList(),
    )
}

suspend fun Filter.Root.toSearchApproximation(
    resolveDid: suspend (handle: ProfileHandle) -> ProfileId? = { null },
): SearchFromFeed {
    val dropped = mutableListOf<Filter.Leaf>()
    val queryTerms = mutableListOf<String>()
    val noneWords = mutableListOf<String>()
    val mediaEmbedKinds = mutableSetOf<Filter.Attribute.Embed.Kind>()
    val authorsInclude = mutableListOf<ProfileId>()
    val authorsExclude = mutableListOf<ProfileId>()
    val mentionsInclude = mutableListOf<ProfileId>()
    val mentionsExclude = mutableListOf<ProfileId>()
    var exactPhrase: String? = null
    var language: String? = null
    var replies = SearchFilter.Replies.PostsAndReplies
    var from = SearchFilter.From.Anyone

    for (leaf in flattenLeaves()) {
        when (leaf) {
            is Filter.Social.UserList -> when (leaf.operator) {
                Filter.Comparator.Set.In -> authorsInclude += leaf.dids.map(::ProfileId)
                Filter.Comparator.Set.NotIn -> authorsExclude += leaf.dids.map(::ProfileId)
            }

            is Filter.Social.Graph ->
                if (leaf.operator == Filter.Comparator.Set.In &&
                    leaf.direction == Filter.Social.Graph.Direction.Following
                ) {
                    from = SearchFilter.From.Following
                } else {
                    dropped += leaf
                }

            is Filter.Entity.Matches -> when (leaf.entityType) {
                Filter.Entity.Type.Languages -> language = language ?: leaf.values.firstOrNull()
                Filter.Entity.Type.Mentions ->
                    mentionsInclude += leaf.values.awaitMapNotNull { resolveDid(ProfileHandle(it)) }
                else -> dropped += leaf
            }

            is Filter.Entity.Excludes -> when (leaf.entityType) {
                Filter.Entity.Type.Mentions ->
                    mentionsExclude += leaf.values.awaitMapNotNull { resolveDid(ProfileHandle(it)) }
                else -> dropped += leaf
            }

            is Filter.Regex.Any ->
                if (leaf.variable == TextVariable) queryTerms += leaf.terms else dropped += leaf

            is Filter.Regex.None ->
                if (leaf.variable == TextVariable) noneWords += leaf.terms else dropped += leaf

            is Filter.Regex.Matches ->
                if (leaf.variable == TextVariable) {
                    exactPhrase = exactPhrase ?: leaf.pattern.unescapeRegex()
                } else {
                    dropped += leaf
                }

            is Filter.Attribute.Embed ->
                if (leaf.operator == Filter.Comparator.Equality.Equal && leaf.embedType in MediaEmbedKinds) {
                    mediaEmbedKinds += leaf.embedType
                } else {
                    dropped += leaf
                }

            is Filter.Attribute.Compare ->
                if (leaf.selector == Filter.Attribute.Compare.Selector.Reply) {
                    when (leaf.targetValue.lowercase()) {
                        "true" -> replies = SearchFilter.Replies.RepliesOnly
                        "false" -> replies = SearchFilter.Replies.PostsOnly
                        else -> dropped += leaf
                    }
                } else {
                    dropped += leaf
                }

            // No search equivalent: text regex negation, ML, analysis, other social filters.
            else -> dropped += leaf
        }
    }

    val people = listOfNotNull(
        authorsInclude.toPersonGroup(
            mode = SearchFilter.PersonGroup.Mode.Include,
            kind = SearchFilter.PersonGroup.Kind.Authors,
        ),
        authorsExclude.toPersonGroup(
            mode = SearchFilter.PersonGroup.Mode.Exclude,
            kind = SearchFilter.PersonGroup.Kind.Authors,
        ),
        mentionsInclude.toPersonGroup(
            mode = SearchFilter.PersonGroup.Mode.Include,
            kind = SearchFilter.PersonGroup.Kind.Mentions,
        ),
        mentionsExclude.toPersonGroup(
            mode = SearchFilter.PersonGroup.Mode.Exclude,
            kind = SearchFilter.PersonGroup.Kind.Mentions,
        ),
    )

    val searchFilter = SearchFilter(
        exactPhrase = exactPhrase,
        noneOfWords = noneWords.takeIf(List<String>::isNotEmpty)?.joinToString(separator = " "),
        since = null,
        until = null,
        language = language,
        media = when {
            mediaEmbedKinds.isEmpty() -> SearchFilter.Media.All
            mediaEmbedKinds == setOf(Filter.Attribute.Embed.Kind.Video) -> SearchFilter.Media.VideosOnly
            else -> SearchFilter.Media.WithMedia
        },
        replies = replies,
        from = from,
        people = people,
    )

    return SearchFromFeed(
        query = queryTerms.joinToString(separator = " "),
        filter = searchFilter,
        droppedLeaves = dropped,
    )
}

/**
 * Maps each element through [transform] concurrently, dropping nulls while preserving order. Used to
 * resolve profile handles/DIDs, which typically each hit the network.
 */
private suspend fun <T, R : Any> List<T>.awaitMapNotNull(
    transform: suspend (T) -> R?,
): List<R> = coroutineScope {
    map { item -> async { transform(item) } }
        .awaitAll()
        .filterNotNull()
}

private val MediaEmbedKinds: List<Filter.Attribute.Embed.Kind> = listOf(
    Filter.Attribute.Embed.Kind.Image,
    Filter.Attribute.Embed.Kind.Video,
    Filter.Attribute.Embed.Kind.Gif,
    Filter.Attribute.Embed.Kind.ImageGroup,
)

private val WhitespaceRegex = "\\s+".toRegex()

private val RegexMetaChars = setOf('.', '^', '$', '*', '+', '?', '(', ')', '[', ']', '{', '}', '|', '\\')

internal val TextVariable: String = Filter.Attribute.Compare.Selector.Text.value

private fun replyCompare(
    isReply: Boolean,
): Filter.Attribute.Compare = Filter.Attribute.Compare(
    selector = Filter.Attribute.Compare.Selector.Reply,
    operator = Filter.Comparator.Equality.Equal,
    targetValue = isReply.toString(),
)

private fun Filter.flattenLeaves(): List<Filter.Leaf> = when (this) {
    is Filter.Root -> filters.flatMap(Filter::flattenLeaves)
    is Filter.Leaf -> listOf(this)
}

private fun List<ProfileId>.toPersonGroup(
    mode: SearchFilter.PersonGroup.Mode,
    kind: SearchFilter.PersonGroup.Kind,
): SearchFilter.PersonGroup? = distinct()
    .takeIf(List<ProfileId>::isNotEmpty)
    ?.let { ids ->
        SearchFilter.PersonGroup(
            mode = mode,
            kind = kind,
            profileIds = ids,
        )
    }

private fun List<SearchFilter.PersonGroup>.didsFor(
    mode: SearchFilter.PersonGroup.Mode,
    kind: SearchFilter.PersonGroup.Kind,
): List<String> = asSequence()
    .filter { it.mode == mode && it.kind == kind }
    .flatMap { it.profileIds }
    .map(ProfileId::id)
    .distinct()
    .toList()

private fun List<SearchFilter.PersonGroup>.mentionDids(
    mode: SearchFilter.PersonGroup.Mode,
): List<ProfileId> = asSequence()
    .filter { it.mode == mode && it.kind == SearchFilter.PersonGroup.Kind.Mentions }
    .flatMap { it.profileIds }
    .distinct()
    .toList()

private fun String?.toTermList(): List<String> = this
    ?.trim()
    ?.split(WhitespaceRegex)
    ?.filter(String::isNotBlank)
    ?: emptyList()

private fun String?.nonBlankTrimmed(): String? = this
    ?.trim()
    ?.takeIf(String::isNotEmpty)

private fun String.escapeForRegex(): String = buildString {
    for (character in this@escapeForRegex) {
        if (character in RegexMetaChars) append('\\')
        append(character)
    }
}

private fun String.unescapeRegex(): String {
    val source = this
    return buildString {
        var index = 0
        while (index < source.length) {
            val character = source[index]
            val next = source.getOrNull(index + 1)
            if (character == '\\' && next != null && next in RegexMetaChars) {
                append(next)
                index += 2
            } else {
                append(character)
                index += 1
            }
        }
    }
}

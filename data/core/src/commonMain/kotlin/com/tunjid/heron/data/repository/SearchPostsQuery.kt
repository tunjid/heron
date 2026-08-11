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

package com.tunjid.heron.data.repository

import app.bsky.feed.SearchPostsV2QueryParams
import app.bsky.feed.SearchPostsV2Sort
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.SearchFilter
import com.tunjid.heron.data.core.models.value
import sh.christian.ozone.api.AtIdentifier
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Language

/**
 * Builds the `app.bsky.feed.searchPostsV2` query params from a raw [query] string and an optional
 * [filter]. Shared by `SearchQuery.OfPosts` (the search screen) and [Timeline.Source.Search]
 * (the media gallery) so both compose queries identically.
 */
internal fun searchPostsV2QueryParams(
    query: String,
    filter: SearchFilter?,
    sort: SearchPostsV2Sort,
    limit: Long,
    cursor: Cursor,
): SearchPostsV2QueryParams = SearchPostsV2QueryParams(
    query = composedSearchQueryString(
        query = query,
        filter = filter,
    ).ifBlank { null },
    sort = sort,
    limit = limit,
    cursor = cursor.value,
    authors = filter.atIdentifiers(
        mode = SearchFilter.PersonGroup.Mode.Include,
        kind = SearchFilter.PersonGroup.Kind.Authors,
    ),
    excludeAuthors = filter.atIdentifiers(
        mode = SearchFilter.PersonGroup.Mode.Exclude,
        kind = SearchFilter.PersonGroup.Kind.Authors,
    ),
    mentions = filter.atIdentifiers(
        mode = SearchFilter.PersonGroup.Mode.Include,
        kind = SearchFilter.PersonGroup.Kind.Mentions,
    ),
    excludeMentions = filter.atIdentifiers(
        mode = SearchFilter.PersonGroup.Mode.Exclude,
        kind = SearchFilter.PersonGroup.Kind.Mentions,
    ),
    since = filter?.since?.toString(),
    until = filter?.until?.toString(),
    languages = filter?.language?.let { listOf(Language(it)) },
    hasMedia = (filter?.media == SearchFilter.Media.WithMedia).trueOrNull(),
    hasVideo = (filter?.media == SearchFilter.Media.VideosOnly).trueOrNull(),
    excludeReplies = (filter?.replies == SearchFilter.Replies.PostsOnly).trueOrNull(),
    repliesOnly = (filter?.replies == SearchFilter.Replies.RepliesOnly).trueOrNull(),
    following = (filter?.from == SearchFilter.From.Following).trueOrNull(),
)

/**
 * Whether there is anything meaningful to search for; an empty [query] with an empty [filter]
 * yields no results and should not hit the network.
 */
internal fun searchQueryHasCriteria(
    query: String,
    filter: SearchFilter?,
): Boolean = query.isNotBlank() || filter?.isEmpty == false

internal val SearchFilter.isEmpty: Boolean
    get() = exactPhrase.isNullOrBlank() &&
        noneOfWords.isNullOrBlank() &&
        since == null &&
        until == null &&
        language == null &&
        media == SearchFilter.Media.All &&
        replies == SearchFilter.Replies.PostsAndReplies &&
        from == SearchFilter.From.Anyone &&
        people.all { it.profileIds.isEmpty() }

private fun composedSearchQueryString(
    query: String,
    filter: SearchFilter?,
): String = buildString {
    append(query.trim())
    filter ?: return@buildString
    filter.exactPhrase
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?.let { phrase ->
            appendSeparatorIfNotEmpty()
            append('"').append(phrase).append('"')
        }
    filter.noneOfWords
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?.split(WhitespaceRegex)
        ?.forEach { word ->
            appendSeparatorIfNotEmpty()
            append('-').append(word)
        }
}

private fun StringBuilder.appendSeparatorIfNotEmpty() {
    if (isNotEmpty()) append(' ')
}

private fun SearchFilter?.atIdentifiers(
    mode: SearchFilter.PersonGroup.Mode,
    kind: SearchFilter.PersonGroup.Kind,
): List<AtIdentifier>? =
    this
        ?.people
        ?.asSequence()
        ?.filter { it.mode == mode && it.kind == kind }
        ?.flatMap { it.profileIds }
        ?.distinct()
        ?.map { Did(it.id) }
        ?.toList()
        ?.takeIf(List<AtIdentifier>::isNotEmpty)

private fun Boolean.trueOrNull(): Boolean? = takeIf { it }

private val WhitespaceRegex = "\\s+".toRegex()

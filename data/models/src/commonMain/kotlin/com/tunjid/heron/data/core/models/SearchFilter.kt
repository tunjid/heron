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

import com.tunjid.heron.data.core.types.ProfileId
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Advanced filters for post searches, mapped onto `app.bsky.feed.searchPostsV2`
 * query parameters. All members are optional; an empty [SearchFilter] adds no constraints.
 *
 * Used both by `SearchQuery.OfPosts` and by [Timeline.Source.Search].
 */
@Serializable
data class SearchFilter(
    val exactPhrase: String? = null,
    val noneOfWords: String? = null,
    val since: LocalDate? = null,
    val until: LocalDate? = null,
    val language: String? = null,
    val media: Media = Media.All,
    val replies: Replies = Replies.PostsAndReplies,
    val from: From = From.Anyone,
    val people: List<PersonGroup> = emptyList(),
) {
    @Serializable
    enum class Media { All, WithMedia, VideosOnly }

    @Serializable
    enum class Replies { PostsAndReplies, PostsOnly, RepliesOnly }

    @Serializable
    enum class From { Anyone, Following }

    @Serializable
    data class PersonGroup @OptIn(ExperimentalUuidApi::class) constructor(
        val mode: Mode = Mode.Include,
        val kind: Kind = Kind.Authors,
        val profileIds: List<ProfileId> = emptyList(),
        val id: String = Uuid.random().toString(),
    ) {
        @Serializable
        enum class Mode { Include, Exclude }

        @Serializable
        enum class Kind { Authors, Mentions }
    }
}

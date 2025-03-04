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

import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
sealed class Timeline {

    abstract val sourceId: String

    abstract val lastRefreshed: Instant?

    abstract val presentation: Presentation

    abstract val supportedPresentations: List<Presentation>

    @Serializable
    sealed class Home(
        val source: Uri,
    ) : Timeline() {

        abstract val position: Int

        abstract val name: String

        override val sourceId: String
            get() = source.uri

        @Serializable
        data class Following(
            override val name: String,
            override val position: Int,
            override val lastRefreshed: Instant?,
            override val presentation: Presentation,
            val signedInProfileId: Id,
        ) : Home(
            source = Constants.timelineFeed,
        ) {
            override val supportedPresentations get() = TextAndEmbedOnlyPresentation
        }

        @Serializable
        data class List(
            override val position: Int,
            override val lastRefreshed: Instant?,
            override val presentation: Presentation,
            val feedList: FeedList,
        ) : Home(
            source = feedList.uri,
        ) {
            override val name: String
                get() = feedList.name

            override val supportedPresentations get() = TextAndEmbedOnlyPresentation
        }

        @Serializable
        data class Feed(
            override val position: Int,
            override val lastRefreshed: Instant?,
            override val presentation: Presentation,
            override val supportedPresentations: kotlin.collections.List<Presentation>,
            val feedGenerator: FeedGenerator,
        ) : Home(
            source = feedGenerator.uri,
        ) {
            override val name: String
                get() = feedGenerator.displayName
        }

    }

    @Serializable
    data class Profile(
        val profileId: Id,
        val type: Type,
        override val lastRefreshed: Instant?,
        override val presentation: Presentation,
    ) : Timeline() {

        override val sourceId: String
            get() = type.sourceId(profileId)

        override val supportedPresentations: List<Presentation>
            get() = when (type) {
                Type.Posts -> TextAndEmbedOnlyPresentation
                Type.Replies -> TextAndEmbedOnlyPresentation
                Type.Likes -> TextAndEmbedOnlyPresentation
                Type.Media -> listOf(
                    Presentation.TextAndEmbed,
                    Presentation.CondensedMedia,
                )
                Type.Videos -> listOf(
                    Presentation.TextAndEmbed,
                    Presentation.CondensedMedia,
                )
            }

        enum class Type(
            val suffix: String,
        ) {
            Posts(suffix = "posts"),
            Replies(suffix = "posts-and-replies"),
            Likes(suffix = "likes"),
            Media(suffix = "media"),
            Videos(suffix = "videos");

            fun sourceId(profileId: Id) = "${profileId.id}-$suffix"
        }
    }

    enum class Presentation(
        val key: String,
    ) {
        TextAndEmbed(key = "presentation-text-and-embed"),
        CondensedMedia(key = "presentation-condensed-media"),
    }
}

private val TextAndEmbedOnlyPresentation = listOf(
    Timeline.Presentation.TextAndEmbed
)

sealed class TimelineItem {

    abstract val id: String
    abstract val post: Post

    val indexedAt
        get() = when (this) {
            is Pinned,
            is Thread,
            is Single,
                -> post.indexedAt

            is Repost -> at
        }

    data class Pinned(
        override val id: String,
        override val post: Post,
    ) : TimelineItem()

    data class Repost(
        override val id: String,
        override val post: Post,
        val by: Profile,
        val at: Instant,
    ) : TimelineItem()

    data class Thread(
        override val id: String,
        val anchorPostIndex: Int,
        val posts: List<Post>,
        val generation: Long?,
        val hasBreak: Boolean,
    ) : TimelineItem() {
        override val post: Post
            get() = posts[anchorPostIndex]
    }

    data class Single(
        override val id: String,
        override val post: Post,
    ) : TimelineItem()
}

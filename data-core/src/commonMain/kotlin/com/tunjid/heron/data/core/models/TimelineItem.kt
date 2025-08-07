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

import com.tunjid.heron.data.core.models.Timeline.Presentation.Media
import com.tunjid.heron.data.core.models.Timeline.Presentation.Text
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.Uri
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
sealed interface Timeline {

    val sourceId: String

    val lastRefreshed: Instant?

    val presentation: Presentation

    val supportedPresentations: List<Presentation>

    @Serializable
    sealed class Home(
        val source: Uri,
    ) : Timeline {

        abstract val position: Int

        abstract val name: String

        override val sourceId: String
            get() = source.uri

        abstract val isPinned: Boolean

        @Serializable
        data class Following(
            override val name: String,
            override val position: Int,
            override val lastRefreshed: Instant?,
            override val presentation: Presentation,
            override val isPinned: Boolean,
        ) : Home(
            source = Constants.timelineFeed,
        ) {
            override val supportedPresentations get() = TextOnlyPresentations
        }

        @Serializable
        data class List(
            override val position: Int,
            override val lastRefreshed: Instant?,
            override val presentation: Presentation,
            override val isPinned: Boolean,
            val feedList: FeedList,
        ) : Home(
            source = feedList.uri,
        ) {
            override val name: String
                get() = feedList.name

            override val supportedPresentations get() = TextOnlyPresentations

            companion object {
                fun stub(
                    list: FeedList
                ) = List(
                    position = 0,
                    lastRefreshed = null,
                    presentation = Text.WithEmbed,
                    isPinned = false,
                    feedList = list,
                )
            }
        }

        @Serializable
        data class Feed(
            override val position: Int,
            override val lastRefreshed: Instant?,
            override val presentation: Presentation,
            override val supportedPresentations: kotlin.collections.List<Presentation>,
            override val isPinned: Boolean,
            val feedGenerator: FeedGenerator,
        ) : Home(
            source = feedGenerator.uri,
        ) {
            override val name: String
                get() = feedGenerator.displayName

            companion object {
                fun stub(
                    feedGenerator: FeedGenerator
                ) = Feed(
                    position = 0,
                    lastRefreshed = null,
                    presentation = Text.WithEmbed,
                    supportedPresentations = emptyList(),
                    isPinned = false,
                    feedGenerator = feedGenerator,
                )
            }
        }

    }

    @Serializable
    data class Profile(
        val profileId: ProfileId,
        val type: Type,
        override val lastRefreshed: Instant?,
        override val presentation: Presentation,
    ) : Timeline {

        override val sourceId: String
            get() = type.sourceId(profileId)

        override val supportedPresentations: List<Presentation>
            get() = when (type) {
                Type.Posts -> TextOnlyPresentations
                Type.Replies -> TextOnlyPresentations
                Type.Likes -> TextOnlyPresentations
                Type.Media -> AllPresentations
                Type.Videos -> AllPresentations
            }

        enum class Type(
            val suffix: String,
        ) {
            Posts(suffix = "posts"),
            Replies(suffix = "posts-and-replies"),
            Likes(suffix = "likes"),
            Media(suffix = "media"),
            Videos(suffix = "videos");

            fun sourceId(profileId: ProfileId) = "${profileId.id}-$suffix"
        }
    }

    @Serializable
    data class StarterPack(
        val starterPack: com.tunjid.heron.data.core.models.StarterPack,
        val listTimeline: Home.List,
    ) : Timeline by listTimeline {
        companion object {
            fun stub(
                starterPack: com.tunjid.heron.data.core.models.StarterPack,
                list: FeedList,
            ) = StarterPack(
                starterPack = starterPack,
                listTimeline = Home.List.stub(list),
            )
        }
    }

    @Serializable
    sealed class Presentation(
        val key: String,
    ) {
        sealed class Text {
            data object WithEmbed : Presentation(
                key = "presentation-text-and-embed",
            )
        }

        sealed class Media {
            data object Expanded : Presentation(
                key = "presentation-expanded-media",
            )

            data object Condensed : Presentation(
                key = "presentation-condensed-media",
            )
        }
    }
}

sealed class TimelineItem {

    abstract val id: String
    abstract val post: Post

    val indexedAt
        get() = when (this) {
            is NoContent,
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

    data class NoContent(
        override val id: String,
        override val post: Post,
    ) : TimelineItem()

    data class Single(
        override val id: String,
        override val post: Post,
    ) : TimelineItem()
}

private val TextOnlyPresentations = listOf(
    Text.WithEmbed
)

private val AllPresentations = listOf(
    Text.WithEmbed,
    Media.Expanded,
    Media.Condensed
)
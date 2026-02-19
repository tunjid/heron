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
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordUri
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
sealed interface Timeline {

    val source: Source

    val lastRefreshed: Instant?

    val itemsAvailable: Long

    val presentation: Presentation

    val supportedPresentations: List<Presentation>

    @Serializable
    sealed interface Source : UrlEncodableModel {
        @Serializable sealed interface Reference : Source

        @Serializable data object Following : Reference

        @Serializable
        sealed interface Record : Reference {
            @Serializable data class List(val uri: ListUri) : Record

            @Serializable data class Feed(val uri: FeedGeneratorUri) : Record
        }

        @Serializable
        data class Profile(val profileId: ProfileId, val type: Timeline.Profile.Type) : Source
    }

    @Serializable
    sealed class Home(override val source: Source.Reference) : Timeline {

        abstract val position: Int

        abstract val name: String

        abstract val isPinned: Boolean

        @Serializable
        data class Following(
            override val name: String,
            override val position: Int,
            override val lastRefreshed: Instant?,
            override val itemsAvailable: Long,
            override val presentation: Presentation,
            override val isPinned: Boolean,
        ) : Home(source = Source.Following) {
            override val supportedPresentations
                get() = TextOnlyPresentations
        }

        @Serializable
        data class List(
            override val position: Int,
            override val lastRefreshed: Instant?,
            override val itemsAvailable: Long,
            override val presentation: Presentation,
            override val isPinned: Boolean,
            val feedList: FeedList,
        ) : Home(source = Source.Record.List(feedList.uri)) {
            override val name: String
                get() = feedList.name

            override val supportedPresentations
                get() = TextOnlyPresentations

            companion object {
                fun stub(list: FeedList) =
                    List(
                        position = 0,
                        lastRefreshed = null,
                        itemsAvailable = list.listItemCount ?: 0,
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
            override val itemsAvailable: Long,
            override val presentation: Presentation,
            override val supportedPresentations: kotlin.collections.List<Presentation>,
            override val isPinned: Boolean,
            val feedGenerator: FeedGenerator,
        ) : Home(source = Source.Record.Feed(feedGenerator.uri)) {
            override val name: String
                get() = feedGenerator.displayName

            companion object {
                fun stub(feedGenerator: FeedGenerator) =
                    Feed(
                        position = 0,
                        lastRefreshed = null,
                        itemsAvailable = 0,
                        presentation = Text.WithEmbed,
                        supportedPresentations = emptyList(),
                        isPinned = false,
                        feedGenerator = feedGenerator,
                    )
            }
        }

        enum class Status {
            Pinned,
            Saved,
            None,
        }
    }

    @Serializable
    data class Profile(
        val profileId: ProfileId,
        val type: Type,
        override val lastRefreshed: Instant?,
        override val itemsAvailable: Long,
        override val presentation: Presentation,
    ) : Timeline {

        override val source: Source
            get() = Source.Profile(profileId, type)

        override val supportedPresentations: List<Presentation>
            get() =
                when (type) {
                    Type.Posts -> TextOnlyPresentations
                    Type.Replies -> TextOnlyPresentations
                    Type.Likes -> TextOnlyPresentations
                    Type.Media -> AllPresentations
                    Type.Videos -> AllPresentations
                }

        enum class Type(val suffix: String) {
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
            fun stub(starterPack: com.tunjid.heron.data.core.models.StarterPack, list: FeedList) =
                StarterPack(starterPack = starterPack, listTimeline = Home.List.stub(list))
        }
    }

    @Serializable
    sealed interface Update {

        sealed interface HomeFeed : Update {
            sealed interface Single : HomeFeed {
                val uri: RecordUri
            }

            sealed interface Pin : Single

            sealed interface Save : Single

            sealed interface Remove : Single
        }

        @Serializable data class Bulk(val timelines: List<Home>) : Update, HomeFeed

        @Serializable
        sealed class OfFeedGenerator : Update {

            @Serializable
            data class Pin(override val uri: FeedGeneratorUri) : OfFeedGenerator(), HomeFeed.Pin

            @Serializable
            data class Save(override val uri: FeedGeneratorUri) : OfFeedGenerator(), HomeFeed.Save

            @Serializable
            data class Remove(override val uri: FeedGeneratorUri) :
                OfFeedGenerator(), HomeFeed.Remove
        }

        @Serializable
        sealed class OfList : Update {

            @Serializable data class Pin(override val uri: ListUri) : OfList(), HomeFeed.Pin

            @Serializable data class Save(override val uri: ListUri) : OfList(), HomeFeed.Save

            @Serializable data class Remove(override val uri: ListUri) : OfList(), HomeFeed.Remove
        }

        @Serializable
        sealed class OfContentLabel : Update {
            @Serializable
            data class LabelVisibilityChange(
                val value: Label.Value,
                val labelCreatorId: ProfileId,
                val visibility: Label.Visibility,
            ) : OfContentLabel()

            @Serializable
            data class AdultLabelVisibilityChange(
                val label: Label.Adult,
                val visibility: Label.Visibility,
            ) : OfContentLabel()
        }

        @Serializable
        sealed class OfLabeler : Update {
            @Serializable
            data class Subscription(val labelCreatorId: ProfileId, val subscribed: Boolean) :
                OfLabeler()
        }

        @Serializable data class OfAdultContent(val enabled: Boolean) : Update

        @Serializable
        sealed class OfMutedWord : Update {

            @Serializable
            data class ReplaceAll(val mutedWordPreferences: List<MutedWordPreference>) :
                OfMutedWord()
        }

        @Serializable
        data class OfInteractionSettings(val preference: PostInteractionSettingsPreference) : Update
    }

    @Serializable
    sealed class Presentation {
        abstract val key: String

        @Serializable
        sealed class Text(override val key: String) : Presentation() {
            @Serializable data object WithEmbed : Text(key = "presentation-text-and-embed")
        }

        @Serializable
        sealed class Media(override val key: String) : Presentation() {
            @Serializable data object Expanded : Media(key = "presentation-expanded-media")

            @Serializable data object Condensed : Media(key = "presentation-condensed-media")

            @Serializable data object Grid : Media(key = "presentation-grid-media")
        }
    }
}

private val TextOnlyPresentations: List<Timeline.Presentation> = listOf(Text.WithEmbed)

private val AllPresentations: List<Timeline.Presentation> =
    listOf(Text.WithEmbed, Media.Expanded, Media.Condensed, Media.Grid)

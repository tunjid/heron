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
import com.tunjid.heron.data.core.models.Timeline.Source
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.core.types.Uri
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
sealed interface Timeline {

    val source: Source

    val lastRefreshed: Instant?

    val presentation: Presentation

    val supportedPresentations: List<Presentation>

    @Serializable
    sealed interface Source : UrlEncodableModel {
        @Serializable
        sealed interface Reference : Source

        @Serializable
        data object Following : Reference

        @Serializable
        sealed interface Record : Reference {
            @Serializable
            data class List(
                val uri: ListUri,
            ) : Record

            @Serializable
            data class Feed(
                val uri: FeedGeneratorUri,
            ) : Record
        }

        @Serializable
        data class Profile(
            val profileId: ProfileId,
            val type: Timeline.Profile.Type,
        ) : Source
    }

    @Serializable
    sealed class Home(
        override val source: Source.Reference,
    ) : Timeline {

        abstract val position: Int

        abstract val name: String

        abstract val isPinned: Boolean

        @Serializable
        data class Following(
            override val name: String,
            override val position: Int,
            override val lastRefreshed: Instant?,
            override val presentation: Presentation,
            override val isPinned: Boolean,
        ) : Home(
            source = Source.Following,
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
            source = Source.Record.List(feedList.uri),
        ) {
            override val name: String
                get() = feedList.name

            override val supportedPresentations get() = TextOnlyPresentations

            companion object {
                fun stub(
                    list: FeedList,
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
            source = Source.Record.Feed(feedGenerator.uri),
        ) {
            override val name: String
                get() = feedGenerator.displayName

            companion object {
                fun stub(
                    feedGenerator: FeedGenerator,
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
        override val presentation: Presentation,
    ) : Timeline {

        override val source: Source
            get() = Source.Profile(profileId, type)

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
            Videos(suffix = "videos"),
            ;

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
    sealed interface Update {

        sealed interface HomeFeed : Update {
            sealed interface Single : HomeFeed {
                val uri: RecordUri
            }

            sealed interface Pin : Single
            sealed interface Save : Single
            sealed interface Remove : Single
        }

        @Serializable
        data class Bulk(
            val timelines: List<Home>,
        ) : Update,
            HomeFeed

        @Serializable
        sealed class OfFeedGenerator : Update {

            @Serializable
            data class Pin(
                override val uri: FeedGeneratorUri,
            ) : OfFeedGenerator(),
                HomeFeed.Pin

            @Serializable
            data class Save(
                override val uri: FeedGeneratorUri,
            ) : OfFeedGenerator(),
                HomeFeed.Save

            @Serializable
            data class Remove(
                override val uri: FeedGeneratorUri,
            ) : OfFeedGenerator(),
                HomeFeed.Remove
        }

        @Serializable
        sealed class OfList : Update {

            @Serializable
            data class Pin(
                override val uri: ListUri,
            ) : OfList(),
                HomeFeed.Pin

            @Serializable
            data class Save(
                override val uri: ListUri,
            ) : OfList(),
                HomeFeed.Save

            @Serializable
            data class Remove(
                override val uri: ListUri,
            ) : OfList(),
                HomeFeed.Remove
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
            data class Subscription(
                val labelCreatorId: ProfileId,
                val subscribed: Boolean,
            ) : OfLabeler()
        }

        @Serializable
        data class OfAdultContent(
            val enabled: Boolean,
        ) : Update

        @Serializable
        sealed class OfMutedWord : Update {

            @Serializable
            data class ReplaceAll(
                val mutedWordPreferences: List<MutedWordPreference>,
            ) : OfMutedWord()
        }

        @Serializable
        data class OfInteractionSettings(
            val preference: PostInteractionSettingsPreference,
        ) : Update
    }

    @Serializable
    sealed class Presentation {
        abstract val key: String

        @Serializable
        sealed class Text(
            override val key: String,
        ) : Presentation() {
            @Serializable
            data object WithEmbed : Text(
                key = "presentation-text-and-embed",
            )
        }

        @Serializable
        sealed class Media(
            override val key: String,
        ) : Presentation() {
            @Serializable
            data object Expanded : Media(
                key = "presentation-expanded-media",
            )

            @Serializable
            data object Condensed : Media(
                key = "presentation-condensed-media",
            )

            @Serializable
            data object Grid : Media(
                key = "presentation-grid-media",
            )
        }
    }
}

val Source.id
    get() = when (this) {
        Source.Following -> Constants.timelineFeed.uri
        is Source.Profile -> type.sourceId(profileId)
        is Source.Record.Feed -> uri.uri
        is Source.Record.List -> uri.uri
    }

val Timeline.sourceId: String
    get() = source.id

val Timeline.Home.uri: Uri
    get() = when (val source = source) {
        Source.Following -> Constants.timelineFeed
        is Source.Record.Feed -> source.uri
        is Source.Record.List -> source.uri
    }

val Timeline.uri: Uri?
    get() = when (this) {
        is Timeline.Home -> uri
        is Timeline.Profile -> null
        is Timeline.StarterPack -> listTimeline.uri
    }

sealed class TimelineItem {

    abstract val id: String
    abstract val post: Post
    abstract val isMuted: Boolean
    abstract val threadGate: ThreadGate?
    abstract val appliedLabels: AppliedLabels
    abstract val signedInProfileId: ProfileId?

    val indexedAt
        get() = when (this) {
            is Pinned,
            is Thread,
            is Single,
            is Loading,
            -> post.indexedAt

            is Repost -> at
        }

    data class Pinned(
        override val id: String,
        override val post: Post,
        override val isMuted: Boolean,
        override val threadGate: ThreadGate?,
        override val appliedLabels: AppliedLabels,
        override val signedInProfileId: ProfileId?,
    ) : TimelineItem()

    data class Repost(
        override val id: String,
        override val post: Post,
        override val isMuted: Boolean,
        override val threadGate: ThreadGate?,
        override val appliedLabels: AppliedLabels,
        override val signedInProfileId: ProfileId?,
        val by: Profile,
        val at: Instant,
    ) : TimelineItem()

    data class Thread(
        override val id: String,
        override val isMuted: Boolean,
        override val appliedLabels: AppliedLabels,
        override val signedInProfileId: ProfileId?,
        val anchorPostIndex: Int,
        val posts: List<Post>,
        val postUrisToThreadGates: Map<PostUri, ThreadGate?>,
        val generation: Long?,
        val hasBreak: Boolean,
    ) : TimelineItem() {
        override val post: Post
            get() = posts[anchorPostIndex]
        override val threadGate: ThreadGate?
            get() = postUrisToThreadGates[post.uri]
    }

    data class Single(
        override val id: String,
        override val post: Post,
        override val isMuted: Boolean,
        override val threadGate: ThreadGate?,
        override val appliedLabels: AppliedLabels,
        override val signedInProfileId: ProfileId?,
    ) : TimelineItem()

    data class Loading @OptIn(ExperimentalUuidApi::class) constructor(
        override val id: String = Uuid.random().toString(),
    ) : TimelineItem() {

        override val post: Post
            get() = LoadingPost

        override val isMuted: Boolean = false
        override val threadGate: ThreadGate? = null
        override val appliedLabels: AppliedLabels = LoadingAppliedLabels
        override val signedInProfileId: ProfileId? = null
    }

    companion object {

        private val LoadingPost = Post(
            cid = Constants.blockedPostId,
            uri = Constants.unknownPostUri,
            author = stubProfile(
                did = Constants.unknownAuthorId,
                handle = Constants.unknownAuthorHandle,
            ),
            replyCount = 0,
            repostCount = 0,
            likeCount = 0,
            quoteCount = 0,
            bookmarkCount = 0,
            indexedAt = Instant.DISTANT_PAST,
            embed = null,
            record = null,
            viewerStats = null,
            labels = emptyList(),
            embeddedRecord = null,
            viewerState = null,
        )

        private val LoadingAppliedLabels = AppliedLabels(
            adultContentEnabled = false,
            labels = emptyList(),
            labelers = emptyList(),
            preferenceLabelsVisibilityMap = emptyMap(),
        )

        val LoadingItems = (0..16).map { Loading() }
    }
}

private val TextOnlyPresentations: List<Timeline.Presentation> = listOf(
    Text.WithEmbed,
)

private val AllPresentations: List<Timeline.Presentation> = listOf(
    Text.WithEmbed,
    Media.Expanded,
    Media.Condensed,
    Media.Grid,
)

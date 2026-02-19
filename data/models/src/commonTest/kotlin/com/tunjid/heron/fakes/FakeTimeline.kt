package com.tunjid.heron.fakes

import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.data.core.models.Labeler
import com.tunjid.heron.data.core.models.Labelers
import com.tunjid.heron.data.core.models.StarterPack
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.stubProfile
import com.tunjid.heron.data.core.types.FeedGeneratorId
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.LabelerId
import com.tunjid.heron.data.core.types.LabelerUri
import com.tunjid.heron.data.core.types.ListId
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.StarterPackId
import com.tunjid.heron.data.core.types.StarterPackUri
import kotlin.time.Instant

object FakeTimeline {

    val following =
        Timeline.Home.Following(
            name = "Following",
            position = 0,
            lastRefreshed = Instant.parse("2024-01-01T00:00:00Z"),
            itemsAvailable = 0,
            presentation = Timeline.Presentation.Text.WithEmbed,
            isPinned = false,
        )

    val profileTimeline =
        Timeline.Profile(
            profileId = ProfileId("did:example:123"),
            type = Timeline.Profile.Type.Posts,
            lastRefreshed = Instant.parse("2024-01-02T00:00:00Z"),
            itemsAvailable = 0,
            presentation = Timeline.Presentation.Text.WithEmbed,
        )

    val sampleLabels: List<Label> =
        listOf(
            Label(
                uri = GenericUri("at://label/1"),
                creatorId = ProfileId("did:example:creator-1"),
                value = Label.Value("spoiler"),
                version = 1L,
                createdAt = Instant.parse("2024-01-01T00:00:00Z"),
            ),
            Label(
                uri = GenericUri("at://label/2"),
                creatorId = ProfileId("did:example:creator-2"),
                value = Label.Value("nsfw"),
                version = 1L,
                createdAt = Instant.parse("2024-01-01T01:00:00Z"),
            ),
            Label(
                uri = GenericUri("at://label/3"),
                creatorId = ProfileId("did:example:creator-3"),
                value = Label.Value("funny"),
                version = 1L,
                createdAt = Instant.parse("2024-01-01T02:00:00Z"),
            ),
        )

    // Optional: corresponding labelers if you need them
    val sampleLabelers: Labelers =
        listOf(
            Labeler(
                uri = LabelerUri("at://labeler/1"),
                cid = LabelerId("bafyreiefgzsaskjfvsrs2ngyvreueldnavzuqlkb4rcdevbskpfpslderd"),
                likeCount = 1,
                creator =
                    stubProfile(ProfileId("did:example:labeler-1"), ProfileHandle("labeler-1")),
                definitions =
                    listOf(
                        Label.Definition(
                            adultOnly = true,
                            blurs = Label.BlurTarget.Media,
                            defaultSetting = Label.Visibility.Hide,
                            identifier = Label.Value("spoiler"),
                            severity = Label.Severity.Alert,
                        )
                    ),
                values = listOf(Label.Value("spoiler")),
            ),
            Labeler(
                uri = LabelerUri("at://labeler/2"),
                cid = LabelerId("bafyreiefgzsaskjfvsrs2ngyvreueldnavzuqlkb4rcdevbskpfpslderu"),
                likeCount = null,
                creator =
                    stubProfile(ProfileId("did:example:labeler-2"), ProfileHandle("labeler-2")),
                definitions =
                    listOf(
                        Label.Definition(
                            adultOnly = true,
                            blurs = Label.BlurTarget.Content,
                            defaultSetting = Label.Visibility.Warn,
                            identifier = Label.Value("nsfw"),
                            severity = Label.Severity.Alert,
                        )
                    ),
                values = listOf(Label.Value("nsfw")),
            ),
        )

    val feedList =
        FeedList(
            cid = ListId("list-1"),
            uri = ListUri("at://feed/list/1"),
            creator = sampleProfile(),
            name = "My List",
            description = "A test feed list for serialization",
            avatar = ImageUri("at://image/avatar/list-1"),
            listItemCount = 12L,
            purpose = "curation",
            indexedAt = Instant.parse("2024-01-01T00:00:00Z"),
            labels = sampleLabels,
        )

    val listTimeline = Timeline.Home.List.stub(feedList)

    val sampleFeedGenerator: FeedGenerator =
        FeedGenerator(
            cid = FeedGeneratorId("fg-1"),
            did = ProfileId("fg-1"),
            uri = FeedGeneratorUri("at://feed/generator/1"),
            avatar = ImageUri("at://image/avatar/fgenerator-1"),
            likeCount = 42L,
            creator = sampleProfile(),
            displayName = "Sample Feed Generator",
            description = "This is a sample feed generator for testing",
            contentMode = "default",
            acceptsInteractions = true,
            indexedAt = Instant.parse("2024-01-01T00:00:00Z"),
            labels = sampleLabels,
        )

    val sampleStarterPack =
        StarterPack(
            cid = StarterPackId("starter-1"),
            uri = StarterPackUri("at://starter/1"),
            name = "Starter Pack",
            description = "A dummy starter pack for testing",
            creator = sampleProfile(),
            list = feedList,
            joinedWeekCount = 3L,
            joinedAllTimeCount = 10L,
            indexedAt = Instant.parse("2024-01-01T00:00:00Z"),
            labels = sampleLabels,
        )

    val starterPackTimeline = Timeline.StarterPack.stub(sampleStarterPack, feedList)

    val feedTimeline = Timeline.Home.Feed.stub(sampleFeedGenerator)

    val all: List<Timeline> =
        listOf(following, listTimeline, feedTimeline, profileTimeline, starterPackTimeline)
}

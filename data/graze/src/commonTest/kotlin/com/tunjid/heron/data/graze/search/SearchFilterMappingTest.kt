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
import com.tunjid.heron.data.graze.search.Graze.FeedFromSearch.MappingNote
import com.tunjid.heron.data.graze.serializers.RootFilterSerializer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

// Filter.* data classes include a random id in equals(), so assertions target types + fields,
// never whole-object equality.
class SearchFilterMappingTest {

    // region forward: SearchFilter -> Filter.Root

    @Test
    fun freeTextQuery_becomesCaseInsensitiveRegexAnyOnText() = runTest {
        val result = SearchFilter().toFeedFilter(
            query = "birds nature",
            resolveHandle = NoHandles,
        )

        val any = assertIs<Filter.Regex.Any>(
            value = result.filter.filters.single(),
        )
        assertEquals(
            expected = TextVariable,
            actual = any.variable,
        )
        assertContentEquals(
            expected = listOf("birds", "nature"),
            actual = any.terms,
        )
        assertTrue(
            actual = any.isCaseInsensitive,
        )
        assertTrue(
            actual = MappingNote.FreeTextApproximated in result.notes,
        )
    }

    @Test
    fun hashtagQuery_becomesHashtagEntityNotRegex() = runTest {
        val result = SearchFilter().toFeedFilter(
            query = "#birds",
            resolveHandle = NoHandles,
        )

        val entity = assertIs<Filter.Entity.Matches>(
            value = result.filter.filters.single(),
        )
        assertEquals(
            expected = Filter.Entity.Type.Hashtags,
            actual = entity.entityType,
        )
        assertContentEquals(
            expected = listOf("birds"),
            actual = entity.values,
        )
        // Hashtag matching is exact, not an approximation.
        assertFalse(
            actual = MappingNote.FreeTextApproximated in result.notes,
        )
    }

    @Test
    fun mixedQuery_splitsHashtagsFromWords() = runTest {
        val result = SearchFilter().toFeedFilter(
            query = "birds #nature",
            resolveHandle = NoHandles,
        )

        val regex = result.filter.filters.filterIsInstance<Filter.Regex.Any>().single()
        val hashtags = result.filter.filters.filterIsInstance<Filter.Entity.Matches>().single()
        assertContentEquals(
            expected = listOf("birds"),
            actual = regex.terms,
        )
        assertEquals(
            expected = Filter.Entity.Type.Hashtags,
            actual = hashtags.entityType,
        )
        assertContentEquals(
            expected = listOf("nature"),
            actual = hashtags.values,
        )
    }

    @Test
    fun exactPhrase_becomesRegexMatchesWithMetacharactersEscaped() = runTest {
        val result = filterOf(exactPhrase = "c++ (beta)").toFeedFilter(
            query = "",
            resolveHandle = NoHandles,
        )

        val matches = assertIs<Filter.Regex.Matches>(
            value = result.filter.filters.single(),
        )
        assertEquals(
            expected = TextVariable,
            actual = matches.variable,
        )
        assertEquals(
            expected = """c\+\+ \(beta\)""",
            actual = matches.pattern,
        )
        assertTrue(
            actual = matches.isCaseInsensitive,
        )
    }

    @Test
    fun noneOfWords_becomeRegexNoneTerms() = runTest {
        val result = filterOf(noneOfWords = "spam  ads").toFeedFilter(
            query = "",
            resolveHandle = NoHandles,
        )

        val none = assertIs<Filter.Regex.None>(
            value = result.filter.filters.single(),
        )
        assertEquals(
            expected = TextVariable,
            actual = none.variable,
        )
        assertContentEquals(
            expected = listOf("spam", "ads"),
            actual = none.terms,
        )
    }

    @Test
    fun language_becomesEntityLangsMatch() = runTest {
        val result = filterOf(language = "en").toFeedFilter(
            query = "",
            resolveHandle = NoHandles,
        )

        val entity = assertIs<Filter.Entity.Matches>(
            value = result.filter.filters.single(),
        )
        assertEquals(
            expected = Filter.Entity.Type.Languages,
            actual = entity.entityType,
        )
        assertContentEquals(
            expected = listOf("en"),
            actual = entity.values,
        )
    }

    @Test
    fun videosOnly_becomesSingleVideoEmbed() = runTest {
        val result = filterOf(media = SearchFilter.Media.VideosOnly).toFeedFilter(
            query = "",
            resolveHandle = NoHandles,
        )

        val embed = assertIs<Filter.Attribute.Embed>(
            value = result.filter.filters.single(),
        )
        assertEquals(
            expected = Filter.Comparator.Equality.Equal,
            actual = embed.operator,
        )
        assertEquals(
            expected = Filter.Attribute.Embed.Kind.Video,
            actual = embed.embedType,
        )
        assertFalse(
            actual = MappingNote.MediaApproximated in result.notes,
        )
    }

    @Test
    fun withMedia_becomesOrOfMediaEmbeds() = runTest {
        val result = filterOf(media = SearchFilter.Media.WithMedia).toFeedFilter(
            query = "",
            resolveHandle = NoHandles,
        )

        val or = assertIs<Filter.Or>(
            value = result.filter.filters.single(),
        )
        val kinds = or.filters.map {
            assertIs<Filter.Attribute.Embed>(
                value = it,
            ).embedType
        }
        assertContentEquals(
            expected = listOf(
                Filter.Attribute.Embed.Kind.Image,
                Filter.Attribute.Embed.Kind.Video,
                Filter.Attribute.Embed.Kind.Gif,
                Filter.Attribute.Embed.Kind.ImageGroup,
            ),
            actual = kinds,
        )
        assertTrue(
            actual = MappingNote.MediaApproximated in result.notes,
        )
    }

    @Test
    fun postsOnly_becomesReplyCompareFalse() = runTest {
        val result = filterOf(replies = SearchFilter.Replies.PostsOnly).toFeedFilter(
            query = "",
            resolveHandle = NoHandles,
        )

        val compare = assertIs<Filter.Attribute.Compare>(
            value = result.filter.filters.single(),
        )
        assertEquals(
            expected = Filter.Attribute.Compare.Selector.Reply,
            actual = compare.selector,
        )
        assertEquals(
            expected = "false",
            actual = compare.targetValue,
        )
        assertTrue(
            actual = MappingNote.RepliesApproximated in result.notes,
        )
    }

    @Test
    fun repliesOnly_becomesReplyCompareTrue() = runTest {
        val result = filterOf(replies = SearchFilter.Replies.RepliesOnly).toFeedFilter(
            query = "",
            resolveHandle = NoHandles,
        )

        val compare = assertIs<Filter.Attribute.Compare>(
            value = result.filter.filters.single(),
        )
        assertEquals(
            expected = "true",
            actual = compare.targetValue,
        )
    }

    @Test
    fun authors_aggregateAcrossGroupsIntoSocialListByDid() = runTest {
        val result = filterOf(
            people = listOf(
                personGroup(
                    SearchFilter.PersonGroup.Mode.Include,
                    SearchFilter.PersonGroup.Kind.Authors,
                    "did:plc:a",
                ),
                personGroup(
                    SearchFilter.PersonGroup.Mode.Include,
                    SearchFilter.PersonGroup.Kind.Authors,
                    "did:plc:b",
                ),
                personGroup(
                    SearchFilter.PersonGroup.Mode.Exclude,
                    SearchFilter.PersonGroup.Kind.Authors,
                    "did:plc:c",
                ),
            ),
        ).toFeedFilter(
            query = "",
            resolveHandle = NoHandles,
        )

        val lists = result.filter.filters.map {
            assertIs<Filter.Social.UserList>(
                value = it,
            )
        }
        val include = lists.single { it.operator == Filter.Comparator.Set.In }
        val exclude = lists.single { it.operator == Filter.Comparator.Set.NotIn }
        assertContentEquals(
            expected = listOf("did:plc:a", "did:plc:b"),
            actual = include.dids,
        )
        assertContentEquals(
            expected = listOf("did:plc:c"),
            actual = exclude.dids,
        )
    }

    @Test
    fun mentions_resolveDidsToHandlesForEntityFilters() = runTest {
        val result = filterOf(
            people = listOf(
                personGroup(
                    SearchFilter.PersonGroup.Mode.Include,
                    SearchFilter.PersonGroup.Kind.Mentions,
                    "did:plc:a",
                ),
                personGroup(
                    SearchFilter.PersonGroup.Mode.Exclude,
                    SearchFilter.PersonGroup.Kind.Mentions,
                    "did:plc:b",
                ),
            ),
        ).toFeedFilter(
            query = "",
            resolveHandle = handles(
                "did:plc:a" to "alice.test",
                "did:plc:b" to "bob.test",
            ),
        )

        val matches = result.filter.filters.filterIsInstance<Filter.Entity.Matches>().single()
        val excludes = result.filter.filters.filterIsInstance<Filter.Entity.Excludes>().single()
        assertEquals(
            expected = Filter.Entity.Type.Mentions,
            actual = matches.entityType,
        )
        assertContentEquals(
            expected = listOf("alice.test"),
            actual = matches.values,
        )
        assertContentEquals(
            expected = listOf("bob.test"),
            actual = excludes.values,
        )
    }

    @Test
    fun mentions_withUnresolvableDid_produceNoLeaf() = runTest {
        val result = filterOf(
            people = listOf(
                personGroup(
                    SearchFilter.PersonGroup.Mode.Include,
                    SearchFilter.PersonGroup.Kind.Mentions,
                    "did:plc:ghost",
                ),
            ),
        ).toFeedFilter(
            query = "",
            resolveHandle = NoHandles,
        )

        assertTrue(
            actual = result.filter.filters.isEmpty(),
        )
    }

    @Test
    fun fromFollowing_withViewerHandle_becomesSocialGraphOnViewerFollows() = runTest {
        val result = SearchFilter(from = SearchFilter.From.Following).toFeedFilter(
            query = "",
            viewerHandle = ProfileHandle("me.test"),
            resolveHandle = NoHandles,
        )

        val graph = assertIs<Filter.Social.Graph>(
            value = result.filter.filters.single(),
        )
        assertEquals(
            expected = "me.test",
            actual = graph.username,
        )
        assertEquals(
            expected = Filter.Comparator.Set.In,
            actual = graph.operator,
        )
        assertEquals(
            expected = Filter.Social.Graph.Direction.Following,
            actual = graph.direction,
        )
    }

    @Test
    fun fromFollowing_withoutViewerHandle_producesNoLeaf() = runTest {
        val result = SearchFilter(from = SearchFilter.From.Following).toFeedFilter(
            query = "",
            viewerHandle = null,
            resolveHandle = NoHandles,
        )

        assertTrue(
            actual = result.filter.filters.isEmpty(),
        )
    }

    @Test
    fun sinceAndUntil_areNeverMapped() = runTest {
        val result = filterOf(
            since = LocalDateStub,
            until = LocalDateStub,
        ).toFeedFilter(
            query = "",
            resolveHandle = NoHandles,
        )

        assertTrue(
            actual = result.filter.filters.isEmpty(),
        )
        assertTrue(
            actual = result.notes.isEmpty(),
        )
    }

    @Test
    fun emptySearch_producesEmptyInvalidRoot() = runTest {
        val result = SearchFilter().toFeedFilter(
            query = "  ",
            resolveHandle = NoHandles,
        )

        assertTrue(
            actual = result.filter.filters.isEmpty(),
        )
        assertFalse(
            actual = result.filter.isValid,
        )
    }

    @Test
    fun anyNonEmptyCriterion_producesValidRoot() = runTest {
        val result = SearchFilter().toFeedFilter(
            query = "birds",
            resolveHandle = NoHandles,
        )

        assertTrue(
            actual = result.filter.isValid,
        )
    }

    @Test
    fun nullFilterWithQuery_mapsOnlyTheQuery() = runTest {
        val result = NullFilter.toFeedFilter(
            query = "birds",
            resolveHandle = NoHandles,
        )

        val any = assertIs<Filter.Regex.Any>(
            value = result.filter.filters.single(),
        )
        assertContentEquals(
            expected = listOf("birds"),
            actual = any.terms,
        )
    }

    @Test
    fun nullFilterWithBlankQuery_producesEmptyInvalidRoot() = runTest {
        val result = NullFilter.toFeedFilter(
            query = "  ",
            resolveHandle = NoHandles,
        )

        assertTrue(
            actual = result.filter.filters.isEmpty(),
        )
        assertFalse(
            actual = result.filter.isValid,
        )
    }

    @Test
    fun nullFilterWithMultiWordQuery_splitsIntoRegexTerms() = runTest {
        val result = NullFilter.toFeedFilter(
            query = "birds nature vibes",
            resolveHandle = NoHandles,
        )

        val any = assertIs<Filter.Regex.Any>(
            value = result.filter.filters.single(),
        )
        assertContentEquals(
            expected = listOf("birds", "nature", "vibes"),
            actual = any.terms,
        )
        assertTrue(
            actual = MappingNote.FreeTextApproximated in result.notes,
        )
    }

    @Test
    fun nullFilterWithViewerHandle_doesNotAddFollowingGraph() = runTest {
        val result = NullFilter.toFeedFilter(
            query = "birds",
            viewerHandle = ProfileHandle("me.test"),
            resolveHandle = NoHandles,
        )

        assertTrue(
            actual = result.filter.filters.none { it is Filter.Social.Graph },
        )
    }

    @Test
    fun overlappingApproximations_deduplicateNotes() = runTest {
        val result = SearchFilter(exactPhrase = "hello world").toFeedFilter(
            query = "birds",
            resolveHandle = NoHandles,
        )

        assertEquals(
            expected = listOf(MappingNote.FreeTextApproximated),
            actual = result.notes,
        )
    }

    // endregion

    // region reverse: Filter.Root -> SearchFilter

    @Test
    fun socialList_becomesAuthorPeopleGroups() = runTest {
        val root = Filter.And(
            filters = listOf(
                Filter.Social.UserList(
                    dids = listOf("did:plc:a"),
                    operator = Filter.Comparator.Set.In,
                ),
                Filter.Social.UserList(
                    dids = listOf("did:plc:b"),
                    operator = Filter.Comparator.Set.NotIn,
                ),
            ),
        )

        val result = root.toSearchApproximation()

        val include = result.filter.people.single { it.mode == SearchFilter.PersonGroup.Mode.Include }
        val exclude = result.filter.people.single { it.mode == SearchFilter.PersonGroup.Mode.Exclude }
        assertEquals(
            expected = SearchFilter.PersonGroup.Kind.Authors,
            actual = include.kind,
        )
        assertContentEquals(
            expected = listOf(ProfileId("did:plc:a")),
            actual = include.profileIds,
        )
        assertContentEquals(
            expected = listOf(ProfileId("did:plc:b")),
            actual = exclude.profileIds,
        )
        assertTrue(
            actual = result.droppedLeaves.isEmpty(),
        )
    }

    @Test
    fun entityMentions_resolveHandlesBackToDids() = runTest {
        val root = Filter.And(
            filters = listOf(
                Filter.Entity.Matches(
                    entityType = Filter.Entity.Type.Mentions,
                    values = listOf("alice.test", "ghost.test"),
                ),
            ),
        )

        val result = root.toSearchApproximation(dids("alice.test" to "did:plc:a"))

        val mentions = result.filter.people.single()
        assertEquals(
            expected = SearchFilter.PersonGroup.Kind.Mentions,
            actual = mentions.kind,
        )
        assertContentEquals(
            expected = listOf(ProfileId("did:plc:a")),
            actual = mentions.profileIds,
        )
    }

    @Test
    fun hashtagEntity_becomesHashtagQueryTerms() = runTest {
        val root = Filter.And(
            filters = listOf(
                Filter.Entity.Matches(
                    entityType = Filter.Entity.Type.Hashtags,
                    values = listOf("birds", "nature"),
                ),
            ),
        )

        val result = root.toSearchApproximation()

        assertEquals(
            expected = "#birds #nature",
            actual = result.query,
        )
        assertTrue(
            actual = result.droppedLeaves.isEmpty(),
        )
    }

    @Test
    fun textRegex_becomeQueryPhraseAndExcludes() = runTest {
        val root = Filter.And(
            filters = listOf(
                Filter.Regex.Any(
                    variable = TextVariable,
                    terms = listOf("birds"),
                    isCaseInsensitive = true,
                ),
                Filter.Regex.Matches(
                    variable = TextVariable,
                    pattern = """good\+vibes""",
                    isCaseInsensitive = true,
                ),
                Filter.Regex.None(
                    variable = TextVariable,
                    terms = listOf("spam"),
                    isCaseInsensitive = true,
                ),
            ),
        )

        val result = root.toSearchApproximation()

        assertEquals(
            expected = "birds",
            actual = result.query,
        )
        assertEquals(
            expected = "good+vibes",
            actual = result.filter.exactPhrase,
        )
        assertEquals(
            expected = "spam",
            actual = result.filter.noneOfWords,
        )
    }

    @Test
    fun singleVideoEmbed_becomesVideosOnly() = runTest {
        val root = Filter.And(
            filters = listOf(
                Filter.Attribute.Embed(
                    operator = Filter.Comparator.Equality.Equal,
                    embedType = Filter.Attribute.Embed.Kind.Video,
                ),
            ),
        )

        assertEquals(
            expected = SearchFilter.Media.VideosOnly,
            actual = root.toSearchApproximation().filter.media,
        )
    }

    @Test
    fun mixedMediaEmbeds_becomeWithMedia() = runTest {
        val root = Filter.Or(
            filters = listOf(
                Filter.Attribute.Embed(
                    operator = Filter.Comparator.Equality.Equal,
                    embedType = Filter.Attribute.Embed.Kind.Image,
                ),
                Filter.Attribute.Embed(
                    operator = Filter.Comparator.Equality.Equal,
                    embedType = Filter.Attribute.Embed.Kind.Video,
                ),
            ),
        )

        // An or-root is flattened to and; media embeds still collapse to WithMedia.
        assertEquals(
            expected = SearchFilter.Media.WithMedia,
            actual = root.toSearchApproximation().filter.media,
        )
    }

    @Test
    fun socialGraphFollowing_becomesFromFollowing() = runTest {
        val root = Filter.And(
            filters = listOf(
                Filter.Social.Graph(
                    username = "me.test",
                    operator = Filter.Comparator.Set.In,
                    direction = Filter.Social.Graph.Direction.Following,
                ),
            ),
        )

        assertEquals(
            expected = SearchFilter.From.Following,
            actual = root.toSearchApproximation().filter.from,
        )
    }

    @Test
    fun unmappableLeaves_areReportedAsDropped() = runTest {
        val root = Filter.And(
            filters = listOf(
                Filter.ML.Moderation.empty(),
                Filter.Analysis.Sentiment.empty(),
                Filter.Social.StarterPack.empty(),
                Filter.Regex.Any(
                    variable = "embed",
                    terms = listOf("x"),
                    isCaseInsensitive = false,
                ),
            ),
        )

        val result = root.toSearchApproximation()

        assertEquals(
            expected = 4,
            actual = result.droppedLeaves.size,
        )
        assertTrue(
            actual = result.droppedLeaves.any { it is Filter.ML.Moderation },
        )
        assertTrue(
            actual = result.droppedLeaves.any { it is Filter.Analysis.Sentiment },
        )
    }

    // endregion

    // region round-trip + serialization

    @Test
    fun roundTrip_preservesStructurallyMappableSubset() = runTest {
        val original = SearchFilter(
            noneOfWords = "spam",
            language = "en",
            people = listOf(
                personGroup(
                    SearchFilter.PersonGroup.Mode.Include,
                    SearchFilter.PersonGroup.Kind.Authors,
                    "did:plc:a",
                ),
                personGroup(
                    SearchFilter.PersonGroup.Mode.Include,
                    SearchFilter.PersonGroup.Kind.Mentions,
                    "did:plc:b",
                ),
            ),
        )
        val forward = original.toFeedFilter(
            query = "",
            resolveHandle = handles("did:plc:b" to "bob.test"),
        )

        val back = forward.filter.toSearchApproximation(dids("bob.test" to "did:plc:b")).filter

        assertEquals(
            expected = "spam",
            actual = back.noneOfWords,
        )
        assertEquals(
            expected = "en",
            actual = back.language,
        )
        assertContentEquals(
            expected = listOf(ProfileId("did:plc:a")),
            actual = back.people.single { it.kind == SearchFilter.PersonGroup.Kind.Authors }.profileIds,
        )
        assertContentEquals(
            expected = listOf(ProfileId("did:plc:b")),
            actual = back.people.single { it.kind == SearchFilter.PersonGroup.Kind.Mentions }.profileIds,
        )
    }

    @Test
    fun forwardFilter_survivesGrazeSerialization() = runTest {
        val feed = SearchFilter(
            noneOfWords = "spam",
            language = "en",
            media = SearchFilter.Media.WithMedia,
            people = listOf(
                personGroup(
                    SearchFilter.PersonGroup.Mode.Include,
                    SearchFilter.PersonGroup.Kind.Authors,
                    "did:plc:a",
                ),
            ),
        ).toFeedFilter(
            query = "birds",
            viewerHandle = ProfileHandle("me.test"),
            resolveHandle = NoHandles,
        )

        val encoded = Json.encodeToString(
            RootFilterSerializer,
            feed.filter,
        )
        val decoded = Json.decodeFromString(
            RootFilterSerializer,
            encoded,
        )

        assertEquals(
            expected = feed.filter.filters.size,
            actual = decoded.filters.size,
        )
        assertTrue(
            actual = decoded.isValid,
        )
    }

    // endregion

    // region helpers

    private val NullFilter: SearchFilter? = null

    private val NoHandles: suspend (ProfileId) -> ProfileHandle? = { null }

    private fun handles(vararg pairs: Pair<String, String>): suspend (ProfileId) -> ProfileHandle? {
        val byDid = pairs.toMap()
        return { did -> byDid[did.id]?.let(::ProfileHandle) }
    }

    private fun dids(vararg pairs: Pair<String, String>): suspend (ProfileHandle) -> ProfileId? {
        val byHandle = pairs.toMap()
        return { handle -> byHandle[handle.id]?.let(::ProfileId) }
    }

    private fun filterOf(
        exactPhrase: String? = null,
        noneOfWords: String? = null,
        since: kotlinx.datetime.LocalDate? = null,
        until: kotlinx.datetime.LocalDate? = null,
        language: String? = null,
        media: SearchFilter.Media = SearchFilter.Media.All,
        replies: SearchFilter.Replies = SearchFilter.Replies.PostsAndReplies,
        from: SearchFilter.From = SearchFilter.From.Anyone,
        people: List<SearchFilter.PersonGroup> = emptyList(),
    ): SearchFilter = SearchFilter(
        exactPhrase = exactPhrase,
        noneOfWords = noneOfWords,
        since = since,
        until = until,
        language = language,
        media = media,
        replies = replies,
        from = from,
        people = people,
    )

    private fun personGroup(
        mode: SearchFilter.PersonGroup.Mode,
        kind: SearchFilter.PersonGroup.Kind,
        vararg dids: String,
    ): SearchFilter.PersonGroup = SearchFilter.PersonGroup(
        mode = mode,
        kind = kind,
        profileIds = dids.map(::ProfileId),
    )

    private val LocalDateStub = kotlinx.datetime.LocalDate.parse("2026-01-01")

    // endregion
}

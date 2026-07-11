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

import app.bsky.actor.ProfileView
import app.bsky.actor.ProfileViewBasic
import app.bsky.actor.SearchActorsQueryParams
import app.bsky.actor.SearchActorsResponse
import app.bsky.actor.SearchActorsTypeaheadQueryParams
import app.bsky.feed.GetSuggestedFeedsQueryParams
import app.bsky.feed.GetSuggestedFeedsResponse
import app.bsky.feed.SearchPostsV2QueryParams
import app.bsky.feed.SearchPostsV2Sort
import app.bsky.unspecced.GetPopularFeedGeneratorsQueryParams
import app.bsky.unspecced.GetPopularFeedGeneratorsResponse
import app.bsky.unspecced.GetSuggestedStarterPacksQueryParams
import app.bsky.unspecced.GetSuggestedUsersQueryParams
import app.bsky.unspecced.GetSuggestedUsersResponse
import app.bsky.unspecced.GetTrendsQueryParams
import app.bsky.unspecced.TrendView
import app.bsky.unspecced.TrendViewStatus
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.StarterPack
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.Trend
import com.tunjid.heron.data.core.models.canRequestData
import com.tunjid.heron.data.core.models.value
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.StarterPackUri
import com.tunjid.heron.data.database.daos.FeedGeneratorDao
import com.tunjid.heron.data.database.daos.StarterPackDao
import com.tunjid.heron.data.database.entities.PopulatedFeedGeneratorEntity
import com.tunjid.heron.data.database.entities.PopulatedStarterPackEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.di.IODispatcher
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.network.models.blueskyEmbeddedRecords
import com.tunjid.heron.data.network.models.externalEmbeddedRecordUris
import com.tunjid.heron.data.network.models.profile
import com.tunjid.heron.data.utilities.distinctUntilChangedMap
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.profileLookup.ProfileLookup
import com.tunjid.heron.data.utilities.recordResolver.RecordResolver
import com.tunjid.heron.data.utilities.sortedWithNetworkList
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import sh.christian.ozone.api.AtIdentifier
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Language

@Serializable
sealed class SearchQuery : CursorQuery {

    abstract val query: String
    abstract val isLocalOnly: Boolean

    @Serializable
    sealed class OfPosts : SearchQuery() {
        abstract val filter: Filter?

        data class Top(
            override val query: String,
            override val isLocalOnly: Boolean,
            override val data: CursorQuery.Data,
            override val filter: Filter? = null,
        ) : OfPosts()

        data class Latest(
            override val query: String,
            override val isLocalOnly: Boolean,
            override val data: CursorQuery.Data,
            override val filter: Filter? = null,
        ) : OfPosts()
    }

    /**
     * Advanced filters for [OfPosts] searches, mapped onto `app.bsky.feed.searchPostsV2`
     * query parameters. All members are optional; an empty [Filter] adds no constraints.
     */
    @Serializable
    data class Filter(
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

        /**
         * One "include/exclude these people" row in the filter sheet, mapping to the
         * `authors`/`excludeAuthors`/`mentions`/`excludeMentions` params.
         */
        @Serializable
        data class PersonGroup(
            val mode: Mode = Mode.Include,
            val kind: Kind = Kind.Authors,
            val profileIds: List<ProfileId> = emptyList(),
        ) {
            @Serializable
            enum class Mode { Include, Exclude }

            @Serializable
            enum class Kind { Authors, Mentions }
        }
    }

    @Serializable
    data class OfProfiles(
        override val query: String,
        override val isLocalOnly: Boolean,
        override val data: CursorQuery.Data,
    ) : SearchQuery()

    @Serializable
    data class OfFeedGenerators(
        override val query: String,
        override val isLocalOnly: Boolean,
        override val data: CursorQuery.Data,
    ) : SearchQuery()
}

interface SearchRepository {
    fun postSearch(
        query: SearchQuery.OfPosts,
        cursor: Cursor,
    ): Flow<CursorList<TimelineItem>>

    fun profileSearch(
        query: SearchQuery.OfProfiles,
        cursor: Cursor,
    ): Flow<CursorList<ProfileWithViewerState>>

    fun feedGeneratorSearch(
        query: SearchQuery.OfFeedGenerators,
        cursor: Cursor,
    ): Flow<CursorList<FeedGenerator>>

    fun autoCompleteProfileSearch(
        query: SearchQuery.OfProfiles,
        cursor: Cursor,
    ): Flow<List<ProfileWithViewerState>>

    fun trends(): Flow<List<Trend>>

    fun suggestedProfiles(
        category: String? = null,
    ): Flow<List<ProfileWithViewerState>>

    fun suggestedStarterPacks(): Flow<List<StarterPack>>

    fun suggestedFeeds(): Flow<List<FeedGenerator>>
}

@Inject
internal class OfflineSearchRepository(
    @param:IODispatcher
    private val ioDispatcher: CoroutineDispatcher,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
    private val networkService: NetworkService,
    private val savedStateDataSource: SavedStateDataSource,
    private val starterPackDao: StarterPackDao,
    private val feedGeneratorDao: FeedGeneratorDao,
    private val recordResolver: RecordResolver,
    private val profileLookup: ProfileLookup,
) : SearchRepository {

    override fun postSearch(
        query: SearchQuery.OfPosts,
        cursor: Cursor,
    ): Flow<CursorList<TimelineItem>> =
        savedStateDataSource.singleSessionFlow { signedInProfileId ->
            if (!query.hasSearchCriteria) return@singleSessionFlow emptyFlow()
            if (!cursor.canRequestData) return@singleSessionFlow emptyFlow()

            val response = networkService.runCatchingWithMonitoredNetworkRetry {
                searchPostsV2(
                    params = query.toSearchPostsV2Params(
                        cursor = cursor,
                    ),
                )
            }
                .getOrNull()
                ?: return@singleSessionFlow emptyFlow()

            multipleEntitySaverProvider.saveInTransaction {
                response.posts.forEach { postView ->
                    add(
                        viewingProfileId = signedInProfileId,
                        postView = postView,
                    )
                }
            }

            val nextCursor = response.cursor?.let(Cursor::Next) ?: Cursor.Final

            // Using the network call as a base, observe the db for user interactions
            recordResolver.timelineItems(
                items = response.posts,
                signedInProfileId = signedInProfileId,
                postUri = { PostUri(it.uri.atUri) },
                associatedRecordUris = { postView ->
                    postView.blueskyEmbeddedRecords(
                        viewingProfileId = signedInProfileId,
                    )
                        .map(Record.Embeddable::embeddableRecordUri)
                        .plus(postView.externalEmbeddedRecordUris())
                },
                associatedProfileIds = {
                    emptyList()
                },
                block = block@{ item ->
                    // Muted items should not show upp in search
                    if (isMuted(post)) return@block
                    push(
                        TimelineItem.Single(
                            id = item.uri.atUri,
                            post = post,
                            isMuted = false,
                            threadGate = threadGate(PostUri(item.uri.atUri)),
                            appliedLabels = appliedLabels,
                            signedInProfileId = signedInProfileId,
                        ),
                    )
                },
            ).map {
                CursorList(
                    items = it,
                    nextCursor = nextCursor,
                )
            }
        }
            .flowOn(ioDispatcher)

    override fun profileSearch(
        query: SearchQuery.OfProfiles,
        cursor: Cursor,
    ): Flow<CursorList<ProfileWithViewerState>> =
        if (query.query.isBlank()) emptyFlow()
        else savedStateDataSource.singleSessionFlow { signedInProfileId ->
            profileLookup.profilesWithViewerState(
                signedInProfileId = signedInProfileId,
                cursor = cursor,
                responseFetcher = {
                    searchActors(
                        params = SearchActorsQueryParams(
                            q = query.query,
                            limit = query.data.limit,
                            cursor = cursor.value,
                        ),
                    )
                },
                responseProfileViews = SearchActorsResponse::actors,
                responseCursor = SearchActorsResponse::cursor,
            )
        }
            .flowOn(ioDispatcher)

    override fun feedGeneratorSearch(
        query: SearchQuery.OfFeedGenerators,
        cursor: Cursor,
    ): Flow<CursorList<FeedGenerator>> =
        if (query.query.isBlank()) emptyFlow()
        else if (!cursor.canRequestData) emptyFlow()
        else flow {
            val response = networkService.runCatchingWithMonitoredNetworkRetry {
                getPopularFeedGeneratorsUnspecced(
                    params = GetPopularFeedGeneratorsQueryParams(
                        query = query.query,
                        limit = query.data.limit,
                        cursor = cursor.value,
                    ),
                )
            }
                .getOrNull()
                ?: return@flow

            multipleEntitySaverProvider.saveInTransaction {
                response.feeds
                    .forEach { generatorView ->
                        add(feedGeneratorView = generatorView)
                    }
            }

            val nextCursor = response.cursor?.let(Cursor::Next) ?: Cursor.Final
            val feedUris = response.feeds.map { it.uri.atUri.let(::FeedGeneratorUri) }

            emitAll(
                feedGeneratorDao.feedGenerators(
                    feedUris = feedUris,
                )
                    .distinctUntilChangedMap { populatedFeedGeneratorEntities ->
                        CursorList(
                            items = populatedFeedGeneratorEntities
                                .map(PopulatedFeedGeneratorEntity::asExternalModel)
                                .sortedWithNetworkList(
                                    networkList = feedUris,
                                    databaseId = { it.uri.uri },
                                    networkId = { it.uri },
                                ),
                            nextCursor = nextCursor,
                        )
                    },
            )
        }
            .flowOn(ioDispatcher)

    override fun autoCompleteProfileSearch(
        query: SearchQuery.OfProfiles,
        cursor: Cursor,
    ): Flow<List<ProfileWithViewerState>> =
        savedStateDataSource.singleSessionFlow { signedInProfileId ->
            profileLookup.profilesWithViewerState(
                signedInProfileId = signedInProfileId,
                cursor = cursor,
                responseFetcher = {
                    searchActorsTypeahead(
                        params = SearchActorsTypeaheadQueryParams(
                            q = query.query,
                            limit = query.data.limit,
                        ),
                    )
                },
                responseProfileViews = {
                    actors.map { basicProfileView ->
                        ProfileView(
                            did = basicProfileView.did,
                            handle = basicProfileView.handle,
                            displayName = basicProfileView.displayName,
                            pronouns = basicProfileView.pronouns,
                            description = null,
                            avatar = basicProfileView.avatar,
                            associated = basicProfileView.associated,
                            indexedAt = null,
                            createdAt = basicProfileView.createdAt,
                            viewer = basicProfileView.viewer,
                            labels = basicProfileView.labels,
                            verification = basicProfileView.verification,
                            status = basicProfileView.status,
                            debug = basicProfileView.debug,
                        )
                    }
                },
                responseCursor = { null },
            )
        }
            .flowOn(ioDispatcher)

    override fun trends(): Flow<List<Trend>> = flow {
        networkService.runCatchingWithMonitoredNetworkRetry {
            getTrendsUnspecced(
                GetTrendsQueryParams(),
            )
        }
            .getOrNull()
            ?.trends
            ?.map(TrendView::trend)
            ?.let {
                emit(it)
            }
    }
        .flowOn(ioDispatcher)

    override fun suggestedProfiles(
        category: String?,
    ): Flow<List<ProfileWithViewerState>> =
        savedStateDataSource.singleAuthorizedSessionFlow { signedInProfileId ->
            profileLookup.profilesWithViewerState(
                signedInProfileId = signedInProfileId,
                cursor = Cursor.Initial,
                responseFetcher = {
                    getSuggestedUsersUnspecced(
                        GetSuggestedUsersQueryParams(
                            category = category,
                        ),
                    )
                },
                responseProfileViews = GetSuggestedUsersResponse::actors,
                responseCursor = { null },
            )
        }
            .filterNotNull()
            .flowOn(ioDispatcher)

    override fun suggestedStarterPacks(): Flow<List<StarterPack>> =
        savedStateDataSource.singleAuthorizedSessionFlow {
            val starterPackViews = networkService.runCatchingWithMonitoredNetworkRetry {
                getSuggestedStarterPacksUnspecced(
                    GetSuggestedStarterPacksQueryParams(),
                )
            }
                .getOrNull()
                ?.starterPacks
                ?: return@singleAuthorizedSessionFlow emptyFlow()

            multipleEntitySaverProvider.saveInTransaction {
                starterPackViews.forEach { starterPack ->
                    add(starterPack = starterPack)
                }
            }

            starterPackDao.starterPacks(
                starterPackViews.mapTo(mutableSetOf()) { it.uri.atUri.let(::StarterPackUri) },
            )
                .map { populatedStarterPackEntities ->
                    populatedStarterPackEntities
                        .sortedWithNetworkList(
                            networkList = starterPackViews,
                            databaseId = { it.entity.uri.uri },
                            networkId = { it.uri.atUri },
                        )
                        .map(PopulatedStarterPackEntity::asExternalModel)
                }
                .distinctUntilChanged()
        }
            .filterNotNull()
            .flowOn(ioDispatcher)

    override fun suggestedFeeds(): Flow<List<FeedGenerator>> =
        savedStateDataSource.singleSessionFlow { signedInProfileId ->
            val generatorViews = networkService.runCatchingWithMonitoredNetworkRetry {
                if (signedInProfileId == null) getPopularFeedGeneratorsUnspecced(
                    params = GetPopularFeedGeneratorsQueryParams(),
                ).map(GetPopularFeedGeneratorsResponse::feeds)
                else getSuggestedFeeds(
                    params = GetSuggestedFeedsQueryParams(),
                ).map(GetSuggestedFeedsResponse::feeds)
            }
                .getOrNull()
                ?: return@singleSessionFlow emptyFlow()

            multipleEntitySaverProvider.saveInTransaction {
                generatorViews.forEach { generatorView ->
                    add(feedGeneratorView = generatorView)
                }
            }

            feedGeneratorDao.feedGenerators(
                generatorViews.map { it.uri.atUri.let(::FeedGeneratorUri) },
            )
                .map { populatedFeedGeneratorEntities ->
                    populatedFeedGeneratorEntities
                        .sortedWithNetworkList(
                            networkList = generatorViews,
                            databaseId = { it.entity.uri.uri },
                            networkId = { it.uri.atUri },
                        )
                        .map(PopulatedFeedGeneratorEntity::asExternalModel)
                }
                .distinctUntilChanged()
        }
            .flowOn(ioDispatcher)
}

private fun TrendView.trend() = Trend(
    topic = topic,
    status = when (status) {
        TrendViewStatus.Hot -> Trend.Status.Hot
        is TrendViewStatus.Unknown,
        null,
        -> null
    },
    displayName = displayName,
    link = link,
    startedAt = startedAt,
    postCount = postCount,
    category = category,
    actors = actors.map(ProfileViewBasic::profile),
)

private val SearchQuery.OfPosts.hasSearchCriteria: Boolean
    get() = query.isNotBlank() || filter?.isEmpty == false

private val SearchQuery.Filter.isEmpty: Boolean
    get() = exactPhrase.isNullOrBlank() &&
        noneOfWords.isNullOrBlank() &&
        since == null &&
        until == null &&
        language == null &&
        media == SearchQuery.Filter.Media.All &&
        replies == SearchQuery.Filter.Replies.PostsAndReplies &&
        from == SearchQuery.Filter.From.Anyone &&
        people.all { it.profileIds.isEmpty() }

private fun SearchQuery.OfPosts.toSearchPostsV2Params(
    cursor: Cursor,
): SearchPostsV2QueryParams {
    val filter = filter
    return SearchPostsV2QueryParams(
        query = composedQueryString().ifBlank { null },
        sort = when (this) {
            is SearchQuery.OfPosts.Latest -> SearchPostsV2Sort.Recent
            is SearchQuery.OfPosts.Top -> SearchPostsV2Sort.Top
        },
        limit = data.limit,
        cursor = cursor.value,
        authors = filter.atIdentifiers(
            mode = SearchQuery.Filter.PersonGroup.Mode.Include,
            kind = SearchQuery.Filter.PersonGroup.Kind.Authors,
        ),
        excludeAuthors = filter.atIdentifiers(
            mode = SearchQuery.Filter.PersonGroup.Mode.Exclude,
            kind = SearchQuery.Filter.PersonGroup.Kind.Authors,
        ),
        mentions = filter.atIdentifiers(
            mode = SearchQuery.Filter.PersonGroup.Mode.Include,
            kind = SearchQuery.Filter.PersonGroup.Kind.Mentions,
        ),
        excludeMentions = filter.atIdentifiers(
            mode = SearchQuery.Filter.PersonGroup.Mode.Exclude,
            kind = SearchQuery.Filter.PersonGroup.Kind.Mentions,
        ),
        since = filter?.since?.toString(),
        until = filter?.until?.toString(),
        languages = filter?.language?.let { listOf(Language(it)) },
        hasMedia = (filter?.media == SearchQuery.Filter.Media.WithMedia).trueOrNull(),
        hasVideo = (filter?.media == SearchQuery.Filter.Media.VideosOnly).trueOrNull(),
        excludeReplies = (filter?.replies == SearchQuery.Filter.Replies.PostsOnly).trueOrNull(),
        repliesOnly = (filter?.replies == SearchQuery.Filter.Replies.RepliesOnly).trueOrNull(),
        following = (filter?.from == SearchQuery.Filter.From.Following).trueOrNull(),
    )
}

private fun SearchQuery.OfPosts.composedQueryString(): String = buildString {
    append(query.trim())
    val filter = filter ?: return@buildString
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

private fun SearchQuery.Filter?.atIdentifiers(
    mode: SearchQuery.Filter.PersonGroup.Mode,
    kind: SearchQuery.Filter.PersonGroup.Kind,
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

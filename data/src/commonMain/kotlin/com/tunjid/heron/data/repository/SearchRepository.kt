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
import app.bsky.actor.SearchActorsTypeaheadQueryParams
import app.bsky.feed.GetSuggestedFeedsQueryParams
import app.bsky.feed.PostView
import app.bsky.feed.SearchPostsQueryParams
import app.bsky.feed.SearchPostsSort
import app.bsky.unspecced.GetPopularFeedGeneratorsQueryParams
import app.bsky.unspecced.GetSuggestedStarterPacksQueryParams
import app.bsky.unspecced.GetSuggestedUsersQueryParams
import app.bsky.unspecced.GetTrendsQueryParams
import app.bsky.unspecced.Status
import app.bsky.unspecced.TrendView
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.models.StarterPack
import com.tunjid.heron.data.core.models.Trend
import com.tunjid.heron.data.core.models.value
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.StarterPackId
import com.tunjid.heron.data.database.daos.FeedGeneratorDao
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.daos.StarterPackDao
import com.tunjid.heron.data.database.entities.PopulatedFeedGeneratorEntity
import com.tunjid.heron.data.database.entities.PopulatedStarterPackEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.network.models.post
import com.tunjid.heron.data.network.models.profile
import com.tunjid.heron.data.network.models.profileViewerStateEntities
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.observeProfileWithViewerStates
import com.tunjid.heron.data.utilities.toProfileWithViewerStates
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable
sealed class SearchQuery : CursorQuery {

    abstract val query: String
    abstract val isLocalOnly: Boolean

    val sourceId
        get() = when (this) {
            is OfPosts.Latest -> "latest-posts"
            is OfPosts.Top -> "top-posts"
            is OfProfiles -> "profiles"
            is OfFeedGenerators -> "feed-generators"
        }

    @Serializable
    sealed class OfPosts : SearchQuery() {
        data class Top(
            override val query: String,
            override val isLocalOnly: Boolean,
            override val data: CursorQuery.Data,
        ) : OfPosts()

        data class Latest(
            override val query: String,
            override val isLocalOnly: Boolean,
            override val data: CursorQuery.Data,
        ) : OfPosts()
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
    ): Flow<CursorList<Post>>

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
        category: String? = null
    ): Flow<List<ProfileWithViewerState>>

    fun suggestedStarterPacks(
    ): Flow<List<StarterPack>>

    fun suggestedFeeds(
    ): Flow<List<FeedGenerator>>
}

internal class OfflineSearchRepository @Inject constructor(
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
    private val networkService: NetworkService,
    private val savedStateDataSource: SavedStateDataSource,
    private val profileDao: ProfileDao,
    private val starterPackDao: StarterPackDao,
    private val feedGeneratorDao: FeedGeneratorDao,
) : SearchRepository {

    override fun postSearch(
        query: SearchQuery.OfPosts,
        cursor: Cursor,
    ): Flow<CursorList<Post>> =
        if (query.query.isBlank()) emptyFlow()
        else flow {
            val response = networkService.runCatchingWithMonitoredNetworkRetry {
                searchPosts(
                    params = SearchPostsQueryParams(
                        q = query.query,
                        limit = query.data.limit,
                        sort = when (query) {
                            is SearchQuery.OfPosts.Latest -> SearchPostsSort.Latest
                            is SearchQuery.OfPosts.Top -> SearchPostsSort.Top
                        },
                        cursor = when (cursor) {
                            Cursor.Initial -> cursor.value
                            is Cursor.Next -> cursor.value
                            Cursor.Pending -> null
                        },
                    )
                )
            }
                .getOrNull()
                ?: return@flow

            val authProfileId = savedStateDataSource.signedInProfileId

            multipleEntitySaverProvider.saveInTransaction {
                response.posts.forEach { postView ->
                    add(
                        viewingProfileId = authProfileId,
                        postView = postView
                    )
                }
            }

            emit(
                CursorList(
                    items = response.posts.map(PostView::post),
                    nextCursor = response.cursor?.let(Cursor::Next) ?: Cursor.Pending
                )
            )
        }

    override fun profileSearch(
        query: SearchQuery.OfProfiles,
        cursor: Cursor,
    ): Flow<CursorList<ProfileWithViewerState>> =
        if (query.query.isBlank()) emptyFlow()
        else flow {
            val response = networkService.runCatchingWithMonitoredNetworkRetry {
                searchActors(
                    params = SearchActorsQueryParams(
                        q = query.query,
                        limit = query.data.limit,
                        cursor = when (cursor) {
                            Cursor.Initial -> cursor.value
                            is Cursor.Next -> cursor.value
                            Cursor.Pending -> null
                        },
                    )
                )
            }
                .getOrNull()
                ?: return@flow

            val signedInProfileId = savedStateDataSource.signedInProfileId

            multipleEntitySaverProvider.saveInTransaction {
                response.actors
                    .forEach { profileView ->
                        add(
                            viewingProfileId = signedInProfileId,
                            profileView = profileView,
                        )
                    }
            }

            val nextCursor = response.cursor?.let(Cursor::Next) ?: Cursor.Pending

            // Emit network results immediately for minimal latency during search
            emit(
                CursorList(
                    items = response.actors.toProfileWithViewerStates(
                        signedInProfileId = signedInProfileId,
                        profileMapper = ProfileView::profile,
                        profileViewerStateEntities = ProfileView::profileViewerStateEntities,
                    ),
                    nextCursor = nextCursor,
                )
            )

            emitAll(
                response.actors.observeProfileWithViewerStates(
                    profileDao = profileDao,
                    signedInProfileId = signedInProfileId,
                    profileMapper = ProfileView::profile,
                    idMapper = { did.did.let(::ProfileId) },
                )
                    .map { profileWithViewerStates ->
                        CursorList(
                            items = profileWithViewerStates,
                            nextCursor = nextCursor,
                        )
                    }
            )
        }

    override fun feedGeneratorSearch(
        query: SearchQuery.OfFeedGenerators,
        cursor: Cursor,
    ): Flow<CursorList<FeedGenerator>> =
        if (query.query.isBlank()) emptyFlow()
        else flow {
            val response = networkService.runCatchingWithMonitoredNetworkRetry {
                getPopularFeedGeneratorsUnspecced(
                    params = GetPopularFeedGeneratorsQueryParams(
                        query = query.query,
                        limit = query.data.limit,
                        cursor = when (cursor) {
                            Cursor.Initial -> cursor.value
                            is Cursor.Next -> cursor.value
                            Cursor.Pending -> null
                        },
                    )
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

            val nextCursor = response.cursor?.let(Cursor::Next) ?: Cursor.Pending
            val feedUris = response.feeds.map { it.uri.atUri.let(::FeedGeneratorUri) }

            emitAll(
                feedGeneratorDao.feedGeneratorsByUri(
                    feedUris = feedUris
                )
                    .map { populatedFeedGeneratorEntities ->
                        CursorList(
                            items = populatedFeedGeneratorEntities
                                .map(PopulatedFeedGeneratorEntity::asExternalModel)
                                .sortedBy { feedUris.indexOf(it.uri) },
                            nextCursor = nextCursor,
                        )
                    }
            )
        }

    override fun autoCompleteProfileSearch(
        query: SearchQuery.OfProfiles,
        cursor: Cursor,
    ): Flow<List<ProfileWithViewerState>> = flow {
        val profileViews = networkService.runCatchingWithMonitoredNetworkRetry {
            searchActorsTypeahead(
                params = SearchActorsTypeaheadQueryParams(
                    q = query.query,
                    limit = query.data.limit,
                )
            )
        }
            .getOrNull()
            ?.actors ?: return@flow

        val signedInProfileId = savedStateDataSource.signedInProfileId

        multipleEntitySaverProvider.saveInTransaction {
            profileViews.forEach { profileView ->
                add(
                    viewingProfileId = signedInProfileId,
                    profileView = profileView,
                )
            }
        }

        // Emit network results immediately for minimal latency during search
        emit(
            profileViews.toProfileWithViewerStates(
                signedInProfileId = signedInProfileId,
                profileMapper = ProfileViewBasic::profile,
                profileViewerStateEntities = ProfileViewBasic::profileViewerStateEntities,
            )
        )

        emitAll(
            profileViews.observeProfileWithViewerStates(
                profileDao = profileDao,
                signedInProfileId = signedInProfileId,
                profileMapper = ProfileViewBasic::profile,
                idMapper = { did.did.let(::ProfileId) },
            )
        )
    }

    override fun trends(): Flow<List<Trend>> = flow {
        networkService.runCatchingWithMonitoredNetworkRetry {
            getTrendsUnspecced(
                GetTrendsQueryParams()
            )
        }
            .getOrNull()
            ?.trends
            ?.map(TrendView::trend)
            ?.let {
                emit(it)
            }
    }

    override fun suggestedProfiles(
        category: String?,
    ): Flow<List<ProfileWithViewerState>> = flow {
        val profileViews = networkService.runCatchingWithMonitoredNetworkRetry {
            getSuggestedUsersUnspecced(
                GetSuggestedUsersQueryParams(
                    category = category
                )
            )
        }
            .getOrNull()
            ?.actors
            ?: return@flow

        val signedInProfileId = savedStateDataSource.signedInProfileId

        multipleEntitySaverProvider.saveInTransaction {
            profileViews.forEach { profileView ->
                add(
                    viewingProfileId = signedInProfileId,
                    profileView = profileView,
                )
            }
        }

        emitAll(
            profileViews.observeProfileWithViewerStates(
                profileDao = profileDao,
                signedInProfileId = signedInProfileId,
                profileMapper = ProfileView::profile,
                idMapper = { did.did.let(::ProfileId) },
            )
        )
    }

    override fun suggestedStarterPacks(): Flow<List<StarterPack>> = flow {
        val starterPackViews = networkService.runCatchingWithMonitoredNetworkRetry {
            getSuggestedStarterPacksUnspecced(
                GetSuggestedStarterPacksQueryParams()
            )
        }
            .getOrNull()
            ?.starterPacks
            ?: return@flow

        multipleEntitySaverProvider.saveInTransaction {
            starterPackViews.forEach { starterPack ->
                add(starterPack = starterPack)
            }
        }

        emitAll(
            starterPackDao.starterPacks(
                starterPackViews.mapTo(mutableSetOf()) { it.cid.cid.let(::StarterPackId) }
            )
                .map { populatedStarterPackEntities ->
                    populatedStarterPackEntities.map(PopulatedStarterPackEntity::asExternalModel)
                }
                .distinctUntilChanged()
        )
    }

    override fun suggestedFeeds(): Flow<List<FeedGenerator>> = flow {
        val generatorViews = networkService.runCatchingWithMonitoredNetworkRetry {
            getSuggestedFeeds(
                GetSuggestedFeedsQueryParams()
            )
        }
            .getOrNull()
            ?.feeds
            ?: return@flow

        multipleEntitySaverProvider.saveInTransaction {
            generatorViews.forEach { generatorView ->
                add(feedGeneratorView = generatorView)
            }
        }

        emitAll(
            feedGeneratorDao.feedGeneratorsByUri(
                generatorViews.map { it.uri.atUri.let(::FeedGeneratorUri) }
            )
                .map { populatedFeedGeneratorEntities ->
                    populatedFeedGeneratorEntities.map(PopulatedFeedGeneratorEntity::asExternalModel)
                }
                .distinctUntilChanged()
        )
    }
}

private fun TrendView.trend() = Trend(
    topic = topic,
    status = when (status) {
        Status.Hot -> Trend.Status.Hot
        is Status.Unknown,
        null -> null
    },
    displayName = displayName,
    link = link,
    startedAt = startedAt,
    postCount = postCount,
    category = category,
    actors = actors.map(ProfileViewBasic::profile),
)

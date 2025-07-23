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

import app.bsky.actor.PreferencesUnion
import app.bsky.actor.PutPreferencesRequest
import app.bsky.actor.SavedFeed
import app.bsky.actor.SavedFeedsPrefV2
import app.bsky.actor.Type
import app.bsky.feed.FeedViewPost
import app.bsky.feed.GetActorLikesQueryParams
import app.bsky.feed.GetActorLikesResponse
import app.bsky.feed.GetAuthorFeedFilter
import app.bsky.feed.GetAuthorFeedQueryParams
import app.bsky.feed.GetAuthorFeedResponse
import app.bsky.feed.GetFeedGeneratorQueryParams
import app.bsky.feed.GetFeedQueryParams
import app.bsky.feed.GetFeedResponse
import app.bsky.feed.GetListFeedQueryParams
import app.bsky.feed.GetListFeedResponse
import app.bsky.feed.GetPostThreadQueryParams
import app.bsky.feed.GetPostThreadResponseThreadUnion
import app.bsky.feed.GetTimelineQueryParams
import app.bsky.feed.GetTimelineResponse
import app.bsky.feed.Token
import app.bsky.graph.GetListQueryParams
import app.bsky.graph.GetStarterPackQueryParams
import com.tunjid.heron.data.core.models.Constants
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.offset
import com.tunjid.heron.data.core.models.value
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.StarterPackUri
import com.tunjid.heron.data.database.daos.FeedGeneratorDao
import com.tunjid.heron.data.database.daos.ListDao
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.daos.StarterPackDao
import com.tunjid.heron.data.database.daos.TimelineDao
import com.tunjid.heron.data.database.entities.EmbeddedPopulatedPostEntity
import com.tunjid.heron.data.database.entities.FeedGeneratorEntity
import com.tunjid.heron.data.database.entities.PopulatedFeedGeneratorEntity
import com.tunjid.heron.data.database.entities.PopulatedListEntity
import com.tunjid.heron.data.database.entities.PostEntity
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.ThreadedPostEntity
import com.tunjid.heron.data.database.entities.TimelinePreferencesEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.utilities.Collections
import com.tunjid.heron.data.utilities.InvalidationTrackerDebounceMillis
import com.tunjid.heron.data.utilities.lookupProfileDid
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.runCatchingUnlessCancelled
import com.tunjid.heron.data.utilities.runCatchingWithNetworkRetry
import com.tunjid.heron.data.utilities.withRefresh
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.take
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.response.AtpResponse
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

sealed interface TimelineRequest {

    data object Following : TimelineRequest

    data class OfProfile(
        val profileHandleOrDid: Id.Profile,
        val type: Timeline.Profile.Type,
    ) : TimelineRequest

    sealed interface OfFeed : TimelineRequest {
        data class WithUri(
            val uri: FeedGeneratorUri,
        ) : OfFeed

        data class WithProfile(
            val profileHandleOrDid: Id.Profile,
            val feedUriSuffix: String,
        ) : OfFeed
    }

    sealed interface OfList : TimelineRequest {
        data class WithUri(
            val uri: ListUri,
        ) : OfList

        data class WithProfile(
            val profileHandleOrDid: Id.Profile,
            val listUriSuffix: String,
        ) : OfList
    }

    sealed interface OfStarterPack : OfList {
        data class WithUri(
            val uri: StarterPackUri,
        ) : OfStarterPack

        data class WithProfile(
            val profileHandleOrDid: Id.Profile,
            val starterPackUriSuffix: String,
        ) : OfStarterPack
    }
}

class TimelineQuery(
    override val data: CursorQuery.Data,
    val timeline: Timeline,
) : CursorQuery {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as TimelineQuery

        if (data != other.data) return false
        if (timeline.sourceId != other.timeline.sourceId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.hashCode()
        result = 31 * result + timeline.sourceId.hashCode()
        return result
    }

}

interface TimelineRepository {
    fun homeTimelines(): Flow<List<Timeline.Home>>

    fun timeline(
        request: TimelineRequest,
    ): Flow<Timeline>

    fun hasUpdates(
        timeline: Timeline,
    ): Flow<Boolean>

    fun timelineItems(
        query: TimelineQuery,
        cursor: Cursor,
    ): Flow<CursorList<TimelineItem>>

    fun postThreadedItems(
        postUri: PostUri,
    ): Flow<List<TimelineItem>>

    suspend fun updatePreferredPresentation(
        timeline: Timeline,
        presentation: Timeline.Presentation,
    ): Boolean

    suspend fun updateHomeTimelines(
        timelines: List<Timeline.Home>,
    ): Boolean
}

@Inject
internal class OfflineTimelineRepository(
    private val postDao: PostDao,
    private val listDao: ListDao,
    private val profileDao: ProfileDao,
    private val timelineDao: TimelineDao,
    private val starterPackDao: StarterPackDao,
    private val feedGeneratorDao: FeedGeneratorDao,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
    private val networkService: NetworkService,
    private val savedStateRepository: SavedStateRepository,
    private val authRepository: AuthRepository,
) : TimelineRepository {

    override fun timelineItems(
        query: TimelineQuery,
        cursor: Cursor,
    ): Flow<CursorList<TimelineItem>> = when (val timeline = query.timeline) {
        is Timeline.Home.Following -> observeAndRefreshTimeline(
            query = query,
            nextCursorFlow = nextCursorFlow(
                query = query,
                currentCursor = cursor,
                currentRequestWithNextCursor = {
                    networkService.api.getTimeline(
                        GetTimelineQueryParams(
                            limit = query.data.limit,
                            cursor = cursor.value,
                        )
                    )
                },
                nextCursor = GetTimelineResponse::cursor,
                networkFeed = GetTimelineResponse::feed,
            )
        )

        is Timeline.Home.Feed -> observeAndRefreshTimeline(
            query = query,
            nextCursorFlow = nextCursorFlow(
                query = query,
                currentCursor = cursor,
                currentRequestWithNextCursor = {
                    networkService.api.getFeed(
                        GetFeedQueryParams(
                            feed = AtUri(timeline.source.uri),
                            limit = query.data.limit,
                            cursor = cursor.value,
                        )
                    )
                },
                nextCursor = GetFeedResponse::cursor,
                networkFeed = GetFeedResponse::feed,
            )
        )

        is Timeline.Home.List -> observeAndRefreshTimeline(
            query = query,
            nextCursorFlow = nextCursorFlow(
                query = query,
                currentCursor = cursor,
                currentRequestWithNextCursor = {
                    networkService.api.getListFeed(
                        GetListFeedQueryParams(
                            list = AtUri(timeline.source.uri),
                            limit = query.data.limit,
                            cursor = cursor.value,
                        )
                    )
                },
                nextCursor = GetListFeedResponse::cursor,
                networkFeed = GetListFeedResponse::feed,
            )
        )

        is Timeline.Profile -> when (timeline.type) {
            Timeline.Profile.Type.Likes -> observeAndRefreshTimeline(
                query = query,
                nextCursorFlow = nextCursorFlow(
                    query = query,
                    currentCursor = cursor,
                    currentRequestWithNextCursor = {
                        networkService.api.getActorLikes(
                            GetActorLikesQueryParams(
                                actor = Did(timeline.profileId.id),
                                limit = query.data.limit,
                                cursor = cursor.value,
                            )
                        )
                    },
                    nextCursor = GetActorLikesResponse::cursor,
                    networkFeed = GetActorLikesResponse::feed,
                )
            )

            Timeline.Profile.Type.Media -> observeAndRefreshTimeline(
                query = query,
                nextCursorFlow = nextCursorFlow(
                    query = query,
                    currentCursor = cursor,
                    currentRequestWithNextCursor = {
                        networkService.api.getAuthorFeed(
                            GetAuthorFeedQueryParams(
                                actor = Did(timeline.profileId.id),
                                limit = query.data.limit,
                                cursor = cursor.value,
                                filter = GetAuthorFeedFilter.PostsWithMedia,
                            )
                        )
                    },
                    nextCursor = GetAuthorFeedResponse::cursor,
                    networkFeed = GetAuthorFeedResponse::feed,
                )
            )

            Timeline.Profile.Type.Posts -> observeAndRefreshTimeline(
                query = query,
                nextCursorFlow = nextCursorFlow(
                    query = query,
                    currentCursor = cursor,
                    currentRequestWithNextCursor = {
                        networkService.api.getAuthorFeed(
                            GetAuthorFeedQueryParams(
                                actor = Did(timeline.profileId.id),
                                limit = query.data.limit,
                                cursor = cursor.value,
                                filter = GetAuthorFeedFilter.PostsNoReplies,
                            )
                        )
                    },
                    nextCursor = GetAuthorFeedResponse::cursor,
                    networkFeed = GetAuthorFeedResponse::feed,
                )
            )

            Timeline.Profile.Type.Replies -> observeAndRefreshTimeline(
                query = query,
                nextCursorFlow = nextCursorFlow(
                    query = query,
                    currentCursor = cursor,
                    currentRequestWithNextCursor = {
                        networkService.api.getAuthorFeed(
                            GetAuthorFeedQueryParams(
                                actor = Did(timeline.profileId.id),
                                limit = query.data.limit,
                                cursor = cursor.value,
                                filter = GetAuthorFeedFilter.PostsWithReplies,
                            )
                        )
                    },
                    nextCursor = GetAuthorFeedResponse::cursor,
                    networkFeed = GetAuthorFeedResponse::feed,
                )
            )

            Timeline.Profile.Type.Videos -> observeAndRefreshTimeline(
                query = query,
                nextCursorFlow = nextCursorFlow(
                    query = query,
                    currentCursor = cursor,
                    currentRequestWithNextCursor = {
                        networkService.api.getAuthorFeed(
                            GetAuthorFeedQueryParams(
                                actor = Did(timeline.profileId.id),
                                limit = query.data.limit,
                                cursor = cursor.value,
                                filter = GetAuthorFeedFilter.PostsWithVideo,
                            )
                        )
                    },
                    nextCursor = GetAuthorFeedResponse::cursor,
                    networkFeed = GetAuthorFeedResponse::feed,
                )
            )
        }

        is Timeline.StarterPack -> timelineItems(
            query = TimelineQuery(
                data = query.data,
                timeline = timeline.listTimeline,
            ),
            cursor = cursor
        )
    }
        .distinctUntilChanged()

    override fun hasUpdates(
        timeline: Timeline,
    ): Flow<Boolean> = when (timeline) {
        is Timeline.Home.Feed -> pollForTimelineUpdates(
            timeline = timeline,
            pollInterval = 10.seconds,
            networkRequestBlock = {
                networkService.api.getFeed(
                    GetFeedQueryParams(
                        feed = AtUri(timeline.source.uri),
                        limit = 1,
                        cursor = null,
                    )
                )
            },
            networkResponseToFeedViews = GetFeedResponse::feed,
        )

        is Timeline.Home.Following -> pollForTimelineUpdates(
            timeline = timeline,
            pollInterval = 10.seconds,
            networkRequestBlock = {
                networkService.api.getTimeline(
                    GetTimelineQueryParams(
                        limit = 1,
                        cursor = null,
                    )
                )
            },
            networkResponseToFeedViews = GetTimelineResponse::feed,
        )

        is Timeline.Home.List -> pollForTimelineUpdates(
            timeline = timeline,
            pollInterval = 10.seconds,
            networkRequestBlock = {
                networkService.api.getListFeed(
                    GetListFeedQueryParams(
                        list = AtUri(timeline.source.uri),
                        limit = 1,
                        cursor = null,
                    )
                )
            },
            networkResponseToFeedViews = GetListFeedResponse::feed,
        )

        is Timeline.Profile -> when (timeline.type) {
            Timeline.Profile.Type.Likes -> pollForTimelineUpdates(
                timeline = timeline,
                pollInterval = 15.seconds,
                networkRequestBlock = {
                    networkService.api.getActorLikes(
                        GetActorLikesQueryParams(
                            actor = Did(timeline.profileId.id),
                            limit = 1,
                            cursor = null,
                        )
                    )
                },
                networkResponseToFeedViews = GetActorLikesResponse::feed,
            )

            Timeline.Profile.Type.Media -> pollForTimelineUpdates(
                timeline = timeline,
                pollInterval = 15.seconds,
                networkRequestBlock = {
                    networkService.api.getAuthorFeed(
                        GetAuthorFeedQueryParams(
                            actor = Did(timeline.profileId.id),
                            limit = 1,
                            cursor = null,
                            filter = GetAuthorFeedFilter.PostsWithMedia,
                        )
                    )
                },
                networkResponseToFeedViews = GetAuthorFeedResponse::feed,
            )

            Timeline.Profile.Type.Posts -> pollForTimelineUpdates(
                timeline = timeline,
                pollInterval = 15.seconds,
                networkRequestBlock = {
                    networkService.api.getAuthorFeed(
                        GetAuthorFeedQueryParams(
                            actor = Did(timeline.profileId.id),
                            limit = 1,
                            cursor = null,
                            filter = GetAuthorFeedFilter.PostsNoReplies,
                        )
                    )
                },
                networkResponseToFeedViews = GetAuthorFeedResponse::feed,
            )

            Timeline.Profile.Type.Replies -> pollForTimelineUpdates(
                timeline = timeline,
                pollInterval = 15.seconds,
                networkRequestBlock = {
                    networkService.api.getAuthorFeed(
                        GetAuthorFeedQueryParams(
                            actor = Did(timeline.profileId.id),
                            limit = 1,
                            cursor = null,
                            filter = GetAuthorFeedFilter.PostsWithReplies,
                        )
                    )
                },
                networkResponseToFeedViews = GetAuthorFeedResponse::feed,
            )

            Timeline.Profile.Type.Videos -> pollForTimelineUpdates(
                timeline = timeline,
                pollInterval = 15.seconds,
                networkRequestBlock = {
                    networkService.api.getAuthorFeed(
                        GetAuthorFeedQueryParams(
                            actor = Did(timeline.profileId.id),
                            limit = 1,
                            cursor = null,
                            filter = GetAuthorFeedFilter.PostsWithVideo,
                        )
                    )
                },
                networkResponseToFeedViews = GetAuthorFeedResponse::feed,
            )
        }

        is Timeline.StarterPack -> hasUpdates(
            timeline = timeline.listTimeline,
        )
    }

    override fun postThreadedItems(
        postUri: PostUri,
    ): Flow<List<TimelineItem>> =
        postDao.postEntitiesByUri(postUris = setOf(postUri))
            .mapNotNull(List<PostEntity>::firstOrNull)
            .take(1)
            .flatMapLatest { postEntity ->
                postDao.postThread(
                    postId = postEntity.cid.id
                )
                    .flatMapLatest { postThread ->
                        val postIds = postThread.map(ThreadedPostEntity::postId).toSet()
                        combine(
                            flow = postDao.posts(
                                postIds = postIds
                            ),
                            flow2 = postDao.embeddedPosts(
                                postIds = postIds
                            ),
                            transform = { posts, embeddedPosts ->
                                val idsToPosts = posts.associateBy { it.entity.cid }
                                val idsToEmbeddedPosts = embeddedPosts.associateBy(
                                    EmbeddedPopulatedPostEntity::postId
                                )

                                postThread.fold(
                                    initial = emptyList<TimelineItem.Thread>(),
                                    operation = { list, thread ->
                                        val populatedPostEntity =
                                            idsToPosts.getValue(thread.entity.cid)
                                        val post = populatedPostEntity.asExternalModel(
                                            quote = idsToEmbeddedPosts[thread.entity.cid]
                                                ?.entity
                                                ?.asExternalModel(quote = null)
                                        )
                                        spinThread(
                                            list = list,
                                            thread = thread,
                                            post = post,
                                        )
                                    },
                                )
                            }
                        )
                    }
            }
            .withRefresh {
                runCatchingWithNetworkRetry {
                    networkService.api.getPostThread(
                        GetPostThreadQueryParams(
                            uri = AtUri(postUri.uri)
                        )
                    )
                }
                    .getOrNull()
                    ?.thread
                    ?.let { thread ->
                        when (thread) {
                            is GetPostThreadResponseThreadUnion.BlockedPost -> Unit
                            is GetPostThreadResponseThreadUnion.NotFoundPost -> Unit
                            is GetPostThreadResponseThreadUnion.ThreadViewPost -> {
                                multipleEntitySaverProvider
                                    .saveInTransaction {
                                        add(
                                            viewingProfileId = savedStateRepository.signedInProfileId,
                                            threadViewPost = thread.value,
                                        )
                                    }
                            }

                            is GetPostThreadResponseThreadUnion.Unknown -> Unit
                        }
                    }
            }
            .distinctUntilChanged()

    override fun homeTimelines(): Flow<List<Timeline.Home>> =
        savedStateRepository.savedState
            .mapNotNull { it.preferences?.timelinePreferences }
            .distinctUntilChanged()
            .flatMapLatest { timelinePreferences ->
                timelinePreferences.mapIndexed { index, preference ->
                    when (Type.safeValueOf(preference.type)) {
                        Type.Feed -> feedGeneratorTimeline(
                            uri = FeedGeneratorUri(preference.value),
                            position = index,
                            isPinned = preference.pinned,
                        )

                        Type.List -> listTimeline(
                            uri = ListUri(preference.value),
                            position = index,
                            isPinned = preference.pinned,
                        )

                        Type.Timeline -> followingTimeline(
                            name = preference.value,
                            position = index,
                            isPinned = preference.pinned,
                        )

                        is Type.Unknown -> emptyFlow()
                    }
                }
                    .merge()
                    .scan(emptyList<Timeline.Home>()) { timelines, timeline ->
                        // Add newest item first
                        (listOf(timeline) + timelines).distinctBy(Timeline.Home::sourceId)
                    }
                    .map { homeTimelines ->
                        homeTimelines.sortedBy(Timeline.Home::position)
                    }
                    .distinctUntilChangedBy { timelines ->
                        timelines.joinToString(
                            separator = "-",
                            transform = { "${it.name}-${it.presentation.key}" },
                        )
                    }
                    .filter(List<Timeline.Home>::isNotEmpty)
            }
            .distinctUntilChanged()
            .debounce(InvalidationTrackerDebounceMillis)

    override fun timeline(
        request: TimelineRequest,
    ): Flow<Timeline> = savedStateRepository.savedState
        .mapNotNull { it.preferences?.timelinePreferences }
        .distinctUntilChanged()
        .flatMapLatest { preferences ->
            flow {
                when (request) {
                    is TimelineRequest.OfFeed.WithUri -> emitAll(
                        feedGeneratorTimeline(
                            uri = request.uri,
                            position = 0,
                            isPinned = preferences.firstOrNull {
                                it.value == request.uri.uri
                            }?.pinned ?: false
                        )
                    )

                    is TimelineRequest.OfList.WithUri -> emitAll(
                        listTimeline(
                            uri = request.uri,
                            position = 0,
                            isPinned = preferences.firstOrNull {
                                it.value == request.uri.uri
                            }?.pinned ?: false
                        )
                    )

                    is TimelineRequest.OfFeed.WithProfile -> {
                        val profileDid = lookupProfileDid(
                            profileId = request.profileHandleOrDid,
                            profileDao = profileDao,
                            networkService = networkService,
                        ) ?: return@flow
                        val uri = FeedGeneratorUri(
                            uri = "at://${profileDid.did}/${Collections.FeedGenerator}/${request.feedUriSuffix}"
                        )
                        emitAll(
                            feedGeneratorTimeline(
                                uri = uri,
                                position = 0,
                                isPinned = preferences.firstOrNull {
                                    it.value == uri.uri
                                }?.pinned ?: false
                            )
                        )
                    }

                    is TimelineRequest.OfList.WithProfile -> {
                        val profileDid = lookupProfileDid(
                            profileId = request.profileHandleOrDid,
                            profileDao = profileDao,
                            networkService = networkService,
                        ) ?: return@flow
                        val uri = ListUri(
                            uri = "at://${profileDid.did}/${Collections.List}/${request.listUriSuffix}"
                        )
                        emitAll(
                            listTimeline(
                                uri = uri,
                                position = 0,
                                isPinned = preferences.firstOrNull {
                                    it.value == uri.uri
                                }?.pinned ?: false
                            )
                        )
                    }

                    is TimelineRequest.OfProfile -> emitAll(
                        profileDao.profiles(
                            ids = listOf(request.profileHandleOrDid)
                        )
                            .mapNotNull(List<ProfileEntity>::firstOrNull)
                            .distinctUntilChangedBy(ProfileEntity::did)
                            .flatMapLatest { profile ->
                                timelineDao.lastFetchKey(
                                    sourceId = request.type.sourceId(profile.did)
                                )
                                    .distinctUntilChanged()
                                    .map { timelinePreferenceEntity ->
                                        Timeline.Profile(
                                            profileId = profile.did,
                                            type = request.type,
                                            lastRefreshed = timelinePreferenceEntity?.lastFetchedAt,
                                            presentation = timelinePreferenceEntity.preferredPresentation(),
                                        )
                                    }
                            }
                    )

                    is TimelineRequest.OfStarterPack.WithProfile -> {
                        val profileDid = lookupProfileDid(
                            profileId = request.profileHandleOrDid,
                            profileDao = profileDao,
                            networkService = networkService,
                        ) ?: return@flow
                        val uri = StarterPackUri(
                            uri = "at://${profileDid.did}/${Collections.StarterPack}/${request.starterPackUriSuffix}"
                        )
                        emitAll(
                            starterPackTimeline(
                                uri = uri,
                            )
                        )
                    }

                    is TimelineRequest.OfStarterPack.WithUri -> emitAll(
                        starterPackTimeline(
                            uri = request.uri,
                        )
                    )

                    TimelineRequest.Following -> emitAll(
                        followingTimeline(
                            // TODO: Get a string resource for this
                            name = "",
                            position = 0,
                            isPinned = preferences.firstOrNull {
                                Type.safeValueOf(it.type) is Type.Timeline
                            }?.pinned ?: false
                        )
                    )

                }
            }
        }

    override suspend fun updatePreferredPresentation(
        timeline: Timeline,
        presentation: Timeline.Presentation,
    ): Boolean {
        return runCatchingUnlessCancelled {
            timelineDao.updatePreferredTimelinePresentation(
                TimelinePreferencesEntity.Partial.PreferredPresentation(
                    sourceId = timeline.sourceId,
                    preferredPresentation = presentation.key,
                )
            )
        }.isSuccess
    }

    override suspend fun updateHomeTimelines(
        timelines: List<Timeline.Home>,
    ): Boolean {
        val existing = runCatchingWithNetworkRetry {
            networkService.api.getPreferences()
        }
            .getOrNull()
            ?.preferences
            ?: return false

        runCatchingWithNetworkRetry {
            networkService.api.putPreferences(
                PutPreferencesRequest(
                    preferences = existing.map { preference ->
                        when (preference) {
                            is PreferencesUnion.SavedFeedsPrefV2 -> {
                                preference.updateFeedPreferencesFrom(timelines)
                            }

                            else -> preference
                        }
                    }
                )
            )
        }.getOrNull() ?: return false

        return authRepository.updateSignedInUser()
    }

    private fun <NetworkResponse : Any> nextCursorFlow(
        query: TimelineQuery,
        currentCursor: Cursor,
        currentRequestWithNextCursor: suspend () -> AtpResponse<NetworkResponse>,
        nextCursor: NetworkResponse.() -> String?,
        networkFeed: NetworkResponse.() -> List<FeedViewPost>,
    ): Flow<Cursor> = com.tunjid.heron.data.utilities.nextCursorFlow(
        currentCursor = currentCursor,
        currentRequestWithNextCursor = currentRequestWithNextCursor,
        nextCursor = nextCursor,
        onResponse = {
            multipleEntitySaverProvider.saveInTransaction {
                if (timelineDao.isFirstRequest(query)) {
                    timelineDao.deleteAllFeedsFor(query.timeline.sourceId)
                    timelineDao.insertOrPartiallyUpdateTimelineFetchedAt(
                        listOf(
                            TimelinePreferencesEntity(
                                sourceId = query.timeline.sourceId,
                                lastFetchedAt = query.data.cursorAnchor,
                                preferredPresentation = null,
                            )
                        )
                    )
                }
                add(
                    viewingProfileId = savedStateRepository.signedInProfileId,
                    timeline = query.timeline,
                    feedViewPosts = networkFeed(),
                )
            }
        }
    )

    private fun <T : Any> pollForTimelineUpdates(
        timeline: Timeline,
        pollInterval: Duration,
        networkRequestBlock: suspend () -> AtpResponse<T>,
        networkResponseToFeedViews: (T) -> List<FeedViewPost>,
    ) = flow {
        while (true) {
            val pollInstant = Clock.System.now()
            runCatchingWithNetworkRetry { networkRequestBlock() }
                .getOrNull()
                ?.let(networkResponseToFeedViews)
                ?.let { fetchedFeedViewPosts ->
                    multipleEntitySaverProvider.saveInTransaction {
                        add(
                            viewingProfileId = savedStateRepository.signedInProfileId,
                            timeline = timeline,
                            feedViewPosts = fetchedFeedViewPosts,
                        )
                    }
                }
                ?: continue

            emit(pollInstant)
            delay(pollInterval.inWholeMilliseconds)
        }
    }
        .flatMapLatest { pollInstant ->
            combine(
                timelineDao.lastFetchKey(timeline.sourceId)
                    .map { it?.lastFetchedAt ?: pollInstant }
                    .distinctUntilChangedBy(Instant::toEpochMilliseconds)
                    .flatMapLatest {
                        timelineDao.feedItems(
                            sourceId = timeline.sourceId,
                            before = it,
                            limit = 1,
                            offset = 0,
                        )
                    },
                timelineDao.feedItems(
                    sourceId = timeline.sourceId,
                    before = pollInstant,
                    limit = 1,
                    offset = 0,
                )
            ) { latestSeen, latestSaved ->
                latestSaved
                    .firstOrNull()
                    ?.id != latestSeen.firstOrNull()?.id
            }
        }

    private fun observeAndRefreshTimeline(
        query: TimelineQuery,
        nextCursorFlow: Flow<Cursor>,
    ): Flow<CursorList<TimelineItem>> =
        combine(
            observeTimeline(query),
            nextCursorFlow,
            ::CursorList,
        )

    private fun observeTimeline(
        query: TimelineQuery,
    ): Flow<List<TimelineItem>> =
        timelineDao.feedItems(
            sourceId = query.timeline.sourceId,
            before = query.data.cursorAnchor,
            offset = query.data.offset,
            limit = query.data.limit,
        )
            .flatMapLatest { itemEntities ->
                val postIds = itemEntities.flatMap {
                    listOfNotNull(
                        it.postId,
                        it.reply?.parentPostId,
                        it.reply?.rootPostId
                    )
                }
                    .toSet()
                combine(
                    postDao.posts(
                        postIds
                    ),
                    postDao.embeddedPosts(
                        postIds
                    ),
                    profileDao.profiles(
                        itemEntities.mapNotNull { it.reposter }
                    )
                ) { posts, embeddedPosts, repostProfiles ->
                    val idsToPosts = posts.associateBy { it.entity.cid }
                    val idsToEmbeddedPosts = embeddedPosts.associateBy { it.postId }
                    val idsToRepostProfiles = repostProfiles.associateBy { it.did }

                    itemEntities.map { entity ->
                        val mainPost = idsToPosts.getValue(entity.postId)
                        val replyParent = entity.reply?.let { idsToPosts[it.parentPostId] }
                        val replyRoot = entity.reply?.let { idsToPosts[it.rootPostId] }
                        val repostedBy = entity.reposter?.let { idsToRepostProfiles[it] }

                        when {
                            replyRoot != null && replyParent != null -> TimelineItem.Thread(
                                id = entity.id,
                                generation = null,
                                anchorPostIndex = 2,
                                hasBreak = entity.reply?.grandParentPostAuthorId != null,
                                posts = listOf(
                                    replyRoot.asExternalModel(
                                        quote = idsToEmbeddedPosts[replyRoot.entity.cid]
                                            ?.entity
                                            ?.asExternalModel(quote = null)
                                    ),
                                    replyParent.asExternalModel(
                                        quote = idsToEmbeddedPosts[replyParent.entity.cid]
                                            ?.entity
                                            ?.asExternalModel(quote = null)
                                    ),
                                    mainPost.asExternalModel(
                                        quote = idsToEmbeddedPosts[entity.postId]
                                            ?.entity
                                            ?.asExternalModel(quote = null)
                                    )
                                ),
                            )

                            repostedBy != null -> TimelineItem.Repost(
                                id = entity.id,
                                post = mainPost.asExternalModel(
                                    quote = idsToEmbeddedPosts[entity.postId]
                                        ?.entity
                                        ?.asExternalModel(quote = null)
                                ),
                                by = repostedBy.asExternalModel(),
                                at = entity.indexedAt,
                            )

                            entity.isPinned -> TimelineItem.Pinned(
                                id = entity.id,
                                post = mainPost.asExternalModel(
                                    quote = idsToEmbeddedPosts[entity.postId]
                                        ?.entity
                                        ?.asExternalModel(quote = null)
                                ),
                            )

                            else -> TimelineItem.Single(
                                id = entity.id,
                                post = mainPost.asExternalModel(
                                    quote = idsToEmbeddedPosts[entity.postId]
                                        ?.entity
                                        ?.asExternalModel(quote = null)
                                ),
                            )
                        }
                    }
                }
            }

    private fun followingTimeline(
        name: String,
        position: Int,
        isPinned: Boolean,
    ) = savedStateRepository.savedState
        .mapNotNull { it.auth?.authProfileId }
        .distinctUntilChanged()
        .flatMapLatest { signedInProfileId ->
            timelineDao.lastFetchKey(Constants.timelineFeed.uri)
                .distinctUntilChanged()
                .map { timelinePreferenceEntity ->
                    Timeline.Home.Following(
                        name = name,
                        position = position,
                        lastRefreshed = timelinePreferenceEntity?.lastFetchedAt,
                        presentation = timelinePreferenceEntity.preferredPresentation(),
                        signedInProfileId = signedInProfileId,
                        isPinned = isPinned,
                    )
                }
        }

    private fun feedGeneratorTimeline(
        uri: FeedGeneratorUri,
        position: Int,
        isPinned: Boolean,
    ) = feedGeneratorDao.feedGenerators(listOf(uri))
        .map(List<PopulatedFeedGeneratorEntity>::firstOrNull)
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { populatedFeedGeneratorEntity ->
            timelineDao.lastFetchKey(populatedFeedGeneratorEntity.entity.uri.uri)
                .distinctUntilChanged()
                .map { timelinePreferenceEntity ->
                    Timeline.Home.Feed(
                        position = position,
                        feedGenerator = populatedFeedGeneratorEntity.asExternalModel(),
                        lastRefreshed = timelinePreferenceEntity?.lastFetchedAt,
                        presentation = timelinePreferenceEntity.preferredPresentation(),
                        supportedPresentations = listOfNotNull(
                            Timeline.Presentation.Text.WithEmbed,
                            Timeline.Presentation.Media.Expanded.takeIf {
                                populatedFeedGeneratorEntity.entity.supportsMediaPresentation()
                            },
                            Timeline.Presentation.Media.Condensed.takeIf {
                                populatedFeedGeneratorEntity.entity.supportsMediaPresentation()
                            },
                        ),
                        isPinned = isPinned,
                    )
                }
        }
        .withRefresh {
            runCatchingWithNetworkRetry(times = 2) {
                networkService.api.getFeedGenerator(
                    GetFeedGeneratorQueryParams(
                        feed = uri.uri.let(::AtUri)
                    )
                )
            }
                .getOrNull()
                ?.view
                ?.let {
                    multipleEntitySaverProvider.saveInTransaction { add(it) }
                }
        }

    private fun listTimeline(
        uri: ListUri,
        position: Int,
        isPinned: Boolean,
    ) = listDao.list(uri.uri)
        .filterNotNull()
        .distinctUntilChangedBy(PopulatedListEntity::entity)
        .flatMapLatest {
            timelineDao.lastFetchKey(it.entity.uri.uri)
                .distinctUntilChanged()
                .map { timelinePreferenceEntity ->
                    Timeline.Home.List(
                        position = position,
                        feedList = it.asExternalModel(),
                        lastRefreshed = timelinePreferenceEntity?.lastFetchedAt,
                        presentation = timelinePreferenceEntity.preferredPresentation(),
                        isPinned = isPinned,
                    )
                }
        }
        .withRefresh {
            runCatchingWithNetworkRetry(times = 2) {
                networkService.api.getList(
                    GetListQueryParams(
                        cursor = null,
                        limit = 1,
                        list = uri.uri.let(::AtUri),
                    )
                )
            }
                .getOrNull()
                ?.list
                ?.let {
                    multipleEntitySaverProvider.saveInTransaction { add(it) }
                }
        }

    private fun starterPackTimeline(
        uri: StarterPackUri,
    ) = starterPackDao.starterPack(uri.uri)
        .mapNotNull { populatedStarterPackEntity ->
            populatedStarterPackEntity?.list?.let { populatedStarterPackEntity to it }
        }
        .filterNotNull()
        .distinctUntilChangedBy { it.first.entity }
        .flatMapLatest { (populatedStarterPackEntity, listEntity) ->
            listTimeline(
                uri = listEntity.uri,
                position = 0,
                isPinned = false,
            ).map { listTimeline ->
                Timeline.StarterPack(
                    starterPack = populatedStarterPackEntity.asExternalModel(),
                    listTimeline = listTimeline,
                )
            }
        }
        .withRefresh {
            runCatchingWithNetworkRetry(times = 2) {
                networkService.api.getStarterPack(
                    GetStarterPackQueryParams(
                        starterPack = uri.uri.let(::AtUri)
                    )
                )
            }
                .getOrNull()
                ?.starterPack
                ?.let {
                    multipleEntitySaverProvider.saveInTransaction { add(it) }
                }
        }

    private fun spinThread(
        list: List<TimelineItem.Thread>,
        thread: ThreadedPostEntity,
        post: Post,
    ) = when {
        list.isEmpty() || thread.generation == 0L -> list + TimelineItem.Thread(
            id = thread.postId.id,
            generation = thread.generation,
            anchorPostIndex = 0,
            hasBreak = false,
            posts = listOf(post),
        )

        thread.generation <= -1L -> list.dropLast(1) + list.last().let {
            it.copy(posts = it.posts + post)
        }

        list.last().posts.first().cid != thread.rootPostId -> list + TimelineItem.Thread(
            id = thread.postId.id,
            generation = thread.generation,
            anchorPostIndex = 0,
            hasBreak = false,
            posts = listOf(post),
        )

        else -> list.dropLast(1) + list.last().let {
            it.copy(posts = it.posts + post)
        }
    }
}


private fun TimelinePreferencesEntity?.preferredPresentation() =
    when (this?.preferredPresentation) {
        Timeline.Presentation.Media.Expanded.key -> Timeline.Presentation.Media.Expanded
        Timeline.Presentation.Media.Condensed.key -> Timeline.Presentation.Media.Condensed
        Timeline.Presentation.Text.WithEmbed.key -> Timeline.Presentation.Text.WithEmbed
        else -> Timeline.Presentation.Text.WithEmbed
    }

private suspend fun TimelineDao.isFirstRequest(query: TimelineQuery): Boolean {
    if (query.data.page != 0) return false
    val lastFetchedAt = lastFetchKey(query.timeline.sourceId).first()?.lastFetchedAt
    return lastFetchedAt?.toEpochMilliseconds() != query.data.cursorAnchor.toEpochMilliseconds()
}

private fun PreferencesUnion.SavedFeedsPrefV2.updateFeedPreferencesFrom(
    timelines: List<Timeline.Home>
): PreferencesUnion.SavedFeedsPrefV2 {
    val savedFeedValuesToIds = value.items.associateBy(
        keySelector = SavedFeed::value,
        valueTransform = SavedFeed::id
    )
    return PreferencesUnion.SavedFeedsPrefV2(
        SavedFeedsPrefV2(
            timelines.mapNotNull { timeline ->
                when (timeline) {
                    is Timeline.Home.Feed -> savedFeedValuesToIds[
                        timeline.feedGenerator.uri.uri
                    ]?.let { id ->
                        SavedFeed(
                            id = id,
                            type = Type.Feed,
                            value = timeline.feedGenerator.uri.uri,
                            pinned = timeline.isPinned,
                        )
                    }

                    is Timeline.Home.Following -> savedFeedValuesToIds[
                        "following"
                    ]?.let { id ->
                        SavedFeed(
                            id = id,
                            type = Type.Timeline,
                            value = "following",
                            pinned = timeline.isPinned,
                        )
                    }

                    is Timeline.Home.List -> savedFeedValuesToIds[
                        timeline.feedList.uri.uri
                    ]?.let { id ->
                        SavedFeed(
                            id = id,
                            type = Type.List,
                            value = timeline.feedList.uri.uri,
                            pinned = timeline.isPinned,
                        )
                    }
                }
            }
        )
    )
}

private fun FeedGeneratorEntity.supportsMediaPresentation() =
    when (contentMode) {
        Token.ContentModeVideo.value,
        "app.bsky.feed.defs#contentModeVideo",
        "app.bsky.feed.defs#contentModePhoto",
        "app.bsky.feed.defs#contentModeImage",
        "app.bsky.feed.defs#contentModeMedia",
            -> true

        else -> false
    }

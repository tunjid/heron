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
import app.bsky.graph.GetListQueryParams
import com.tunjid.heron.data.core.models.Constants
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.UriLookup
import com.tunjid.heron.data.core.models.value
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.daos.TimelineDao
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.ThreadedPopulatedPostEntity
import com.tunjid.heron.data.database.entities.TimelineFetchKeyEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.utilities.CursorQuery
import com.tunjid.heron.data.utilities.InvalidationTrackerDebounceMillis
import com.tunjid.heron.data.utilities.lookupUri
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.runCatchingWithNetworkRetry
import com.tunjid.heron.data.utilities.withRefresh
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
import me.tatarka.inject.annotations.Inject
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.response.AtpResponse
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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

    fun lookupTimeline(
        lookup: UriLookup.Timeline,
    ): Flow<Timeline>

    fun hasUpdates(
        timeline: Timeline,
    ): Flow<Boolean>

    fun timelineItems(
        query: TimelineQuery,
        cursor: Cursor,
    ): Flow<CursorList<TimelineItem>>

    fun postThreadedItems(
        postUri: Uri,
    ): Flow<List<TimelineItem>>
}

@Inject
class OfflineTimelineRepository(
    private val postDao: PostDao,
    private val profileDao: ProfileDao,
    private val timelineDao: TimelineDao,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
    private val networkService: NetworkService,
    private val savedStateRepository: SavedStateRepository,
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
        }
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
                            filter = GetAuthorFeedFilter.PostsNoReplies,
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
                            filter = GetAuthorFeedFilter.PostsNoReplies,
                        )
                    )
                },
                networkResponseToFeedViews = GetAuthorFeedResponse::feed,
            )
        }
    }

    override fun postThreadedItems(
        postUri: Uri,
    ): Flow<List<TimelineItem>> =
        postDao.postEntitiesByUri(postUris = setOf(postUri))
            .mapNotNull { it.firstOrNull() }
            .take(1)
            .flatMapLatest { postEntity ->
                postDao.postThread(postId = postEntity.cid.id)
                    .map {
                        it.fold(
                            initial = emptyList(),
                            operation = ::spinThread,
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
                            atUri = AtUri(preference.value),
                            position = index,
                        )

                        Type.List -> listTimeline(
                            atUri = AtUri(preference.value),
                            position = index,
                        )

                        Type.Timeline -> timelineDao.lastFetchKey(Constants.timelineFeed.uri)
                            .distinctUntilChanged()
                            .map { fetchKeyEntity ->
                                Timeline.Home.Following(
                                    name = preference.value,
                                    position = index,
                                    lastRefreshed = fetchKeyEntity?.lastFetchedAt,
                                )
                            }

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
                    .distinctUntilChangedBy {
                        it.joinToString(
                            separator = "-",
                            transform = Timeline.Home::name,
                        )
                    }
                    .filter(List<Timeline.Home>::isNotEmpty)
            }
            .distinctUntilChanged()
            .debounce(InvalidationTrackerDebounceMillis)

    override fun lookupTimeline(
        lookup: UriLookup.Timeline,
    ): Flow<Timeline> = flow {
        val atUri = lookupUri(
            networkService = networkService,
            profileDao = profileDao,
            uriLookup = lookup,
        ) ?: return@flow

        emitAll(
            when (lookup) {
                is UriLookup.Timeline.FeedGenerator -> feedGeneratorTimeline(
                    atUri = atUri,
                    position = 0
                )
                    .withRefresh {
                        runCatchingWithNetworkRetry(times = 2) {
                            networkService.api.getFeedGenerator(
                                GetFeedGeneratorQueryParams(
                                    feed = atUri
                                )
                            )
                        }
                            .getOrNull()
                            ?.view
                            ?.let {
                                multipleEntitySaverProvider.saveInTransaction { add(it) }
                            }
                    }

                is UriLookup.Timeline.List -> listTimeline(
                    atUri = atUri,
                    position = 0
                )
                    .withRefresh {
                        runCatchingWithNetworkRetry(times = 2) {
                            networkService.api.getList(
                                GetListQueryParams(
                                    cursor = null,
                                    limit = 1,
                                    list = atUri,
                                )
                            )
                        }
                            .getOrNull()
                            ?.list
                            ?.let {
                                multipleEntitySaverProvider.saveInTransaction { add(it) }
                            }
                    }

                is UriLookup.Timeline.Profile -> {
                    profileDao.profiles(
                        ids = listOf(Id(lookup.profileHandleOrDid))
                    )
                        .mapNotNull(List<ProfileEntity>::firstOrNull)
                        .distinctUntilChangedBy(ProfileEntity::did)
                        .map {
                            Timeline.Profile(
                                profileId = it.did,
                                type = lookup.type,
                            )
                        }
                }
            }
        )
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
                    timelineDao.upsertFeedFetchKey(
                        TimelineFetchKeyEntity(
                            sourceId = query.timeline.sourceId,
                            lastFetchedAt = query.data.cursorAnchor,
                            filterDescription = null,
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
            offset = query.data.page * query.data.limit,
            limit = query.data.limit,
        )
            .flatMapLatest { itemEntities ->
                combine(
                    postDao.posts(
                        itemEntities.flatMap {
                            listOfNotNull(
                                it.postId,
                                it.reply?.parentPostId,
                                it.reply?.rootPostId
                            )
                        }
                            .toSet()
                    ),
                    postDao.embeddedPosts(
                        itemEntities.map { it.postId }.toSet()
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

    private fun feedGeneratorTimeline(
        atUri: AtUri,
        position: Int,
    ) = timelineDao.feedGenerator(atUri.atUri)
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest {
            timelineDao.lastFetchKey(it.uri.uri)
                .distinctUntilChanged()
                .map { fetchKeyEntity ->
                    Timeline.Home.Feed(
                        position = position,
                        feedGenerator = it.asExternalModel(),
                        lastRefreshed = fetchKeyEntity?.lastFetchedAt,
                    )
                }
        }

    private fun listTimeline(
        atUri: AtUri,
        position: Int,
    ) = timelineDao.list(atUri.atUri)
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest {
            timelineDao.lastFetchKey(it.uri.uri)
                .distinctUntilChanged()
                .map { fetchKeyEntity ->
                    Timeline.Home.List(
                        position = position,
                        feedList = it.asExternalModel(),
                        lastRefreshed = fetchKeyEntity?.lastFetchedAt,
                    )
                }
        }

    private fun spinThread(
        list: List<TimelineItem.Thread>,
        thread: ThreadedPopulatedPostEntity,
    ) = when {
        list.isEmpty() || thread.generation == 0L -> list + TimelineItem.Thread(
            id = thread.postId.id,
            generation = thread.generation,
            anchorPostIndex = 0,
            hasBreak = false,
            posts = listOf(
                thread.entity.asExternalModel(
                    quote = null
                )
            )
        )

        thread.generation <= -1L -> list.dropLast(1) + list.last().let {
            it.copy(
                posts = it.posts + thread.entity.asExternalModel(quote = null)
            )
        }

        list.last().posts.first().cid != thread.rootPostId -> list + TimelineItem.Thread(
            id = thread.postId.id,
            generation = thread.generation,
            anchorPostIndex = 0,
            hasBreak = false,
            posts = listOf(
                thread.entity.asExternalModel(
                    quote = null
                )
            )
        )

        else -> list.dropLast(1) + list.last().let {
            it.copy(
                posts = it.posts + thread.entity.asExternalModel(quote = null)
            )
        }
    }
}

private suspend fun TimelineDao.isFirstRequest(query: TimelineQuery): Boolean {
    if (query.data.page != 0) return false
    val lastFetchedAt = lastFetchKey(query.timeline.sourceId).first()?.lastFetchedAt
    return lastFetchedAt?.toEpochMilliseconds() != query.data.cursorAnchor.toEpochMilliseconds()
}

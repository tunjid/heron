package com.tunjid.heron.data.repository

import app.bsky.actor.Type
import app.bsky.feed.FeedViewPost
import app.bsky.feed.GetAuthorFeedFilter
import app.bsky.feed.GetAuthorFeedQueryParams
import app.bsky.feed.GetAuthorFeedResponse
import app.bsky.feed.GetFeedQueryParams
import app.bsky.feed.GetFeedResponse
import app.bsky.feed.GetListFeedQueryParams
import app.bsky.feed.GetListFeedResponse
import app.bsky.feed.GetPostThreadQueryParams
import app.bsky.feed.GetPostThreadResponseThreadUnion
import app.bsky.feed.GetTimelineQueryParams
import app.bsky.feed.GetTimelineResponse
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.NetworkCursor
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.cursor
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.TransactionWriter
import com.tunjid.heron.data.database.daos.EmbedDao
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.daos.TimelineDao
import com.tunjid.heron.data.database.entities.PostThreadEntity
import com.tunjid.heron.data.database.entities.ThreadedPopulatedPostEntity
import com.tunjid.heron.data.database.entities.TimelineFetchKeyEntity
import com.tunjid.heron.data.database.entities.TimelineItemEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.network.models.feedItemEntity
import com.tunjid.heron.data.network.models.postEntity
import com.tunjid.heron.data.network.models.postViewerStatisticsEntity
import com.tunjid.heron.data.network.models.profileEntity
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaver
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.runCatchingWithNetworkRetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.take
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import me.tatarka.inject.annotations.Inject
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.response.AtpResponse

class TimelineQuery(
    val data: Data,
    val timeline: Timeline,
) {
    @Serializable
    data class Data(
        val page: Int,

        val firstRequestInstant: Instant,

        /**
         * How many items to fetch for a query.
         */
        val limit: Long = 50L,
    )

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

    fun timelineItems(
        query: TimelineQuery,
        networkCursor: NetworkCursor,
    ): Flow<CursorList<TimelineItem>>

    fun postThreadedItems(
        postUri: Uri,
    ): Flow<List<TimelineItem>>
}

@Inject
class OfflineTimelineRepository(
    private val embedDao: EmbedDao,
    private val postDao: PostDao,
    private val profileDao: ProfileDao,
    private val timelineDao: TimelineDao,
    private val transactionWriter: TransactionWriter,
    private val networkService: NetworkService,
    private val savedStateRepository: SavedStateRepository,
) : TimelineRepository {

    override fun timelineItems(
        query: TimelineQuery,
        networkCursor: NetworkCursor,
    ): Flow<CursorList<TimelineItem>> = when (val timeline = query.timeline) {
        is Timeline.Home.Following -> fetchTimeline(
            query = query,
            networkCursorFlow = networkCursorFlow(
                query = query,
                currentNetworkCursor = networkCursor,
                networkRequest = {
                    networkService.api.getTimeline(
                        GetTimelineQueryParams(
                            limit = it.data.limit,
                            cursor = networkCursor.cursor,
                        )
                    )
                },
                nextNetworkCursor = GetTimelineResponse::cursor,
                networkFeed = GetTimelineResponse::feed,
            )
        )

        is Timeline.Home.Feed -> fetchTimeline(
            query = query,
            networkCursorFlow = networkCursorFlow(
                query = query,
                currentNetworkCursor = networkCursor,
                networkRequest = {
                    networkService.api.getFeed(
                        GetFeedQueryParams(
                            feed = AtUri(timeline.feedUri.uri),
                            limit = it.data.limit,
                            cursor = networkCursor.cursor,
                        )
                    )
                },
                nextNetworkCursor = GetFeedResponse::cursor,
                networkFeed = GetFeedResponse::feed,
            )
        )

        is Timeline.Home.List -> fetchTimeline(
            query = query,
            networkCursorFlow = networkCursorFlow(
                query = query,
                currentNetworkCursor = networkCursor,
                networkRequest = {
                    networkService.api.getListFeed(
                        GetListFeedQueryParams(
                            list = AtUri(timeline.listUri.uri),
                            limit = it.data.limit,
                            cursor = networkCursor.cursor,
                        )
                    )
                },
                nextNetworkCursor = GetListFeedResponse::cursor,
                networkFeed = GetListFeedResponse::feed,
            )
        )

        is Timeline.Profile.Media -> fetchTimeline(
            query = query,
            networkCursorFlow = networkCursorFlow(
                query = query,
                currentNetworkCursor = networkCursor,
                networkRequest = {
                    networkService.api.getAuthorFeed(
                        GetAuthorFeedQueryParams(
                            actor = Did(timeline.profileId.id),
                            limit = it.data.limit,
                            cursor = networkCursor.cursor,
                            filter = GetAuthorFeedFilter.PostsWithMedia,
                        )
                    )
                },
                nextNetworkCursor = GetAuthorFeedResponse::cursor,
                networkFeed = GetAuthorFeedResponse::feed,
            )
        )

        is Timeline.Profile.Posts -> fetchTimeline(
            query = query,
            networkCursorFlow = networkCursorFlow(
                query = query,
                currentNetworkCursor = networkCursor,
                networkRequest = {
                    networkService.api.getAuthorFeed(
                        GetAuthorFeedQueryParams(
                            actor = Did(timeline.profileId.id),
                            limit = it.data.limit,
                            cursor = networkCursor.cursor,
                            filter = GetAuthorFeedFilter.PostsNoReplies,
                        )
                    )
                },
                nextNetworkCursor = GetAuthorFeedResponse::cursor,
                networkFeed = GetAuthorFeedResponse::feed,
            )
        )

        is Timeline.Profile.Replies -> fetchTimeline(
            query = query,
            networkCursorFlow = networkCursorFlow(
                query = query,
                currentNetworkCursor = networkCursor,
                networkRequest = {
                    networkService.api.getAuthorFeed(
                        GetAuthorFeedQueryParams(
                            actor = Did(timeline.profileId.id),
                            limit = it.data.limit,
                            cursor = networkCursor.cursor,
                            filter = GetAuthorFeedFilter.PostsWithReplies,
                        )
                    )
                },
                nextNetworkCursor = GetAuthorFeedResponse::cursor,
                networkFeed = GetAuthorFeedResponse::feed,
            )
        )
    }

    override fun postThreadedItems(
        postUri: Uri,
    ): Flow<List<TimelineItem>> =
        merge(
            flow {
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
                                val authProfileId =
                                    savedStateRepository.savedState.value.auth?.authProfileId
                                if (authProfileId != null)
                                    multipleEntitySaver().apply {
                                        add(
                                            viewingProfileId = authProfileId,
                                            threadViewPost = thread.value,
                                        )
                                        saveInTransaction()
                                    }
                            }

                            is GetPostThreadResponseThreadUnion.Unknown -> Unit
                        }
                    }
            },
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
                },
        )

    override fun homeTimelines(): Flow<List<Timeline.Home>> =
        savedStateRepository.savedState
            .mapNotNull { it.preferences?.timelinePreferences }
            .distinctUntilChanged()
            .flatMapLatest { timelinePreferences ->
                timelinePreferences.mapIndexed { index, preference ->
                    when (Type.safeValueOf(preference.type)) {
                        Type.Feed -> timelineDao.feedGenerator(preference.value)
                            .filterNotNull()
                            .map {
                                Timeline.Home.Feed(
                                    name = it.displayName,
                                    feedUri = it.uri,
                                    position = index,
                                )
                            }

                        Type.List -> timelineDao.list(preference.value)
                            .filterNotNull()
                            .map {
                                Timeline.Home.List(
                                    listUri = it.uri,
                                    name = it.name,
                                    position = index,
                                )
                            }

                        Type.Timeline -> flowOf(
                            Timeline.Home.Following(
                                name = preference.value,
                                position = index,
                            )
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
                    .distinctUntilChangedBy {
                        it.joinToString(
                            separator = "-",
                            transform = Timeline.Home::name,
                        )
                    }
                    .filter(List<Timeline.Home>::isNotEmpty)
                    // Debounce for about 2 frames on a 60 hz display
                    .debounce(32)
            }

    private fun <NetworkResponse : Any> networkCursorFlow(
        query: TimelineQuery,
        currentNetworkCursor: NetworkCursor,
        networkRequest: suspend (TimelineQuery) -> AtpResponse<NetworkResponse>,
        nextNetworkCursor: NetworkResponse.() -> String?,
        networkFeed: NetworkResponse.() -> List<FeedViewPost>,
    ): Flow<NetworkCursor> = flow {
        // Emit pending downstream
        emit(NetworkCursor.Pending)

        // Do nothing, can't tell what the next items are
        if (currentNetworkCursor == NetworkCursor.Pending) return@flow

        runCatchingWithNetworkRetry {
            networkRequest(query)
        }
            .getOrNull()
            ?.let { response ->
                response.nextNetworkCursor()
                    ?.let(NetworkCursor::Next)
                    ?.let { emit(it) }

                val authProfileId = savedStateRepository.savedState.value.auth?.authProfileId
                if (authProfileId != null) multipleEntitySaver().persistTimeline(
                    feedViews = response.networkFeed(),
                    query = query,
                    viewingProfileId = authProfileId,
                )
            }
    }

    private fun fetchTimeline(
        query: TimelineQuery,
        networkCursorFlow: Flow<NetworkCursor>,
    ): Flow<CursorList<TimelineItem>> =
        combine(
            observeTimeline(query),
            networkCursorFlow,
            ::CursorList,
        )

    private suspend fun MultipleEntitySaver.persistTimeline(
        viewingProfileId: Id,
        feedViews: List<FeedViewPost>,
        query: TimelineQuery,
    ) {
        val feedItemEntities = mutableListOf<TimelineItemEntity>()

        for (feedView in feedViews) {
            // Extract data from feed
            feedItemEntities.add(feedView.feedItemEntity(query.timeline.sourceId))

            // Extract data from post
            add(
                viewingProfileId = viewingProfileId,
                postView = feedView.post,
            )
            feedView.reason?.profileEntity()?.let(::add)

            feedView.reply?.let {
                it.root.postEntity().let(::add)
                it.root.profileEntity()?.let(::add)
                it.root.postViewerStatisticsEntity()?.let(::add)

                val parentPostEntity = it.parent.postEntity().also(::add)
                it.parent.profileEntity()?.let(::add)
                it.parent.postViewerStatisticsEntity()?.let(::add)

                it.grandparentAuthor?.profileEntity()?.let(::add)

                add(
                    PostThreadEntity(
                        postId = feedView.post.postEntity().cid,
                        parentPostId = parentPostEntity.cid,
                    )
                )
            }
        }

        saveInTransaction(
            beforeSave = {
                if (timelineDao.isFirstRequest(query)) {
                    timelineDao.deleteAllFeedsFor(query.timeline.sourceId)
                    timelineDao.upsertFeedFetchKey(
                        TimelineFetchKeyEntity(
                            sourceId = query.timeline.sourceId,
                            lastFetchedAt = query.data.firstRequestInstant,
                            filterDescription = null,
                        )
                    )
                }
            },
            afterSave = {
                timelineDao.upsertTimelineItems(feedItemEntities)
            }
        )
    }

    private fun observeTimeline(
        query: TimelineQuery,
    ): Flow<List<TimelineItem>> =
        timelineDao.feedItems(
            sourceId = query.timeline.sourceId,
            before = query.data.firstRequestInstant,
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

    private fun spinThread(
        list: List<TimelineItem.Thread>,
        thread: ThreadedPopulatedPostEntity,
    ) = when {
        list.isEmpty() || thread.generation == 0L -> list + TimelineItem.Thread(
            id = thread.postId.id,
            generation = thread.generation,
            anchorPostIndex = 0,
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

    private fun multipleEntitySaver() = MultipleEntitySaver(
        postDao = postDao,
        embedDao = embedDao,
        profileDao = profileDao,
        timelineDao = timelineDao,
        transactionWriter = transactionWriter,
    )
}

private suspend fun TimelineDao.isFirstRequest(query: TimelineQuery): Boolean {
    if (query.data.page != 0) return false
    val lastFetchedAt = lastFetchKey(query.timeline.sourceId)?.lastFetchedAt
    return lastFetchedAt?.toEpochMilliseconds() != query.data.firstRequestInstant.toEpochMilliseconds()
}

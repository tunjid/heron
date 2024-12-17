package com.tunjid.heron.data.repository

import app.bsky.feed.FeedViewPost
import app.bsky.feed.GetAuthorFeedQueryParams
import app.bsky.feed.GetAuthorFeedResponse
import app.bsky.feed.GetTimelineQueryParams
import app.bsky.feed.GetTimelineResponse
import com.tunjid.heron.data.MultipleEntitySaver
import com.tunjid.heron.data.add
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.NetworkCursor
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.cursor
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.TransactionWriter
import com.tunjid.heron.data.database.daos.EmbedDao
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.daos.TimelineDao
import com.tunjid.heron.data.database.entities.PostEntity
import com.tunjid.heron.data.database.entities.PostThreadEntity
import com.tunjid.heron.data.database.entities.TimelineFetchKeyEntity
import com.tunjid.heron.data.database.entities.TimelineItemEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.database.entities.emptyProfileEntity
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.network.models.feedItemEntity
import com.tunjid.heron.data.network.models.postEntity
import com.tunjid.heron.data.network.models.postViewerStatisticsEntity
import com.tunjid.heron.data.network.models.profileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import me.tatarka.inject.annotations.Inject
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.response.AtpResponse

sealed interface TimelineQuery {
    @Serializable
    data class Data(
        val page: Int,

        val firstRequestInstant: Instant,

        /**
         * How many items to fetch for a query.
         */
        val limit: Long = 50L,
    )

    val data: Data

    @Serializable
    data class Home(
        /**
         * The backing source of the feed, be it a list or other feed generator output.
         * It is null for a signed in user's timeline.
         */
        val source: Uri,
        override val data: Data,
    ) : TimelineQuery

    @Serializable
    data class Profile(
        val profileId: Id,
        override val data: Data,
    ) : TimelineQuery
}

private val TimelineQuery.sourceId
    get() = when (this) {
        is TimelineQuery.Home -> source.uri
        is TimelineQuery.Profile -> profileId.id
    }

interface TimelineRepository {
    fun timeline(
        query: TimelineQuery.Home,
        networkCursor: NetworkCursor,
    ): Flow<CursorList<TimelineItem>>

    fun profileTimeline(
        query: TimelineQuery.Profile,
        networkCursor: NetworkCursor,
    ): Flow<CursorList<TimelineItem>>
}

@Inject
class OfflineTimelineRepository(
    embedDao: EmbedDao,
    private val postDao: PostDao,
    private val profileDao: ProfileDao,
    private val timelineDao: TimelineDao,
    private val networkService: NetworkService,
    private val savedStateRepository: SavedStateRepository,
    transactionWriter: TransactionWriter,
) : TimelineRepository {

    private val multipleEntitySaver = MultipleEntitySaver(
        postDao = postDao,
        embedDao = embedDao,
        profileDao = profileDao,
        transactionWriter = transactionWriter,
    )

    override fun timeline(
        query: TimelineQuery.Home,
        networkCursor: NetworkCursor,
    ): Flow<CursorList<TimelineItem>> = fetchFeed(
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

    override fun profileTimeline(
        query: TimelineQuery.Profile,
        networkCursor: NetworkCursor,
    ): Flow<CursorList<TimelineItem>> = fetchFeed(
        query = query,
        networkCursorFlow = networkCursorFlow(
            query = query,
            currentNetworkCursor = networkCursor,
            networkRequest = {
                networkService.api.getAuthorFeed(
                    GetAuthorFeedQueryParams(
                        actor = Did(it.profileId.id),
                        limit = it.data.limit,
                        cursor = networkCursor.cursor,
                    )
                )
            },
            nextNetworkCursor = GetAuthorFeedResponse::cursor,
            networkFeed = GetAuthorFeedResponse::feed,
        )
    )

    private fun <Query : TimelineQuery, NetworkResponse : Any> networkCursorFlow(
        query: Query,
        currentNetworkCursor: NetworkCursor,
        networkRequest: suspend (Query) -> AtpResponse<NetworkResponse>,
        nextNetworkCursor: NetworkResponse.() -> String?,
        networkFeed: NetworkResponse.() -> List<FeedViewPost>,
    ): Flow<NetworkCursor> = flow {
        // Emit pending downstream
        emit(NetworkCursor.Pending)

        // Do nothing, can't tell what the next items are
        if (currentNetworkCursor == NetworkCursor.Pending) return@flow

        kotlin.runCatching {
            when (val networkPostsResponse = networkRequest(query)) {
                is AtpResponse.Failure -> {
                    // TODO Exponential backoff / network monitoring
                }

                is AtpResponse.Success -> {
                    networkPostsResponse.response.nextNetworkCursor()
                        ?.let(NetworkCursor::Next)
                        ?.let { emit(it) }

                    val authProfileId = savedStateRepository.savedState.value.auth?.authProfileId
                    if (authProfileId != null) multipleEntitySaver.persistTimeline(
                        feedViews = networkPostsResponse.response.networkFeed(),
                        query = query,
                        viewingProfileId = authProfileId,
                    )
                }
            }
        }
    }

    private fun fetchFeed(
        query: TimelineQuery,
        networkCursorFlow: Flow<NetworkCursor>,
    ): Flow<CursorList<TimelineItem>> = flow {
        emitAll(
            combine(
                observeTimeline(query),
                networkCursorFlow,
                ::CursorList,
            )
        )
    }

    private suspend fun MultipleEntitySaver.persistTimeline(
        viewingProfileId: Id,
        feedViews: List<FeedViewPost>,
        query: TimelineQuery,
    ) {
        val feedItemEntities = mutableListOf<TimelineItemEntity>()

        // Add the signed in user
        add(emptyProfileEntity(viewingProfileId))

        for (feedView in feedViews) {
            // Extract data from feed
            feedItemEntities.add(feedView.feedItemEntity(query.sourceId))

            // Extract data from post
            add(
                viewingProfileId = viewingProfileId,
                postView = feedView.post,
            )
            feedView.reason?.profileEntity()?.let(::add)

            feedView.reply?.let {
                val rootPostEntity = it.root.postEntity().also(::add)
                it.root.profileEntity()?.let(::add)
                it.root.postViewerStatisticsEntity()?.let(::add)

                val parentPostEntity = it.parent.postEntity().also(::add)
                it.parent.profileEntity()?.let(::add)
                it.parent.postViewerStatisticsEntity()?.let(::add)

                add(
                    PostThreadEntity(
                        postId = feedView.post.postEntity().cid,
                        parentPostId = parentPostEntity.cid,
                        rootPostId = rootPostEntity.cid
                    )
                )
            }
        }

        saveInTransaction(
            beforeSave = {
                if (timelineDao.isFirstRequest(query)) {
                    timelineDao.deleteAllFeedsFor(query.sourceId)
                    timelineDao.upsertFeedFetchKey(
                        TimelineFetchKeyEntity(
                            sourceId = query.sourceId,
                            lastFetchedAt = query.data.firstRequestInstant
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
        query: TimelineQuery
    ): Flow<List<TimelineItem>> =
        timelineDao.feedItems(
            sourceId = query.sourceId,
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
                            replyRoot != null && replyParent != null -> TimelineItem.Reply(
                                id = entity.id,
                                sourceId = query.sourceId,
                                post = mainPost.asExternalModel(
                                    quote = idsToEmbeddedPosts[entity.postId]
                                        ?.entity
                                        ?.asExternalModel(quote = null)
                                ),
                                rootPost = replyRoot.asExternalModel(
                                    quote = idsToEmbeddedPosts[replyRoot.entity.cid]
                                        ?.entity
                                        ?.asExternalModel(quote = null)
                                ),
                                parentPost = replyParent.asExternalModel(
                                    quote = idsToEmbeddedPosts[replyParent.entity.cid]
                                        ?.entity
                                        ?.asExternalModel(quote = null)
                                ),
                            )

                            repostedBy != null -> TimelineItem.Repost(
                                id = entity.id,
                                sourceId = query.sourceId,
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
                                sourceId = query.sourceId,
                                post = mainPost.asExternalModel(
                                    quote = idsToEmbeddedPosts[entity.postId]
                                        ?.entity
                                        ?.asExternalModel(quote = null)
                                ),
                            )

                            else -> TimelineItem.Single(
                                id = entity.id,
                                sourceId = query.sourceId,
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
}

private suspend fun TimelineDao.isFirstRequest(query: TimelineQuery): Boolean {
    if (query.data.page != 0) return false
    val lastFetchedAt = lastFetchKey(query.sourceId)?.lastFetchedAt
    return lastFetchedAt?.toEpochMilliseconds() != query.data.firstRequestInstant.toEpochMilliseconds()
}

package com.tunjid.heron.data.repository

import app.bsky.feed.FeedViewPost
import app.bsky.feed.GetAuthorFeedQueryParams
import app.bsky.feed.GetAuthorFeedResponse
import app.bsky.feed.GetTimelineQueryParams
import app.bsky.feed.GetTimelineResponse
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.TransactionWriter
import com.tunjid.heron.data.database.daos.EmbedDao
import com.tunjid.heron.data.database.daos.FeedDao
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.entities.FeedFetchKeyEntity
import com.tunjid.heron.data.database.entities.TimelineItemEntity
import com.tunjid.heron.data.database.entities.PostEntity
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.database.entities.postembeds.ExternalEmbedEntity
import com.tunjid.heron.data.database.entities.postembeds.ImageEntity
import com.tunjid.heron.data.database.entities.postembeds.PostEmbed
import com.tunjid.heron.data.database.entities.postembeds.PostExternalEmbedEntity
import com.tunjid.heron.data.database.entities.postembeds.PostImageEntity
import com.tunjid.heron.data.database.entities.postembeds.PostPostEntity
import com.tunjid.heron.data.database.entities.postembeds.PostVideoEntity
import com.tunjid.heron.data.database.entities.postembeds.VideoEntity
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.network.models.embedEntities
import com.tunjid.heron.data.network.models.feedItemEntity
import com.tunjid.heron.data.network.models.postEntity
import com.tunjid.heron.data.network.models.postExternalEmbedEntity
import com.tunjid.heron.data.network.models.postImageEntity
import com.tunjid.heron.data.network.models.postVideoEntity
import com.tunjid.heron.data.network.models.profileEntity
import com.tunjid.heron.data.network.models.quotedPostEmbedEntities
import com.tunjid.heron.data.network.models.quotedPostEntity
import com.tunjid.heron.data.network.models.quotedPostProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import me.tatarka.inject.annotations.Inject
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.response.AtpResponse

sealed interface TimelineQuery {
    val page: Int

    /**
     * The instant the first request was made, as pagination is only valid for [Home] values
     * that all share the same [firstRequestInstant].
     */
    val firstRequestInstant: Instant

    /**
     * How many items to fetch for a query.
     */
    val limit: Long get() = 50

    /**
     * The cursor used to fetch items. Null if this is the first request.
     */
    val nextItemCursor: CursorList.DoubleCursor?

    @Serializable
    data class Home(
        override val page: Int,
        /**
         * The backing source of the feed, be it a list or other feed generator output.
         * It is null for a signed in user's timeline.
         */
        val source: Uri,
        /**
         * The instant the first request was made, as pagination is only valid for [Home] values
         * that all share the same [firstRequestInstant].
         */
        override val firstRequestInstant: Instant,

        /**
         * The cursor used to fetch items. Null if this is the first request.
         */
        override val nextItemCursor: CursorList.DoubleCursor? = null,
    ) : TimelineQuery

    @Serializable
    data class Profile(
        val profileId: Id,
        override val page: Int,

        /**
         * The instant the first request was made, as pagination is only valid for [Home] values
         * that all share the same [firstRequestInstant].
         */
        override val firstRequestInstant: Instant,

        /**
         * The cursor used to fetch items. Null if this is the first request.
         */
        override val nextItemCursor: CursorList.DoubleCursor? = null,
    ) : TimelineQuery
}

val TimelineQuery.sourceId
    get() = when (this) {
        is TimelineQuery.Home -> source.uri
        is TimelineQuery.Profile -> profileId.id
    }

interface TimelineRepository {
    fun timeline(query: TimelineQuery.Home): Flow<CursorList<TimelineItem>>
    fun profileTimeline(query: TimelineQuery.Profile): Flow<CursorList<TimelineItem>>
}

@Inject
class OfflineTimelineRepository(
    private val feedDao: FeedDao,
    private val postDao: PostDao,
    private val embedDao: EmbedDao,
    private val profileDao: ProfileDao,
    private val transactionWriter: TransactionWriter,
    private val networkService: NetworkService,
) : TimelineRepository {

    override fun timeline(
        query: TimelineQuery.Home
    ): Flow<CursorList<TimelineItem>> = fetchFeed(
        query = query,
        networkCursorFlow = networkCursorFlow(
            query = query,
            network = {
                networkService.api.getTimeline(
                    GetTimelineQueryParams(
                        limit = it.limit,
                        cursor = it.nextItemCursor?.remote,
                    )
                )
            },
            cursor = GetTimelineResponse::cursor,
            feed = GetTimelineResponse::feed,
        )
    )

    override fun profileTimeline(
        query: TimelineQuery.Profile
    ): Flow<CursorList<TimelineItem>> = fetchFeed(
        query = query,
        networkCursorFlow = networkCursorFlow(
            query = query,
            network = {
                networkService.api.getAuthorFeed(
                    GetAuthorFeedQueryParams(
                        actor = Did(it.profileId.id),
                        limit = it.limit,
                        cursor = it.nextItemCursor?.remote,
                    )
                )
            },
            cursor = GetAuthorFeedResponse::cursor,
            feed = GetAuthorFeedResponse::feed,
        )
    )

    private fun <Query : TimelineQuery, NetworkResponse : Any> networkCursorFlow(
        query: Query,
        network: suspend (Query) -> AtpResponse<NetworkResponse>,
        cursor: NetworkResponse.() -> String?,
        feed: NetworkResponse.() -> List<FeedViewPost>,
    ) = flow {
        emit(null)
        kotlin.runCatching {
            when (val networkPostsResponse = network(query)) {
                is AtpResponse.Failure -> {
                    // TODO Exponential backoff / network monitoring
                }

                is AtpResponse.Success -> {
                    emit(networkPostsResponse.response.cursor())
                    transactionWriter.saveFeed(networkPostsResponse.response.feed(), query)
                }
            }
        }
    }

    private fun fetchFeed(
        query: TimelineQuery,
        networkCursorFlow: Flow<String?>,
    ): Flow<CursorList<TimelineItem>> = flow {
        emitAll(
            combine(
                networkCursorFlow,
                readFeed(query),
            ) { networkCursor, feed ->
                CursorList(
                    items = feed,
                    nextCursor = CursorList.DoubleCursor(
                        remote = networkCursor,
                        local = feed.lastOrNull()?.indexedAt
                    )
                )
            }
        )
    }

    private suspend fun TransactionWriter.saveFeed(
        feedViews: List<FeedViewPost>,
        query: TimelineQuery
    ) {
        val feedItemEntities = mutableListOf<TimelineItemEntity>()
        val postEntities = mutableListOf<PostEntity>()
        val profileEntities = mutableListOf<ProfileEntity>()
        val postPostEntities = mutableListOf<PostPostEntity>()

        val externalEmbedEntities = mutableListOf<ExternalEmbedEntity>()
        val postExternalEmbedEntities = mutableListOf<PostExternalEmbedEntity>()

        val imageEntities = mutableListOf<ImageEntity>()
        val postImageEntities = mutableListOf<PostImageEntity>()

        val videoEntities = mutableListOf<VideoEntity>()
        val postVideoEntities = mutableListOf<PostVideoEntity>()

        for (feedView in feedViews) {
            // Extract data from feed
            feedItemEntities.add(feedView.feedItemEntity(query.sourceId))

            feedView.reply?.let {
                postEntities.add(it.root.postEntity())
                it.root.profileEntity()?.let(profileEntities::add)

                postEntities.add(it.parent.postEntity())
                it.parent.profileEntity()?.let(profileEntities::add)
            }

            feedView.reason?.profileEntity()?.let(profileEntities::add)

            // Extract data from post
            val postEntity = feedView.post.postEntity()

            postEntities.add(postEntity)
            profileEntities.add(feedView.post.profileEntity())

            feedView.post.quotedPostEntity()?.let { embeddedPostEntity ->
                postEntities.add(embeddedPostEntity)
                postPostEntities.add(
                    PostPostEntity(
                        postId = postEntity.cid,
                        embeddedPostId = embeddedPostEntity.cid,
                    )
                )
                feedView.post.quotedPostEmbedEntities().forEach { embedEntity ->
                    associatePostEmbeds(
                        postEntity = embeddedPostEntity,
                        embedEntity = embedEntity,
                        externalEmbedEntities = externalEmbedEntities,
                        postExternalEmbedEntities = postExternalEmbedEntities,
                        imageEntities = imageEntities,
                        postImageEntities = postImageEntities,
                        videoEntities = videoEntities,
                        postVideoEntities = postVideoEntities
                    )
                }
            }
            feedView.post.quotedPostProfileEntity()?.let(profileEntities::add)

            feedView.post.embedEntities().forEach { embedEntity ->
                associatePostEmbeds(
                    postEntity = postEntity,
                    embedEntity = embedEntity,
                    externalEmbedEntities = externalEmbedEntities,
                    postExternalEmbedEntities = postExternalEmbedEntities,
                    imageEntities = imageEntities,
                    postImageEntities = postImageEntities,
                    videoEntities = videoEntities,
                    postVideoEntities = postVideoEntities
                )
            }
        }

        inTransaction {
            if (query.isInitialRequest()
                && feedDao.lastFetchKey(query.sourceId)?.lastFetchedAt != query.firstRequestInstant
            ) {
                feedDao.deleteAllFeedsFor(query.sourceId)
                feedDao.upsertFeedFetchKey(
                    FeedFetchKeyEntity(
                        sourceId = query.sourceId,
                        lastFetchedAt = query.firstRequestInstant
                    )
                )
            }

            // Order matters to satisfy foreign key constraints
            profileDao.insertOrPartiallyUpdateProfiles(profileEntities)
            postDao.upsertPosts(postEntities)

            embedDao.upsertExternalEmbeds(externalEmbedEntities)
            embedDao.upsertImages(imageEntities)
            embedDao.upsertVideos(videoEntities)

            postDao.insertOrIgnorePostPosts(postPostEntities)

            postDao.insertOrIgnorePostExternalEmbeds(postExternalEmbedEntities)
            postDao.insertOrIgnorePostImages(postImageEntities)
            postDao.insertOrIgnorePostVideos(postVideoEntities)

            feedDao.upsertTimelineItems(feedItemEntities)
        }
    }

    private fun associatePostEmbeds(
        postEntity: PostEntity,
        embedEntity: PostEmbed,
        externalEmbedEntities: MutableList<ExternalEmbedEntity>,
        postExternalEmbedEntities: MutableList<PostExternalEmbedEntity>,
        imageEntities: MutableList<ImageEntity>,
        postImageEntities: MutableList<PostImageEntity>,
        videoEntities: MutableList<VideoEntity>,
        postVideoEntities: MutableList<PostVideoEntity>
    ) {
        when (embedEntity) {
            is ExternalEmbedEntity -> {
                externalEmbedEntities.add(embedEntity)
                postExternalEmbedEntities.add(
                    postEntity.postExternalEmbedEntity(embedEntity)
                )
            }

            is ImageEntity -> {
                imageEntities.add(embedEntity)
                postImageEntities.add(
                    postEntity.postImageEntity(embedEntity)
                )
            }

            is VideoEntity -> {
                videoEntities.add(embedEntity)
                postVideoEntities.add(
                    postEntity.postVideoEntity(embedEntity)
                )
            }
        }
    }

    private fun readFeed(
        query: TimelineQuery
    ): Flow<List<TimelineItem>> =
        when (val local = query.nextItemCursor?.local) {
            null -> emptyFlow()
            else -> feedDao.feedItems(
                sourceId = query.sourceId,
                before = local,
                limit = query.limit,
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
        }
}

private fun TimelineQuery.isInitialRequest() =
    page == 0 && nextItemCursor == null
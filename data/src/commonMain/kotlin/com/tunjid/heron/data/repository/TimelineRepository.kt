package com.tunjid.heron.data.repository

import app.bsky.feed.FeedViewPost
import app.bsky.feed.GetAuthorFeedQueryParams
import app.bsky.feed.GetAuthorFeedResponse
import app.bsky.feed.GetTimelineQueryParams
import app.bsky.feed.GetTimelineResponse
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
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.TimelineFetchKeyEntity
import com.tunjid.heron.data.database.entities.TimelineItemEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.database.entities.emptyProfileEntity
import com.tunjid.heron.data.database.entities.postembeds.ExternalEmbedEntity
import com.tunjid.heron.data.database.entities.postembeds.ImageEntity
import com.tunjid.heron.data.database.entities.postembeds.PostEmbed
import com.tunjid.heron.data.database.entities.postembeds.PostExternalEmbedEntity
import com.tunjid.heron.data.database.entities.postembeds.PostImageEntity
import com.tunjid.heron.data.database.entities.postembeds.PostPostEntity
import com.tunjid.heron.data.database.entities.postembeds.PostVideoEntity
import com.tunjid.heron.data.database.entities.postembeds.VideoEntity
import com.tunjid.heron.data.database.entities.profile.PostViewerStatisticsEntity
import com.tunjid.heron.data.database.entities.profile.ProfileProfileRelationshipsEntity
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.network.models.embedEntities
import com.tunjid.heron.data.network.models.feedItemEntity
import com.tunjid.heron.data.network.models.postEntity
import com.tunjid.heron.data.network.models.postExternalEmbedEntity
import com.tunjid.heron.data.network.models.postImageEntity
import com.tunjid.heron.data.network.models.postVideoEntity
import com.tunjid.heron.data.network.models.postViewerStatisticsEntity
import com.tunjid.heron.data.network.models.profileEntity
import com.tunjid.heron.data.network.models.profileProfileRelationshipsEntities
import com.tunjid.heron.data.network.models.quotedPostEmbedEntities
import com.tunjid.heron.data.network.models.quotedPostEntity
import com.tunjid.heron.data.network.models.quotedPostProfileEntity
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
    private val timelineDao: TimelineDao,
    private val postDao: PostDao,
    private val embedDao: EmbedDao,
    private val profileDao: ProfileDao,
    private val transactionWriter: TransactionWriter,
    private val networkService: NetworkService,
    private val savedStateRepository: SavedStateRepository,
) : TimelineRepository {

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
                    if (authProfileId != null) transactionWriter.persistTimeline(
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

    private suspend fun TransactionWriter.persistTimeline(
        viewingProfileId: Id,
        feedViews: List<FeedViewPost>,
        query: TimelineQuery,
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

        val postThreadEntities = mutableListOf<PostThreadEntity>()
        val postViewerStatisticsEntities = mutableListOf<PostViewerStatisticsEntity>()
        val profileProfileRelationshipsEntities = mutableListOf<ProfileProfileRelationshipsEntity>()

        // Add the signed in user
        profileEntities.add(emptyProfileEntity(viewingProfileId))

        for (feedView in feedViews) {
            // Extract data from feed
            feedItemEntities.add(feedView.feedItemEntity(query.sourceId))

            // Extract data from post
            val postEntity = feedView.post.postEntity()
            val postAuthorEntity = feedView.post.profileEntity()

            feedView.reply?.let {
                val rootPostEntity = it.root.postEntity()
                postEntities.add(rootPostEntity)
                it.root.profileEntity()?.let(profileEntities::add)
                it.root.postViewerStatisticsEntity()
                    ?.let(postViewerStatisticsEntities::add)

                val parentPostEntity = it.parent.postEntity()
                postEntities.add(parentPostEntity)
                it.parent.profileEntity()?.let(profileEntities::add)
                it.parent.postViewerStatisticsEntity()
                    ?.let(postViewerStatisticsEntities::add)

                postThreadEntities.add(
                    PostThreadEntity(
                        postId = postEntity.cid,
                        parentPostId = parentPostEntity.cid,
                        rootPostId = rootPostEntity.cid
                    )
                )
            }

            feedView.reason?.profileEntity()?.let(profileEntities::add)

            feedView.post.viewer?.postViewerStatisticsEntity(
                postId = postEntity.cid,
            )?.let(postViewerStatisticsEntities::add)

            profileProfileRelationshipsEntities.addAll(
                feedView.post.author.profileProfileRelationshipsEntities(
                    viewingProfileId = viewingProfileId,
                )
            )

            postEntities.add(postEntity)
            profileEntities.add(postAuthorEntity)

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
            if (timelineDao.isFirstRequest(query)) {
                timelineDao.deleteAllFeedsFor(query.sourceId)
                timelineDao.upsertFeedFetchKey(
                    TimelineFetchKeyEntity(
                        sourceId = query.sourceId,
                        lastFetchedAt = query.data.firstRequestInstant
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

            timelineDao.upsertTimelineItems(feedItemEntities)

            postDao.upsertPostThreads(postThreadEntities)
            postDao.upsertPostStatistics(postViewerStatisticsEntities)
            profileDao.upsertProfileProfileRelationships(
                profileProfileRelationshipsEntities
            )
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

package com.tunjid.heron.data.repository

import app.bsky.feed.FeedViewPost
import app.bsky.feed.GetTimelineQueryParams
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.FeedItem
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.TransactionWriter
import com.tunjid.heron.data.database.daos.EmbedDao
import com.tunjid.heron.data.database.daos.FeedDao
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.entities.postembeds.ExternalEmbedEntity
import com.tunjid.heron.data.database.entities.FeedFetchKeyEntity
import com.tunjid.heron.data.database.entities.FeedItemEntity
import com.tunjid.heron.data.database.entities.postembeds.ImageEntity
import com.tunjid.heron.data.database.entities.PostEntity
import com.tunjid.heron.data.database.entities.postembeds.PostExternalEmbedEntity
import com.tunjid.heron.data.database.entities.postembeds.PostImageEntity
import com.tunjid.heron.data.database.entities.postembeds.PostVideoEntity
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.database.entities.postembeds.VideoEntity
import com.tunjid.heron.data.database.entities.postembeds.asExternalModel
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.network.models.embedEntities
import com.tunjid.heron.data.network.models.feedItemEntity
import com.tunjid.heron.data.network.models.postEntity
import com.tunjid.heron.data.network.models.postExternalEmbedEntity
import com.tunjid.heron.data.network.models.postImageEntity
import com.tunjid.heron.data.network.models.postVideoEntity
import com.tunjid.heron.data.network.models.profileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import me.tatarka.inject.annotations.Inject
import sh.christian.ozone.api.response.AtpResponse

@Serializable
data class FeedQuery(
    val page: Int,
    /**
     * The backing source of the feed, be it a list or other feed generator output.
     * It is null for a signed in user's timeline.
     */
    val source: Uri,
    /**
     * The instant the first request was made, as pagination is only valid for [FeedQuery] values
     * that all share the same [firstRequestInstant].
     */
    val firstRequestInstant: Instant,
    /**
     * How many items to fetch for a query.
     */
    val limit: Long = 50,
    /**
     * The cursor used to fetch items. Null if this is the first request.
     */
    val nextItemCursor: CursorList.DoubleCursor? = null,
)

interface FeedRepository {
    fun timeline(query: FeedQuery): Flow<CursorList<FeedItem>>
}

@Inject
class OfflineFeedRepository(
    private val feedDao: FeedDao,
    private val postDao: PostDao,
    private val embedDao: EmbedDao,
    private val profileDao: ProfileDao,
    private val transactionWriter: TransactionWriter,
    private val networkService: NetworkService,
) : FeedRepository {

    override fun timeline(query: FeedQuery): Flow<CursorList<FeedItem>> = flow {
        val networkCursorFlow = flow {
            emit(null)
            kotlin.runCatching {
                val networkPostsResponse = networkService.api.getTimeline(
                    GetTimelineQueryParams(
                        limit = query.limit,
                        cursor = query.nextItemCursor?.remote,
                    )
                )

                when (networkPostsResponse) {
                    is AtpResponse.Failure -> {
                        // TODO
                    }

                    is AtpResponse.Success -> {
                        emit(networkPostsResponse.response.cursor)
                        transactionWriter.saveFeed(networkPostsResponse.response.feed, query)
                    }
                }
            }
        }
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
        query: FeedQuery
    ) {
        val feedItemEntities = mutableListOf<FeedItemEntity>()
        val postEntities = mutableListOf<PostEntity>()
        val profileEntities = mutableListOf<ProfileEntity>()

        val externalEmbedEntities = mutableListOf<ExternalEmbedEntity>()
        val postExternalEmbedEntities = mutableListOf<PostExternalEmbedEntity>()

        val imageEntities = mutableListOf<ImageEntity>()
        val postImageEntities = mutableListOf<PostImageEntity>()

        val videoEntities = mutableListOf<VideoEntity>()
        val postVideoEntities = mutableListOf<PostVideoEntity>()

        for (feedView in feedViews) {
            // Extract data from feed
            feedItemEntities.add(feedView.feedItemEntity(query.source))

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

            feedView.post.embedEntities().forEach { embedEntity ->
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
        }

        inTransaction {
            if (query.isInitialRequest()
                && feedDao.lastFetchKey(query.source)?.lastFetchedAt != query.firstRequestInstant
            ) {
                feedDao.deleteAllFeedsFor(query.source)
                feedDao.upsertFeedFetchKey(
                    FeedFetchKeyEntity(
                        feedUri = query.source,
                        lastFetchedAt = query.firstRequestInstant
                    )
                )
            }

            // Order matters to satisfy foreign key constraints
            profileDao.upsertProfiles(profileEntities)
            postDao.upsertPosts(postEntities)

            embedDao.upsertExternalEmbeds(externalEmbedEntities)
            embedDao.upsertImages(imageEntities)
            embedDao.upsertVideos(videoEntities)

            postDao.insertOrIgnorePostExternalEmbeds(postExternalEmbedEntities)
            postDao.insertOrIgnorePostImages(postImageEntities)
            postDao.insertOrIgnorePostVideos(postVideoEntities)

            feedDao.upsertFeedItems(feedItemEntities)
        }
    }

    private fun readFeed(
        query: FeedQuery
    ): Flow<List<FeedItem>> =
        when (val local = query.nextItemCursor?.local) {
            null -> emptyFlow()
            else -> feedDao.feedItems(
                source = query.source,
                before = local,
                limit = query.limit,
            )
                .flatMapLatest { itemEntities ->
                    combine(
                        postDao.posts(
                            itemEntities.map { it.postId }
                        ),
                        postDao.posts(
                            itemEntities.mapNotNull { it.reply?.parentPostId }
                        ),
                        postDao.posts(
                            itemEntities.mapNotNull { it.reply?.rootPostId }
                        ),
                        profileDao.profiles(
                            itemEntities.mapNotNull { it.reposter }
                        )
                    ) { mainPosts, replyParents, replyRoots, repostProfiles ->
                        val idsToMainPosts = mainPosts.associateBy { it.entity.cid }
                        val idsToReplyParents = replyParents.associateBy { it.entity.cid }
                        val idsToReplyRoots = replyRoots.associateBy { it.entity.cid }
                        val idsToRepostProfiles = repostProfiles.associateBy { it.did }

                        itemEntities.map { entity ->
                            val mainPost = idsToMainPosts.getValue(entity.postId)
                            val replyParent =
                                entity.reply?.let { idsToReplyParents[it.parentPostId] }
                            val replyRoot = entity.reply?.let { idsToReplyRoots[it.rootPostId] }
                            val repostedBy = entity.reposter?.let { idsToRepostProfiles[it] }

                            when {
                                replyRoot != null && replyParent != null -> FeedItem.Reply(
                                    id = entity.id,
                                    post = mainPost.asExternalModel(),
                                    rootPost = replyRoot.asExternalModel(),
                                    parentPost = replyParent.asExternalModel(),
                                )

                                repostedBy != null -> FeedItem.Repost(
                                    id = entity.id,
                                    post = mainPost.asExternalModel(),
                                    by = repostedBy.asExternalModel(),
                                    at = entity.indexedAt,
                                )

                                entity.isPinned -> FeedItem.Pinned(
                                    id = entity.id,
                                    post = mainPost.asExternalModel(),
                                )

                                else -> FeedItem.Single(
                                    id = entity.id,
                                    post = mainPost.asExternalModel(),
                                )
                            }
                        }
                    }
                }
        }
}

private fun FeedQuery.isInitialRequest() =
    page == 0 && nextItemCursor == null
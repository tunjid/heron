package com.tunjid.heron.data.repository

import app.bsky.feed.FeedViewPost
import app.bsky.feed.GetTimelineQueryParams
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.FeedItem
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.daos.EmbedDao
import com.tunjid.heron.data.database.daos.FeedDao
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.entities.ExternalEmbedEntity
import com.tunjid.heron.data.database.entities.FeedItemEntity
import com.tunjid.heron.data.database.entities.ImageEntity
import com.tunjid.heron.data.database.entities.PostEntity
import com.tunjid.heron.data.database.entities.PostExternalEmbedEntity
import com.tunjid.heron.data.database.entities.PostImageEntity
import com.tunjid.heron.data.database.entities.PostVideoEntity
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.VideoEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.network.models.embedEntities
import com.tunjid.heron.data.network.models.feedItemEntity
import com.tunjid.heron.data.network.models.postEntity
import com.tunjid.heron.data.network.models.postExternalEmbedEntity
import com.tunjid.heron.data.network.models.postImageEntity
import com.tunjid.heron.data.network.models.postVideoEntity
import com.tunjid.heron.data.network.models.profileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Instant
import me.tatarka.inject.annotations.Inject

data class FeedQuery(
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
    val nextItemCursor: String? = null,
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
    private val networkService: NetworkService,
) : FeedRepository {

    override fun timeline(query: FeedQuery): Flow<CursorList<FeedItem>> = flow {
        val networkPostsResponse = networkService.api.getTimeline(
            GetTimelineQueryParams(
                limit = query.limit,
                cursor = query.nextItemCursor,
            )
        )

        saveFeed(networkPostsResponse.requireResponse().feed, query)

        val fetched = feedDao.feedItems(
            source = query.source,
            limit = 10,
        )
            .first()

        fetched.forEachIndexed { index, feedItemEntity ->
            postDao.post(
                postId = feedItemEntity.postId
            )
                .first()
                .asExternalModel()
                .also { println("main post @$index. embed: ${it.embed}") }
            feedItemEntity.reply?.let { reply ->
                println(reply)
                postDao.post(
                    postId = reply.rootPostId
                ).first()
                    .asExternalModel()
                    .also { println("post reply root @$index. embed: ${it.embed}") }
                postDao.post(
                    postId = reply.parentPostId
                )
                    .first()
                    .asExternalModel()
                    .also { println("post reply parent @$index. embed: ${it.embed}") }
            }
        }

        println(fetched)
    }

    private suspend fun saveFeed(
        feed: List<FeedViewPost>,
        query: FeedQuery
    ) {
        val feedItemEntities = mutableListOf<FeedItemEntity>()
        val postEntities = mutableListOf<PostEntity>()
        val postAuthorEntities = mutableListOf<ProfileEntity>()

        val externalEmbedEntities by lazy { mutableListOf<ExternalEmbedEntity>() }
        val postExternalEmbedEntities by lazy { mutableListOf<PostExternalEmbedEntity>() }

        val imageEntities by lazy { mutableListOf<ImageEntity>() }
        val postImageEntities by lazy { mutableListOf<PostImageEntity>() }

        val videoEntities by lazy { mutableListOf<VideoEntity>() }
        val postVideoEntities by lazy { mutableListOf<PostVideoEntity>() }

        feed.forEach { feedView ->
            // Extract data from feed
            feedItemEntities.add(feedView.feedItemEntity(query.source))

            feedView.reply?.let {
                postEntities.add(it.root.postEntity())
                postEntities.add(it.parent.postEntity())
            }

            // Extract data from post
            val postEntity = feedView.post.postEntity()

            postEntities.add(postEntity)
            postAuthorEntities.add(feedView.post.profileEntity())

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

        feedDao.deleteAllFeedsFor(query.source)

        // Order matters to satisfy foreign key constraints
        profileDao.upsertProfiles(postAuthorEntities)
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

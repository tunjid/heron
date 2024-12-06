package com.tunjid.heron.data.repository

import app.bsky.feed.GetTimelineQueryParams
import app.bsky.feed.PostView
import app.bsky.feed.PostViewEmbedUnion
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.daos.EmbedDao
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.entities.ExternalEmbedEntity
import com.tunjid.heron.data.database.entities.ImageEntity
import com.tunjid.heron.data.database.entities.PostEntity
import com.tunjid.heron.data.database.entities.PostExternalEmbedEntity
import com.tunjid.heron.data.database.entities.PostImageEntity
import com.tunjid.heron.data.database.entities.PostVideoEntity
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.VideoEntity
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.di.SingletonScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import me.tatarka.inject.annotations.Inject

data class TimelineQuery(
    val limit: Long = 50,
    val cursor: String? = null,
)

interface FeedRepository {
    fun timeline(query: TimelineQuery): Flow<List<Post>>
}

@SingletonScope
@Inject
class ImplFeedRepository(
    private val postDao: PostDao,
    private val embedDao: EmbedDao,
    private val profileDao: ProfileDao,
    private val networkService: NetworkService,
) : FeedRepository {

    override fun timeline(query: TimelineQuery): Flow<List<Post>> = flow {
        val networkPostsResponse = networkService.api.getTimeline(
            GetTimelineQueryParams(
                limit = query.limit,
                cursor = query.cursor,
            )
        )

        val postEntities = mutableListOf<PostEntity>()
        val postAuthorEntities = mutableListOf<ProfileEntity>()

        val externalEmbedEntities by lazy { mutableListOf<ExternalEmbedEntity>() }
        val externalEmbedCrossRefEntities by lazy { mutableListOf<PostExternalEmbedEntity>() }

        val imageEntities by lazy { mutableListOf<ImageEntity>() }
        val imageCrossRefEmbedEntities by lazy { mutableListOf<PostImageEntity>() }

        val videoEntities by lazy { mutableListOf<VideoEntity>() }
        val videoCrossRefEntities by lazy { mutableListOf<PostVideoEntity>() }

        networkPostsResponse.requireResponse().feed.forEach { feedView ->
            val postEntity = feedView.post.postEntity()
            postEntities.add(postEntity)
            postAuthorEntities.add(feedView.post.profileEntity())
            feedView.post.embedEntities().forEach { embedEntity ->
                when (embedEntity) {
                    is ExternalEmbedEntity -> {
                        externalEmbedEntities.add(embedEntity)
                        externalEmbedCrossRefEntities.add(
                            postEntity.postExternalEmbedEntity(embedEntity)
                        )
                    }

                    is ImageEntity -> {
                        imageEntities.add(embedEntity)
                        imageCrossRefEmbedEntities.add(
                            postEntity.postImageEntity(embedEntity)
                        )
                    }

                    is VideoEntity -> {
                        videoEntities.add(embedEntity)
                        videoCrossRefEntities.add(
                            postEntity.postVideoEntity(embedEntity)
                        )
                    }
                }
            }
        }

        profileDao.upsertProfiles(postAuthorEntities)
        postDao.upsertPosts(postEntities)

        embedDao.upsertExternalEmbeds(externalEmbedEntities)
        embedDao.upsertImages(imageEntities)
        embedDao.upsertVideos(videoEntities)

        postDao.insertOrIgnoreExternalEmbedCrossRefEntities(externalEmbedCrossRefEntities)
        postDao.insertOrIgnoreImageCrossRefEntities(imageCrossRefEmbedEntities)
        postDao.insertOrIgnoreVideoCrossRefEntities(videoCrossRefEntities)
    }

    private fun PostEntity.postVideoEntity(
        embedEntity: VideoEntity
    ) = PostVideoEntity(
        postId = cid,
        videoId = embedEntity.cid,
    )

    private fun PostEntity.postImageEntity(
        embedEntity: ImageEntity
    ) = PostImageEntity(
        postId = cid,
        imageUri = embedEntity.fullSize,
    )

    private fun PostEntity.postExternalEmbedEntity(
        embedEntity: ExternalEmbedEntity
    ) = PostExternalEmbedEntity(
        postId = cid,
        externalEmbedUri = embedEntity.uri,
    )

}

private fun PostView.postEntity() =
    PostEntity(
        cid = Id(cid.cid),
        uri = Uri(uri.atUri),
        authorId = Id(author.did.did),
        replyCount = replyCount,
        repostCount = repostCount,
        likeCount = likeCount,
        quoteCount = quoteCount,
        indexedAt = indexedAt,
    )

private fun PostView.profileEntity() =
    ProfileEntity(
        did = Id(author.did.did),
        handle = Id(author.handle.handle),
        displayName = author.displayName,
        description = null,
        avatar = author.avatar?.uri?.let(::Uri),
        banner = null,
        followersCount = 0,
        followsCount = 0,
        postsCount = 0,
        joinedViaStarterPack = null,
        indexedAt = null,
        createdAt = author.createdAt,
    )

private fun PostView.embedEntities() =
    when (val embed = embed) {
        is PostViewEmbedUnion.ExternalView -> listOf(
            ExternalEmbedEntity(
                uri = Uri(embed.value.external.uri.uri),
                title = embed.value.external.title,
                description = embed.value.external.description,
                thumb = embed.value.external.thumb?.uri?.let(::Uri),
            )
        )

        is PostViewEmbedUnion.ImagesView -> embed.value.images.map {
            ImageEntity(
                fullSize = Uri(it.fullsize.uri),
                thumb = Uri(it.thumb.uri),
                alt = it.alt,
                width = it.aspectRatio?.width,
                height = it.aspectRatio?.height,
            )
        }

        is PostViewEmbedUnion.RecordView -> emptyList()
        is PostViewEmbedUnion.RecordWithMediaView -> emptyList()
        is PostViewEmbedUnion.Unknown -> emptyList()
        is PostViewEmbedUnion.VideoView -> listOf(
            VideoEntity(
                cid = Id(embed.value.cid.cid),
                playlist = Uri(embed.value.playlist.uri),
                thumbnail = embed.value.thumbnail?.uri?.let(::Uri),
                alt = embed.value.alt,
                width = embed.value.aspectRatio?.width,
                height = embed.value.aspectRatio?.height,
            )
        )

        null -> emptyList()
    }

package com.tunjid.heron.data.repository

import app.bsky.feed.GetLikesQueryParams
import app.bsky.feed.GetLikesResponse
import app.bsky.feed.GetQuotesQueryParams
import app.bsky.feed.GetQuotesResponse
import app.bsky.feed.GetRepostedByQueryParams
import app.bsky.feed.GetRepostedByResponse
import app.bsky.feed.Like
import app.bsky.feed.PostReplyRef
import app.bsky.feed.Repost
import app.bsky.richtext.Facet
import app.bsky.richtext.FacetByteSlice
import app.bsky.richtext.FacetFeatureUnion.Link
import app.bsky.richtext.FacetFeatureUnion.Mention
import app.bsky.richtext.FacetFeatureUnion.Tag
import app.bsky.richtext.FacetLink
import app.bsky.richtext.FacetMention
import app.bsky.richtext.FacetTag
import com.atproto.repo.CreateRecordRequest
import com.atproto.repo.CreateRecordValidationStatus
import com.atproto.repo.DeleteRecordRequest
import com.atproto.repo.StrongRef
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.ProfileWithRelationship
import com.tunjid.heron.data.core.models.value
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.TransactionWriter
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.daos.partialUpsert
import com.tunjid.heron.data.database.entities.PopulatedProfileEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.database.entities.profile.PostViewerStatisticsEntity
import com.tunjid.heron.data.database.entities.profile.asExternalModel
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.utilities.CursorQuery
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.nextCursorFlow
import com.tunjid.heron.data.utilities.runCatchingWithNetworkRetry
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import me.tatarka.inject.annotations.Inject
import sh.christian.ozone.BlueskyJson
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Cid
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Nsid
import sh.christian.ozone.api.model.JsonContent
import app.bsky.feed.Like as BskyLike
import app.bsky.feed.Post as BskyPost
import app.bsky.feed.Repost as BskyRepost
import sh.christian.ozone.api.Uri as BskyUri

@Serializable
data class PostDataQuery(
    val postId: Id,
    override val data: CursorQuery.Data,
) : CursorQuery

interface PostRepository {

    fun likedBy(
        query: PostDataQuery,
        cursor: Cursor,
    ): Flow<CursorList<ProfileWithRelationship>>

    fun repostedBy(
        query: PostDataQuery,
        cursor: Cursor,
    ): Flow<CursorList<ProfileWithRelationship>>

    fun quotes(
        query: PostDataQuery,
        cursor: Cursor,
    ): Flow<CursorList<Post>>

    suspend fun sendInteraction(
        interaction: Post.Interaction,
    )

    suspend fun createPost(
        request: Post.Create.Request,
        replyTo: Post.Create.Reply?,
    )
}

class OfflinePostRepository @Inject constructor(
    private val postDao: PostDao,
    private val profileDao: ProfileDao,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
    private val networkService: NetworkService,
    private val transactionWriter: TransactionWriter,
    private val savedStateRepository: SavedStateRepository,
) : PostRepository {
    override fun likedBy(
        query: PostDataQuery,
        cursor: Cursor,
    ): Flow<CursorList<ProfileWithRelationship>> =
        combine(
            postDao.likedBy(
                postId = query.postId.id,
                viewingProfileId = savedStateRepository.signedInProfileId?.id,
            )
                .map(List<PopulatedProfileEntity>::asExternalModels),
            nextCursorFlow(
                currentCursor = cursor,
                currentRequestWithNextCursor = {
                    networkService.api.getLikes(
                        GetLikesQueryParams(
                            uri = query.postId.id.let(::AtUri),
                            limit = query.data.limit,
                            cursor = cursor.value,
                        )
                    )
                },
                nextCursor = GetLikesResponse::cursor,
                onResponse = {
                    multipleEntitySaverProvider.saveInTransaction {
                        val viewingProfileId = savedStateRepository.signedInProfileId
                            ?: return@saveInTransaction
                        likes.forEach {
                            add(
                                viewingProfileId = viewingProfileId,
                                postId = query.postId,
                                like = it,
                            )
                        }
                    }
                },
            ),
            ::CursorList
        )
            .distinctUntilChanged()


    override fun repostedBy(
        query: PostDataQuery,
        cursor: Cursor,
    ): Flow<CursorList<ProfileWithRelationship>> =
        combine(
            postDao.repostedBy(
                postId = query.postId.id,
                viewingProfileId = savedStateRepository.signedInProfileId?.id,
            )
                .map(List<PopulatedProfileEntity>::asExternalModels),

            nextCursorFlow(
                currentCursor = cursor,
                currentRequestWithNextCursor = {
                    networkService.api.getRepostedBy(
                        GetRepostedByQueryParams(
                            uri = query.postId.id.let(::AtUri),
                            limit = query.data.limit,
                            cursor = cursor.value,
                        )
                    )
                },
                nextCursor = GetRepostedByResponse::cursor,
                onResponse = {
                    // TODO: Figure out how to get indexedAt for reposts
                },
            ),
            ::CursorList
        )
            .distinctUntilChanged()

    override fun quotes(
        query: PostDataQuery,
        cursor: Cursor,
    ): Flow<CursorList<Post>> =
        combine(
            postDao.quotedPosts(
                quotedPostId = query.postId.id,
            )
                .map { populatedPostEntities ->
                    populatedPostEntities.map {
                        it.asExternalModel(quote = null)
                    }
                },
            nextCursorFlow(
                currentCursor = cursor,
                currentRequestWithNextCursor = {
                    networkService.api.getQuotes(
                        GetQuotesQueryParams(
                            uri = query.postId.id.let(::AtUri),
                            limit = query.data.limit,
                            cursor = cursor.value,
                        )
                    )
                },
                nextCursor = GetQuotesResponse::cursor,
                onResponse = {
                    multipleEntitySaverProvider.saveInTransaction {
                        val viewingProfileId = savedStateRepository.signedInProfileId
                            ?: return@saveInTransaction
                        posts.forEach {
                            add(
                                viewingProfileId = viewingProfileId,
                                postView = it,
                            )
                        }
                    }
                },
            ),
            ::CursorList,
        )
            .distinctUntilChanged()

    override suspend fun createPost(
        request: Post.Create.Request,
        replyTo: Post.Create.Reply?,
    ) {
        val resolvedLinks: List<Post.Link> = coroutineScope {
            request.links.map { link ->
                async {
                    when (val target = link.target) {
                        is Post.LinkTarget.ExternalLink -> link
                        is Post.LinkTarget.UserDidMention -> link
                        is Post.LinkTarget.Hashtag -> link
                        is Post.LinkTarget.UserHandleMention -> {
                            profileDao.profiles(ids = listOf(target.handle))
                                .first()
                                .firstOrNull()
                                ?.let { profile ->
                                    link.copy(
                                        target = Post.LinkTarget.UserDidMention(profile.did)
                                    )
                                }
                        }
                    }
                }
            }.awaitAll()
        }.filterNotNull()

        val replyRef = replyTo?.let { original ->
            PostReplyRef(
                root = StrongRef(
                    uri = original.root.uri.uri.let(::AtUri),
                    cid = original.root.cid.id.let(::Cid),
                ),
                parent = StrongRef(
                    uri = original.parent.uri.uri.let(::AtUri),
                    cid = original.parent.cid.id.let(::Cid),
                ),
            )
        }

        val createRecordRequest = CreateRecordRequest(
            repo = request.authorId.id.let(::Did),
            collection = Nsid(PostCollection),
            record = BskyPost(
                text = request.text,
                reply = replyRef,
                facets = resolvedLinks.map { link ->
                    Facet(
                        index = FacetByteSlice(
                            byteStart = link.start.toLong(),
                            byteEnd = link.end.toLong(),
                        ),
                        features = when (val target = link.target) {
                            is Post.LinkTarget.ExternalLink -> listOf(
                                Link(FacetLink(target.uri.uri.let(::BskyUri)))
                            )

                            is Post.LinkTarget.UserDidMention -> listOf(
                                Mention(FacetMention(target.did.id.let(::Did)))
                            )

                            is Post.LinkTarget.Hashtag -> listOf(
                                Tag(FacetTag(target.tag))
                            )

                            is Post.LinkTarget.UserHandleMention -> emptyList()
                        },
                    )
                },
                createdAt = Clock.System.now(),
            )
                .asJsonContent(BskyPost.serializer()),
        )

        networkService.api.createRecord(createRecordRequest)
    }

    override suspend fun sendInteraction(
        interaction: Post.Interaction,
    ) {
        val authorId = savedStateRepository.signedInProfileId ?: return
        when (interaction) {
            is Post.Interaction.Create -> runCatchingWithNetworkRetry {
                networkService.api.createRecord(
                    CreateRecordRequest(
                        repo = authorId.id.let(::Did),
                        collection = Nsid(
                            when (interaction) {
                                is Post.Interaction.Create.Like -> LikeCollection
                                is Post.Interaction.Create.Repost -> RepostCollection
                            }
                        ),
                        record = when (interaction) {
                            is Post.Interaction.Create.Like -> BskyLike(
                                subject = StrongRef(
                                    cid = interaction.postId.id.let(::Cid),
                                    uri = interaction.postUri.uri.let(::AtUri),
                                ),
                                createdAt = Clock.System.now(),
                            ).asJsonContent(Like.serializer())

                            is Post.Interaction.Create.Repost -> BskyRepost(
                                subject = StrongRef(
                                    cid = interaction.postId.id.let(::Cid),
                                    uri = interaction.postUri.uri.let(::AtUri),
                                ),
                                createdAt = Clock.System.now(),
                            ).asJsonContent(Repost.serializer())
                        },
                    )
                )
            }
                .getOrNull()
                ?.let {
                    if (it.validationStatus !is CreateRecordValidationStatus.Valid) return@let
                    upsertInteraction(
                        partial = when (interaction) {
                            is Post.Interaction.Create.Like -> PostViewerStatisticsEntity.Partial.Like(
                                likeUri = it.uri.atUri.let(::Uri),
                                postId = interaction.postId,
                            )

                            is Post.Interaction.Create.Repost -> PostViewerStatisticsEntity.Partial.Repost(
                                repostUri = it.uri.atUri.let(::Uri),
                                postId = interaction.postId,
                            )
                        }
                    )
                }

            is Post.Interaction.Delete -> runCatchingWithNetworkRetry {
                networkService.api.deleteRecord(
                    DeleteRecordRequest(
                        repo = authorId.id.let(::Did),
                        collection = Nsid(
                            when (interaction) {
                                is Post.Interaction.Delete.RemoveRepost -> LikeCollection
                                is Post.Interaction.Delete.Unlike -> RepostCollection
                            }
                        ),
                        rkey = when (interaction) {
                            is Post.Interaction.Delete.RemoveRepost -> interaction.repostUri.uri
                            is Post.Interaction.Delete.Unlike -> interaction.likeUri.uri
                        }
                    )
                )
            }
                .getOrNull()
                ?.let {
                    upsertInteraction(
                        partial = when (interaction) {
                            is Post.Interaction.Delete.Unlike -> PostViewerStatisticsEntity.Partial.Like(
                                likeUri = null,
                                postId = interaction.postId,
                            )

                            is Post.Interaction.Delete.RemoveRepost -> PostViewerStatisticsEntity.Partial.Repost(
                                repostUri = null,
                                postId = interaction.postId,
                            )
                        }
                    )
                }
        }

    }

    private suspend fun upsertInteraction(
        partial: PostViewerStatisticsEntity.Partial,
    ) = transactionWriter.inTransaction {
        partialUpsert(
            items = listOf(partial.asFull()),
            partialMapper = { listOf(partial) },
            insertEntities = postDao::insertOrIgnorePostStatistics,
            updatePartials = {
                when (partial) {
                    is PostViewerStatisticsEntity.Partial.Like ->
                        postDao.updatePostStatisticsLikes(listOf(partial))

                    is PostViewerStatisticsEntity.Partial.Repost ->
                        postDao.updatePostStatisticsReposts(listOf(partial))
                }
            }
        )
    }
}

private fun List<PopulatedProfileEntity>.asExternalModels() =
    map {
        ProfileWithRelationship(
            profile = it.profileEntity.asExternalModel(),
            relationship = it.relationship?.asExternalModel(),
        )
    }


private fun <T> T.asJsonContent(
    serializer: KSerializer<T>,
): JsonContent = BlueskyJson.decodeFromString(
    BlueskyJson.encodeToString(serializer, this)
)

private const val PostCollection = "app.bsky.feed.post"

private const val RepostCollection = "app.bsky.feed.repost"

private const val LikeCollection = "app.bsky.feed.like"
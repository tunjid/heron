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

import app.bsky.feed.GetLikesQueryParams
import app.bsky.feed.GetLikesResponse
import app.bsky.feed.GetQuotesQueryParams
import app.bsky.feed.GetQuotesResponse
import app.bsky.feed.GetRepostedByQueryParams
import app.bsky.feed.GetRepostedByResponse
import app.bsky.feed.Like
import app.bsky.feed.PostEmbedUnion
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
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.models.value
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.TransactionWriter
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.daos.partialUpsert
import com.tunjid.heron.data.database.entities.PopulatedPostEntity
import com.tunjid.heron.data.database.entities.PopulatedProfileEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.database.entities.profile.PostViewerStatisticsEntity
import com.tunjid.heron.data.database.entities.profile.asExternalModel
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.utilities.Collections
import com.tunjid.heron.data.utilities.CursorQuery
import com.tunjid.heron.data.utilities.asJsonContent
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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import me.tatarka.inject.annotations.Inject
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Cid
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Nsid
import tools.ozone.moderation.GetRecordQueryParams
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
    ): Flow<CursorList<ProfileWithViewerState>>

    fun repostedBy(
        query: PostDataQuery,
        cursor: Cursor,
    ): Flow<CursorList<ProfileWithViewerState>>

    fun quotes(
        query: PostDataQuery,
        cursor: Cursor,
    ): Flow<CursorList<Post>>

    suspend fun sendInteraction(
        interaction: Post.Interaction,
    )

    suspend fun createPost(
        request: Post.Create.Request,
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
    ): Flow<CursorList<ProfileWithViewerState>> = withPostUri(query.postId) { postAtUri ->
        combine(
            postDao.likedBy(
                postId = query.postId.id,
                viewingProfileId = savedStateRepository.signedInProfileId?.id,
                offset = query.data.page * query.data.limit,
                limit = query.data.limit,
            )
                .map(List<PopulatedProfileEntity>::asExternalModels),
            nextCursorFlow(
                currentCursor = cursor,
                currentRequestWithNextCursor = {
                    networkService.api.getLikes(
                        GetLikesQueryParams(
                            uri = postAtUri,
                            limit = query.data.limit,
                            cursor = cursor.value,
                        )
                    )
                },
                nextCursor = GetLikesResponse::cursor,
                onResponse = {
                    multipleEntitySaverProvider.saveInTransaction {
                        likes.forEach {
                            add(
                                viewingProfileId = savedStateRepository.signedInProfileId,
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
    }

    override fun repostedBy(
        query: PostDataQuery,
        cursor: Cursor,
    ): Flow<CursorList<ProfileWithViewerState>> = withPostUri(query.postId) { postAtUri ->
        combine(
            postDao.repostedBy(
                postId = query.postId.id,
                viewingProfileId = savedStateRepository.signedInProfileId?.id,
                offset = query.data.page * query.data.limit,
                limit = query.data.limit,
            )
                .map(List<PopulatedProfileEntity>::asExternalModels),

            nextCursorFlow(
                currentCursor = cursor,
                currentRequestWithNextCursor = {
                    networkService.api.getRepostedBy(
                        GetRepostedByQueryParams(
                            uri = postAtUri,
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
    }

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
                        posts.forEach {
                            add(
                                viewingProfileId = savedStateRepository.signedInProfileId,
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

        val reply = request.metadata.reply?.parent?.let { parent ->
            val parentRef = StrongRef(
                uri = parent.uri.uri.let(::AtUri),
                cid = parent.cid.id.let(::Cid),
            )
            when (val ref = parent.record?.replyRef) {
                // Starting a new thread
                null -> PostReplyRef(
                    root = parentRef,
                    parent = parentRef,
                )
                // Continuing a thread
                else -> PostReplyRef(
                    root = StrongRef(
                        uri = ref.rootUri.uri.let(::AtUri),
                        cid = ref.rootCid.id.let(::Cid),
                    ),
                    parent = parentRef,
                )
            }
        }

        val embed = request.metadata.quote?.interaction?.postUri?.let { postUri ->
            runCatchingWithNetworkRetry {
                networkService.api.getRecord(
                    params = GetRecordQueryParams(
                        uri = AtUri(postUri.uri)
                    )
                )
            }
                .getOrNull()
                ?.value
                ?.decodeAs<PostEmbedUnion>()
        }

        val createRecordRequest = CreateRecordRequest(
            repo = request.authorId.id.let(::Did),
            collection = Nsid(Collections.Post),
            record = BskyPost(
                text = request.text,
                reply = reply,
                embed = embed,
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

        runCatchingWithNetworkRetry {
            networkService.api.createRecord(createRecordRequest)
        }
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
                                is Post.Interaction.Create.Like -> Collections.Like
                                is Post.Interaction.Create.Repost -> Collections.Repost
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
                                is Post.Interaction.Delete.RemoveRepost -> Collections.Repost
                                is Post.Interaction.Delete.Unlike -> Collections.Like
                            }
                        ),
                        rkey = Collections.recordKey(
                            when (interaction) {
                                is Post.Interaction.Delete.RemoveRepost -> interaction.repostUri
                                is Post.Interaction.Delete.Unlike -> interaction.likeUri
                            }
                        )
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

    private fun <T> withPostUri(
        postId: Id,
        block: (AtUri) -> Flow<T>,
    ): Flow<T> = flow {
        postDao.posts(setOf(postId))
            .firstOrNull(List<PopulatedPostEntity>::isNotEmpty)
            ?.first()
            ?.entity
            ?.uri
            ?.uri
            ?.let(::AtUri)
            ?.let(block)
            ?.let { emitAll(it) }
    }
}

private fun List<PopulatedProfileEntity>.asExternalModels() =
    map {
        ProfileWithViewerState(
            profile = it.profileEntity.asExternalModel(),
            viewerState = it.relationship?.asExternalModel(),
        )
    }

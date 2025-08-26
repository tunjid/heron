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
import app.bsky.feed.PostReplyRef
import app.bsky.feed.Repost
import app.bsky.video.GetJobStatusQueryParams
import com.atproto.repo.CreateRecordRequest
import com.atproto.repo.CreateRecordValidationStatus
import com.atproto.repo.DeleteRecordRequest
import com.atproto.repo.StrongRef
import com.atproto.repo.UploadBlobResponse
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Link
import com.tunjid.heron.data.core.models.MediaFile
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.PostUri
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.models.offset
import com.tunjid.heron.data.core.models.value
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.data.database.TransactionWriter
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.daos.partialUpsert
import com.tunjid.heron.data.database.entities.PopulatedProfileEntity
import com.tunjid.heron.data.database.entities.PostEntity
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.database.entities.profile.PostViewerStatisticsEntity
import com.tunjid.heron.data.database.entities.profile.asExternalModel
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.utilities.Collections
import com.tunjid.heron.data.utilities.MediaBlob
import com.tunjid.heron.data.utilities.asJsonContent
import com.tunjid.heron.data.utilities.facet
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.nextCursorFlow
import com.tunjid.heron.data.utilities.postEmbedUnion
import com.tunjid.heron.data.utilities.refreshProfile
import com.tunjid.heron.data.utilities.resolveLinks
import com.tunjid.heron.data.utilities.with
import com.tunjid.heron.data.utilities.withRefresh
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Cid
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Nsid
import sh.christian.ozone.api.model.Blob
import sh.christian.ozone.api.response.AtpResponse
import app.bsky.feed.Like as BskyLike
import app.bsky.feed.Post as BskyPost
import app.bsky.feed.Repost as BskyRepost

@Serializable
data class PostDataQuery(
    val profileId: Id.Profile,
    val postRecordKey: RecordKey,
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

    fun post(
        uri: PostUri,
    ): Flow<Post>

    suspend fun sendInteraction(
        interaction: Post.Interaction,
    )

    suspend fun createPost(
        request: Post.Create.Request,
    )
}

internal class OfflinePostRepository @Inject constructor(
    private val postDao: PostDao,
    private val profileDao: ProfileDao,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
    private val networkService: NetworkService,
    private val transactionWriter: TransactionWriter,
    private val savedStateDataSource: SavedStateDataSource,
) : PostRepository {
    override fun likedBy(
        query: PostDataQuery,
        cursor: Cursor,
    ): Flow<CursorList<ProfileWithViewerState>> = withPostEntity(
        query.profileId,
        query.postRecordKey,
    ) { postEntity ->
        combine(
            postDao.likedBy(
                postUri = postEntity.cid.id,
                viewingProfileId = savedStateDataSource.signedInProfileId?.id,
                offset = query.data.offset,
                limit = query.data.limit,
            )
                .map(List<PopulatedProfileEntity>::asExternalModels),
            networkService.nextCursorFlow(
                currentCursor = cursor,
                currentRequestWithNextCursor = {
                    getLikes(
                        GetLikesQueryParams(
                            uri = postEntity.uri.uri.let(::AtUri),
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
                                viewingProfileId = savedStateDataSource.signedInProfileId,
                                postUri = postEntity.uri,
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
    ): Flow<CursorList<ProfileWithViewerState>> = withPostEntity(
        query.profileId,
        query.postRecordKey,
    ) { postEntity ->
        combine(
            postDao.repostedBy(
                postUri = postEntity.cid.id,
                viewingProfileId = savedStateDataSource.signedInProfileId?.id,
                offset = query.data.offset,
                limit = query.data.limit,
            )
                .map(List<PopulatedProfileEntity>::asExternalModels),

            networkService.nextCursorFlow(
                currentCursor = cursor,
                currentRequestWithNextCursor = {
                    getRepostedBy(
                        GetRepostedByQueryParams(
                            uri = postEntity.uri.uri.let(::AtUri),
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
    ): Flow<CursorList<Post>> = withPostEntity(
        profileId = query.profileId,
        postRecordKey = query.postRecordKey,
    ) { postEntity ->
        savedStateDataSource.observedSignedInProfileId
            .flatMapLatest { signedInProfileId ->
                combine(
                    postDao.quotedPosts(
                        viewingProfileId = signedInProfileId?.id,
                        quotedPostUri = postEntity.uri.uri,
                    )
                        .map { populatedPostEntities ->
                            populatedPostEntities.map {
                                it.asExternalModel(quote = null)
                            }
                        },
                    networkService.nextCursorFlow(
                        currentCursor = cursor,
                        currentRequestWithNextCursor = {
                            getQuotes(
                                GetQuotesQueryParams(
                                    uri = postEntity.uri.uri.let(::AtUri),
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
                                        viewingProfileId = savedStateDataSource.signedInProfileId,
                                        postView = it,
                                    )
                                }
                            }
                        },
                    ),
                    ::CursorList,
                )
                    .distinctUntilChanged()
            }
    }

    override fun post(
        uri: PostUri,
    ): Flow<Post> =
        savedStateDataSource.observedSignedInProfileId
            .flatMapLatest { signedInProfileId ->
                postDao.posts(
                    viewingProfileId = signedInProfileId?.id,
                    postUris = setOf(uri),
                )
                    .mapNotNull {
                        it.firstOrNull()?.asExternalModel(quote = null)
                    }
            }

    override suspend fun createPost(
        request: Post.Create.Request,
    ) {
        val resolvedLinks: List<Link> = resolveLinks(
            profileDao = profileDao,
            networkService = networkService,
            links = request.links,
        )

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
        val blobs = if (request.metadata.mediaFiles.isNotEmpty()) coroutineScope {
            request.metadata.mediaFiles.map { file ->
                async {
                    networkService.runCatchingWithMonitoredNetworkRetry {
                        when (file) {
                            is MediaFile.Photo -> uploadBlob(file.data)
                                .map(UploadBlobResponse::blob)

                            is MediaFile.Video -> networkService.uploadVideoBlob(file.data)
                        }
                    }
                        .map(file::with)
                }
            }
                .awaitAll()
                .mapNotNull(Result<MediaBlob>::getOrNull)
        }
        else emptyList()

        val createRecordRequest = CreateRecordRequest(
            repo = request.authorId.id.let(::Did),
            collection = Nsid(Collections.Post),
            record = BskyPost(
                text = request.text,
                reply = reply,
                embed = postEmbedUnion(
                    repost = request.metadata.quote?.interaction,
                    mediaBlobs = blobs,
                ),
                facets = resolvedLinks.facet(),
                createdAt = Clock.System.now(),
            )
                .asJsonContent(BskyPost.serializer()),
        )

        networkService.runCatchingWithMonitoredNetworkRetry {
            createRecord(createRecordRequest)
        }
    }

    override suspend fun sendInteraction(
        interaction: Post.Interaction,
    ) {
        val authorId = savedStateDataSource.signedInProfileId ?: return
        when (interaction) {
            is Post.Interaction.Create -> networkService.runCatchingWithMonitoredNetworkRetry {
                createRecord(
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
                            is Post.Interaction.Create.Like -> {
                                PostViewerStatisticsEntity.Partial.Like(
                                    likeUri = it.uri.atUri.let(::GenericUri),
                                    postUri = interaction.postUri,
                                    viewingProfileId = authorId,
                                )
                            }

                            is Post.Interaction.Create.Repost -> PostViewerStatisticsEntity.Partial.Repost(
                                repostUri = it.uri.atUri.let(::GenericUri),
                                postUri = interaction.postUri,
                                viewingProfileId = authorId,
                            )
                        }
                    )
                    postDao.updateLikeCount(postUri =interaction.postUri.uri, isIncrement = true)
                }

            is Post.Interaction.Delete -> networkService.runCatchingWithMonitoredNetworkRetry {
                deleteRecord(
                    DeleteRecordRequest(
                        repo = authorId.id.let(::Did),
                        collection = Nsid(
                            when (interaction) {
                                is Post.Interaction.Delete.RemoveRepost -> Collections.Repost
                                is Post.Interaction.Delete.Unlike -> Collections.Like
                            }
                        ),
                        rkey = Collections.rKey(
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
                            is Post.Interaction.Delete.Unlike -> {
                                PostViewerStatisticsEntity.Partial.Like(
                                    likeUri = null,
                                    postUri = interaction.postUri,
                                    viewingProfileId = authorId,
                                )
                            }

                            is Post.Interaction.Delete.RemoveRepost -> PostViewerStatisticsEntity.Partial.Repost(
                                repostUri = null,
                                postUri = interaction.postUri,
                                viewingProfileId = authorId,
                            )
                        }
                    )
                    postDao.updateLikeCount(postUri =interaction.postUri.uri, isIncrement = false)
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

    private fun <T> withPostEntity(
        profileId: Id.Profile,
        postRecordKey: RecordKey,
        block: (PostEntity) -> Flow<T>,
    ): Flow<T> = savedStateDataSource.observedSignedInProfileId
        .flatMapLatest { signedInProfileId ->
            postDao.postEntitiesByUri(
                viewingProfileId = signedInProfileId?.id,
                postUris = setOf(
                    profileDao.profiles(listOf(profileId))
                        .filter(List<ProfileEntity>::isNotEmpty)
                        .map(List<ProfileEntity>::first)
                        .distinctUntilChanged()
                        .map {
                            PostUri(
                                profileId = it.did,
                                postRecordKey = postRecordKey
                            )
                        }
                        .first()
                )
            )
                .first(List<PostEntity>::isNotEmpty)
                .first()
                .let(block)
        }
        .withRefresh {
            refreshProfile(
                profileId = profileId,
                profileDao = profileDao,
                networkService = networkService,
                multipleEntitySaverProvider = multipleEntitySaverProvider,
                savedStateDataSource = savedStateDataSource,
            )
        }
}

private fun List<PopulatedProfileEntity>.asExternalModels() =
    map {
        ProfileWithViewerState(
            profile = it.profileEntity.asExternalModel(),
            viewerState = it.relationship?.asExternalModel(),
        )
    }

private suspend fun NetworkService.uploadVideoBlob(
    data: ByteArray,
): AtpResponse<Blob> {
    val uploadResponse = api.uploadVideo(data)
    val status = uploadResponse.requireResponse().jobStatus

    // Fail fast here if the upload failed
    uploadResponse.requireResponse()
        .jobStatus
        .blob
        ?.let { processedBlob ->
            return@uploadVideoBlob uploadResponse.map { processedBlob }
        }

    repeat(20) {
        val statusResponse = runCatchingWithMonitoredNetworkRetry {
            getJobStatus(GetJobStatusQueryParams(status.jobId))
        }
        statusResponse
            .getOrNull()
            ?.jobStatus
            ?.blob
            ?.let { processedBlob ->
                return@uploadVideoBlob uploadResponse.map { processedBlob }
            }
        delay(2_000)
    }

    throw Exception("Video upload timed out")
}
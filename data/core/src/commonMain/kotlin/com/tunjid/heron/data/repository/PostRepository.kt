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

import app.bsky.bookmark.CreateBookmarkRequest
import app.bsky.bookmark.DeleteBookmarkRequest
import app.bsky.feed.GetLikesQueryParams
import app.bsky.feed.GetLikesResponse
import app.bsky.feed.GetQuotesQueryParams
import app.bsky.feed.GetQuotesResponse
import app.bsky.feed.GetRepostedByQueryParams
import app.bsky.feed.GetRepostedByResponse
import app.bsky.feed.Like
import app.bsky.feed.Like as BskyLike
import app.bsky.feed.Post as BskyPost
import app.bsky.feed.PostReplyRef
import app.bsky.feed.Repost
import app.bsky.feed.Repost as BskyRepost
import com.atproto.repo.CreateRecordRequest
import com.atproto.repo.CreateRecordResponse
import com.atproto.repo.CreateRecordValidationStatus
import com.atproto.repo.DeleteRecordRequest
import com.atproto.repo.StrongRef
import com.atproto.repo.UploadBlobResponse
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Link
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.PostUri
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.models.offset
import com.tunjid.heron.data.core.models.value
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.data.core.utilities.File
import com.tunjid.heron.data.core.utilities.Outcome
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
import com.tunjid.heron.data.files.FileManager
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.network.VideoUploadService
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
import com.tunjid.heron.data.utilities.runCatchingUnlessCancelled
import com.tunjid.heron.data.utilities.toOutcome
import com.tunjid.heron.data.utilities.with
import com.tunjid.heron.data.utilities.withRefresh
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Cid
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Nsid
import sh.christian.ozone.api.model.Blob

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
    ): Outcome

    suspend fun createPost(
        request: Post.Create.Request,
    ): Outcome
}

internal class OfflinePostRepository @Inject constructor(
    private val postDao: PostDao,
    private val profileDao: ProfileDao,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
    private val networkService: NetworkService,
    private val videoUploadService: VideoUploadService,
    private val transactionWriter: TransactionWriter,
    private val fileManager: FileManager,
    private val savedStateDataSource: SavedStateDataSource,
) : PostRepository {

    override fun likedBy(
        query: PostDataQuery,
        cursor: Cursor,
    ): Flow<CursorList<ProfileWithViewerState>> =
        savedStateDataSource.singleSessionFlow { signedInProfileId ->
            withPostEntity(
                signedInProfileId = signedInProfileId,
                profileId = query.profileId,
                postRecordKey = query.postRecordKey,
            ) { postEntity ->
                combine(
                    postDao.likedBy(
                        postUri = postEntity.uri.uri,
                        viewingProfileId = signedInProfileId?.id,
                        offset = query.data.offset,
                        limit = query.data.limit,
                    )
                        .distinctUntilChanged()
                        .map(List<PopulatedProfileEntity>::asExternalModels),

                    networkService.nextCursorFlow(
                        currentCursor = cursor,
                        currentRequestWithNextCursor = {
                            getLikes(
                                GetLikesQueryParams(
                                    uri = postEntity.uri.uri.let(::AtUri),
                                    limit = query.data.limit,
                                    cursor = cursor.value,
                                ),
                            )
                        },
                        nextCursor = GetLikesResponse::cursor,
                        onResponse = {
                            multipleEntitySaverProvider.saveInTransaction {
                                likes.forEach {
                                    add(
                                        viewingProfileId = signedInProfileId,
                                        postUri = postEntity.uri,
                                        like = it,
                                    )
                                }
                            }
                        },
                    ),
                    ::CursorList,
                ).distinctUntilChanged()
            }
        }

    override fun repostedBy(
        query: PostDataQuery,
        cursor: Cursor,
    ): Flow<CursorList<ProfileWithViewerState>> =
        savedStateDataSource.singleSessionFlow { signedInProfileId ->
            withPostEntity(
                signedInProfileId = signedInProfileId,
                profileId = query.profileId,
                postRecordKey = query.postRecordKey,
            ) { postEntity ->
                combine(
                    postDao.repostedBy(
                        postUri = postEntity.uri.uri,
                        viewingProfileId = signedInProfileId?.id,
                        offset = query.data.offset,
                        limit = query.data.limit,
                    )
                        .distinctUntilChanged()
                        .map(List<PopulatedProfileEntity>::asExternalModels),

                    networkService.nextCursorFlow(
                        currentCursor = cursor,
                        currentRequestWithNextCursor = {
                            getRepostedBy(
                                GetRepostedByQueryParams(
                                    uri = postEntity.uri.uri.let(::AtUri),
                                    limit = query.data.limit,
                                    cursor = cursor.value,
                                ),
                            )
                        },
                        nextCursor = GetRepostedByResponse::cursor,
                        onResponse = {
                            // TODO: Figure out how to get indexedAt for reposts
                        },
                    ),
                    ::CursorList,
                )
                    .distinctUntilChanged()
            }
        }

    override fun quotes(
        query: PostDataQuery,
        cursor: Cursor,
    ): Flow<CursorList<Post>> =
        savedStateDataSource.singleSessionFlow { signedInProfileId ->
            withPostEntity(
                signedInProfileId = signedInProfileId,
                profileId = query.profileId,
                postRecordKey = query.postRecordKey,
            ) { postEntity ->
                combine(
                    postDao.quotedPosts(
                        viewingProfileId = signedInProfileId?.id,
                        quotedPostUri = postEntity.uri.uri,
                    )
                        .distinctUntilChanged()
                        .map { populatedPostEntities ->
                            populatedPostEntities.map {
                                it.asExternalModel(
                                    quote = null,
                                    embeddedRecord = null,
                                )
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
                                ),
                            )
                        },
                        nextCursor = GetQuotesResponse::cursor,
                        onResponse = {
                            multipleEntitySaverProvider.saveInTransaction {
                                posts.forEach {
                                    add(
                                        viewingProfileId = signedInProfileId,
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
        savedStateDataSource.singleSessionFlow { signedInProfileId ->
            postDao.posts(
                viewingProfileId = signedInProfileId?.id,
                postUris = setOf(uri),
            )
                .distinctUntilChanged()
                .mapNotNull {
                    it.firstOrNull()?.asExternalModel(
                        quote = null,
                        embeddedRecord = null,
                    )
                }
        }

    override suspend fun createPost(
        request: Post.Create.Request,
    ): Outcome = savedStateDataSource.inCurrentProfileSession currentSession@{ signedInProfileId ->
        if (signedInProfileId == null) return@currentSession expiredSessionOutcome()

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
        val blobs = runCatchingUnlessCancelled {
            when {
                request.metadata.embeddedMedia.isNotEmpty() -> coroutineScope {
                    request.metadata.embeddedMedia.map { file ->
                        async {
                            when (file) {
                                is File.Media.Photo -> networkService.uploadImageBlob(
                                    data = fileManager.readBytes(file),
                                )
                                is File.Media.Video -> videoUploadService.uploadVideo(
                                    file = file,
                                )
                            }
                                .map(file::with)
                                .onSuccess { fileManager.delete(file) }
                        }
                    }
                        .awaitAll()
                        .mapNotNull(Result<MediaBlob?>::getOrNull)
                }
                @Suppress("DEPRECATION")
                // Deprecated media upload path is no longer supported
                request.metadata.mediaFiles.isNotEmpty() -> emptyList()
                else -> emptyList()
            }
        }.getOrNull() ?: emptyList()

        val mediaToUploadCount = request.metadata.embeddedMedia.size.takeIf { it > 0 }
            ?: @Suppress("DEPRECATION") request.metadata.mediaFiles.size

        if (mediaToUploadCount > 0 && blobs.size != mediaToUploadCount) {
            return@currentSession Outcome.Failure(Exception("Media upload failed"))
        }

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
        }.toOutcome()
    } ?: expiredSessionOutcome()

    override suspend fun sendInteraction(
        interaction: Post.Interaction,
    ): Outcome = savedStateDataSource.inCurrentProfileSession { signedInProfileId ->
        if (signedInProfileId == null) return@inCurrentProfileSession expiredSessionOutcome()

        when (interaction) {
            is Post.Interaction.Create -> networkService.runCatchingWithMonitoredNetworkRetry {
                when (interaction) {
                    is Post.Interaction.Create.Bookmark -> createBookmark(
                        CreateBookmarkRequest(
                            uri = interaction.postUri.uri.let(::AtUri),
                            cid = interaction.postId.id.let(::Cid),
                        ),
                    ).map { true to interaction.postId.id }

                    is Post.Interaction.Create.Like -> createRecord(
                        CreateRecordRequest(
                            repo = signedInProfileId.id.let(::Did),
                            collection = Nsid(Collections.Like),
                            record = BskyLike(
                                subject = StrongRef(
                                    cid = interaction.postId.id.let(::Cid),
                                    uri = interaction.postUri.uri.let(::AtUri),
                                ),
                                createdAt = Clock.System.now(),
                            ).asJsonContent(Like.serializer()),
                        ),
                    ).map(CreateRecordResponse::successWithUri)

                    is Post.Interaction.Create.Repost -> createRecord(
                        CreateRecordRequest(
                            repo = signedInProfileId.id.let(::Did),
                            collection = Nsid(Collections.Repost),
                            record = BskyRepost(
                                subject = StrongRef(
                                    cid = interaction.postId.id.let(::Cid),
                                    uri = interaction.postUri.uri.let(::AtUri),
                                ),
                                createdAt = Clock.System.now(),
                            ).asJsonContent(Repost.serializer()),
                        ),
                    ).map(CreateRecordResponse::successWithUri)
                }
            }.toOutcome { (succeeded, uriOrCidString) ->
                if (!succeeded) throw Exception("Record creation failed validation")
                transactionWriter.inTransaction {
                    when (interaction) {
                        is Post.Interaction.Create.Like -> {
                            upsertInteraction(
                                partial = PostViewerStatisticsEntity.Partial.Like(
                                    likeUri = uriOrCidString.let(::GenericUri),
                                    postUri = interaction.postUri,
                                    viewingProfileId = signedInProfileId,
                                ),
                            )
                            postDao.updateLikeCount(interaction.postUri.uri, true)
                        }
                        is Post.Interaction.Create.Repost -> {
                            upsertInteraction(
                                partial = PostViewerStatisticsEntity.Partial.Repost(
                                    repostUri = uriOrCidString.let(::GenericUri),
                                    postUri = interaction.postUri,
                                    viewingProfileId = signedInProfileId,
                                ),
                            )
                            postDao.updateRepostCount(interaction.postUri.uri, true)
                        }
                        is Post.Interaction.Create.Bookmark -> {
                            upsertInteraction(
                                partial = PostViewerStatisticsEntity.Partial.Bookmark(
                                    bookmarked = true,
                                    postUri = interaction.postUri,
                                    viewingProfileId = signedInProfileId,
                                ),
                            )
                            postDao.updateBookmarkCount(interaction.postUri.uri, true)
                        }
                    }
                }
            }

            is Post.Interaction.Delete -> networkService.runCatchingWithMonitoredNetworkRetry {
                when (interaction) {
                    is Post.Interaction.Delete.RemoveBookmark -> deleteBookmark(
                        DeleteBookmarkRequest(interaction.postUri.uri.let(::AtUri)),
                    )
                    is Post.Interaction.Delete.RemoveRepost -> deleteRecord(
                        DeleteRecordRequest(
                            repo = signedInProfileId.id.let(::Did),
                            collection = Nsid(Collections.Repost),
                            rkey = Collections.rKey(interaction.repostUri),
                        ),
                    )
                    is Post.Interaction.Delete.Unlike -> deleteRecord(
                        DeleteRecordRequest(
                            repo = signedInProfileId.id.let(::Did),
                            collection = Nsid(Collections.Like),
                            rkey = Collections.rKey(interaction.likeUri),
                        ),
                    )
                }
            }.toOutcome {
                transactionWriter.inTransaction {
                    when (interaction) {
                        is Post.Interaction.Delete.Unlike -> {
                            upsertInteraction(
                                partial = PostViewerStatisticsEntity.Partial.Like(
                                    likeUri = null,
                                    postUri = interaction.postUri,
                                    viewingProfileId = signedInProfileId,
                                ),
                            )
                            postDao.updateLikeCount(interaction.postUri.uri, false)
                        }
                        is Post.Interaction.Delete.RemoveRepost -> {
                            upsertInteraction(
                                partial = PostViewerStatisticsEntity.Partial.Repost(
                                    repostUri = null,
                                    postUri = interaction.postUri,
                                    viewingProfileId = signedInProfileId,
                                ),
                            )
                            postDao.updateRepostCount(interaction.postUri.uri, false)
                        }
                        is Post.Interaction.Delete.RemoveBookmark -> {
                            upsertInteraction(
                                partial = PostViewerStatisticsEntity.Partial.Bookmark(
                                    bookmarked = false,
                                    postUri = interaction.postUri,
                                    viewingProfileId = signedInProfileId,
                                ),
                            )
                            postDao.updateBookmarkCount(interaction.postUri.uri, false)
                        }
                    }
                }
            }
        }
    } ?: expiredSessionOutcome()

    private suspend fun upsertInteraction(
        partial: PostViewerStatisticsEntity.Partial,
    ) {
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

                    is PostViewerStatisticsEntity.Partial.Bookmark ->
                        postDao.updatePostStatisticsBookmarks(listOf(partial))
                }
            },
        )
    }

    private fun <T> withPostEntity(
        signedInProfileId: ProfileId?,
        profileId: Id.Profile,
        postRecordKey: RecordKey,
        block: (PostEntity) -> Flow<T>,
    ): Flow<T> = flow {
        emitAll(
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
                                postRecordKey = postRecordKey,
                            )
                        }
                        .first(),
                ),
            )
                .first(List<PostEntity>::isNotEmpty)
                .first()
                .let(block),
        )
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

private fun CreateRecordResponse.successWithUri(): Pair<Boolean, String> =
    Pair(validationStatus is CreateRecordValidationStatus.Valid, uri.atUri)

private suspend fun NetworkService.uploadImageBlob(
    data: ByteArray,
): Result<Blob> = runCatchingWithMonitoredNetworkRetry {
    api.uploadBlob(data)
        .map(UploadBlobResponse::blob)
}

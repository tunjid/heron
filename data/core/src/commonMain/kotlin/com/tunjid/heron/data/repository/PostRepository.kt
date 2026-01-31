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
import app.bsky.bookmark.GetBookmarksQueryParams
import app.bsky.bookmark.GetBookmarksResponse
import app.bsky.feed.GetLikesQueryParams
import app.bsky.feed.GetLikesResponse
import app.bsky.feed.GetPostsQueryParams
import app.bsky.feed.GetQuotesQueryParams
import app.bsky.feed.GetQuotesResponse
import app.bsky.feed.GetRepostedByQueryParams
import app.bsky.feed.GetRepostedByResponse
import app.bsky.feed.Like
import app.bsky.feed.Like as BskyLike
import app.bsky.feed.Post as BskyPost
import app.bsky.feed.PostReplyRef
import app.bsky.feed.PostView
import app.bsky.feed.Repost
import app.bsky.feed.Repost as BskyRepost
import com.atproto.repo.ApplyWritesCreate
import com.atproto.repo.ApplyWritesRequest
import com.atproto.repo.ApplyWritesRequestWriteUnion
import com.atproto.repo.CreateRecordRequest
import com.atproto.repo.CreateRecordResponse
import com.atproto.repo.CreateRecordValidationStatus
import com.atproto.repo.DeleteRecordRequest
import com.atproto.repo.PutRecordRequest
import com.atproto.repo.StrongRef
import com.atproto.repo.UploadBlobResponse
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Link
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.PostUri
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.offset
import com.tunjid.heron.data.core.models.value
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.LikeUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.data.core.types.RepostUri
import com.tunjid.heron.data.core.types.ThreadGateUri
import com.tunjid.heron.data.core.types.recordKey
import com.tunjid.heron.data.core.utilities.File
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.database.TransactionWriter
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.partialUpsert
import com.tunjid.heron.data.database.entities.PopulatedProfileEntity
import com.tunjid.heron.data.database.entities.PostEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.database.entities.asExternalModelWithViewerState
import com.tunjid.heron.data.database.entities.profile.PostViewerStatisticsEntity
import com.tunjid.heron.data.files.FileManager
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.network.VideoUploadService
import com.tunjid.heron.data.network.models.toNetworkRecord
import com.tunjid.heron.data.utilities.MediaBlob
import com.tunjid.heron.data.utilities.TidGenerator
import com.tunjid.heron.data.utilities.asJsonContent
import com.tunjid.heron.data.utilities.facet
import com.tunjid.heron.data.utilities.mapCatchingUnlessCancelled
import com.tunjid.heron.data.utilities.mapNotNullDistinctUntilChanged
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.nextCursorFlow
import com.tunjid.heron.data.utilities.postEmbedUnion
import com.tunjid.heron.data.utilities.profileLookup.ProfileLookup
import com.tunjid.heron.data.utilities.recordResolver.RecordResolver
import com.tunjid.heron.data.utilities.runCatchingUnlessCancelled
import com.tunjid.heron.data.utilities.toOutcome
import com.tunjid.heron.data.utilities.with
import com.tunjid.heron.data.utilities.withRefresh
import dev.zacsweers.metro.Inject
import io.ktor.utils.io.ByteReadChannel
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.io.Source
import kotlinx.serialization.Serializable
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Cid
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Nsid
import sh.christian.ozone.api.RKey
import sh.christian.ozone.api.model.Blob
import sh.christian.ozone.api.model.JsonContent

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
    ): Flow<CursorList<TimelineItem>>

    fun saved(
        query: CursorQuery,
        cursor: Cursor,
    ): Flow<CursorList<TimelineItem>>

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
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
    private val networkService: NetworkService,
    private val videoUploadService: VideoUploadService,
    private val transactionWriter: TransactionWriter,
    private val tidGenerator: TidGenerator,
    private val fileManager: FileManager,
    private val savedStateDataSource: SavedStateDataSource,
    private val profileLookup: ProfileLookup,
    private val recordResolver: RecordResolver,
) : PostRepository {

    override fun likedBy(
        query: PostDataQuery,
        cursor: Cursor,
    ): Flow<CursorList<ProfileWithViewerState>> =
        savedStateDataSource.singleSessionFlow { signedInProfileId ->
            withResolvedPostUri(
                signedInProfileId = signedInProfileId,
                profileId = query.profileId,
                postRecordKey = query.postRecordKey,
            ) { postUri ->
                combine(
                    postDao.likedBy(
                        postUri = postUri.uri,
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
                                    uri = postUri.uri.let(::AtUri),
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
                                        postUri = postUri,
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
            withResolvedPostUri(
                signedInProfileId = signedInProfileId,
                profileId = query.profileId,
                postRecordKey = query.postRecordKey,
            ) { postUri ->
                combine(
                    postDao.repostedBy(
                        postUri = postUri.uri,
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
                                    uri = postUri.uri.let(::AtUri),
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
    ): Flow<CursorList<TimelineItem>> =
        savedStateDataSource.singleSessionFlow { signedInProfileId ->
            withResolvedPostUri(
                signedInProfileId = signedInProfileId,
                profileId = query.profileId,
                postRecordKey = query.postRecordKey,
            ) { postUri ->
                combine(
                    postDao.quotedPostUriAndEmbeddedRecordUris(
                        quotedPostUri = postUri.uri,
                        offset = query.data.offset,
                        limit = query.data.limit,
                    )
                        .distinctUntilChanged()
                        .flatMapLatest { bookmarkedPostUriAndEmbeddedRecordUris ->
                            recordResolver.timelineItems(
                                items = bookmarkedPostUriAndEmbeddedRecordUris,
                                signedInProfileId = signedInProfileId,
                                postUri = PostEntity.UriWithEmbeddedRecordUri::uri,
                                associatedRecordUris = {
                                    listOfNotNull(it.embeddedRecordUri)
                                },
                                associatedProfileIds = {
                                    emptyList()
                                },
                                block = { item ->
                                    list += TimelineItem.Single(
                                        id = item.uri.uri,
                                        post = post,
                                        isMuted = isMuted(post),
                                        threadGate = threadGate(item.uri),
                                        appliedLabels = appliedLabels,
                                        signedInProfileId = signedInProfileId,
                                    )
                                },
                            )
                        },
                    networkService.nextCursorFlow(
                        currentCursor = cursor,
                        currentRequestWithNextCursor = {
                            getQuotes(
                                GetQuotesQueryParams(
                                    uri = postUri.uri.let(::AtUri),
                                    limit = query.data.limit,
                                    cursor = cursor.value,
                                ),
                            )
                        },
                        nextCursor = GetQuotesResponse::cursor,
                        onResponse = {
                            multipleEntitySaverProvider.saveInTransaction {
                                posts.forEach { postView ->
                                    add(
                                        viewingProfileId = signedInProfileId,
                                        postView = postView,
                                    )
                                }
                            }
                        },
                    ),
                    ::CursorList,
                )
            }
        }

    override fun saved(
        query: CursorQuery,
        cursor: Cursor,
    ): Flow<CursorList<TimelineItem>> =
        savedStateDataSource.singleSessionFlow { signedInProfileId ->
            if (signedInProfileId == null) {
                return@singleSessionFlow emptyFlow()
            }

            combine(
                postDao.bookmarkedPostUriAndEmbeddedRecordUris(
                    viewingProfileId = signedInProfileId.id,
                    offset = query.data.offset,
                    limit = query.data.limit,
                )
                    .distinctUntilChanged()
                    .flatMapLatest { bookmarkedPostUriAndEmbeddedRecordUris ->
                        recordResolver.timelineItems(
                            items = bookmarkedPostUriAndEmbeddedRecordUris,
                            signedInProfileId = signedInProfileId,
                            postUri = PostEntity.UriWithEmbeddedRecordUri::uri,
                            associatedRecordUris = {
                                listOfNotNull(it.embeddedRecordUri)
                            },
                            associatedProfileIds = {
                                emptyList()
                            },
                            block = { item ->
                                list += TimelineItem.Single(
                                    id = item.uri.uri,
                                    post = post,
                                    isMuted = isMuted(post),
                                    threadGate = threadGate(item.uri),
                                    appliedLabels = appliedLabels,
                                    signedInProfileId = signedInProfileId,
                                )
                            },
                        )
                    },
                networkService.nextCursorFlow(
                    currentCursor = cursor,
                    currentRequestWithNextCursor = {
                        getBookmarks(
                            GetBookmarksQueryParams(
                                limit = query.data.limit,
                                cursor = cursor.value,
                            ),
                        )
                    },
                    nextCursor = GetBookmarksResponse::cursor,
                    onResponse = {
                        multipleEntitySaverProvider.saveInTransaction {
                            bookmarks.forEach { bookmarkView ->
                                add(
                                    viewingProfileId = signedInProfileId,
                                    bookmarkView = bookmarkView,
                                )
                            }
                        }
                    },
                ),
                ::CursorList,
            )
        }

    override fun post(
        uri: PostUri,
    ): Flow<Post> =
        savedStateDataSource.singleSessionFlow { signedInProfileId ->
            postDao.posts(
                viewingProfileId = signedInProfileId?.id,
                postUris = setOf(uri),
            )
                .mapNotNullDistinctUntilChanged {
                    it.firstOrNull()?.asExternalModel(
                        embeddedRecord = null,
                    )
                }
        }

    override suspend fun createPost(
        request: Post.Create.Request,
    ): Outcome = savedStateDataSource.inCurrentProfileSession currentSession@{ signedInProfileId ->
        if (signedInProfileId == null) return@currentSession expiredSessionOutcome()

        val writes = mutableListOf<ApplyWritesCreate>()
        val now = Clock.System.now()

        val postTid = tidGenerator.generate()
        val rKey = RKey(postTid)
        val postUri = PostUri(
            profileId = request.authorId,
            postRecordKey = RecordKey(postTid),
        )

        val blobsResult = request.mediaBlobs()
        val blobs = blobsResult.getOrNull() ?: return@currentSession Outcome.Failure(
            requireNotNull(blobsResult.exceptionOrNull()),
        )

        writes.add(
            ApplyWritesCreate(
                collection = Nsid(PostUri.NAMESPACE),
                rkey = rKey,
                value = request.postNetworkRecord(
                    blobs = blobs,
                    createdAt = now,
                ),
            ),
        )

        val threadGateAllowed = request.metadata.allowed
        if (threadGateAllowed != null) writes.add(
            ApplyWritesCreate(
                collection = Nsid(ThreadGateUri.NAMESPACE),
                rkey = rKey,
                value = threadGateAllowed.toNetworkRecord(
                    postUri = postUri,
                    createdAt = now,
                ),
            ),
        )

        networkService.runCatchingWithMonitoredNetworkRetry {
            applyWrites(
                ApplyWritesRequest(
                    repo = request.authorId.id.let(::Did),
                    writes = writes.map(ApplyWritesRequestWriteUnion::Create),
                    validate = true,
                ),
            )
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
                            collection = Nsid(LikeUri.NAMESPACE),
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
                            collection = Nsid(RepostUri.NAMESPACE),
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
                                    likeUri = uriOrCidString.let(::LikeUri),
                                    postUri = interaction.postUri,
                                    viewingProfileId = signedInProfileId,
                                ),
                            )
                            postDao.updateLikeCount(
                                postUri = interaction.postUri.uri,
                                isIncrement = true,
                            )
                        }
                        is Post.Interaction.Create.Repost -> {
                            upsertInteraction(
                                partial = PostViewerStatisticsEntity.Partial.Repost(
                                    repostUri = uriOrCidString.let(::RepostUri),
                                    postUri = interaction.postUri,
                                    viewingProfileId = signedInProfileId,
                                ),
                            )
                            postDao.updateRepostCount(
                                postUri = interaction.postUri.uri,
                                isIncrement = true,
                            )
                        }
                        is Post.Interaction.Create.Bookmark -> {
                            upsertInteraction(
                                partial = PostViewerStatisticsEntity.Partial.Bookmark(
                                    bookmarked = true,
                                    postUri = interaction.postUri,
                                    viewingProfileId = signedInProfileId,
                                ),
                            )
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
                            collection = Nsid(RepostUri.NAMESPACE),
                            rkey = interaction.repostUri.recordKey.value.let(::RKey),
                        ),
                    )
                    is Post.Interaction.Delete.Unlike -> deleteRecord(
                        DeleteRecordRequest(
                            repo = signedInProfileId.id.let(::Did),
                            collection = Nsid(LikeUri.NAMESPACE),
                            rkey = interaction.likeUri.recordKey.value.let(::RKey),
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
                        }
                    }
                }
            }
            is Post.Interaction.Upsert.Gate -> {
                val repo = signedInProfileId.id.let(::Did)
                val collection = Nsid(ThreadGateUri.NAMESPACE)
                val record = interaction.toNetworkRecord()
                val recordKey = interaction.postUri
                    .recordKey
                    .value
                    .let(::RKey)

                networkService.runCatchingWithMonitoredNetworkRetry {
                    putRecord(
                        PutRecordRequest(
                            repo = repo,
                            collection = collection,
                            record = record,
                            rkey = recordKey,
                        ),
                    )
                }
                    .mapCatchingUnlessCancelled { response ->
                        // Initialize with starting record cid
                        val updatedRecordId = response.cid
                        var updatedPostView: PostView? = null

                        for (i in 0 until MaxThreadGateUpdateAttempts) {
                            val fetchedPostView =
                                networkService.runCatchingWithMonitoredNetworkRetry {
                                    getPosts(
                                        GetPostsQueryParams(
                                            listOf(interaction.postUri.uri.let(::AtUri)),
                                        ),
                                    )
                                }
                                    .getOrNull()
                                    ?.posts
                                    ?.firstOrNull()

                            if (fetchedPostView != null && updatedRecordId == fetchedPostView.threadgate?.cid) {
                                updatedPostView = fetchedPostView
                                break
                            }
                            delay(ThreadGateUpsertPollDelay)
                        }

                        requireNotNull(updatedPostView) {
                            "Failed to update thread gate"
                        }
                    }
                    .toOutcome { postView ->
                        multipleEntitySaverProvider.saveInTransaction {
                            add(
                                viewingProfileId = signedInProfileId,
                                postView = postView,
                            )
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

    private fun <T> withResolvedPostUri(
        signedInProfileId: ProfileId?,
        profileId: Id.Profile,
        postRecordKey: RecordKey,
        block: (PostUri) -> Flow<T>,
    ): Flow<T> = flow {
        val profileDid = profileLookup.lookupProfileDid(
            profileId = profileId,
        ) ?: return@flow

        val resolvedId = ProfileId(profileDid.did)
        val postUri = PostUri(
            profileId = resolvedId,
            postRecordKey = postRecordKey,
        )

        emitAll(
            block(postUri)
                .withRefresh {
                    profileLookup.refreshProfile(
                        signedInProfileId = signedInProfileId,
                        profileId = resolvedId,
                    )
                },
        )
    }

    private suspend fun Post.Create.Request.postNetworkRecord(
        blobs: List<MediaBlob>,
        createdAt: Instant,
    ): JsonContent {
        val resolvedLinks: List<Link> = profileLookup.resolveProfileHandleLinks(
            links = links,
        )
        val reply = metadata.reply?.parent?.let { parent ->
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
        return BskyPost(
            text = text,
            reply = reply,
            embed = postEmbedUnion(
                embeddedRecordReference = metadata.embeddedRecordReference,
                mediaBlobs = blobs,
            ),
            facets = resolvedLinks.facet(),
            createdAt = createdAt,
        )
            .asJsonContent(BskyPost.serializer())
    }

    private suspend fun Post.Create.Request.mediaBlobs(): Result<List<MediaBlob>> =
        runCatchingUnlessCancelled {
            val blobs = coroutineScope {
                metadata.embeddedMedia.map { file ->
                    async {
                        when (file) {
                            is File.Media.Photo -> fileManager.source(file).use {
                                networkService.uploadImageBlob(data = it)
                            }
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

            val mediaToUploadCount = metadata.embeddedMedia.size

            if (mediaToUploadCount > 0 && blobs.size != mediaToUploadCount) {
                throw Exception("Media upload failed")
            }

            return@runCatchingUnlessCancelled blobs
        }
}

private fun List<PopulatedProfileEntity>.asExternalModels() =
    map(PopulatedProfileEntity::asExternalModelWithViewerState)

private fun CreateRecordResponse.successWithUri(): Pair<Boolean, String> =
    Pair(validationStatus is CreateRecordValidationStatus.Valid, uri.atUri)

private suspend fun NetworkService.uploadImageBlob(
    data: Source,
): Result<Blob> = runCatchingWithMonitoredNetworkRetry {
    uploadBlob(ByteReadChannel(data))
        .map(UploadBlobResponse::blob)
}

private val ThreadGateUpsertPollDelay = 2.seconds
private const val MaxThreadGateUpdateAttempts = 4

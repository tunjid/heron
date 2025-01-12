package com.tunjid.heron.data.repository

import app.bsky.feed.GetLikesQueryParams
import app.bsky.feed.GetLikesResponse
import app.bsky.feed.GetQuotesQueryParams
import app.bsky.feed.GetQuotesResponse
import app.bsky.feed.GetRepostedByQueryParams
import app.bsky.feed.GetRepostedByResponse
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.ProfileWithRelationship
import com.tunjid.heron.data.core.models.value
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.entities.PopulatedProfileEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.database.entities.profile.asExternalModel
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.utilities.CursorQuery
import com.tunjid.heron.data.utilities.InvalidationTrackerDebounceMillis
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.nextCursorFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import sh.christian.ozone.api.AtUri

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
}

class OfflinePostRepository(
    private val postDao: PostDao,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
    private val networkService: NetworkService,
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
            .debounce(InvalidationTrackerDebounceMillis)


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
            .debounce(InvalidationTrackerDebounceMillis)

    override fun quotes(
        query: PostDataQuery,
        cursor: Cursor,
    ): Flow<CursorList<Post>> =
        combine(
            postDao.quotedPosts(
                quotedPostId = query.postId.id,
            )
                .map {
                    it.map { it.asExternalModel(quote = null) }
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
}

private fun List<PopulatedProfileEntity>.asExternalModels() =
    map {
        ProfileWithRelationship(
            profile = it.profileEntity.asExternalModel(),
            relationship = it.relationship?.asExternalModel(),
        )
    }

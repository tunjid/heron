package com.tunjid.heron.data.repository

import app.bsky.actor.SearchActorsQueryParams
import app.bsky.actor.SearchActorsTypeaheadQueryParams
import app.bsky.feed.SearchPostsQueryParams
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.SearchResult
import com.tunjid.heron.data.core.models.value
import com.tunjid.heron.data.database.entities.profile.asExternalModel
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.network.models.post
import com.tunjid.heron.data.network.models.profile
import com.tunjid.heron.data.network.models.profileProfileRelationshipsEntities
import com.tunjid.heron.data.utilities.CursorQuery
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.runCatchingWithNetworkRetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import me.tatarka.inject.annotations.Inject

@Serializable
sealed class SearchQuery : CursorQuery {

    abstract val query: String
    abstract val isLocalOnly: Boolean

    val sourceId
        get() = when (this) {
            is Post.Latest -> "latest-posts"
            is Post.Top -> "top-posts"
            is Profile -> "profiles"
        }

    @Serializable
    sealed class Post : SearchQuery() {
        data class Top(
            override val query: String,
            override val isLocalOnly: Boolean,
            override val data: CursorQuery.Data,
        ) : Post()

        data class Latest(
            override val query: String,
            override val isLocalOnly: Boolean,
            override val data: CursorQuery.Data,
        ) : Post()
    }

    @Serializable
    data class Profile(
        override val query: String,
        override val isLocalOnly: Boolean,
        override val data: CursorQuery.Data,
    ) : SearchQuery()
}

interface SearchRepository {
    fun postSearch(
        query: SearchQuery.Post,
        cursor: Cursor,
    ): Flow<CursorList<SearchResult.Post>>

    fun profileSearch(
        query: SearchQuery.Profile,
        cursor: Cursor,
    ): Flow<CursorList<SearchResult.Profile>>

    fun autoCompleteProfileSearch(
        query: SearchQuery.Profile,
        cursor: Cursor,
    ): Flow<List<SearchResult.Profile>>

}

class OfflineSearchRepository @Inject constructor(
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
    private val networkService: NetworkService,
    private val savedStateRepository: SavedStateRepository,
) : SearchRepository {

    override fun postSearch(
        query: SearchQuery.Post,
        cursor: Cursor,
    ): Flow<CursorList<SearchResult.Post>> = flow {
        val response = runCatchingWithNetworkRetry {
            networkService.api.searchPosts(
                params = SearchPostsQueryParams(
                    q = query.query,
                    limit = query.data.limit,
                    cursor = when (cursor) {
                        Cursor.Initial -> cursor.value
                        is Cursor.Next -> cursor.value
                        Cursor.Pending -> null
                    },
                )
            )
        }
            .getOrNull()

        response?.posts?.let {
            multipleEntitySaverProvider.saveInTransaction {
                val authProfileId = savedStateRepository.signedInProfileId
                val posts = it.map { postView ->
                    if (authProfileId != null) add(
                        viewingProfileId = authProfileId,
                        postView = postView
                    )
                    SearchResult.Post(
                        post = postView.post(),
                    )
                }
                emit(
                    CursorList(
                        items = posts,
                        nextCursor = response.cursor?.let(Cursor::Next) ?: Cursor.Pending
                    )
                )
            }
        }
    }

    override fun profileSearch(
        query: SearchQuery.Profile,
        cursor: Cursor,
    ): Flow<CursorList<SearchResult.Profile>> = flow {
        val response = runCatchingWithNetworkRetry {
            networkService.api.searchActors(
                params = SearchActorsQueryParams(
                    q = query.query,
                    limit = query.data.limit,
                    cursor = when (cursor) {
                        Cursor.Initial -> cursor.value
                        is Cursor.Next -> cursor.value
                        Cursor.Pending -> null
                    },
                )
            )
        }
            .getOrNull()

        multipleEntitySaverProvider.saveInTransaction {
            val authProfileId = savedStateRepository.signedInProfileId
            response?.actors
                ?.mapNotNull { profileView ->
                    if (authProfileId != null) add(
                        viewingProfileId = authProfileId,
                        profileView = profileView,
                    )
                    savedStateRepository.signedInProfileId?.let {
                        SearchResult.Profile(
                            profile = profileView.profile(),
                            relationship = profileView.profileProfileRelationshipsEntities(
                                viewingProfileId = it
                            ).first().asExternalModel()
                        )
                    }
                }
                ?.let { profiles ->
                    emit(
                        CursorList(
                            items = profiles,
                            nextCursor = response.cursor?.let(Cursor::Next)
                                ?: Cursor.Pending
                        )
                    )
                }
        }
    }

    override fun autoCompleteProfileSearch(
        query: SearchQuery.Profile,
        cursor: Cursor,
    ): Flow<List<SearchResult.Profile>> = flow {
        val response = runCatchingWithNetworkRetry {
            networkService.api.searchActorsTypeahead(
                params = SearchActorsTypeaheadQueryParams(
                    q = query.query,
                    limit = query.data.limit,
                )
            )
        }
            .getOrNull()

        multipleEntitySaverProvider.saveInTransaction {
            val authProfileId = savedStateRepository.signedInProfileId
            response?.actors
                ?.mapNotNull { profileView ->
                    if (authProfileId != null) add(
                        viewingProfileId = authProfileId,
                        profileView = profileView,
                    )
                    savedStateRepository.signedInProfileId?.let {
                        SearchResult.Profile(
                            profile = profileView.profile(),
                            relationship = profileView.profileProfileRelationshipsEntities(
                                viewingProfileId = it
                            ).first().asExternalModel()
                        )
                    }
                }
                ?.let { profiles ->
                    emit(
                        profiles
                    )
                }
        }
    }

}
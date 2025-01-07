package com.tunjid.heron.data.repository

import app.bsky.actor.SearchActorsQueryParams
import app.bsky.actor.SearchActorsTypeaheadQueryParams
import app.bsky.feed.SearchPostsQueryParams
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.SearchResult
import com.tunjid.heron.data.core.models.value
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.entities.asExternalModel
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
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
    private val profileDao: ProfileDao,
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
            ?: return@flow

        val authProfileId = savedStateRepository.signedInProfileId
            ?: return@flow

        multipleEntitySaverProvider.saveInTransaction {
            val posts = response.posts.map { postView ->
                add(
                    viewingProfileId = authProfileId,
                    postView = postView
                )
                when (query) {
                    is SearchQuery.Post.Latest -> SearchResult.Post.Top(
                        post = postView.post(),
                    )

                    is SearchQuery.Post.Top -> SearchResult.Post.Latest(
                        post = postView.post(),
                    )
                }
            }
            emit(
                CursorList(
                    items = posts,
                    nextCursor = response.cursor?.let(Cursor::Next) ?: Cursor.Pending
                )
            )
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
            ?: return@flow

        val authProfileId = savedStateRepository.signedInProfileId
            ?: return@flow

        val profileIds = mutableListOf<Id>()

        multipleEntitySaverProvider.saveInTransaction {
            response.actors.forEach { profileView ->
                profileIds.add(profileView.did.did.let(::Id))
                add(
                    viewingProfileId = authProfileId,
                    profileView = profileView,
                )
            }
        }
        emitAll(
            combine(
                flow = profileDao.profiles(profileIds),
                flow2 = profileDao.relationships(
                    profileId = authProfileId.id,
                    otherProfileIds = profileIds.toSet()
                ),
                transform = { profiles, relationships ->
                    val otherProfileIdsToRelationships = relationships
                        .associateBy { it.otherProfileId }
                    profiles
                        .sortedBy { profile ->
                            response.actors.indexOfFirst { profile.did.id == it.did.did }
                        }
                        .mapNotNull { profile ->
                            SearchResult.Profile(
                                profile = profile.asExternalModel(),
                                relationship = otherProfileIdsToRelationships[profile.did]
                                    ?.asExternalModel()
                                    ?: return@mapNotNull null
                            )
                        }
                        .let { results ->
                            CursorList(
                                items = results,
                                nextCursor = response.cursor
                                    ?.let(Cursor::Next)
                                    ?: Cursor.Pending
                            )
                        }
                }
            )
        )
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
            ?: return@flow

        val authProfileId = savedStateRepository.signedInProfileId
            ?: return@flow

        multipleEntitySaverProvider.saveInTransaction {
            response.actors
                .map { profileView ->
                    add(
                        viewingProfileId = authProfileId,
                        profileView = profileView,
                    )
                    SearchResult.Profile(
                        profile = profileView.profile(),
                        relationship = profileView.profileProfileRelationshipsEntities(
                            viewingProfileId = authProfileId
                        ).first().asExternalModel()
                    )
                }
                .let { profiles ->
                    emit(
                        profiles
                    )
                }
        }
    }

}
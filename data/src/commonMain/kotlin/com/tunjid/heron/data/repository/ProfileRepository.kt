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

import app.bsky.actor.ProfileView
import app.bsky.graph.GetFollowersQueryParams
import app.bsky.graph.GetFollowsQueryParams
import app.bsky.graph.GetListQueryParams
import app.bsky.graph.GetListResponse
import com.atproto.repo.CreateRecordRequest
import com.atproto.repo.CreateRecordValidationStatus
import com.atproto.repo.DeleteRecordRequest
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.ListMember
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileViewerState
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.models.value
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.daos.ListDao
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.entities.PopulatedListMemberEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.database.entities.profile.ProfileViewerStateEntity
import com.tunjid.heron.data.database.entities.profile.asExternalModel
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.network.models.profile
import com.tunjid.heron.data.network.models.profileViewerStateEntities
import com.tunjid.heron.data.utilities.Collections
import com.tunjid.heron.data.utilities.CursorQuery
import com.tunjid.heron.data.utilities.asJsonContent
import com.tunjid.heron.data.utilities.lookupProfileDid
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.nextCursorFlow
import com.tunjid.heron.data.utilities.observeProfileWithViewerStates
import com.tunjid.heron.data.utilities.offset
import com.tunjid.heron.data.utilities.refreshProfile
import com.tunjid.heron.data.utilities.runCatchingWithNetworkRetry
import com.tunjid.heron.data.utilities.toProfileWithViewerStates
import com.tunjid.heron.data.utilities.withRefresh
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import me.tatarka.inject.annotations.Inject
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Nsid
import app.bsky.graph.Follow as BskyFollow

@Serializable
data class ProfilesQuery(
    val profileId: Id.Profile,
    override val data: CursorQuery.Data,
) : CursorQuery

@Serializable
data class ListMemberQuery(
    val listUri: Uri,
    override val data: CursorQuery.Data,
) : CursorQuery

interface ProfileRepository {

    fun signedInProfile(): Flow<Profile>

    fun profile(profileId: Id.Profile): Flow<Profile>

    fun profileRelationships(
        profileIds: Set<Id.Profile>,
    ): Flow<List<ProfileViewerState>>

    fun listMembers(
        query: ListMemberQuery,
        cursor: Cursor,
    ): Flow<CursorList<ListMember>>

    fun followers(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<ProfileWithViewerState>>

    fun following(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<ProfileWithViewerState>>

    suspend fun sendConnection(
        connection: Profile.Connection,
    )
}

class OfflineProfileRepository @Inject constructor(
    private val profileDao: ProfileDao,
    private val listDao: ListDao,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
    private val networkService: NetworkService,
    private val savedStateRepository: SavedStateRepository,
) : ProfileRepository {

    override fun signedInProfile(): Flow<Profile> =
        signedInProfileId()
            .flatMapLatest { profileDao.profiles(listOf(it)) }
            .mapNotNull { it.firstOrNull()?.asExternalModel() }

    override fun profile(
        profileId: Id.Profile,
    ): Flow<Profile> =
        profileDao.profiles(listOf(profileId))
            .distinctUntilChanged()
            .map { it.firstOrNull()?.asExternalModel() }
            .filterNotNull()
            .withRefresh {
                refreshProfile(
                    profileId = profileId,
                    profileDao = profileDao,
                    networkService = networkService,
                    multipleEntitySaverProvider = multipleEntitySaverProvider,
                    savedStateRepository = savedStateRepository,
                )
            }
            .distinctUntilChanged()

    override fun profileRelationships(
        profileIds: Set<Id.Profile>,
    ): Flow<List<ProfileViewerState>> =
        signedInProfileId()
            .flatMapLatest {
                profileDao.viewerState(
                    profileId = it.id,
                    otherProfileIds = profileIds,
                )
            }
            .distinctUntilChanged()
            .map { viewerEntities ->
                viewerEntities.map(ProfileViewerStateEntity::asExternalModel)
            }

    override fun listMembers(
        query: ListMemberQuery,
        cursor: Cursor
    ): Flow<CursorList<ListMember>> =
        combine(
            listDao.listMembers(
                listUri = query.listUri.uri,
                offset = query.data.offset,
                limit = query.data.limit,
            )
                .map {
                    it.map(PopulatedListMemberEntity::asExternalModel)
                },
            nextCursorFlow(
                currentCursor = cursor,
                currentRequestWithNextCursor = {
                    networkService.api.getList(
                        GetListQueryParams(
                            list = query.listUri.uri.let(::AtUri),
                            limit = query.data.limit,
                            cursor = cursor.value,
                        )
                    )
                },
                nextCursor = GetListResponse::cursor,
                onResponse = {
                    multipleEntitySaverProvider.saveInTransaction {
                        add(list)
                        items.forEach { listItemView ->
                            add(
                                listUri = list.uri.atUri.let(::ListUri),
                                listItemView = listItemView,
                            )
                        }
                    }
                },
            ),
            ::CursorList
        )
            .distinctUntilChanged()

    override fun followers(
        query: ProfilesQuery,
        cursor: Cursor
    ): Flow<CursorList<ProfileWithViewerState>> = flow {
        val profileDid = lookupProfileDid(
            profileId = query.profileId,
            profileDao = profileDao,
            networkService = networkService,
        ) ?: return@flow

        val response = runCatchingWithNetworkRetry {
            networkService.api.getFollowers(
                GetFollowersQueryParams(
                    actor = profileDid,
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

        val signedInProfileId = savedStateRepository.signedInProfileId

        multipleEntitySaverProvider.saveInTransaction {
            response.followers
                .forEach { profileView ->
                    add(
                        viewingProfileId = signedInProfileId,
                        profileView = profileView,
                    )
                }
        }

        val nextCursor = response.cursor?.let(Cursor::Next) ?: Cursor.Pending

        // Emit network results immediately for minimal latency during search
        emit(
            CursorList(
                items = response.followers.toProfileWithViewerStates(
                    signedInProfileId = signedInProfileId,
                    profileMapper = ProfileView::profile,
                    profileViewerStateEntities = ProfileView::profileViewerStateEntities,
                ),
                nextCursor = nextCursor,
            )
        )

        emitAll(
            response.followers.observeProfileWithViewerStates(
                profileDao = profileDao,
                signedInProfileId = signedInProfileId,
                profileMapper = ProfileView::profile,
                idMapper = { did.did.let(::ProfileId) },
            )
                .map { profileWithViewerStates ->
                    CursorList(
                        items = profileWithViewerStates,
                        nextCursor = nextCursor,
                    )
                }
        )
    }

    override fun following(
        query: ProfilesQuery,
        cursor: Cursor
    ): Flow<CursorList<ProfileWithViewerState>> = flow {
        val profileDid = lookupProfileDid(
            profileId = query.profileId,
            profileDao = profileDao,
            networkService = networkService,
        ) ?: return@flow

        val response = runCatchingWithNetworkRetry {
            networkService.api.getFollows(
                GetFollowsQueryParams(
                    actor = profileDid,
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

        val signedInProfileId = savedStateRepository.signedInProfileId

        multipleEntitySaverProvider.saveInTransaction {
            response.follows
                .forEach { profileView ->
                    add(
                        viewingProfileId = signedInProfileId,
                        profileView = profileView,
                    )
                }
        }

        val nextCursor = response.cursor?.let(Cursor::Next) ?: Cursor.Pending

        // Emit network results immediately for minimal latency during search
        emit(
            CursorList(
                items = response.follows.toProfileWithViewerStates(
                    signedInProfileId = signedInProfileId,
                    profileMapper = ProfileView::profile,
                    profileViewerStateEntities = ProfileView::profileViewerStateEntities,
                ),
                nextCursor = nextCursor,
            )
        )

        emitAll(
            response.follows.observeProfileWithViewerStates(
                profileDao = profileDao,
                signedInProfileId = signedInProfileId,
                profileMapper = ProfileView::profile,
                idMapper = { did.did.let(::ProfileId) },
            )
                .map { profileWithViewerStates ->
                    CursorList(
                        items = profileWithViewerStates,
                        nextCursor = nextCursor,
                    )
                }
        )
    }

    override suspend fun sendConnection(
        connection: Profile.Connection,
    ) {
        when (connection) {
            is Profile.Connection.Follow -> runCatchingWithNetworkRetry {
                networkService.api.createRecord(
                    CreateRecordRequest(
                        repo = connection.signedInProfileId.id.let(::Did),
                        collection = Nsid(Collections.Follow),
                        record = BskyFollow(
                            subject = connection.profileId.id.let(::Did),
                            createdAt = Clock.System.now(),
                        ).asJsonContent(BskyFollow.serializer()),
                    )
                )
            }
                .getOrNull()
                ?.let {
                    if (it.validationStatus !is CreateRecordValidationStatus.Valid) return@let
                    profileDao.updatePartialProfileViewers(
                        listOf(
                            ProfileViewerStateEntity.Partial(
                                profileId = connection.signedInProfileId,
                                otherProfileId = connection.profileId,
                                following = it.uri.atUri.let(::GenericUri),
                                followedBy = connection.followedBy,
                            )
                        )
                    )
                }

            is Profile.Connection.Unfollow -> runCatchingWithNetworkRetry {
                networkService.api.deleteRecord(
                    DeleteRecordRequest(
                        repo = connection.signedInProfileId.id.let(::Did),
                        collection = Nsid(Collections.Follow),
                        rkey = Collections.rKey(connection.followUri),
                    )
                )
            }
                .getOrNull()
                ?.let {
                    profileDao.updatePartialProfileViewers(
                        listOf(
                            ProfileViewerStateEntity.Partial(
                                profileId = connection.signedInProfileId,
                                otherProfileId = connection.profileId,
                                following = null,
                                followedBy = connection.followedBy,
                            )
                        )
                    )
                }
        }
    }

    private fun signedInProfileId() = savedStateRepository.savedState
        .mapNotNull { it.auth?.authProfileId }
        .distinctUntilChanged()
}

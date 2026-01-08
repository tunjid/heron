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

import app.bsky.actor.Profile as BskyProfile
import app.bsky.feed.GetActorFeedsQueryParams
import app.bsky.feed.GetActorFeedsResponse
import app.bsky.graph.Block as BskyBlock
import app.bsky.graph.Follow as BskyFollow
import app.bsky.graph.GetActorStarterPacksQueryParams
import app.bsky.graph.GetActorStarterPacksResponse
import app.bsky.graph.GetFollowersQueryParams
import app.bsky.graph.GetFollowersResponse
import app.bsky.graph.GetFollowsQueryParams
import app.bsky.graph.GetFollowsResponse
import app.bsky.graph.GetListQueryParams
import app.bsky.graph.GetListResponse
import app.bsky.graph.GetListsQueryParams
import app.bsky.graph.GetListsResponse
import app.bsky.graph.MuteActorRequest
import app.bsky.graph.UnmuteActorRequest
import com.atproto.repo.CreateRecordRequest
import com.atproto.repo.CreateRecordValidationStatus
import com.atproto.repo.DeleteRecordRequest
import com.atproto.repo.GetRecordQueryParams
import com.atproto.repo.PutRecordRequest
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.ListMember
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileViewerState
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.models.StarterPack
import com.tunjid.heron.data.core.models.offset
import com.tunjid.heron.data.core.models.value
import com.tunjid.heron.data.core.types.BlockUri
import com.tunjid.heron.data.core.types.FollowUri
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.RecordCreationException
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.core.types.recordKey
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.database.daos.FeedGeneratorDao
import com.tunjid.heron.data.database.daos.ListDao
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.daos.StarterPackDao
import com.tunjid.heron.data.database.entities.PopulatedFeedGeneratorEntity
import com.tunjid.heron.data.database.entities.PopulatedListEntity
import com.tunjid.heron.data.database.entities.PopulatedListMemberEntity
import com.tunjid.heron.data.database.entities.PopulatedProfileEntity
import com.tunjid.heron.data.database.entities.PopulatedStarterPackEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.database.entities.profile.ProfileViewerStateEntity
import com.tunjid.heron.data.database.entities.profile.asExternalModel
import com.tunjid.heron.data.files.FileManager
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.utilities.Collections
import com.tunjid.heron.data.utilities.asJsonContent
import com.tunjid.heron.data.utilities.mapCatchingUnlessCancelled
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.nextCursorFlow
import com.tunjid.heron.data.utilities.profileLookup.ProfileLookup
import com.tunjid.heron.data.utilities.profileLookup.lookupProfileDid
import com.tunjid.heron.data.utilities.profileLookup.refreshProfile
import com.tunjid.heron.data.utilities.toOutcome
import com.tunjid.heron.data.utilities.withRefresh
import dev.zacsweers.metro.Inject
import io.ktor.utils.io.ByteReadChannel
import kotlin.time.Clock
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.Serializable
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Nsid
import sh.christian.ozone.api.RKey

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

    fun commonFollowers(
        otherProfileId: Id.Profile,
        limit: Long,
    ): Flow<List<Profile>>

    fun listMembers(
        query: ListMemberQuery,
        cursor: Cursor,
    ): Flow<CursorList<ListMember>>

    fun followers(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<ProfileWithViewerState>>

    fun starterPacks(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<StarterPack>>

    fun lists(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<FeedList>>

    fun feedGenerators(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<FeedGenerator>>

    fun following(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<ProfileWithViewerState>>

    suspend fun sendConnection(
        connection: Profile.Connection,
    ): Outcome

    suspend fun updateRestriction(
        restriction: Profile.Restriction,
    ): Outcome

    suspend fun updateProfile(
        update: Profile.Update,
    ): Outcome
}

internal class OfflineProfileRepository @Inject constructor(
    private val profileDao: ProfileDao,
    private val listDao: ListDao,
    private val starterPackDao: StarterPackDao,
    private val feedGeneratorDao: FeedGeneratorDao,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
    private val profileLookup: ProfileLookup,
    private val networkService: NetworkService,
    private val fileManager: FileManager,
    private val savedStateDataSource: SavedStateDataSource,
) : ProfileRepository {

    override fun signedInProfile(): Flow<Profile> =
        savedStateDataSource.singleAuthorizedSessionFlow { signedInProfileId ->
            profileDao.profiles(listOf(signedInProfileId))
                .mapNotNull { it.firstOrNull()?.asExternalModel() }
        }

    override fun profile(
        profileId: Id.Profile,
    ): Flow<Profile> =
        profileDao.profiles(listOf(profileId))
            .distinctUntilChanged()
            .mapNotNull { it.firstOrNull()?.asExternalModel() }
            .withRefresh {
                refreshProfile(
                    profileId = profileId,
                    profileDao = profileDao,
                    networkService = networkService,
                    multipleEntitySaverProvider = multipleEntitySaverProvider,
                    savedStateDataSource = savedStateDataSource,
                )
            }
            .distinctUntilChanged()

    override fun profileRelationships(
        profileIds: Set<Id.Profile>,
    ): Flow<List<ProfileViewerState>> =
        savedStateDataSource.singleAuthorizedSessionFlow {
            profileDao.viewerState(
                profileId = it.id,
                otherProfileIds = profileIds,
            )
                .distinctUntilChanged()
                .map { viewerEntities ->
                    viewerEntities.map(ProfileViewerStateEntity::asExternalModel)
                }
        }

    override fun commonFollowers(
        otherProfileId: Id.Profile,
        limit: Long,
    ): Flow<List<Profile>> =
        savedStateDataSource.singleAuthorizedSessionFlow { signedInProfileId ->
            val otherProfileResolvedId = lookupProfileDid(
                profileId = otherProfileId,
                profileDao = profileDao,
                networkService = networkService,
            )?.did ?: return@singleAuthorizedSessionFlow emptyFlow()

            profileDao.commonFollowers(
                profileId = signedInProfileId.id,
                otherProfileId = otherProfileResolvedId,
                limit = limit,
            )
                .distinctUntilChanged()
                .map { profileEntities ->
                    profileEntities.map(PopulatedProfileEntity::asExternalModel)
                }
        }

    override fun listMembers(
        query: ListMemberQuery,
        cursor: Cursor,
    ): Flow<CursorList<ListMember>> =
        savedStateDataSource.singleAuthorizedSessionFlow { signedInProfileId ->
            combine(
                listDao.listMembers(
                    listUri = query.listUri.uri,
                    signedInUserId = signedInProfileId.id,
                    offset = query.data.offset,
                    limit = query.data.limit,
                )
                    .map {
                        it.map(PopulatedListMemberEntity::asExternalModel)
                    },
                networkService.nextCursorFlow(
                    currentCursor = cursor,
                    currentRequestWithNextCursor = {
                        getList(
                            GetListQueryParams(
                                list = query.listUri.uri.let(::AtUri),
                                limit = query.data.limit,
                                cursor = cursor.value,
                            ),
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
                ::CursorList,
            )
                .distinctUntilChanged()
        }

    override fun followers(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<ProfileWithViewerState>> =
        savedStateDataSource.singleSessionFlow { signedInProfileId ->
            val profileDid = lookupProfileDid(
                profileId = query.profileId,
                profileDao = profileDao,
                networkService = networkService,
            ) ?: return@singleSessionFlow emptyFlow()

            profileLookup.profilesWithViewerState(
                signedInProfileId = signedInProfileId,
                responseFetcher = {
                    getFollowers(
                        GetFollowersQueryParams(
                            actor = profileDid,
                            limit = query.data.limit,
                            cursor = when (cursor) {
                                Cursor.Initial -> cursor.value
                                is Cursor.Next -> cursor.value
                                Cursor.Pending -> null
                            },
                        ),
                    )
                },
                responseProfileViews = GetFollowersResponse::followers,
                responseCursor = GetFollowersResponse::cursor,
            )
        }

    override fun following(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<ProfileWithViewerState>> =
        savedStateDataSource.singleSessionFlow { signedInProfileId ->
            val profileDid = lookupProfileDid(
                profileId = query.profileId,
                profileDao = profileDao,
                networkService = networkService,
            ) ?: return@singleSessionFlow emptyFlow()

            profileLookup.profilesWithViewerState(
                signedInProfileId = signedInProfileId,
                responseFetcher = {
                    getFollows(
                        GetFollowsQueryParams(
                            actor = profileDid,
                            limit = query.data.limit,
                            cursor = when (cursor) {
                                Cursor.Initial -> cursor.value
                                is Cursor.Next -> cursor.value
                                Cursor.Pending -> null
                            },
                        ),
                    )
                },
                responseProfileViews = GetFollowsResponse::follows,
                responseCursor = GetFollowsResponse::cursor,
            )
        }

    override fun starterPacks(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<StarterPack>> = flow {
        val profileDid = lookupProfileDid(
            profileId = query.profileId,
            profileDao = profileDao,
            networkService = networkService,
        ) ?: return@flow

        emitAll(
            combine(
                starterPackDao.profileStarterPacks(
                    creatorId = profileDid.did,
                    offset = query.data.offset,
                    limit = query.data.limit,
                )
                    .map { populatedStarterPackEntities ->
                        populatedStarterPackEntities.map(PopulatedStarterPackEntity::asExternalModel)
                    },
                networkService.nextCursorFlow(
                    currentCursor = cursor,
                    currentRequestWithNextCursor = {
                        getActorStarterPacks(
                            params = GetActorStarterPacksQueryParams(
                                actor = profileDid,
                                limit = query.data.limit,
                                cursor = cursor.value,
                            ),
                        )
                    },
                    nextCursor = GetActorStarterPacksResponse::cursor,
                    onResponse = {
                        multipleEntitySaverProvider.saveInTransaction {
                            starterPacks.forEach(::add)
                        }
                    },
                ),
                ::CursorList,
            )
                .distinctUntilChanged(),
        )
    }

    override fun lists(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<FeedList>> = flow {
        val profileDid = lookupProfileDid(
            profileId = query.profileId,
            profileDao = profileDao,
            networkService = networkService,
        ) ?: return@flow

        emitAll(
            combine(
                listDao.profileLists(
                    creatorId = profileDid.did,
                    offset = query.data.offset,
                    limit = query.data.limit,
                )
                    .map { populatedListEntities ->
                        populatedListEntities.map(PopulatedListEntity::asExternalModel)
                    },
                networkService.nextCursorFlow(
                    currentCursor = cursor,
                    currentRequestWithNextCursor = {
                        getLists(
                            params = GetListsQueryParams(
                                actor = profileDid,
                                limit = query.data.limit,
                                cursor = cursor.value,
                            ),
                        )
                    },
                    nextCursor = GetListsResponse::cursor,
                    onResponse = {
                        multipleEntitySaverProvider.saveInTransaction {
                            lists.forEach(::add)
                        }
                    },
                ),
                ::CursorList,
            )
                .distinctUntilChanged(),
        )
    }

    override fun feedGenerators(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<FeedGenerator>> = flow {
        val profileDid = lookupProfileDid(
            profileId = query.profileId,
            profileDao = profileDao,
            networkService = networkService,
        ) ?: return@flow

        emitAll(
            combine(
                feedGeneratorDao.profileFeedGenerators(
                    creatorId = profileDid.did,
                    offset = query.data.offset,
                    limit = query.data.limit,
                )
                    .map { populatedFeedGeneratorEntities ->
                        populatedFeedGeneratorEntities.map(PopulatedFeedGeneratorEntity::asExternalModel)
                    },
                networkService.nextCursorFlow(
                    currentCursor = cursor,
                    currentRequestWithNextCursor = {
                        getActorFeeds(
                            params = GetActorFeedsQueryParams(
                                actor = profileDid,
                                limit = query.data.limit,
                                cursor = cursor.value,
                            ),
                        )
                    },
                    nextCursor = GetActorFeedsResponse::cursor,
                    onResponse = {
                        multipleEntitySaverProvider.saveInTransaction {
                            feeds.forEach(::add)
                        }
                    },
                ),
                ::CursorList,
            )
                .distinctUntilChanged(),
        )
    }

    override suspend fun sendConnection(
        connection: Profile.Connection,
    ): Outcome = when (connection) {
        is Profile.Connection.Follow -> networkService.runCatchingWithMonitoredNetworkRetry {
            createRecord(
                CreateRecordRequest(
                    repo = connection.signedInProfileId.id.let(::Did),
                    collection = Nsid(FollowUri.NAMESPACE),
                    record = BskyFollow(
                        subject = connection.profileId.id.let(::Did),
                        createdAt = Clock.System.now(),
                    ).asJsonContent(BskyFollow.serializer()),
                ),
            )
        }
            .toOutcome {
                if (it.validationStatus !is CreateRecordValidationStatus.Valid) {
                    throw RecordCreationException(
                        profileId = connection.signedInProfileId,
                        collection = FollowUri.NAMESPACE,
                    )
                }
                profileDao.updatePartialProfileViewers(
                    listOf(
                        ProfileViewerStateEntity.Partial(
                            profileId = connection.signedInProfileId,
                            otherProfileId = connection.profileId,
                            following = it.uri.atUri.let(::FollowUri),
                            followedBy = connection.followedBy,
                        ),
                    ),
                )
            }

        is Profile.Connection.Unfollow -> networkService.runCatchingWithMonitoredNetworkRetry {
            deleteRecord(
                DeleteRecordRequest(
                    repo = connection.signedInProfileId.id.let(::Did),
                    collection = Nsid(FollowUri.NAMESPACE),
                    rkey = connection.followUri.recordKey.value.let(::RKey),
                ),
            )
        }
            .toOutcome {
                profileDao.updatePartialProfileViewers(
                    listOf(
                        ProfileViewerStateEntity.Partial(
                            profileId = connection.signedInProfileId,
                            otherProfileId = connection.profileId,
                            following = null,
                            followedBy = connection.followedBy,
                        ),
                    ),
                )
            }
    }

    override suspend fun updateRestriction(
        restriction: Profile.Restriction,
    ): Outcome = when (restriction) {
        is Profile.Restriction.Block.Add -> networkService.runCatchingWithMonitoredNetworkRetry {
            createRecord(
                CreateRecordRequest(
                    repo = restriction.signedInProfileId.id.let(::Did),
                    collection = Nsid(BlockUri.NAMESPACE),
                    record = BskyBlock(
                        subject = restriction.profileId.id.let(::Did),
                        createdAt = Clock.System.now(),
                    ).asJsonContent(BskyBlock.serializer()),
                ),
            )
        }.toOutcome {
            if (it.validationStatus !is CreateRecordValidationStatus.Valid) {
                throw RecordCreationException(
                    profileId = restriction.signedInProfileId,
                    collection = BlockUri.NAMESPACE,
                )
            }
            profileDao.updatePartialProfileViewer(
                ProfileViewerStateEntity.BlockPartial(
                    profileId = restriction.signedInProfileId,
                    otherProfileId = restriction.profileId,
                    blocking = it.uri.atUri.let(::BlockUri),
                ),
            )
        }
        is Profile.Restriction.Block.Remove -> networkService.runCatchingWithMonitoredNetworkRetry {
            deleteRecord(
                DeleteRecordRequest(
                    repo = restriction.signedInProfileId.id.let(::Did),
                    collection = Nsid(BlockUri.NAMESPACE),
                    rkey = restriction.blockUri.recordKey.value.let(::RKey),
                ),
            )
        }.toOutcome {
            profileDao.updatePartialProfileViewer(
                ProfileViewerStateEntity.BlockPartial(
                    profileId = restriction.signedInProfileId,
                    otherProfileId = restriction.profileId,
                    blocking = null,
                ),
            )
        }
        is Profile.Restriction.Mute -> networkService.runCatchingWithMonitoredNetworkRetry {
            when (restriction) {
                is Profile.Restriction.Mute.Add -> muteActor(
                    MuteActorRequest(restriction.profileId.id.let(::Did)),
                )
                is Profile.Restriction.Mute.Remove -> unmuteActor(
                    UnmuteActorRequest(restriction.profileId.id.let(::Did)),
                )
            }
        }.toOutcome {
            profileDao.updatePartialProfileViewer(
                ProfileViewerStateEntity.MutedPartial(
                    profileId = restriction.signedInProfileId,
                    otherProfileId = restriction.profileId,
                    muted = restriction is Profile.Restriction.Mute.Add,
                ),
            )
        }
    }

    override suspend fun updateProfile(
        update: Profile.Update,
    ): Outcome = coroutineScope {
        val (avatarBlob, bannerBlob) = @Suppress("DEPRECATION")
        when {
            update.avatarFile != null || update.bannerFile != null -> listOf(
                update.avatarFile,
                update.bannerFile,
            ).map { file ->
                async {
                    if (file == null) return@async null
                    networkService.runCatchingWithMonitoredNetworkRetry {
                        fileManager.source(file).use { source ->
                            uploadBlob(ByteReadChannel(source))
                        }
                    }
                        .onSuccess { fileManager.delete(file) }
                        .getOrNull()
                        ?.blob
                }
            }.awaitAll()
            else -> listOf(
                update.avatar,
                update.banner,
            ).map { file ->
                async {
                    if (file == null) null
                    else networkService.runCatchingWithMonitoredNetworkRetry {
                        uploadBlob(ByteReadChannel(file.data))
                    }
                        .getOrNull()
                        ?.blob
                }
            }.awaitAll()
        }

        networkService.runCatchingWithMonitoredNetworkRetry {
            getRecord(
                GetRecordQueryParams(
                    repo = update.profileId.id.let(::Did),
                    collection = Nsid(Collections.Profile),
                    rkey = Collections.SelfRecordKey,
                ),
            )
        }
            .mapCatchingUnlessCancelled {
                val existingProfile = it.value.decodeAs<BskyProfile>()

                val request = PutRecordRequest(
                    repo = update.profileId.id.let(::Did),
                    collection = Nsid(Collections.Profile),
                    rkey = Collections.SelfRecordKey,
                    record = existingProfile.copy(
                        displayName = update.displayName,
                        description = update.description,
                        avatar = avatarBlob ?: existingProfile.avatar,
                        banner = bannerBlob ?: existingProfile.banner,
                    )
                        .asJsonContent(BskyProfile.serializer()),
                )

                networkService.runCatchingWithMonitoredNetworkRetry {
                    putRecord(request)
                }.getOrThrow()

                refreshProfile(
                    profileId = update.profileId,
                    profileDao = profileDao,
                    networkService = networkService,
                    multipleEntitySaverProvider = multipleEntitySaverProvider,
                    savedStateDataSource = savedStateDataSource,
                )
            }
            .toOutcome()
    }
}

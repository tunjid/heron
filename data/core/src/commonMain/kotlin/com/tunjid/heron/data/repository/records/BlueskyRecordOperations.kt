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

package com.tunjid.heron.data.repository.records

import app.bsky.feed.Generator
import app.bsky.feed.GetActorFeedsQueryParams
import app.bsky.feed.GetActorFeedsResponse
import app.bsky.graph.GetActorStarterPacksQueryParams
import app.bsky.graph.GetActorStarterPacksResponse
import app.bsky.graph.GetBlocksQueryParams
import app.bsky.graph.GetBlocksResponse
import app.bsky.graph.GetListQueryParams
import app.bsky.graph.GetListResponse
import app.bsky.graph.GetListsQueryParams
import app.bsky.graph.GetListsResponse
import app.bsky.graph.Listitem
import com.atproto.repo.CreateRecordRequest
import com.atproto.repo.DeleteRecordRequest
import com.atproto.repo.GetRecordQueryParams
import com.atproto.repo.ListRecordsQueryParams
import com.atproto.repo.ListRecordsResponse
import com.atproto.repo.PutRecordRequest
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.DataQuery
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.Labeler
import com.tunjid.heron.data.core.models.ListMember
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.models.StarterPack
import com.tunjid.heron.data.core.models.offset
import com.tunjid.heron.data.core.models.value
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.ListMemberUri
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.data.core.types.UnauthorizedException
import com.tunjid.heron.data.core.types.profileId
import com.tunjid.heron.data.core.types.recordUriOrNull
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.core.utilities.asFailureOutcome
import com.tunjid.heron.data.database.daos.FeedGeneratorDao
import com.tunjid.heron.data.database.daos.ListDao
import com.tunjid.heron.data.database.daos.StarterPackDao
import com.tunjid.heron.data.database.entities.ListMemberEntity
import com.tunjid.heron.data.database.entities.PopulatedFeedGeneratorEntity
import com.tunjid.heron.data.database.entities.PopulatedListEntity
import com.tunjid.heron.data.database.entities.PopulatedListMemberEntity
import com.tunjid.heron.data.database.entities.PopulatedStarterPackEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.di.AppMainScope
import com.tunjid.heron.data.di.IODispatcher
import com.tunjid.heron.data.graze.GrazeDid
import com.tunjid.heron.data.graze.GrazeFeed
import com.tunjid.heron.data.network.FeedCreationService
import com.tunjid.heron.data.network.GrazeResponse
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.repository.CreatedListMembersQuery
import com.tunjid.heron.data.repository.ListMemberQuery
import com.tunjid.heron.data.repository.ProfilesQuery
import com.tunjid.heron.data.repository.SavedStateDataSource
import com.tunjid.heron.data.repository.expiredSessionOutcome
import com.tunjid.heron.data.repository.expiredSessionResult
import com.tunjid.heron.data.repository.inCurrentProfileSession
import com.tunjid.heron.data.repository.singleAuthorizedSessionFlow
import com.tunjid.heron.data.repository.singleSessionFlow
import com.tunjid.heron.data.utilities.asJsonContent
import com.tunjid.heron.data.utilities.cursorQueryRefreshTracker.CursorQueryRefreshTracker
import com.tunjid.heron.data.utilities.cursorQueryRefreshTracker.feedGeneratorsIdentity
import com.tunjid.heron.data.utilities.cursorQueryRefreshTracker.listMembersIdentity
import com.tunjid.heron.data.utilities.cursorQueryRefreshTracker.listsIdentity
import com.tunjid.heron.data.utilities.cursorQueryRefreshTracker.starterPacksIdentity
import com.tunjid.heron.data.utilities.distinctUntilChangedMap
import com.tunjid.heron.data.utilities.mapCatchingUnlessCancelled
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.nextCursorFlow
import com.tunjid.heron.data.utilities.profileLookup.ProfileLookup
import com.tunjid.heron.data.utilities.recordResolver.RecordResolver
import com.tunjid.heron.data.utilities.toOutcome
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Nsid
import sh.christian.ozone.api.RKey

interface BlueskyRecordOperations {

    val subscribedLabelers: Flow<List<Labeler>>

    val recentLists: Flow<List<FeedList>>

    fun blocks(
        query: DataQuery,
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

    fun listMembers(
        query: ListMemberQuery,
        cursor: Cursor,
    ): Flow<CursorList<ListMember>>

    fun createdListMembers(
        query: CreatedListMembersQuery,
        cursor: Cursor,
    ): Flow<CursorList<ListMember>>
    fun feedGenerators(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<FeedGenerator>>

    suspend fun updateGrazeFeed(
        update: GrazeFeed.Update,
    ): Result<GrazeFeed>

    suspend fun addListMember(
        create: ListMember.Create,
    ): Outcome
}

internal class OfflineFirstBlueskyRecordOperations @Inject constructor(
    @AppMainScope
    appMainScope: CoroutineScope,
    recordResolver: RecordResolver,
    @param:IODispatcher
    private val ioDispatcher: CoroutineDispatcher,
    private val listDao: ListDao,
    private val starterPackDao: StarterPackDao,
    private val feedGeneratorDao: FeedGeneratorDao,
    private val savedStateDataSource: SavedStateDataSource,
    private val feedCreationService: FeedCreationService,
    private val profileLookup: ProfileLookup,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
    private val networkService: NetworkService,
    private val cursorQueryRefreshTracker: CursorQueryRefreshTracker,
) : BlueskyRecordOperations {

    override val subscribedLabelers: Flow<List<Labeler>> =
        recordResolver.subscribedLabelers

    override val recentLists: Flow<List<FeedList>> =
        savedStateDataSource.singleAuthorizedSessionFlow { profileId ->
            listDao.profileLists(
                creatorId = profileId.id,
                limit = 30,
                offset = 0,
            ).distinctUntilChangedMap { it.map(PopulatedListEntity::asExternalModel) }
        }
            .flowOn(ioDispatcher)
            .stateIn(
                scope = appMainScope,
                started = SharingStarted.WhileSubscribed(1_000),
                initialValue = emptyList(),
            )

    override fun blocks(
        query: DataQuery,
        cursor: Cursor,
    ): Flow<CursorList<ProfileWithViewerState>> =
        savedStateDataSource.singleAuthorizedSessionFlow { signedInProfileId ->
            profileLookup.profilesWithViewerState(
                signedInProfileId = signedInProfileId,
                cursor = cursor,
                responseFetcher = {
                    getBlocks(
                        GetBlocksQueryParams(
                            limit = query.data.limit,
                            cursor = cursor.value,
                        ),
                    )
                },
                responseProfileViews = GetBlocksResponse::blocks,
                responseCursor = GetBlocksResponse::cursor,
            )
        }
            .flowOn(ioDispatcher)

    override fun starterPacks(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<StarterPack>> =
        savedStateDataSource.singleSessionFlow {
            val profileDid = profileLookup.lookupProfileDid(
                profileId = query.profileId,
            ) ?: return@singleSessionFlow emptyFlow()

            combine(
                starterPackDao.profileStarterPacks(
                    creatorId = profileDid.did,
                    offset = query.data.offset,
                    limit = query.data.limit,
                )
                    .distinctUntilChangedMap { populatedStarterPackEntities ->
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
                        val shouldRefresh = cursorQueryRefreshTracker
                            .isFirstPageForDifferentAnchor(
                                query = query,
                                identity = query::starterPacksIdentity,
                            )
                        multipleEntitySaverProvider.saveInTransaction {
                            if (shouldRefresh) {
                                starterPackDao.deleteStarterPacksForCreator(profileDid.did)
                            }
                            starterPacks.forEach(::add)
                        }
                    },
                ),
                ::CursorList,
            )
                .distinctUntilChanged()
        }
            .flowOn(ioDispatcher)

    override fun lists(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<FeedList>> =
        savedStateDataSource.singleSessionFlow {
            val profileDid = profileLookup.lookupProfileDid(
                profileId = query.profileId,
            ) ?: return@singleSessionFlow emptyFlow()

            combine(
                listDao.profileLists(
                    creatorId = profileDid.did,
                    offset = query.data.offset,
                    limit = query.data.limit,
                )
                    .distinctUntilChangedMap { populatedListEntities ->
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
                        val shouldRefresh = cursorQueryRefreshTracker
                            .isFirstPageForDifferentAnchor(
                                query = query,
                                identity = query::listsIdentity,
                            )
                        multipleEntitySaverProvider.saveInTransaction {
                            if (shouldRefresh) {
                                listDao.deleteListsForCreator(profileDid.did)
                            }
                            lists.forEach(::add)
                        }
                    },
                ),
                ::CursorList,
            )
                .distinctUntilChanged()
        }
            .flowOn(ioDispatcher)

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
                    .distinctUntilChangedMap {
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
                        val shouldRefresh = cursorQueryRefreshTracker
                            .isFirstPageForDifferentAnchor(
                                query = query,
                                identity = query::listMembersIdentity,
                            )
                        multipleEntitySaverProvider.saveInTransaction {
                            if (shouldRefresh) {
                                listDao.deleteListMembersForList(query.listUri.uri)
                            }
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
            .flowOn(ioDispatcher)

    override fun createdListMembers(
        query: CreatedListMembersQuery,
        cursor: Cursor,
    ): Flow<CursorList<ListMember>> =
        savedStateDataSource.singleAuthorizedSessionFlow { signedInProfileId ->
            combine(
                listDao.listMembersByProfile(
                    profileId = query.profileId.id,
                    signedInUserId = signedInProfileId.id,
                )
                    .distinctUntilChangedMap { entities ->
                        entities.map(PopulatedListMemberEntity::asExternalModel)
                    },
                networkService.nextCursorFlow(
                    currentCursor = cursor,
                    currentRequestWithNextCursor = {
                        listRecords(
                            ListRecordsQueryParams(
                                repo = Did(signedInProfileId.id),
                                collection = Nsid("app.bsky.graph.listitem"),
                                limit = query.data.limit,
                                cursor = cursor.value,
                            ),
                        )
                    },
                    nextCursor = ListRecordsResponse::cursor,
                    onResponse = {
                        multipleEntitySaverProvider.saveInTransaction {
                            records.forEach { record ->
                                val listItem = record.value.decodeAs<Listitem>()
                                add(
                                    listMemberUri = ListMemberUri(record.uri.atUri),
                                    listItem = listItem,
                                )
                            }
                        }
                    },
                ),
                ::CursorList,
            )
                .distinctUntilChanged()
        }
            .flowOn(ioDispatcher)

    override fun feedGenerators(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<FeedGenerator>> =
        savedStateDataSource.singleSessionFlow {
            val profileDid = profileLookup.lookupProfileDid(
                profileId = query.profileId,
            ) ?: return@singleSessionFlow emptyFlow()

            combine(
                feedGeneratorDao.profileFeedGenerators(
                    creatorId = profileDid.did,
                    offset = query.data.offset,
                    limit = query.data.limit,
                )
                    .distinctUntilChangedMap { populatedFeedGeneratorEntities ->
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
                        val shouldRefresh = cursorQueryRefreshTracker
                            .isFirstPageForDifferentAnchor(
                                query = query,
                                identity = query::feedGeneratorsIdentity,
                            )
                        multipleEntitySaverProvider.saveInTransaction {
                            if (shouldRefresh) {
                                feedGeneratorDao.deleteFeedGeneratorsForCreator(profileDid.did)
                            }
                            feeds.forEach(::add)
                        }
                    },
                ),
                ::CursorList,
            )
                .distinctUntilChanged()
        }
            .flowOn(ioDispatcher)

    override suspend fun updateGrazeFeed(
        update: GrazeFeed.Update,
    ): Result<GrazeFeed> = savedStateDataSource.inCurrentProfileSession { profileId ->
        if (profileId == null) return@inCurrentProfileSession expiredSessionResult()

        feedCreationService.updateGrazeFeed(
            update = update,
        ).mapCatchingUnlessCancelled { response ->
            val put = update as? GrazeFeed.Update.Put
            networkService.updateFeedRecord(
                response = response,
                profileId = profileId,
                editableFeed = put?.feed,
            )
            when (response) {
                is GrazeResponse.Read -> {
                    GrazeFeed.Created(
                        recordKey = update.recordKey,
                        filter = response.algorithm.manifest.filter,
                    )
                }
                is GrazeResponse.Created,
                is GrazeResponse.Edited,
                -> {
                    check(update is GrazeFeed.Update.Put)
                    GrazeFeed.Created(
                        recordKey = update.recordKey,
                        filter = update.feed.filter,
                    )
                }
                is GrazeResponse.Deleted -> {
                    GrazeFeed.Deleted(
                        recordKey = update.recordKey,
                    )
                }
            }
        }
    } ?: expiredSessionResult()

    override suspend fun addListMember(
        create: ListMember.Create,
    ): Outcome = savedStateDataSource.inCurrentProfileSession { signedInProfileId ->
        if (signedInProfileId == null) return@inCurrentProfileSession expiredSessionOutcome()

        val listOwnerId = create.listUri.profileId()

        if (listOwnerId != signedInProfileId)
            return@inCurrentProfileSession UnauthorizedException(
                signedInProfileId = signedInProfileId,
                profileId = listOwnerId,
            ).asFailureOutcome()

        val prospectiveMemberRefreshOutcome = profileLookup.refreshProfile(
            signedInProfileId = signedInProfileId,
            profileId = create.subjectId,
        )
        if (prospectiveMemberRefreshOutcome is Outcome.Failure)
            return@inCurrentProfileSession prospectiveMemberRefreshOutcome

        val createdAt = Clock.System.now()
        val recordKey = RecordKey.generate()

        networkService.runCatchingWithMonitoredNetworkRetry {
            createRecord(
                CreateRecordRequest(
                    repo = Did(listOwnerId.id),
                    collection = Nsid(ListMemberUri.NAMESPACE),
                    rkey = RKey(recordKey.value),
                    record = Listitem(
                        subject = Did(create.subjectId.id),
                        list = AtUri(create.listUri.uri),
                        createdAt = createdAt,
                    ).asJsonContent(Listitem.serializer()),
                ),
            )
        }.mapCatchingUnlessCancelled {
            val listMemberUri = requireNotNull(
                recordUriOrNull(
                    profileId = listOwnerId,
                    namespace = ListMemberUri.NAMESPACE,
                    recordKey = recordKey,
                ),
            ) as ListMemberUri
            multipleEntitySaverProvider.saveInTransaction {
                add(
                    ListMemberEntity(
                        uri = listMemberUri,
                        listUri = create.listUri,
                        subjectId = create.subjectId,
                        createdAt = createdAt,
                    ),
                )
            }
        }
            .toOutcome()
    } ?: expiredSessionOutcome()
}

private suspend fun NetworkService.updateFeedRecord(
    editableFeed: GrazeFeed.Editable?,
    response: GrazeResponse,
    profileId: ProfileId,
) {
    runCatchingWithMonitoredNetworkRetry {
        when (response) {
            is GrazeResponse.Created -> createRecord(
                CreateRecordRequest(
                    repo = Did(profileId.id),
                    collection = Nsid(FeedGeneratorUri.NAMESPACE),
                    rkey = RKey(response.rkey.value),
                    record = Generator(
                        did = Did(GrazeDid.id),
                        displayName = editableFeed?.displayName ?: "Graze Feed",
                        description = editableFeed?.description
                            ?: "A custom feed created with \uD80C\uDD63 and \uD83D\uDC2E",
                        createdAt = Clock.System.now(),
                        contentMode = response.contentMode,
                    ).asJsonContent(Generator.serializer()),
                ),
            )
            is GrazeResponse.Edited,
            is GrazeResponse.Read,
            -> {
                val currentRecordResponse = getRecord(
                    GetRecordQueryParams(
                        repo = Did(profileId.id),
                        collection = Nsid(FeedGeneratorUri.NAMESPACE),
                        rkey = RKey(response.rkey.value),
                    ),
                ).requireResponse()

                val currentRecord = currentRecordResponse
                    .value
                    .decodeAs<Generator>()

                putRecord(
                    PutRecordRequest(
                        repo = Did(profileId.id),
                        collection = Nsid(FeedGeneratorUri.NAMESPACE),
                        rkey = RKey(response.rkey.value),
                        record = currentRecord.copy(
                            displayName = editableFeed?.displayName ?: currentRecord.displayName,
                            description = editableFeed?.description ?: currentRecord.description,
                            contentMode = when (response) {
                                is GrazeResponse.Read -> response.contentMode
                                is GrazeResponse.Edited -> response.contentMode
                            },
                        ).asJsonContent(Generator.serializer()),
                        swapRecord = currentRecordResponse.cid,
                    ),
                )
            }
            is GrazeResponse.Deleted -> {
                deleteRecord(
                    DeleteRecordRequest(
                        repo = Did(profileId.id),
                        collection = Nsid(FeedGeneratorUri.NAMESPACE),
                        rkey = RKey(response.rkey.value),
                    ),
                )
            }
        }
    }
}

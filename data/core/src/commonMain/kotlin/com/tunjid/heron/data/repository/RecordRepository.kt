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

import app.bsky.feed.Generator as BskyFeed
import app.bsky.feed.GetActorFeedsQueryParams
import app.bsky.feed.GetActorFeedsResponse
import app.bsky.graph.GetActorStarterPacksQueryParams
import app.bsky.graph.GetActorStarterPacksResponse
import app.bsky.graph.GetBlocksQueryParams
import app.bsky.graph.GetBlocksResponse
import app.bsky.graph.GetListsQueryParams
import app.bsky.graph.GetListsResponse
import com.atproto.repo.CreateRecordRequest
import com.atproto.repo.DeleteRecordRequest
import com.atproto.repo.GetRecordQueryParams
import com.atproto.repo.PutRecordRequest
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.DataQuery
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.Labeler
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.StarterPack
import com.tunjid.heron.data.core.models.offset
import com.tunjid.heron.data.core.models.value
import com.tunjid.heron.data.core.types.EmbeddableRecordUri
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.LabelerUri
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.StarterPackUri
import com.tunjid.heron.data.database.daos.FeedGeneratorDao
import com.tunjid.heron.data.database.daos.LabelDao
import com.tunjid.heron.data.database.daos.ListDao
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.StarterPackDao
import com.tunjid.heron.data.database.entities.PopulatedFeedGeneratorEntity
import com.tunjid.heron.data.database.entities.PopulatedListEntity
import com.tunjid.heron.data.database.entities.PopulatedStarterPackEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.di.AppMainScope
import com.tunjid.heron.data.graze.GrazeDid
import com.tunjid.heron.data.graze.GrazeFeed
import com.tunjid.heron.data.network.FeedCreationService
import com.tunjid.heron.data.network.GrazeResponse
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.utilities.asJsonContent
import com.tunjid.heron.data.utilities.mapCatchingUnlessCancelled
import com.tunjid.heron.data.utilities.mapDistinctUntilChanged
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.nextCursorFlow
import com.tunjid.heron.data.utilities.profileLookup.ProfileLookup
import com.tunjid.heron.data.utilities.recordResolver.RecordResolver
import com.tunjid.heron.data.utilities.withRefresh
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Nsid
import sh.christian.ozone.api.RKey

interface RecordRepository {

    val subscribedLabelers: Flow<List<Labeler>>

    val recentLists: Flow<List<FeedList>>

    fun embeddableRecord(
        uri: EmbeddableRecordUri,
    ): Flow<Record.Embeddable>

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

    fun feedGenerators(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<FeedGenerator>>

    suspend fun updateGrazeFeed(
        update: GrazeFeed.Update,
    ): Result<GrazeFeed>
}

internal class OfflineRecordRepository @Inject constructor(
    @AppMainScope
    appMainScope: CoroutineScope,
    private val postDao: PostDao,
    private val listDao: ListDao,
    private val labelDao: LabelDao,
    private val starterPackDao: StarterPackDao,
    private val feedGeneratorDao: FeedGeneratorDao,
    private val savedStateDataSource: SavedStateDataSource,
    private val recordResolver: RecordResolver,
    private val feedCreationService: FeedCreationService,
    private val profileLookup: ProfileLookup,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
    private val networkService: NetworkService,
) : RecordRepository {

    override val subscribedLabelers: Flow<List<Labeler>> =
        recordResolver.subscribedLabelers

    override val recentLists: Flow<List<FeedList>> =
        savedStateDataSource.singleAuthorizedSessionFlow { profileId ->
            listDao.profileLists(
                creatorId = profileId.id,
                limit = 30,
                offset = 0,
            ).map { it.map(PopulatedListEntity::asExternalModel) }
        }
            .stateIn(
                scope = appMainScope,
                started = SharingStarted.WhileSubscribed(1_000),
                initialValue = emptyList(),
            )

    override fun embeddableRecord(uri: EmbeddableRecordUri): Flow<Record.Embeddable> =
        when (uri) {
            is FeedGeneratorUri -> feedGeneratorDao.feedGenerators(
                listOf(uri),
            )
                .mapDistinctUntilChanged { it.firstOrNull()?.asExternalModel() }

            is ListUri -> listDao.lists(
                listOf(uri),
            )
                .mapDistinctUntilChanged { it.firstOrNull()?.asExternalModel() }

            is StarterPackUri -> starterPackDao.starterPacks(
                listOf(uri),
            )
                .mapDistinctUntilChanged { it.firstOrNull()?.asExternalModel() }
            is LabelerUri -> labelDao.labelers(
                listOf(uri),
            )
                .mapDistinctUntilChanged { it.firstOrNull()?.asExternalModel() }

            is PostUri ->
                savedStateDataSource
                    .singleSessionFlow { profileId ->
                        postDao.posts(
                            viewingProfileId = profileId?.id,
                            postUris = listOf(uri),
                        )
                            .mapDistinctUntilChanged {
                                it.firstOrNull()?.asExternalModel(
                                    embeddedRecord = null,
                                )
                            }
                    }
        }
            .filterNotNull()
            .withRefresh {
                recordResolver.resolve(uri)
            }

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
                .distinctUntilChanged()
        }

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
                .distinctUntilChanged()
        }

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
                .distinctUntilChanged()
        }

    override suspend fun updateGrazeFeed(
        update: GrazeFeed.Update,
    ): Result<GrazeFeed> = savedStateDataSource.inCurrentProfileSession { profileId ->
        if (profileId == null) return@inCurrentProfileSession expiredSessionResult()

        feedCreationService.updateGrazeFeed(
            update = update,
        ).mapCatchingUnlessCancelled { response ->
            networkService.updateFeedRecord(response, profileId)
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
}

private suspend fun NetworkService.updateFeedRecord(
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
                    record = BskyFeed(
                        did = Did(GrazeDid.id),
                        displayName = "Graze Feed",
                        description = "A custom feed created with \uD80C\uDD63 and \uD83D\uDC2E",
                        createdAt = Clock.System.now(),
                        contentMode = response.contentMode,
                    ).asJsonContent(BskyFeed.serializer()),
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
                    .decodeAs<BskyFeed>()

                putRecord(
                    PutRecordRequest(
                        repo = Did(profileId.id),
                        collection = Nsid(FeedGeneratorUri.NAMESPACE),
                        rkey = RKey(response.rkey.value),
                        record = currentRecord.copy(
                            contentMode = when (response) {
                                is GrazeResponse.Read -> response.contentMode
                                is GrazeResponse.Edited -> response.contentMode
                            },
                        ).asJsonContent(BskyFeed.serializer()),
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

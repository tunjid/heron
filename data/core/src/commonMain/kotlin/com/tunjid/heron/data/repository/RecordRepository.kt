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
import com.atproto.repo.CreateRecordRequest
import com.atproto.repo.DeleteRecordRequest
import com.atproto.repo.GetRecordQueryParams
import com.atproto.repo.PutRecordRequest
import com.tunjid.heron.data.core.models.Labeler
import com.tunjid.heron.data.core.models.Record
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
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.graze.GrazeFeed
import com.tunjid.heron.data.network.FeedCreationService
import com.tunjid.heron.data.network.GrazeDid
import com.tunjid.heron.data.network.GrazeResponse
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.utilities.asJsonContent
import com.tunjid.heron.data.utilities.mapCatchingUnlessCancelled
import com.tunjid.heron.data.utilities.mapDistinctUntilChanged
import com.tunjid.heron.data.utilities.recordResolver.RecordResolver
import com.tunjid.heron.data.utilities.withRefresh
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Nsid
import sh.christian.ozone.api.RKey

interface RecordRepository {

    val subscribedLabelers: Flow<List<Labeler>>

    fun embeddableRecord(
        uri: EmbeddableRecordUri,
    ): Flow<Record.Embeddable>

    suspend fun updateGrazeFeed(
        update: GrazeFeed.Update,
    ): Result<GrazeFeed?>
}

internal class OfflineRecordRepository @Inject constructor(
    private val postDao: PostDao,
    private val listDao: ListDao,
    private val labelDao: LabelDao,
    private val starterPackDao: StarterPackDao,
    private val feedGeneratorDao: FeedGeneratorDao,
    private val savedStateDataSource: SavedStateDataSource,
    private val recordResolver: RecordResolver,
    private val feedCreationService: FeedCreationService,
    private val networkService: NetworkService,
) : RecordRepository {

    override val subscribedLabelers: Flow<List<Labeler>> =
        recordResolver.subscribedLabelers

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

    override suspend fun updateGrazeFeed(
        update: GrazeFeed.Update,
    ): Result<GrazeFeed?> = savedStateDataSource.inCurrentProfileSession { profileId ->
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
                    null
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
                        did = GrazeDid,
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

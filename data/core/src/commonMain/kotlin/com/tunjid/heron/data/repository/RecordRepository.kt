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

import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.types.EmbeddableRecordUri
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.LabelerUri
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.core.types.StandardDocumentUri
import com.tunjid.heron.data.core.types.StandardPublicationUri
import com.tunjid.heron.data.core.types.StarterPackUri
import com.tunjid.heron.data.core.types.UnauthorizedException
import com.tunjid.heron.data.core.types.profileId
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.core.utilities.asFailureOutcome
import com.tunjid.heron.data.database.daos.FeedGeneratorDao
import com.tunjid.heron.data.database.daos.LabelDao
import com.tunjid.heron.data.database.daos.ListDao
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.StandardSiteDao
import com.tunjid.heron.data.database.daos.StarterPackDao
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.di.IODispatcher
import com.tunjid.heron.data.repository.records.BlueskyRecordOperations
import com.tunjid.heron.data.repository.records.RockskyRecordOperations
import com.tunjid.heron.data.repository.records.StandardSiteRecordOperations
import com.tunjid.heron.data.utilities.distinctUntilChangedMap
import com.tunjid.heron.data.utilities.recordResolver.RecordResolver
import com.tunjid.heron.data.utilities.withRefresh
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn

interface RecordRepository :
    BlueskyRecordOperations,
    RockskyRecordOperations,
    StandardSiteRecordOperations {

    fun embeddableRecord(
        uri: EmbeddableRecordUri,
    ): Flow<Record.Embeddable>

    fun embeddableRecords(
        uris: Set<EmbeddableRecordUri>,
    ): Flow<List<Record.Embeddable>>

    suspend fun deleteRecord(
        uri: RecordUri,
    ): Outcome
}

@Inject
internal class OfflineFirstRecordRepository(
    blueskyRecordOperations: BlueskyRecordOperations,
    rockSkyRecordOperations: RockskyRecordOperations,
    standardSiteRecordOperations: StandardSiteRecordOperations,
    @param:IODispatcher
    private val ioDispatcher: CoroutineDispatcher,
    private val postDao: PostDao,
    private val listDao: ListDao,
    private val labelDao: LabelDao,
    private val starterPackDao: StarterPackDao,
    private val feedGeneratorDao: FeedGeneratorDao,
    private val standardSiteDao: StandardSiteDao,
    private val savedStateDataSource: SavedStateDataSource,
    private val recordResolver: RecordResolver,
) : RecordRepository,
    BlueskyRecordOperations by blueskyRecordOperations,
    RockskyRecordOperations by rockSkyRecordOperations,
    StandardSiteRecordOperations by standardSiteRecordOperations {

    override fun embeddableRecord(uri: EmbeddableRecordUri): Flow<Record.Embeddable> =
        when (uri) {
            is FeedGeneratorUri -> feedGeneratorDao.feedGenerators(
                listOf(uri),
            )
                .distinctUntilChangedMap { it.firstOrNull()?.asExternalModel() }

            is ListUri -> listDao.lists(
                listOf(uri),
            )
                .distinctUntilChangedMap { it.firstOrNull()?.asExternalModel() }

            is StarterPackUri -> starterPackDao.starterPacks(
                listOf(uri),
            )
                .distinctUntilChangedMap { it.firstOrNull()?.asExternalModel() }
            is LabelerUri -> labelDao.labelers(
                listOf(uri),
            )
                .distinctUntilChangedMap { it.firstOrNull()?.asExternalModel() }

            is PostUri ->
                savedStateDataSource
                    .singleSessionFlow { profileId ->
                        postDao.posts(
                            viewingProfileId = profileId?.id,
                            postUris = listOf(uri),
                        )
                            .distinctUntilChangedMap {
                                it.firstOrNull()?.asExternalModel(
                                    embeddedRecords = emptyList(),
                                )
                            }
                    }

            is StandardDocumentUri ->
                savedStateDataSource
                    .singleSessionFlow { profileId ->
                        standardSiteDao.documents(
                            viewingProfileId = profileId?.id,
                            uris = listOf(uri),
                        )
                            .distinctUntilChangedMap { it.firstOrNull()?.asExternalModel() }
                    }

            is StandardPublicationUri ->
                savedStateDataSource
                    .singleSessionFlow { profileId ->
                        standardSiteDao.publications(
                            viewingProfileId = profileId?.id,
                            uris = listOf(uri),
                        )
                            .distinctUntilChangedMap { it.firstOrNull()?.asExternalModel() }
                    }
        }
            .filterNotNull()
            .withRefresh {
                recordResolver.resolve(uri)
            }
            .flowOn(ioDispatcher)

    override fun embeddableRecords(
        uris: Set<EmbeddableRecordUri>,
    ): Flow<List<Record.Embeddable>> =
        if (uris.isEmpty()) flowOf(emptyList())
        else savedStateDataSource.singleSessionFlow {
            recordResolver.embeddableRecords(
                uris = uris,
                viewingProfileId = it,
            )
        }

    override suspend fun deleteRecord(
        uri: RecordUri,
    ): Outcome = savedStateDataSource.inCurrentProfileSession { signedInProfileId ->
        if (signedInProfileId == null) return@inCurrentProfileSession expiredSessionOutcome()

        val recordOwnerId = uri.profileId()
        if (recordOwnerId != signedInProfileId) return@inCurrentProfileSession UnauthorizedException(
            signedInProfileId = signedInProfileId,
            profileId = recordOwnerId,
        ).asFailureOutcome()

        recordResolver.deleteRecord(uri)
    } ?: expiredSessionOutcome()
}

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

import com.tunjid.heron.data.core.models.LinkPreview
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.RecordPostReference
import com.tunjid.heron.data.core.types.EmbeddableRecordUri
import com.tunjid.heron.data.core.types.EventId
import com.tunjid.heron.data.core.types.EventUri
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.LabelerUri
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.core.types.StandardDocumentId
import com.tunjid.heron.data.core.types.StandardDocumentUri
import com.tunjid.heron.data.core.types.StandardPublicationUri
import com.tunjid.heron.data.core.types.StarterPackUri
import com.tunjid.heron.data.core.types.UnauthorizedException
import com.tunjid.heron.data.core.types.profileId
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.core.utilities.asFailureOutcome
import com.tunjid.heron.data.database.daos.EventDao
import com.tunjid.heron.data.database.daos.FeedGeneratorDao
import com.tunjid.heron.data.database.daos.LabelDao
import com.tunjid.heron.data.database.daos.ListDao
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.StandardSiteDao
import com.tunjid.heron.data.database.daos.StarterPackDao
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.di.IODispatcher
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.repository.records.BlueskyRecordOperations
import com.tunjid.heron.data.repository.records.EventRecordOperations
import com.tunjid.heron.data.repository.records.RockskyRecordOperations
import com.tunjid.heron.data.repository.records.StandardSiteRecordOperations
import com.tunjid.heron.data.repository.records.editRecord
import com.tunjid.heron.data.repository.records.withBskyPostRef
import com.tunjid.heron.data.utilities.distinctUntilChangedMap
import com.tunjid.heron.data.utilities.mapCatchingUnlessCancelled
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.profileLookup.ProfileLookup
import com.tunjid.heron.data.utilities.recordResolver.RecordResolver
import com.tunjid.heron.data.utilities.toOutcome
import com.tunjid.heron.data.utilities.withRefresh
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

interface RecordRepository :
    BlueskyRecordOperations,
    EventRecordOperations,
    RockskyRecordOperations,
    StandardSiteRecordOperations {

    fun embeddableRecord(
        uri: EmbeddableRecordUri,
    ): Flow<Record.Embeddable>

    fun embeddableRecords(
        uris: Set<EmbeddableRecordUri>,
    ): Flow<List<Record.Embeddable>>

    suspend fun externalLinkPreview(
        url: GenericUri,
    ): LinkPreview?

    suspend fun deleteRecord(
        uri: RecordUri,
    ): Outcome

    suspend fun updatePostReference(
        reference: RecordPostReference,
    ): Outcome
}

@Inject
internal class OfflineFirstRecordRepository(
    blueskyRecordOperations: BlueskyRecordOperations,
    eventRecordOperations: EventRecordOperations,
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
    private val eventDao: EventDao,
    private val savedStateDataSource: SavedStateDataSource,
    private val recordResolver: RecordResolver,
    private val profileLookup: ProfileLookup,
    private val networkService: NetworkService,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
) : RecordRepository,
    BlueskyRecordOperations by blueskyRecordOperations,
    EventRecordOperations by eventRecordOperations,
    RockskyRecordOperations by rockSkyRecordOperations,
    StandardSiteRecordOperations by standardSiteRecordOperations {

    override fun embeddableRecord(
        uri: EmbeddableRecordUri,
    ): Flow<Record.Embeddable> = flow {
        // Resolve the URI's authority to a DID first; handle based URIs (eg. parsed from
        // bsky.app links) do not match the DID keyed records persisted in the daos.
        val resolvedUri = profileLookup.withDidAuthority(uri)
        emitAll(
            when (resolvedUri) {
                is FeedGeneratorUri -> feedGeneratorDao.feedGenerators(
                    listOf(resolvedUri),
                )
                    .distinctUntilChangedMap { it.firstOrNull()?.asExternalModel() }

                is ListUri -> listDao.lists(
                    listOf(resolvedUri),
                )
                    .distinctUntilChangedMap { it.firstOrNull()?.asExternalModel() }

                is StarterPackUri -> starterPackDao.starterPacks(
                    listOf(resolvedUri),
                )
                    .distinctUntilChangedMap { it.firstOrNull()?.asExternalModel() }
                is LabelerUri -> labelDao.labelers(
                    listOf(resolvedUri),
                )
                    .distinctUntilChangedMap { it.firstOrNull()?.asExternalModel() }

                is PostUri ->
                    savedStateDataSource
                        .singleSessionFlow { profileId ->
                            postDao.posts(
                                viewingProfileId = profileId?.id,
                                postUris = listOf(resolvedUri),
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
                                uris = listOf(resolvedUri),
                            )
                                .distinctUntilChangedMap { it.firstOrNull()?.asExternalModel() }
                        }

                is StandardPublicationUri ->
                    savedStateDataSource
                        .singleSessionFlow { profileId ->
                            standardSiteDao.publications(
                                viewingProfileId = profileId?.id,
                                uris = listOf(resolvedUri),
                            )
                                .distinctUntilChangedMap { it.firstOrNull()?.asExternalModel() }
                        }

                is EventUri ->
                    savedStateDataSource
                        .singleSessionFlow { profileId ->
                            eventDao.events(
                                viewingProfileId = profileId?.id,
                                uris = listOf(resolvedUri),
                            )
                                .distinctUntilChangedMap { it.firstOrNull()?.asExternalModel() }
                        }
            }
                .filterNotNull()
                .withRefresh {
                    recordResolver.resolve(resolvedUri)
                },
        )
    }
        .flowOn(ioDispatcher)

    override fun embeddableRecords(
        uris: Set<EmbeddableRecordUri>,
    ): Flow<List<Record.Embeddable>> =
        if (uris.isEmpty()) flowOf(emptyList())
        else savedStateDataSource.singleSessionFlow { viewingProfileId ->
            // Resolve each URI's authority to a DID first; handle based URIs (eg. parsed from
            // bsky.app links) do not match the DID keyed records persisted in the daos.
            val resolvedUris = coroutineScope {
                uris.map { uri ->
                    async { profileLookup.withDidAuthority(uri) }
                }.awaitAll().toSet()
            }
            recordResolver.embeddableRecords(
                uris = resolvedUris,
                viewingProfileId = viewingProfileId,
            )
        }

    override suspend fun externalLinkPreview(
        url: GenericUri,
    ): LinkPreview? = withContext(ioDispatcher) {
        recordResolver.resolveExternalLink(url)
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

    override suspend fun updatePostReference(
        reference: RecordPostReference,
    ): Outcome = savedStateDataSource.inCurrentProfileSession { signedInProfileId ->
        if (signedInProfileId == null) return@inCurrentProfileSession expiredSessionOutcome()

        // The record lives in its author's repo, so only its author can rewrite it.
        val recordOwnerId = reference.recordUri.profileId()
        if (recordOwnerId != signedInProfileId) return@inCurrentProfileSession UnauthorizedException(
            signedInProfileId = signedInProfileId,
            profileId = recordOwnerId,
        ).asFailureOutcome()

        networkService.editRecord(
            uri = reference.recordUri,
            transform = {
                withBskyPostRef(reference)
            },
        ).mapCatchingUnlessCancelled { cid ->
            // Persist the change locally without relying on the AppView, which lags behind
            // the PDS write.
            val linkedPost = reference as? RecordPostReference.Link
            when (val recordUri = reference.recordUri) {
                is EventUri -> {
                    val cachedEntity = eventDao.event(
                        viewingProfileId = signedInProfileId.id,
                        uri = recordUri,
                    )
                        .first()
                        ?.entity
                    if (cachedEntity != null) multipleEntitySaverProvider.saveInTransaction {
                        add(
                            cachedEntity.copy(
                                cid = cid.cid.let(::EventId),
                                bskyPostUri = linkedPost?.postUri,
                                bskyPostCid = linkedPost?.postCid,
                                bskyPostShowComments = linkedPost?.let {
                                    cachedEntity.bskyPostShowComments ?: true
                                },
                            ),
                        )
                    }
                }
                is StandardDocumentUri -> {
                    val cachedEntity = standardSiteDao.documents(
                        viewingProfileId = signedInProfileId.id,
                        uris = setOf(recordUri),
                    )
                        .first()
                        .firstOrNull()
                        ?.entity
                    if (cachedEntity != null) multipleEntitySaverProvider.saveInTransaction {
                        add(
                            cachedEntity.copy(
                                cid = cid.cid.let(::StandardDocumentId),
                                bskyPostRefUri = linkedPost?.postUri,
                                bskyPostRefCid = linkedPost?.postCid,
                            ),
                        )
                    }
                }
            }
        }
            .toOutcome()
    } ?: expiredSessionOutcome()
}

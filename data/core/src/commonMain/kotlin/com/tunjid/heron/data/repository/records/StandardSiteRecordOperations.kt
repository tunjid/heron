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

import com.atproto.repo.CreateRecordRequest
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.StandardDocument
import com.tunjid.heron.data.core.models.StandardPublication
import com.tunjid.heron.data.core.models.StandardSubscription
import com.tunjid.heron.data.core.models.offset
import com.tunjid.heron.data.core.models.value
import com.tunjid.heron.data.core.types.StandardPublicationUri
import com.tunjid.heron.data.core.types.StandardSubscriptionId
import com.tunjid.heron.data.core.types.StandardSubscriptionUri
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.database.daos.StandardSiteDao
import com.tunjid.heron.data.database.entities.PopulatedStandardDocumentEntity
import com.tunjid.heron.data.database.entities.PopulatedStandardPublicationEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.di.IODispatcher
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.repository.ProfilesQuery
import com.tunjid.heron.data.repository.SavedStateDataSource
import com.tunjid.heron.data.repository.expiredSessionOutcome
import com.tunjid.heron.data.repository.inCurrentProfileSession
import com.tunjid.heron.data.repository.singleAuthorizedSessionFlow
import com.tunjid.heron.data.repository.singleSessionFlow
import com.tunjid.heron.data.utilities.asJsonContent
import com.tunjid.heron.data.utilities.distinctUntilChangedMap
import com.tunjid.heron.data.utilities.mapCatchingUnlessCancelled
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.nextCursorFlow
import com.tunjid.heron.data.utilities.profileLookup.ProfileLookup
import com.tunjid.heron.data.utilities.toOutcome
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.Serializable
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Nsid
import sh.christian.ozone.api.RKey
import site.standard.graph.Subscription
import site.standard.heron.GetDocumentsQueryParams
import site.standard.heron.GetDocumentsResponse
import site.standard.heron.GetSubscriptionsQueryParams
import site.standard.heron.GetSubscriptionsResponse

@Serializable
data class StandardPublicationDocumentsQuery(
    val publicationUri: StandardPublicationUri,
    override val data: CursorQuery.Data,
) : CursorQuery

interface StandardSiteRecordOperations {

    fun authorDocuments(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<StandardDocument>>

    fun publicationDocuments(
        query: StandardPublicationDocumentsQuery,
        cursor: Cursor,
    ): Flow<CursorList<StandardDocument>>

    fun subscribedPublications(
        query: CursorQuery,
        cursor: Cursor,
    ): Flow<CursorList<StandardPublication>>

    suspend fun createSubscription(
        create: StandardSubscription.Create,
    ): Outcome
}

internal class OfflineFirstStandardSiteRecordOperations @Inject constructor(
    @param:IODispatcher
    private val ioDispatcher: CoroutineDispatcher,
    private val standardSiteDao: StandardSiteDao,
    private val savedStateDataSource: SavedStateDataSource,
    private val profileLookup: ProfileLookup,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
    private val networkService: NetworkService,
) : StandardSiteRecordOperations {

    override fun authorDocuments(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<StandardDocument>> =
        savedStateDataSource.singleSessionFlow { signedInProfileId ->
            val profileDid = profileLookup.lookupProfileDid(
                profileId = query.profileId,
            ) ?: return@singleSessionFlow emptyFlow()

            combine(
                standardSiteDao.authorDocuments(
                    viewingProfileId = signedInProfileId?.id,
                    authorId = profileDid.did,
                    offset = query.data.offset,
                    limit = query.data.limit,
                )
                    .distinctUntilChangedMap { populatedStandardDocumentEntities ->
                        populatedStandardDocumentEntities.map(PopulatedStandardDocumentEntity::asExternalModel)
                    },
                networkService.nextCursorFlow(
                    currentCursor = cursor,
                    currentRequestWithNextCursor = {
                        getDocuments(
                            params = GetDocumentsQueryParams(
                                author = profileDid,
                                limit = query.data.limit,
                                cursor = cursor.value,
                            ),
                        )
                    },
                    nextCursor = GetDocumentsResponse::cursor,
                    onResponse = {
                        multipleEntitySaverProvider.saveInTransaction {
                            documents.forEach {
                                add(
                                    documentView = it,
                                    viewingProfileId = signedInProfileId,
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

    override fun publicationDocuments(
        query: StandardPublicationDocumentsQuery,
        cursor: Cursor,
    ): Flow<CursorList<StandardDocument>> =
        savedStateDataSource.singleSessionFlow { signedInProfileId ->
            combine(
                standardSiteDao.publicationDocuments(
                    viewingProfileId = signedInProfileId?.id,
                    publicationUri = query.publicationUri.uri,
                    offset = query.data.offset,
                    limit = query.data.limit,
                )
                    .distinctUntilChangedMap { populatedStandardDocumentEntities ->
                        populatedStandardDocumentEntities.map(PopulatedStandardDocumentEntity::asExternalModel)
                    },
                networkService.nextCursorFlow(
                    currentCursor = cursor,
                    currentRequestWithNextCursor = {
                        getDocuments(
                            params = GetDocumentsQueryParams(
                                publication = AtUri(query.publicationUri.uri),
                                limit = query.data.limit,
                                cursor = cursor.value,
                            ),
                        )
                    },
                    nextCursor = GetDocumentsResponse::cursor,
                    onResponse = {
                        multipleEntitySaverProvider.saveInTransaction {
                            documents.forEach {
                                add(
                                    documentView = it,
                                    viewingProfileId = signedInProfileId,
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

    override fun subscribedPublications(
        query: CursorQuery,
        cursor: Cursor,
    ): Flow<CursorList<StandardPublication>> =
        savedStateDataSource.singleAuthorizedSessionFlow { signedInProfileId ->
            combine(
                standardSiteDao.subscribedPublications(
                    viewingProfileId = signedInProfileId.id,
                    limit = query.data.limit,
                    offset = query.data.offset,
                )
                    .distinctUntilChangedMap { populatedStandardPublicationEntities ->
                        populatedStandardPublicationEntities.map(
                            PopulatedStandardPublicationEntity::asExternalModel,
                        )
                    },
                networkService.nextCursorFlow(
                    currentCursor = cursor,
                    currentRequestWithNextCursor = {
                        getSubscriptions(
                            params = GetSubscriptionsQueryParams(
                                limit = query.data.limit,
                                cursor = cursor.value,
                            ),
                        )
                    },
                    nextCursor = GetSubscriptionsResponse::cursor,
                    onResponse = {
                        multipleEntitySaverProvider.saveInTransaction {
                            publications.forEach {
                                add(
                                    publicationView = it,
                                    viewingProfileId = signedInProfileId,
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

    override suspend fun createSubscription(
        create: StandardSubscription.Create,
    ): Outcome = savedStateDataSource.inCurrentProfileSession { signedInProfileId ->
        if (signedInProfileId == null) return@inCurrentProfileSession expiredSessionOutcome()

        val subscription = Subscription(
            publication = AtUri(create.publicationUri.uri),
        )
        networkService.runCatchingWithMonitoredNetworkRetry {
            createRecord(
                CreateRecordRequest(
                    repo = Did(signedInProfileId.id),
                    collection = Nsid(StandardSubscriptionUri.NAMESPACE),
                    rkey = RKey(create.recordKey.value),
                    record = subscription
                        .asJsonContent(Subscription.serializer()),
                ),
            )
        }.mapCatchingUnlessCancelled { response ->
            multipleEntitySaverProvider.saveInTransaction {
                add(
                    subscriptionUri = response.uri.atUri.let(::StandardSubscriptionUri),
                    subscriptionCid = response.cid.cid.let(::StandardSubscriptionId),
                    subscription = subscription,
                    viewingProfileId = signedInProfileId,
                    sortedAt = create.sortedAt,
                )
            }
        }
            .toOutcome()
    } ?: expiredSessionOutcome()
}

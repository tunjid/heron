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

import com.atproto.repo.PutRecordRequest
import com.atproto.repo.StrongRef
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Event
import com.tunjid.heron.data.core.models.Rsvp
import com.tunjid.heron.data.core.models.offset
import com.tunjid.heron.data.core.models.value
import com.tunjid.heron.data.core.types.EventUri
import com.tunjid.heron.data.core.types.RsvpId
import com.tunjid.heron.data.core.types.RsvpUri
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.database.daos.EventDao
import com.tunjid.heron.data.database.entities.PopulatedEventEntity
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
import com.tunjid.heron.data.utilities.cursorQueryRefreshTracker.CursorQueryRefreshTracker
import com.tunjid.heron.data.utilities.cursorQueryRefreshTracker.authorEventsIdentity
import com.tunjid.heron.data.utilities.cursorQueryRefreshTracker.rsvpedEventsIdentity
import com.tunjid.heron.data.utilities.distinctUntilChangedMap
import com.tunjid.heron.data.utilities.mapCatchingUnlessCancelled
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.nextCursorFlow
import com.tunjid.heron.data.utilities.profileLookup.ProfileLookup
import com.tunjid.heron.data.utilities.recordResolver.RecordResolver
import com.tunjid.heron.data.utilities.toOutcome
import com.tunjid.heron.data.utilities.withRefresh
import community.lexicon.calendar.Rsvp as LexiconRsvp
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Cid
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Nsid
import sh.christian.ozone.api.RKey
import social.heron.calendar.GetEventsQueryParams
import social.heron.calendar.GetEventsResponse
import social.heron.calendar.GetRsvpedEventsQueryParams
import social.heron.calendar.GetRsvpedEventsResponse

@Serializable
data class RsvpedEventsQuery(
    val status: Rsvp.Status?,
    override val data: CursorQuery.Data,
) : CursorQuery

interface EventRecordOperations {

    fun event(
        uri: EventUri,
    ): Flow<Event>

    fun authorEvents(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<Event>>

    fun rsvpedEvents(
        query: RsvpedEventsQuery,
        cursor: Cursor,
    ): Flow<CursorList<Event>>

    suspend fun putRsvp(
        put: Rsvp.Put,
    ): Outcome
}

@Inject
internal class OfflineFirstEventRecordOperations(
    @param:IODispatcher
    private val ioDispatcher: CoroutineDispatcher,
    private val eventDao: EventDao,
    private val savedStateDataSource: SavedStateDataSource,
    private val profileLookup: ProfileLookup,
    private val multipleEntitySaverProvider: MultipleEntitySaverProvider,
    private val networkService: NetworkService,
    private val recordResolver: RecordResolver,
    private val cursorQueryRefreshTracker: CursorQueryRefreshTracker,
) : EventRecordOperations {

    override fun event(
        uri: EventUri,
    ): Flow<Event> =
        savedStateDataSource.singleSessionFlow { signedInProfileId ->
            eventDao.event(
                viewingProfileId = signedInProfileId?.id,
                uri = uri,
            )
                .filterNotNull()
                .map(PopulatedEventEntity::asExternalModel)
                .withRefresh {
                    recordResolver.resolve(uri)
                }
        }
            .flowOn(ioDispatcher)

    override fun authorEvents(
        query: ProfilesQuery,
        cursor: Cursor,
    ): Flow<CursorList<Event>> =
        savedStateDataSource.singleSessionFlow { signedInProfileId ->
            val profileDid = profileLookup.lookupProfileDid(
                profileId = query.profileId,
            ) ?: return@singleSessionFlow emptyFlow()

            combine(
                eventDao.authorEvents(
                    viewingProfileId = signedInProfileId?.id,
                    authorId = profileDid.did,
                    limit = query.data.limit,
                    offset = query.data.offset,
                )
                    .distinctUntilChangedMap { populatedEventEntities ->
                        populatedEventEntities.map(PopulatedEventEntity::asExternalModel)
                    },
                networkService.nextCursorFlow(
                    currentCursor = cursor,
                    currentRequestWithNextCursor = {
                        getEvents(
                            params = GetEventsQueryParams(
                                author = profileDid,
                                limit = query.data.limit,
                                cursor = cursor.value,
                            ),
                        )
                    },
                    nextCursor = GetEventsResponse::cursor,
                    onResponse = {
                        val shouldRefresh = cursorQueryRefreshTracker
                            .isFirstPageForDifferentAnchor(
                                query = query,
                                identity = query::authorEventsIdentity,
                            )
                        multipleEntitySaverProvider.saveInTransaction {
                            if (shouldRefresh) {
                                eventDao.deleteEventsForAuthor(profileDid.did)
                            }
                            events.forEach {
                                add(
                                    eventView = it,
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

    override fun rsvpedEvents(
        query: RsvpedEventsQuery,
        cursor: Cursor,
    ): Flow<CursorList<Event>> =
        savedStateDataSource.singleAuthorizedSessionFlow { signedInProfileId ->
            combine(
                eventDao.rsvpedEvents(
                    viewingProfileId = signedInProfileId.id,
                    status = query.status?.id,
                    limit = query.data.limit,
                    offset = query.data.offset,
                )
                    .distinctUntilChangedMap { populatedEventEntities ->
                        populatedEventEntities.map(PopulatedEventEntity::asExternalModel)
                    },
                networkService.nextCursorFlow(
                    currentCursor = cursor,
                    currentRequestWithNextCursor = {
                        getRsvpedEvents(
                            params = GetRsvpedEventsQueryParams(
                                status = query.status?.id,
                                limit = query.data.limit,
                                cursor = cursor.value,
                            ),
                        )
                    },
                    nextCursor = GetRsvpedEventsResponse::cursor,
                    onResponse = {
                        val shouldRefresh = cursorQueryRefreshTracker
                            .isFirstPageForDifferentAnchor(
                                query = query,
                                identity = query::rsvpedEventsIdentity,
                            )
                        multipleEntitySaverProvider.saveInTransaction {
                            if (shouldRefresh) {
                                eventDao.deleteRsvpsForViewer(
                                    viewingProfileId = signedInProfileId.id,
                                    status = query.status?.id,
                                )
                            }
                            events.forEach {
                                add(
                                    eventView = it,
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
            .filterNotNull()
            .flowOn(ioDispatcher)

    override suspend fun putRsvp(
        put: Rsvp.Put,
    ): Outcome = savedStateDataSource.inCurrentProfileSession { signedInProfileId ->
        if (signedInProfileId == null) return@inCurrentProfileSession expiredSessionOutcome()

        val rsvp = LexiconRsvp(
            subject = StrongRef(
                uri = AtUri(put.eventUri.uri),
                cid = Cid(put.eventCid.id),
            ),
            status = put.status.id,
            createdAt = put.sortedAt,
        )
        // putRecord creates the record if absent and replaces it otherwise, so changing
        // a response reuses its record key instead of writing a second record.
        networkService.runCatchingWithMonitoredNetworkRetry {
            putRecord(
                PutRecordRequest(
                    repo = Did(signedInProfileId.id),
                    collection = Nsid(RsvpUri.NAMESPACE),
                    rkey = RKey(put.recordKey.value),
                    record = rsvp
                        .asJsonContent(LexiconRsvp.serializer()),
                ),
            )
        }.mapCatchingUnlessCancelled { response ->
            multipleEntitySaverProvider.saveInTransaction {
                add(
                    rsvpUri = response.uri.atUri.let(::RsvpUri),
                    rsvpCid = response.cid.cid.let(::RsvpId),
                    rsvp = rsvp,
                    sortedAt = put.sortedAt,
                    viewingProfileId = signedInProfileId,
                )
            }
        }
            .toOutcome()
    } ?: expiredSessionOutcome()
}

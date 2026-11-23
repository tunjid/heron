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
import com.tunjid.heron.data.core.models.fromBase64EncodedUrl
import com.tunjid.heron.data.core.models.offset
import com.tunjid.heron.data.core.models.toUrlEncodedBase64
import com.tunjid.heron.data.core.models.value
import com.tunjid.heron.data.core.types.EventId
import com.tunjid.heron.data.core.types.EventUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RsvpId
import com.tunjid.heron.data.core.types.RsvpUri
import com.tunjid.heron.data.core.types.UnauthorizedException
import com.tunjid.heron.data.core.types.profileId
import com.tunjid.heron.data.core.types.recordKey
import com.tunjid.heron.data.core.utilities.File
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.database.daos.EventDao
import com.tunjid.heron.data.database.entities.EventEntity
import com.tunjid.heron.data.database.entities.PopulatedEventEntity
import com.tunjid.heron.data.database.entities.PopulatedRsvpEntity
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.di.IODispatcher
import com.tunjid.heron.data.files.FileManager
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.repository.ProfilesQuery
import com.tunjid.heron.data.repository.SavedStateDataSource
import com.tunjid.heron.data.repository.expiredSessionOutcome
import com.tunjid.heron.data.repository.inCurrentProfileSession
import com.tunjid.heron.data.repository.singleAuthorizedSessionFlow
import com.tunjid.heron.data.repository.singleSessionFlow
import com.tunjid.heron.data.utilities.MediaBlob
import com.tunjid.heron.data.utilities.asJsonContent
import com.tunjid.heron.data.utilities.cursorQueryRefreshTracker.CursorQueryRefreshTracker
import com.tunjid.heron.data.utilities.cursorQueryRefreshTracker.authorEventsIdentity
import com.tunjid.heron.data.utilities.cursorQueryRefreshTracker.eventRsvpsIdentity
import com.tunjid.heron.data.utilities.cursorQueryRefreshTracker.rsvpedEventsIdentity
import com.tunjid.heron.data.utilities.distinctUntilChangedMap
import com.tunjid.heron.data.utilities.imageUri
import com.tunjid.heron.data.utilities.mapCatchingUnlessCancelled
import com.tunjid.heron.data.utilities.mediaFileUploads
import com.tunjid.heron.data.utilities.multipleEntitysaver.MultipleEntitySaverProvider
import com.tunjid.heron.data.utilities.multipleEntitysaver.add
import com.tunjid.heron.data.utilities.multipleEntitysaver.stubProfileEntity
import com.tunjid.heron.data.utilities.nextCursorFlow
import com.tunjid.heron.data.utilities.profileLookup.ProfileLookup
import com.tunjid.heron.data.utilities.recordResolver.RecordResolver
import com.tunjid.heron.data.utilities.runCatchingUnlessCancelled
import com.tunjid.heron.data.utilities.toOutcome
import com.tunjid.heron.data.utilities.uploadFileBlob
import com.tunjid.heron.data.utilities.withRefresh
import community.lexicon.calendar.Event as LexiconEvent
import community.lexicon.calendar.EventAspectRatio
import community.lexicon.calendar.EventLocationUnion
import community.lexicon.calendar.EventMedia
import community.lexicon.calendar.EventMediaRole
import community.lexicon.calendar.EventToken
import community.lexicon.calendar.EventUri as LexiconEventUri
import community.lexicon.calendar.Rsvp as LexiconRsvp
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Cid
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Nsid
import sh.christian.ozone.api.RKey
import sh.christian.ozone.api.Uri
import sh.christian.ozone.api.model.Blob
import social.heron.calendar.GetEventsQueryParams
import social.heron.calendar.GetEventsResponse
import social.heron.calendar.GetRsvpedEventsQueryParams
import social.heron.calendar.GetRsvpedEventsResponse
import social.heron.calendar.GetRsvpsQueryParams
import social.heron.calendar.GetRsvpsResponse

@Serializable
data class RsvpedEventsQuery(
    val status: Rsvp.Status?,
    override val data: CursorQuery.Data,
) : CursorQuery

@Serializable
data class EventRsvpsQuery(
    val eventUri: EventUri,
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

    fun rsvps(
        query: EventRsvpsQuery,
        cursor: Cursor,
    ): Flow<CursorList<Rsvp>>

    suspend fun putRsvp(
        put: Rsvp.Put,
    ): Outcome

    suspend fun putEvent(
        put: Event.Put,
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
    private val fileManager: FileManager,
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

    override fun rsvps(
        query: EventRsvpsQuery,
        cursor: Cursor,
    ): Flow<CursorList<Rsvp>> =
        savedStateDataSource.singleAuthorizedSessionFlow { signedInProfileId ->
            combine(
                eventDao.rsvps(
                    eventUri = query.eventUri,
                    status = query.status?.id,
                    limit = query.data.limit,
                    offset = query.data.offset,
                )
                    .distinctUntilChangedMap { populatedRsvpEntities ->
                        populatedRsvpEntities.map(PopulatedRsvpEntity::asExternalModel)
                    },
                networkService.nextCursorFlow(
                    currentCursor = cursor,
                    currentRequestWithNextCursor = {
                        getRsvps(
                            params = GetRsvpsQueryParams(
                                uri = query.eventUri.uri.let(::AtUri),
                                status = query.status?.id,
                                limit = query.data.limit,
                                cursor = cursor.value,
                            ),
                        )
                    },
                    nextCursor = GetRsvpsResponse::cursor,
                    onResponse = {
                        val shouldRefresh = cursorQueryRefreshTracker
                            .isFirstPageForDifferentAnchor(
                                query = query,
                                identity = query::eventRsvpsIdentity,
                            )
                        multipleEntitySaverProvider.saveInTransaction {
                            if (shouldRefresh) {
                                eventDao.deleteRsvpsForEvent(
                                    eventUri = query.eventUri,
                                    viewingProfileId = signedInProfileId.id,
                                    status = query.status?.id,
                                )
                            }
                            rsvps.forEach(::add)
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
                val rsvpUri = response.uri.atUri.let(::RsvpUri)
                val profileId = rsvpUri.profileId()
                add(stubProfileEntity(profileId.id.let(::Did)))
                add(
                    rsvpUri = response.uri.atUri.let(::RsvpUri),
                    rsvpCid = response.cid.cid.let(::RsvpId),
                    rsvp = rsvp,
                    sortedAt = put.sortedAt,
                    authorId = profileId,
                )

                eventDao.adjustRsvpCounts(
                    eventUri = put.eventUri.uri,
                    previousStatus = put.previousStatus?.id,
                    newStatus = put.status.id,
                    goingStatus = Rsvp.Status.Going.id,
                    interestedStatus = Rsvp.Status.Interested.id,
                    notGoingStatus = Rsvp.Status.NotGoing.id,
                )
            }
        }
            .toOutcome()
    } ?: expiredSessionOutcome()

    override suspend fun putEvent(
        put: Event.Put,
    ): Outcome = savedStateDataSource.inCurrentProfileSession { signedInProfileId ->
        if (signedInProfileId == null) return@inCurrentProfileSession expiredSessionOutcome()

        networkService.putEventRecord(
            put = put,
            signedInProfileId = signedInProfileId,
            fileManager = fileManager,
            eventDao = eventDao,
            multipleEntitySaverProvider = multipleEntitySaverProvider,
        )
            .toOutcome {
                put.thumbnail?.let { thumbnail ->
                    runCatchingUnlessCancelled { fileManager.delete(thumbnail) }
                }
            }
    } ?: expiredSessionOutcome()
}

/**
 * Writes [put] to the signed-in user's repo and saves it locally, returning the uploaded
 * thumbnail blob so a post announcing the event can reuse it. The thumbnail file is left for
 * the caller to delete once its whole write succeeds, so a retry can upload it again.
 */
internal suspend fun NetworkService.putEventRecord(
    put: Event.Put,
    signedInProfileId: ProfileId,
    fileManager: FileManager,
    eventDao: EventDao,
    multipleEntitySaverProvider: MultipleEntitySaverProvider,
): Result<Blob?> {
    val eventOwnerId = put.uri.profileId()
    if (eventOwnerId != signedInProfileId) return Result.failure(
        UnauthorizedException(
            signedInProfileId = signedInProfileId,
            profileId = eventOwnerId,
        ),
    )

    // The uploaded copy carries the thumbnail's dimensions even if the picked file lacked them.
    val uploadedThumbnail = fileManager.mediaFileUploads(
        files = listOfNotNull(put.thumbnail),
    ) { file ->
        uploadFileBlob(file = file)
    }
        .getOrElse { return Result.failure(it) }
        .filterIsInstance<MediaBlob.Image.Local>()
        .firstOrNull()
    val thumbnail = uploadedThumbnail?.file
    val thumbnailBlob = uploadedThumbnail?.blob

    return when (put) {
        is Event.Create -> {
            val record = put.asLexiconEvent(
                thumbnail = thumbnail,
                thumbnailBlob = thumbnailBlob,
                createdAt = Clock.System.now(),
            )
            // putRecord creates the record if absent and replaces it otherwise, so retrying a
            // create rewrites the same record instead of writing a second one.
            runCatchingWithMonitoredNetworkRetry {
                putRecord(
                    PutRecordRequest(
                        repo = Did(signedInProfileId.id),
                        collection = Nsid(EventUri.NAMESPACE),
                        rkey = RKey(put.uri.recordKey.value),
                        record = record.asJsonContent(LexiconEvent.serializer()),
                    ),
                )
            }.mapCatchingUnlessCancelled { response ->
                multipleEntitySaverProvider.saveInTransaction {
                    add(
                        eventUri = put.uri,
                        eventCid = response.cid.cid.let(::EventId),
                        event = record,
                    )
                }
                thumbnailBlob
            }
        }
        is Event.Edit -> editRecord(
            uri = put.uri,
            transform = {
                withEventDetails(
                    details = put.details,
                    thumbnail = thumbnail,
                    thumbnailBlob = thumbnailBlob,
                )
            },
        ).mapCatchingUnlessCancelled { cid ->
            // Persist the change locally without relying on the AppView, which lags behind
            // the PDS write.
            val cachedEntity = eventDao.event(
                viewingProfileId = signedInProfileId.id,
                uri = put.uri,
            )
                .first()
                ?.entity
            if (cachedEntity != null) multipleEntitySaverProvider.saveInTransaction {
                add(
                    cachedEntity.withDetails(
                        cid = cid.cid.let(::EventId),
                        details = put.details,
                        thumbnail = thumbnail,
                        thumbnailBlob = thumbnailBlob,
                    ),
                )
            }
            thumbnailBlob
        }
    }
}

private fun Event.Create.asLexiconEvent(
    thumbnail: File.Media.Photo?,
    thumbnailBlob: Blob?,
    createdAt: Instant,
) = LexiconEvent(
    name = details.name,
    description = details.description,
    createdAt = createdAt,
    startsAt = details.startsAt,
    endsAt = details.endsAt,
    mode = details.mode
        .takeUnless { it == Event.Mode.Unknown }
        ?.id
        ?.let(EventToken::safeValueOf),
    status = EventToken.Scheduled,
    locations = details.onlineLink?.let { link ->
        listOf(
            EventLocationUnion.CalendarEventUri(
                value = LexiconEventUri(
                    uri = Uri(link.uri),
                ),
            ),
        )
    },
    rsvpExpected = true,
    media = thumbnailBlob?.let { blob ->
        listOf(
            EventMedia(
                role = EventMediaRole.Thumbnail,
                content = blob,
                alt = thumbnail?.altText,
                aspect_ratio = thumbnail
                    ?.takeIf { it.width > 0 && it.height > 0 }
                    ?.let {
                        EventAspectRatio(
                            width = it.width.toLong(),
                            height = it.height.toLong(),
                        )
                    },
            ),
        )
    },
    timezone = details.timezone,
)

/**
 * Applies [details] to a cached event the way [withEventDetails] applies them to its record.
 */
private fun EventEntity.withDetails(
    cid: EventId,
    details: Event.Details,
    thumbnail: File.Media.Photo?,
    thumbnailBlob: Blob?,
): EventEntity {
    val places = base64EncodedPlaces?.fromBase64EncodedUrl<Event.Places>()
    val otherLocations = places?.locations
        .orEmpty()
        .filterNot { it is Event.Location.Online }
    return copy(
        cid = cid,
        name = details.name,
        description = details.description,
        startsAt = details.startsAt,
        endsAt = details.endsAt,
        mode = if (details.mode == Event.Mode.Unknown) mode else details.mode.id,
        base64EncodedPlaces = Event.Places(
            locations = otherLocations + listOfNotNull(
                details.onlineLink?.let {
                    Event.Location.Online(
                        uri = it,
                        name = null,
                    )
                },
            ),
            links = places?.links.orEmpty(),
        ).toUrlEncodedBase64(),
        thumbnail = thumbnailBlob?.imageUri(profileId = uri.profileId()) ?: this.thumbnail,
        thumbnailWidth = if (thumbnailBlob != null) thumbnail?.width?.toLong() else thumbnailWidth,
        thumbnailHeight = if (thumbnailBlob != null) thumbnail?.height?.toLong() else thumbnailHeight,
    )
}

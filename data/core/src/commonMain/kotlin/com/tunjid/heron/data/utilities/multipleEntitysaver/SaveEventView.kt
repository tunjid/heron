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

package com.tunjid.heron.data.utilities.multipleEntitysaver

import com.tunjid.heron.data.core.models.Event
import com.tunjid.heron.data.core.models.toUrlEncodedBase64
import com.tunjid.heron.data.core.types.EventId
import com.tunjid.heron.data.core.types.EventUri
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RsvpId
import com.tunjid.heron.data.core.types.RsvpUri
import com.tunjid.heron.data.core.types.profileId
import com.tunjid.heron.data.core.types.recordKey
import com.tunjid.heron.data.database.entities.EventEntity
import com.tunjid.heron.data.database.entities.RsvpEntity
import com.tunjid.heron.data.utilities.safeDecodeAs
import com.tunjid.heron.data.utilities.tidInstant
import community.lexicon.calendar.Event as LexiconEvent
import community.lexicon.calendar.EventLocationUnion
import community.lexicon.calendar.EventUri as LexiconEventUri
import community.lexicon.calendar.Rsvp as LexiconRsvp
import kotlin.time.Instant
import social.heron.calendar.EventView

internal fun MultipleEntitySaver.add(
    eventView: EventView,
    viewingProfileId: ProfileId?,
) {
    val event = eventView.record.safeDecodeAs<LexiconEvent>() ?: return
    val eventUri = eventView.uri.atUri.let(::EventUri)

    add(eventView.author)
    add(
        EventEntity(
            uri = eventUri,
            cid = eventView.cid.cid.let(::EventId),
            authorId = eventView.author.did.did.let(::ProfileId),
            name = event.name,
            description = event.description,
            createdAt = event.createdAt,
            startsAt = event.startsAt,
            endsAt = event.endsAt,
            mode = event.mode?.value,
            status = event.status?.value,
            base64EncodedPlaces = Event.Places(
                locations = event.locations
                    ?.mapNotNull(EventLocationUnion::asLocation)
                    .orEmpty(),
                links = event.uris
                    ?.map(LexiconEventUri::asExternalLink)
                    .orEmpty(),
            ).toUrlEncodedBase64(),
            rsvpExpected = event.rsvpExpected,
            sortedAt = eventView.sortedAt,
            goingCount = eventView.goingCount,
            interestedCount = eventView.interestedCount,
            notGoingCount = eventView.notGoingCount,
        ),
    )

    viewingProfileId?.let { profileId ->
        eventView.viewerRsvp?.let {
            add(
                RsvpEntity(
                    uri = it.uri.atUri.let(::RsvpUri),
                    cid = it.cid.cid.let(::RsvpId),
                    eventUri = eventUri,
                    viewingProfileId = profileId,
                    status = it.status,
                    sortedAt = it.sortedAt,
                ),
            )
        } ?: remove(
            RsvpEntity.Deletion(
                eventUri = eventUri,
                viewingProfileId = profileId,
            ),
        )
    }
}

internal fun MultipleEntitySaver.add(
    rsvpUri: RsvpUri,
    rsvpCid: RsvpId?,
    rsvp: LexiconRsvp,
    sortedAt: Instant,
    viewingProfileId: ProfileId,
) {
    val eventUri = EventUri(rsvp.subject.uri.atUri)
    add(
        stubEventEntity(
            eventUri = eventUri,
            name = "",
        ),
    )
    add(
        RsvpEntity(
            uri = rsvpUri,
            cid = rsvpCid,
            eventUri = eventUri,
            viewingProfileId = viewingProfileId,
            status = rsvp.status,
            sortedAt = sortedAt,
        ),
    )
}

internal fun stubEventEntity(
    eventUri: EventUri,
    name: String,
    description: String? = null,
): EventEntity {
    val createdAt = eventUri.recordKey.tidInstant ?: Instant.DISTANT_PAST
    return EventEntity(
        uri = eventUri,
        cid = null,
        authorId = eventUri.profileId(),
        name = name,
        description = description,
        createdAt = createdAt,
        startsAt = null,
        endsAt = null,
        mode = null,
        status = null,
        base64EncodedPlaces = null,
        rsvpExpected = null,
        sortedAt = createdAt,
        goingCount = 0,
        interestedCount = 0,
        notGoingCount = 0,
    )
}

private fun EventLocationUnion.asLocation(): Event.Location? = when (this) {
    is EventLocationUnion.CalendarEventUri -> Event.Location.Online(
        uri = GenericUri(value.uri.uri),
        name = value.name,
    )
    is EventLocationUnion.LocationAddress -> Event.Location.Address(
        country = value.country,
        postalCode = value.postalCode,
        region = value.region,
        locality = value.locality,
        street = value.street,
        name = value.name,
    )
    is EventLocationUnion.LocationFsq -> Event.Location.Foursquare(
        placeId = value.fsq_place_id,
        latitude = value.latitude,
        longitude = value.longitude,
        name = value.name,
    )
    is EventLocationUnion.LocationGeo -> Event.Location.Geo(
        latitude = value.latitude,
        longitude = value.longitude,
        altitude = value.altitude,
        name = value.name,
    )
    is EventLocationUnion.LocationHthree -> Event.Location.H3(
        value = value.value,
        name = value.name,
    )
    is EventLocationUnion.Unknown -> null
}

private fun LexiconEventUri.asExternalLink() = Event.ExternalLink(
    uri = GenericUri(uri.uri),
    name = name,
)

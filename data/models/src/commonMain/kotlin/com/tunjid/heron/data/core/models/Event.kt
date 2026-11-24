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

package com.tunjid.heron.data.core.models

import com.tunjid.heron.data.core.types.EmbeddableRecordUri
import com.tunjid.heron.data.core.types.EventId
import com.tunjid.heron.data.core.types.EventUri
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.data.core.types.profileId
import com.tunjid.heron.data.core.types.recordKey
import com.tunjid.heron.data.core.utilities.File
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Event(
    val uri: EventUri,
    val cid: EventId?,
    val author: Profile,
    val name: String,
    val description: String?,
    val createdAt: Instant,
    val startsAt: Instant?,
    val endsAt: Instant?,
    val mode: Mode,
    val status: Status,
    val places: Places,
    val rsvpExpected: Boolean,
    val sortedAt: Instant,
    val goingCount: Long,
    val interestedCount: Long,
    val notGoingCount: Long,
    val thumbnail: ImageUri?,
    val thumbnailAspectRatio: Float?,
    val rsvp: Rsvp?,
    val bskyPostRef: BskyPostRef?,
) : Record.Embeddable.External,
    UrlEncodableModel {
    override val reference: Record.Reference =
        Record.Reference(
            id = cid,
            uri = uri,
        )

    override val embeddableRecordUri: EmbeddableRecordUri = uri

    companion object {
        fun uri(
            authorId: ProfileId,
            recordKey: RecordKey,
        ): EventUri = EventUri(
            uri = "at://${authorId.id}/${EventUri.NAMESPACE}/${recordKey.value}",
        )

        fun atmoRsvpLink(
            authorId: ProfileId,
            recordKey: RecordKey,
        ): GenericUri = GenericUri(
            uri = "https://atmo.rsvp/p/${authorId.id}/e/${recordKey.value}",
        )
    }

    @Serializable
    data class Details(
        val name: String,
        val description: String?,
        val startsAt: Instant?,
        val endsAt: Instant?,
        val mode: Mode,
        val onlineLink: GenericUri?,
        val timezone: String?,
    )

    @Serializable
    sealed interface Put {
        val uri: EventUri
        val details: Details
        val thumbnail: File.Media.Photo?
    }

    @Serializable
    data class Create(
        override val uri: EventUri,
        override val details: Details,
        override val thumbnail: File.Media.Photo?,
    ) : Put

    @Serializable
    data class Edit(
        override val uri: EventUri,
        override val details: Details,
        override val thumbnail: File.Media.Photo?,
    ) : Put

    @Serializable
    data class BskyPostRef(
        val uri: PostUri,
        val cid: PostId,
        val showComments: Boolean,
    )

    enum class Mode(
        val id: String,
    ) {
        Unknown("unknown"),
        InPerson("community.lexicon.calendar.event#inperson"),
        Virtual("community.lexicon.calendar.event#virtual"),
        Hybrid("community.lexicon.calendar.event#hybrid"),
        ;

        companion object {
            fun fromId(
                id: String?,
            ) = when (id) {
                null -> InPerson
                else -> entries.find { it.id == id } ?: Unknown
            }
        }
    }

    enum class Status(
        val id: String,
    ) {
        Unknown("unknown"),
        Planned("community.lexicon.calendar.event#planned"),
        Scheduled("community.lexicon.calendar.event#scheduled"),
        Rescheduled("community.lexicon.calendar.event#rescheduled"),
        Cancelled("community.lexicon.calendar.event#cancelled"),
        Postponed("community.lexicon.calendar.event#postponed"),
        ;

        companion object {
            fun fromId(
                id: String?,
            ) = when (id) {
                null -> Scheduled
                else -> entries.find { it.id == id } ?: Unknown
            }
        }
    }

    @Serializable
    data class Places(
        val locations: List<Location>,
        val links: List<ExternalLink>,
    ) : UrlEncodableModel

    @Serializable
    data class ExternalLink(
        val uri: GenericUri,
        val name: String?,
    )

    @Serializable
    sealed class Location {
        abstract val name: String?

        @Serializable
        data class Online(
            val uri: GenericUri,
            override val name: String?,
        ) : Location()

        @Serializable
        data class Address(
            val country: String,
            val postalCode: String?,
            val region: String?,
            val locality: String?,
            val street: String?,
            override val name: String?,
        ) : Location()

        @Serializable
        data class Geo(
            val latitude: String,
            val longitude: String,
            val altitude: String?,
            override val name: String?,
        ) : Location()

        @Serializable
        data class Foursquare(
            val placeId: String,
            val latitude: String?,
            val longitude: String?,
            override val name: String?,
        ) : Location()

        @Serializable
        data class H3(
            val value: String,
            override val name: String?,
        ) : Location()
    }
}

val Event.details: Event.Details
    get() = Event.Details(
        name = name,
        description = description,
        startsAt = startsAt,
        endsAt = endsAt,
        mode = mode,
        onlineLink = places.locations
            .firstNotNullOfOrNull { (it as? Event.Location.Online)?.uri },
        timezone = null,
    )

/**
 * The external embed of a post announcing this event: a link to its atmo.rsvp page, which Heron
 * resolves back to the event. The thumbnail is filled in from the event's own when posting.
 */
val Event.Put.externalEmbed: ExternalEmbed
    get() = ExternalEmbed(
        uri = Event.atmoRsvpLink(
            authorId = uri.profileId(),
            recordKey = uri.recordKey,
        ),
        title = details.name,
        description = details.description.orEmpty(),
        thumb = null,
    )

/**
 * A [LinkPreview] for a post announcing this event, carrying a local [Event] so the composer
 * can render the event card before anything is written.
 */
fun Event.Put.asLinkPreview(
    author: Profile,
    now: Instant,
): LinkPreview = LinkPreview(
    embed = externalEmbed,
    records = listOf(
        Event(
            uri = uri,
            cid = null,
            author = author,
            name = details.name,
            description = details.description,
            createdAt = now,
            startsAt = details.startsAt,
            endsAt = details.endsAt,
            mode = details.mode,
            status = Event.Status.Scheduled,
            places = Event.Places(
                locations = listOfNotNull(
                    details.onlineLink?.let {
                        Event.Location.Online(
                            uri = it,
                            name = null,
                        )
                    },
                ),
                links = emptyList(),
            ),
            rsvpExpected = true,
            sortedAt = now,
            goingCount = 0,
            interestedCount = 0,
            notGoingCount = 0,
            thumbnail = null,
            thumbnailAspectRatio = null,
            rsvp = null,
            bskyPostRef = null,
        ),
    ),
)

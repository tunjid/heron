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

import com.tunjid.heron.data.core.types.EventId
import com.tunjid.heron.data.core.types.EventUri
import com.tunjid.heron.data.core.types.GenericUri
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
    val rsvp: Rsvp?,
) : Record {
    override val reference: Record.Reference =
        Record.Reference(
            id = cid,
            uri = uri,
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

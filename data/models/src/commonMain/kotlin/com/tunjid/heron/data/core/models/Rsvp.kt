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
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.data.core.types.RsvpId
import com.tunjid.heron.data.core.types.RsvpUri
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Rsvp(
    val uri: RsvpUri,
    val cid: RsvpId?,
    val eventUri: EventUri,
    val status: Status,
    val sortedAt: Instant,
) : Record {
    override val reference: Record.Reference =
        Record.Reference(
            id = cid,
            uri = uri,
        )

    enum class Status(
        val id: String,
    ) {
        Unknown("unknown"),
        Going("community.lexicon.calendar.rsvp#going"),
        Interested("community.lexicon.calendar.rsvp#interested"),
        NotGoing("community.lexicon.calendar.rsvp#notgoing"),
        ;

        companion object {
            fun fromId(
                id: String,
            ) = entries.find { it.id == id } ?: Unknown
        }
    }

    /**
     * Writes the signed in user's response to an event. Changing an existing
     * response reuses its [recordKey] so at most one RSVP exists per event.
     */
    @Serializable
    data class Put(
        val eventUri: EventUri,
        val eventCid: EventId,
        val status: Status,
        val recordKey: RecordKey = RecordKey.generate(),
        val sortedAt: Instant = Clock.System.now(),
    )
}

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

import com.tunjid.heron.data.core.models.Event
import com.tunjid.heron.data.core.models.RecordPostReference
import com.tunjid.heron.data.core.types.EventUri
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.StandardDocumentUri
import com.tunjid.heron.data.network.BlueskyJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Instant
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class RecordEditsTest {

    @Test
    fun eventDetails_keepFieldsHeronDoesNotManage() {
        val edited = AtmoRsvpEvent.withDetails()

        listOf(
            "theme",
            "preferences",
            "createdWith",
            "bskyPostRef",
            "media",
            "timezone",
            "createdAt",
            "status",
        ).forEach { key ->
            assertEquals(AtmoRsvpEvent[key], edited[key], key)
        }
    }

    @Test
    fun eventDetails_overwriteTheFieldsHeronManages() {
        val edited = AtmoRsvpEvent.withDetails()

        assertEquals("Renamed", edited.string("name"))
        assertEquals("2026-12-01T18:00:00Z", edited.string("startsAt"))
        assertEquals(Event.Mode.Hybrid.id, edited.string("mode"))
        // Cleared fields are removed rather than written as null.
        assertFalse("description" in edited)
        assertFalse("endsAt" in edited)
    }

    @Test
    fun eventDetails_replaceOnlyTheOnlineLocation() {
        val locations = AtmoRsvpEvent.withDetails()
            .getValue("locations")
            .jsonArray

        assertEquals(
            listOf(
                "community.lexicon.location.address",
                "community.lexicon.calendar.event#uri",
            ),
            locations.map { it.jsonObject.string("\$type") },
        )
        assertEquals("https://meet.example.com/new", locations[1].jsonObject.string("uri"))
    }

    @Test
    fun linkingAnEvent_keepsTheAuthorsShowCommentsChoice() {
        val ref = AtmoRsvpEvent.withBskyPostRef(
            RecordPostReference.Link(
                recordUri = EventUri("at://did:plc:author/community.lexicon.calendar.event/3event"),
                postUri = PostUri("at://did:plc:author/app.bsky.feed.post/3new"),
                postCid = PostId("bafynew"),
            ),
        ).getValue("bskyPostRef").jsonObject

        assertEquals("at://did:plc:author/app.bsky.feed.post/3new", ref.string("uri"))
        assertEquals("bafynew", ref.string("cid"))
        assertFalse(ref.getValue("showComments").jsonPrimitive.boolean)
    }

    @Test
    fun linkingADocument_writesAPlainStrongRef() {
        val edited = JsonObject(emptyMap()).withBskyPostRef(
            RecordPostReference.Link(
                recordUri = StandardDocumentUri("at://did:plc:author/site.standard.document/3doc"),
                postUri = PostUri("at://did:plc:author/app.bsky.feed.post/3new"),
                postCid = PostId("bafynew"),
            ),
        )

        assertEquals(setOf("uri", "cid"), edited.getValue("bskyPostRef").jsonObject.keys)
    }

    @Test
    fun unlinking_removesTheReferenceAndNothingElse() {
        val edited = AtmoRsvpEvent.withBskyPostRef(
            RecordPostReference.Unlink(
                recordUri = EventUri("at://did:plc:author/community.lexicon.calendar.event/3event"),
            ),
        )

        assertEquals(AtmoRsvpEvent.keys - "bskyPostRef", edited.keys)
    }
}

private fun JsonObject.withDetails() = withEventDetails(
    details = Details,
    thumbnail = null,
    thumbnailBlob = null,
)

private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content

private val Details = Event.Details(
    name = "Renamed",
    description = null,
    startsAt = Instant.parse("2026-12-01T18:00:00Z"),
    endsAt = null,
    mode = Event.Mode.Hybrid,
    onlineLink = GenericUri("https://meet.example.com/new"),
    timezone = null,
)

// Shaped like a record atmo.rsvp writes, including fields Heron's lexicon doesn't declare.
private val AtmoRsvpEvent = BlueskyJson.parseToJsonElement(
    """
    {
        "${'$'}type": "community.lexicon.calendar.event",
        "name": "Launch party",
        "description": "Come through",
        "createdAt": "2026-11-01T00:00:00Z",
        "startsAt": "2026-11-20T18:00:00Z",
        "endsAt": "2026-11-20T21:00:00Z",
        "mode": "community.lexicon.calendar.event#inperson",
        "status": "community.lexicon.calendar.event#planned",
        "locations": [
            { "${'$'}type": "community.lexicon.location.address", "country": "DE", "locality": "Berlin" },
            { "${'$'}type": "community.lexicon.calendar.event#uri", "uri": "https://meet.example.com/old" }
        ],
        "media": [
            {
                "role": "thumbnail",
                "content": { "${'$'}type": "blob", "ref": { "${'$'}link": "bafkreithumb" }, "mimeType": "image/webp", "size": 100 },
                "aspect_ratio": { "width": 800, "height": 800 }
            }
        ],
        "timezone": "Europe/Berlin",
        "theme": { "name": "warp", "baseColor": "slate" },
        "preferences": { "showInDiscovery": true },
        "createdWith": "https://atmo.rsvp",
        "bskyPostRef": {
            "uri": "at://did:plc:author/app.bsky.feed.post/3old",
            "cid": "bafyold",
            "showComments": false
        }
    }
    """,
).jsonObject

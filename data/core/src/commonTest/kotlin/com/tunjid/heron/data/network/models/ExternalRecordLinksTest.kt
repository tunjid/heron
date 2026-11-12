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

package com.tunjid.heron.data.network.models

import app.bsky.feed.PostView
import com.tunjid.heron.data.core.types.EventUri
import com.tunjid.heron.data.network.BlueskyJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExternalRecordLinksTest {

    @Test
    fun handleUrl_byAuthor_mapsToDidEventUri() {
        assertEquals(
            listOf(AuthorEventUri),
            postView(link = "https://atmo.rsvp/p/bjoernsta.de/e/3myx2khkx424z").externalRecordUris(),
        )
    }

    @Test
    fun handleUrl_matchesAuthorHandleIgnoringCase() {
        assertEquals(
            listOf(AuthorEventUri),
            postView(link = "https://atmo.rsvp/p/BjoernSta.de/e/3myx2khkx424z").externalRecordUris(),
        )
    }

    @Test
    fun didUrl_byAuthor_mapsToDidEventUri() {
        assertEquals(
            listOf(AuthorEventUri),
            postView(link = "https://atmo.rsvp/p/did:plc:author/e/3myx2khkx424z").externalRecordUris(),
        )
    }

    @Test
    fun trailingSlashQueryAndFragment_areIgnored() {
        assertEquals(
            listOf(AuthorEventUri),
            postView(link = "https://www.atmo.rsvp/p/bjoernsta.de/e/3myx2khkx424z/?ref=bsky#top").externalRecordUris(),
        )
    }

    @Test
    fun handleUrl_byMentionedProfile_mapsToMentionedDid() {
        val text = "Going to @organiser.bsky.social's event 🎉"
        val start = "Going to ".encodeToByteArray().size
        val end = start + "@organiser.bsky.social".encodeToByteArray().size
        assertEquals(
            listOf(OrganiserEventUri),
            postView(
                link = "https://atmo.rsvp/p/organiser.bsky.social/e/3myx2khkx424z",
                text = text,
                facets = """
                    [{
                        "index": { "byteStart": $start, "byteEnd": $end },
                        "features": [{ "${'$'}type": "app.bsky.richtext.facet#mention", "did": "did:plc:organiser" }]
                    }]
                """,
            ).externalRecordUris(),
        )
    }

    @Test
    fun handleUrl_byMentionedProfileAfterMultiByteText_usesByteOffsets() {
        val text = "🎉🎉 @organiser.bsky.social"
        val start = "🎉🎉 ".encodeToByteArray().size
        val end = text.encodeToByteArray().size
        assertEquals(
            listOf(OrganiserEventUri),
            postView(
                link = "https://atmo.rsvp/p/organiser.bsky.social/e/3myx2khkx424z",
                text = text,
                facets = """
                    [{
                        "index": { "byteStart": $start, "byteEnd": $end },
                        "features": [{ "${'$'}type": "app.bsky.richtext.facet#mention", "did": "did:plc:organiser" }]
                    }]
                """,
            ).externalRecordUris(),
        )
    }

    @Test
    fun handleUrl_byQuotedAuthor_mapsToQuotedAuthorDid() {
        assertEquals(
            listOf(OrganiserEventUri),
            postView(
                link = "https://atmo.rsvp/p/organiser.bsky.social/e/3myx2khkx424z",
                quotedAuthor = true,
            ).externalRecordUris(),
        )
    }

    @Test
    fun otherActor_isNotResolved() {
        assertEquals(
            emptyList(),
            postView(link = "https://atmo.rsvp/p/someone.else/e/3myx2khkx424z").externalRecordUris(),
        )
    }

    @Test
    fun recordUri_keepsTheUrlsAuthorityForReadTimeResolution() {
        assertEquals(
            EventUri("at://someone.else/community.lexicon.calendar.event/3myx2khkx424z"),
            "https://atmo.rsvp/p/someone.else/e/3myx2khkx424z?ref=bsky".externalRecordUriOrNull(),
        )
    }

    @Test
    fun sameRecord_matchesAcrossHandleAndDidAuthorities() {
        val handleUri = EventUri("at://organiser.bsky.social/community.lexicon.calendar.event/3myx2khkx424z")
        assertTrue(OrganiserEventUri.isSameRecordAs(handleUri))
        assertFalse(
            OrganiserEventUri.isSameRecordAs(
                EventUri("at://organiser.bsky.social/community.lexicon.calendar.event/3other"),
            ),
        )
    }

    @Test
    fun nonEventPaths_areNotResolved() {
        listOf(
            "https://atmo.rsvp/p/bjoernsta.de",
            "https://atmo.rsvp/p/bjoernsta.de/e/",
            "https://atmo.rsvp/p/bjoernsta.de/x/3myx2khkx424z",
            "https://atmo.rsvp/p/bjoernsta.de/e/3myx2khkx424z/rsvps",
            "https://example.com/p/bjoernsta.de/e/3myx2khkx424z",
        ).forEach { link ->
            assertEquals(emptyList(), postView(link = link).externalRecordUris(), link)
        }
    }
}

private fun postView(
    link: String,
    text: String = "",
    facets: String = "[]",
    quotedAuthor: Boolean = false,
): PostView {
    val external = """
        "external": { "uri": "$link", "title": "Event", "description": "" }
    """
    val embed =
        if (quotedAuthor) """
            {
                "${'$'}type": "app.bsky.embed.recordWithMedia#view",
                "record": {
                    "record": {
                        "${'$'}type": "app.bsky.embed.record#viewRecord",
                        "uri": "at://did:plc:organiser/app.bsky.feed.post/3quoted",
                        "cid": "bafyquoted",
                        "author": { "did": "did:plc:organiser", "handle": "organiser.bsky.social" },
                        "value": { "${'$'}type": "app.bsky.feed.post", "text": "", "createdAt": "2026-11-11T00:00:00Z" },
                        "indexedAt": "2026-11-11T00:00:00Z"
                    }
                },
                "media": { "${'$'}type": "app.bsky.embed.external#view", $external }
            }
        """
        else """{ "${'$'}type": "app.bsky.embed.external#view", $external }"""

    return BlueskyJson.decodeFromString(
        """
        {
            "uri": "at://did:plc:author/app.bsky.feed.post/3post",
            "cid": "bafypost",
            "author": { "did": "did:plc:author", "handle": "bjoernsta.de" },
            "record": {
                "${'$'}type": "app.bsky.feed.post",
                "text": ${BlueskyJson.encodeToString(text)},
                "facets": $facets,
                "createdAt": "2026-11-11T00:00:00Z"
            },
            "embed": $embed,
            "indexedAt": "2026-11-11T00:00:00Z"
        }
        """,
    )
}

private val AuthorEventUri = EventUri("at://did:plc:author/community.lexicon.calendar.event/3myx2khkx424z")

private val OrganiserEventUri = EventUri("at://did:plc:organiser/community.lexicon.calendar.event/3myx2khkx424z")

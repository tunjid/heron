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

import com.tunjid.heron.data.core.models.StandardDocument
import com.tunjid.heron.data.core.models.primaryRecord
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.network.BlueskyJson
import com.tunjid.heron.data.utilities.recordResolver.CardyExtractResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.decodeFromString

class ExternalLinkPreviewTest {

    @Test
    fun standardSiteUrl_mapsToDocumentWithPublication() {
        val response = BlueskyJson.decodeFromString<CardyExtractResponse>(StandardSiteJson)
        val preview = assertNotNull(
            response.asLinkPreview(GenericUri("https://tunjid.com/articles/example")),
        )

        // The hydrated view wins over the legacy top-level fields.
        assertEquals(
            "Embracing Compose Snapshot State for UI Layer State Production",
            preview.embed.title,
        )
        assertEquals(
            "https://tunjid.com/articles/embracing-compose-snapshot-state",
            preview.embed.uri.uri,
        )

        // Both backing records are surfaced; the document is primary and nests its publication.
        assertEquals(2, preview.records.size)
        val document = assertNotNull(preview.primaryRecord as? StandardDocument)
        assertNotNull(document.cid)
        val publication = assertNotNull(document.publication)
        assertEquals("Adetunji Dahunsi", publication.name)
    }

    @Test
    fun plainSiteUrl_mapsToBasicCardWithoutRecords() {
        val response = BlueskyJson.decodeFromString<CardyExtractResponse>(PlainSiteJson)
        val requestedUrl = GenericUri("https://example.com")
        val preview = assertNotNull(response.asLinkPreview(requestedUrl))

        assertTrue(preview.records.isEmpty())
        assertEquals("Example Domain", preview.embed.title)
        assertEquals(requestedUrl, preview.embed.uri)
    }

    @Test
    fun failedExtraction_returnsNull() {
        val response = BlueskyJson.decodeFromString<CardyExtractResponse>(FailedJson)
        assertNull(response.asLinkPreview(GenericUri("https://whtwnd.com/example")))
    }
}

private const val StandardSiteJson = """
{
  "error": "",
  "likely_type": "html",
  "url": "embracing-compose-snapshot-state",
  "title": "Embracing Compose Snapshot State for UI Layer State Production",
  "description": "Use Compose State and not StateFlow to get the best out of the UI layer",
  "image": "https://cardyb.bsky.app/v1/image?url=legacy",
  "view": {
    "external": {
      "uri": "https://tunjid.com/articles/embracing-compose-snapshot-state",
      "title": "Embracing Compose Snapshot State for UI Layer State Production",
      "description": "Getting the best out of Kotlin, coroutines and compose in the UI layer",
      "thumb": "https://cdn.bsky.app/img/feed_thumbnail/plain/did:plc:6q4y7p2wft3tncsffspts3m5/bafthumb",
      "createdAt": "2026-05-15T21:13:33.000Z",
      "source": {
        "uri": "https://tunjid.com",
        "icon": "https://cdn.bsky.app/img/avatar/plain/did:plc:6q4y7p2wft3tncsffspts3m5/baficon",
        "title": "Adetunji Dahunsi",
        "description": "A collection of my thoughts and experiences."
      },
      "associatedProfiles": [
        {
          "did": "did:plc:6q4y7p2wft3tncsffspts3m5",
          "handle": "tunji.dev",
          "displayName": "TJ",
          "avatar": "https://cdn.bsky.app/img/avatar/plain/did:plc:6q4y7p2wft3tncsffspts3m5/bafavatar"
        }
      ],
      "associatedRefs": [
        {
          "uri": "at://did:plc:6q4y7p2wft3tncsffspts3m5/site.standard.document/3mlwarqwqe2ap",
          "cid": "bafyreiaojsow3evw7j4mn6ady3q7ji7i2ttgbukz5hzslsswfg2coqauee"
        },
        {
          "uri": "at://did:plc:6q4y7p2wft3tncsffspts3m5/site.standard.publication/3ddckoeex22s5",
          "cid": "bafyreidfsiixcefy5klglf7xi2rgrjono4io6apclplzdu2vrkcwoomqci"
        }
      ]
    }
  }
}
"""

private const val PlainSiteJson = """
{
  "error": "",
  "likely_type": "html",
  "url": "",
  "title": "Example Domain",
  "description": "",
  "image": ""
}
"""

private const val FailedJson = """
{
  "error": "Unable to generate link preview",
  "likely_type": "",
  "url": "",
  "title": "",
  "description": "",
  "image": ""
}
"""

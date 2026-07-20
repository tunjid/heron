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

import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.ThreadGate
import com.tunjid.heron.data.core.types.DraftId
import com.tunjid.heron.data.core.types.FileUri
import com.tunjid.heron.data.core.types.GenericId
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.asRecordUriOrNull
import com.tunjid.heron.data.core.utilities.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Instant

class PostDraftConversionsTest {

    @Test
    fun postDraft_roundTripsThroughNetworkDraft() {
        val draft = sampleDraft()

        val networkDraft = draft.toNetworkDraft()

        // Draft level metadata survives.
        assertEquals(draft.posts.size, networkDraft.posts.size)
        assertEquals(listOf("en", "fr"), networkDraft.langs?.map { it.tag })
        assertNotNull(networkDraft.threadgateAllow)

        val requests = networkDraft.asRequests(draft.authorId)
        val request = requests.single()

        assertEquals("Draft body", request.text)

        val photo = request.metadata.embeddedMedia
            .filterIsInstance<File.Media.Photo>()
            .single()
        assertEquals("/media/photo.jpg", photo.uri.uri)
        assertEquals("a cat", photo.altText)

        val quote = assertNotNull(request.metadata.embeddedRecordReference)
        assertEquals(QuoteUri, quote.uri.uri)
        assertEquals(QuoteCid, quote.id?.id)
    }

    @Test
    fun postDraft_roundTripsThroughEntity() {
        val draft = sampleDraft().copy(
            id = DraftId("3ktid123"),
            createdAt = Instant.parse("2024-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2024-01-02T00:00:00Z"),
        )

        val restored = draft.asEntity(
            fallback = Instant.parse("2024-06-01T00:00:00Z"),
        ).asExternalModel()

        // The entity cache is lossless: unlike the network mapping it round-trips the whole model,
        // including media dimensions dropped by the stash lexicon.
        assertEquals(draft.id, restored.id)
        assertEquals(draft.authorId, restored.authorId)
        assertEquals(draft.createdAt, restored.createdAt)
        assertEquals(draft.updatedAt, restored.updatedAt)
        assertEquals(draft.langs, restored.langs)
        assertEquals(draft, restored)
    }

    private fun sampleDraft(): Post.Draft = Post.Draft(
        id = null,
        authorId = ProfileId("did:plc:author"),
        langs = listOf("en", "fr"),
        posts = listOf(
            Post.Create.Request(
                authorId = ProfileId("did:plc:author"),
                text = "Draft body",
                links = emptyList(),
                metadata = Post.Create.Metadata(
                    embeddedMedia = listOf(
                        File.Media.Photo(
                            uri = FileUri("/media/photo.jpg"),
                            width = 800,
                            height = 600,
                            altText = "a cat",
                        ),
                    ),
                    embeddedRecordReference = Record.Reference(
                        id = GenericId(QuoteCid),
                        uri = assertNotNull(QuoteUri.asRecordUriOrNull()),
                    ),
                    allowed = ThreadGate.Allowed(
                        allowsFollowing = true,
                        allowsFollowers = false,
                        allowsMentioned = true,
                    ),
                ),
            ),
        ),
    )
}

private const val QuoteUri = "at://did:plc:quoted/app.bsky.feed.post/3kquotedpost"
private const val QuoteCid = "bafyreiquotedcid"

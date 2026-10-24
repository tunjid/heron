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

package com.tunjid.heron.data.utilities.profileLinks

import com.atproto.repo.StrongRef
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.types.AtProtoException
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.ProfileLinkUri
import com.tunjid.heron.data.lexicons.BlueskyApi
import com.tunjid.heron.data.lexicons.XrpcBlueskyApi
import com.tunjid.heron.data.network.NetworkService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Cid
import sh.christian.ozone.api.response.AtpResponse

class ProfileLinkWriterTest {

    private val profileId = ProfileId("did:plc:kat")

    @Test
    fun unchangedLinkKeepsItsRecord() = runRealTimeTest {
        val server = FakeServer(
            records = mapOf(
                "a" to linkRecord(url = "https://ko-fi.com/kat", title = "Ko-fi"),
            ),
        )

        val writes = server.writer().write(
            profileId = profileId,
            links = listOf(
                link(rkey = "a", url = "https://ko-fi.com/kat", title = "Ko-fi"),
            ),
            existingRefs = listOf(ref("a")),
        )

        assertEquals(
            expected = listOf(linkUri("a")),
            actual = writes.refs.map { it.uri.atUri },
        )
        assertTrue(writes.removed.isEmpty())
        assertTrue(server.writes.isEmpty())
        assertTrue(server.thirdPartyRequests.isEmpty())
    }

    @Test
    fun reorderingOnlyReordersTheRefs() = runRealTimeTest {
        val server = FakeServer(
            records = mapOf(
                "a" to linkRecord(url = "https://ko-fi.com/kat", title = null),
                "b" to linkRecord(url = "https://example.com", title = null),
            ),
        )

        val writes = server.writer().write(
            profileId = profileId,
            links = listOf(
                link(rkey = "b", url = "https://example.com", title = null),
                link(rkey = "a", url = "https://ko-fi.com/kat", title = null),
            ),
            existingRefs = listOf(ref("a"), ref("b")),
        )

        assertEquals(
            expected = listOf(linkUri("b"), linkUri("a")),
            actual = writes.refs.map { it.uri.atUri },
        )
        assertTrue(server.writes.isEmpty())
    }

    @Test
    fun newLinkIsCreatedWithItsNormalizedUrlAndIcon() = runRealTimeTest {
        val server = FakeServer()

        val writes = server.writer().write(
            profileId = profileId,
            links = listOf(
                link(rkey = null, url = "patreon.com/kat", title = "  Patreon  "),
            ),
            existingRefs = emptyList(),
        )

        val (method, record) = server.writes.single()
        assertEquals(expected = "createRecord", actual = method)
        assertEquals(expected = "https://patreon.com/kat", actual = record.string("url"))
        assertEquals(expected = "Patreon", actual = record.string("title"))
        assertEquals(
            expected = "image/png",
            actual = record["icon"]?.jsonObject?.string("mimeType"),
        )
        assertEquals(
            expected = listOf(linkUri("created-0")),
            actual = writes.refs.map { it.uri.atUri },
        )
    }

    @Test
    fun editedTitleRewritesTheSameRecord() = runRealTimeTest {
        val server = FakeServer(
            records = mapOf(
                "a" to linkRecord(url = "https://ko-fi.com/kat", title = "Ko-fi"),
            ),
        )

        val writes = server.writer().write(
            profileId = profileId,
            links = listOf(
                link(rkey = "a", url = "https://ko-fi.com/kat", title = "Tip me"),
            ),
            existingRefs = listOf(ref("a")),
        )

        val (method, record) = server.writes.single()
        assertEquals(expected = "putRecord:a", actual = method)
        assertEquals(expected = "Tip me", actual = record.string("title"))
        assertEquals(expected = CreatedAt, actual = record.string("createdAt"))
        // A destination that was already checked isn't checked again
        assertTrue(server.thirdPartyRequests.none { it.contains("safelink") })
        assertEquals(
            expected = listOf(linkUri("a")),
            actual = writes.refs.map { it.uri.atUri },
        )
    }

    @Test
    fun removedLinksAreDeletedAfterwards() = runRealTimeTest {
        val server = FakeServer(
            records = mapOf(
                "a" to linkRecord(url = "https://ko-fi.com/kat", title = null),
                "b" to linkRecord(url = "https://example.com", title = null),
            ),
        )
        val writer = server.writer()

        val writes = writer.write(
            profileId = profileId,
            links = listOf(
                link(rkey = "a", url = "https://ko-fi.com/kat", title = null),
            ),
            existingRefs = listOf(ref("a"), ref("b")),
        )
        assertEquals(
            expected = listOf(ProfileLinkUri(linkUri("b"))),
            actual = writes.removed,
        )
        // Nothing is deleted until the caller says the profile stopped pointing at it
        assertTrue(server.deletes.isEmpty())

        writer.deleteRemoved(
            profileId = profileId,
            writes = writes,
        )
        assertEquals(expected = listOf("b"), actual = server.deletes)
    }

    @Test
    fun linksBreakingTheRulesAreDroppedWithoutAnyRequests() = runRealTimeTest {
        val server = FakeServer()

        val writes = server.writer().write(
            profileId = profileId,
            links = listOf(
                link(rkey = null, url = "http://example.com", title = null),
                link(rkey = null, url = "https://videy.at/v/abc", title = null),
                link(rkey = null, url = "https://bit.ly/abc", title = null),
                link(rkey = null, url = "not a url", title = null),
            ),
            existingRefs = emptyList(),
        )

        assertTrue(writes.refs.isEmpty())
        assertTrue(server.writes.isEmpty())
        assertTrue(server.thirdPartyRequests.isEmpty())
    }

    @Test
    fun duplicatesAndLinksPastTheLimitAreDropped() = runRealTimeTest {
        val server = FakeServer()

        val writes = server.writer().write(
            profileId = profileId,
            links = listOf(
                link(rkey = null, url = "ko-fi.com/kat", title = null),
                link(rkey = null, url = "https://Ko-fi.com/kat", title = null),
            ) + (0 until 12).map { index ->
                link(rkey = null, url = "https://example$index.com", title = null)
            },
            existingRefs = emptyList(),
        )

        assertEquals(expected = 10, actual = writes.refs.size)
        assertEquals(
            expected = setOf("https://ko-fi.com/kat") + (0 until 9).map { "https://example$it.com" },
            actual = server.writes.mapTo(mutableSetOf()) { it.second.string("url") },
        )
    }

    @Test
    fun safelinkBlockDropsANewLink() = runRealTimeTest {
        val server = FakeServer(
            safelinkBlocked = setOf("https://blocked.example.com"),
        )

        val writes = server.writer().write(
            profileId = profileId,
            links = listOf(
                link(rkey = null, url = "https://blocked.example.com", title = null),
                link(rkey = null, url = "https://ok.example.com", title = null),
            ),
            existingRefs = emptyList(),
        )

        assertEquals(expected = 1, actual = writes.refs.size)
        assertEquals(
            expected = "https://ok.example.com",
            actual = server.writes.single().second.string("url"),
        )
    }

    @Test
    fun missingIconStillSavesTheLink() = runRealTimeTest {
        val server = FakeServer(
            hasFavicon = false,
        )

        server.writer().write(
            profileId = profileId,
            links = listOf(
                link(rkey = null, url = "https://example.com", title = null),
            ),
            existingRefs = emptyList(),
        )

        assertNull(server.writes.single().second["icon"])
    }

    private fun link(
        rkey: String?,
        url: String,
        title: String?,
    ) = Profile.Update.Link(
        uri = rkey?.let { ProfileLinkUri(linkUri(it)) },
        url = GenericUri(url),
        title = title,
    )

    private fun ref(
        rkey: String,
    ) = StrongRef(
        uri = AtUri(linkUri(rkey)),
        cid = Cid("cid-$rkey"),
    )

    private fun linkUri(
        rkey: String,
    ) = "at://${profileId.id}/${ProfileLinkUri.NAMESPACE}/$rkey"

    private fun linkRecord(
        url: String,
        title: String?,
    ): String = buildString {
        append("""{"${'$'}type":"app.bsky.actor.link","url":"$url","createdAt":"$CreatedAt"""")
        if (title != null) append(""","title":"$title"""")
        append("}")
    }

    private inner class FakeServer(
        private val records: Map<String, String> = emptyMap(),
        private val safelinkBlocked: Set<String> = emptySet(),
        private val hasFavicon: Boolean = true,
    ) {
        /** Record writes, as the XRPC method (with the rkey for puts) and the record. */
        val writes = mutableListOf<Pair<String, JsonObject>>()
        val deletes = mutableListOf<String>()
        val thirdPartyRequests = mutableListOf<String>()

        private var createdCount = 0
        private val mutex = Mutex()

        private val httpClient = HttpClient(
            MockEngine { request ->
                // Link writes run in parallel
                mutex.withLock { handle(request) }
            },
        ) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        fun writer(): ProfileLinkWriter = AtProtoProfileLinkWriter(
            networkService = FakeNetworkService(XrpcBlueskyApi(httpClient)),
            httpClient = httpClient,
        )

        private suspend fun MockRequestHandleScope.handle(
            request: HttpRequestData,
        ): HttpResponseData {
            val url = request.url
            return when {
                url.host == "cardyb.bsky.app" -> {
                    thirdPartyRequests += url.toString()
                    if (hasFavicon) respond(
                        content = byteArrayOf(1, 2, 3, 4),
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Image.PNG.toString()),
                    )
                    else respond(content = "", status = HttpStatusCode.NotFound)
                }
                url.host == "go.bsky.app" -> {
                    thirdPartyRequests += url.toString()
                    val action = if (url.parameters["u"] in safelinkBlocked) "block" else "none"
                    respondJson("""{"action":"$action"}""")
                }
                url.encodedPath.endsWith("com.atproto.repo.getRecord") -> {
                    val rkey = url.parameters["rkey"]!!
                    val record = records[rkey]
                    if (record == null) respond(
                        content = """{"error":"RecordNotFound","message":"Could not locate record"}""",
                        status = HttpStatusCode.BadRequest,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                    else respondJson("""{"uri":"${linkUri(rkey)}","cid":"cid-$rkey","value":$record}""")
                }
                url.encodedPath.endsWith("com.atproto.repo.createRecord") -> {
                    writes += "createRecord" to request.bodyJson().getValue("record").jsonObject
                    val rkey = "created-${createdCount++}"
                    respondJson("""{"uri":"${linkUri(rkey)}","cid":"cid-$rkey"}""")
                }
                url.encodedPath.endsWith("com.atproto.repo.putRecord") -> {
                    val body = request.bodyJson()
                    val rkey = body.string("rkey")
                    writes += "putRecord:$rkey" to body.getValue("record").jsonObject
                    respondJson("""{"uri":"${linkUri(rkey)}","cid":"cid-$rkey-v2"}""")
                }
                url.encodedPath.endsWith("com.atproto.repo.deleteRecord") -> {
                    deletes += request.bodyJson().string("rkey")
                    respondJson("{}")
                }
                url.encodedPath.endsWith("com.atproto.repo.uploadBlob") -> respondJson(
                    """{"blob":{"${'$'}type":"blob","ref":{"${'$'}link":"bafkreiicon"},"mimeType":"image/png","size":4}}""",
                )
                else -> error("Unexpected request: $url")
            }
        }
    }
}

/**
 * The writer's network timeouts must run in real time: under [runTest]'s virtual clock
 * they'd expire before the mock engine, which runs on its own dispatcher, responds.
 */
private fun runRealTimeTest(
    block: suspend () -> Unit,
) = runTest {
    withContext(Dispatchers.Default) {
        block()
    }
}

private class FakeNetworkService(
    private val api: BlueskyApi,
) : NetworkService {
    override suspend fun <T : Any> runCatchingWithMonitoredNetworkRetry(
        times: Int,
        initialDelay: Duration,
        maxDelay: Duration,
        factor: Double,
        block: suspend BlueskyApi.() -> AtpResponse<T>,
    ): Result<T> = runCatching {
        when (val response = api.block()) {
            is AtpResponse.Success -> response.response
            is AtpResponse.Failure -> throw AtProtoException(
                statusCode = response.statusCode.code,
                error = response.error?.error,
                message = response.error?.message,
            )
        }
    }
}

private fun MockRequestHandleScope.respondJson(
    content: String,
) = respond(
    content = content,
    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
)

private suspend fun HttpRequestData.bodyJson(): JsonObject =
    Json.parseToJsonElement(body.toByteArray().decodeToString()).jsonObject

private fun JsonObject.string(
    key: String,
): String = getValue(key).jsonPrimitive.content

private const val CreatedAt = "2026-10-01T00:00:00Z"

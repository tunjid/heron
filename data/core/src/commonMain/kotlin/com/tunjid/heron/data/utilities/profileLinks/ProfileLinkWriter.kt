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

import app.bsky.actor.Link as BskyLink
import com.atproto.repo.CreateRecordRequest
import com.atproto.repo.DeleteRecordRequest
import com.atproto.repo.GetRecordQueryParams
import com.atproto.repo.PutRecordRequest
import com.atproto.repo.StrongRef
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileLinks
import com.tunjid.heron.data.core.models.isShowableProfileLink
import com.tunjid.heron.data.core.models.normalizeProfileLinkUrl
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.ProfileLinkUri
import com.tunjid.heron.data.core.types.isNotFound
import com.tunjid.heron.data.core.types.recordKey
import com.tunjid.heron.data.logging.LogPriority
import com.tunjid.heron.data.logging.logcat
import com.tunjid.heron.data.logging.loggableText
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.utilities.asJsonContent
import com.tunjid.heron.data.utilities.runCatchingUnlessCancelled
import com.tunjid.heron.data.utilities.safeDecodeAs
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Nsid
import sh.christian.ozone.api.RKey
import sh.christian.ozone.api.Uri as LexiconUri
import sh.christian.ozone.api.model.Blob

internal data class ProfileLinkWrites(
    val refs: List<StrongRef>,
    val removed: List<ProfileLinkUri>,
)

internal interface ProfileLinkWriter {

    /**
     * Writes link records so they match [links]: unchanged links keep their record,
     * edited ones are rewritten in place, and new ones are created. Links that aren't
     * usable web links, are shorteners, or repeat an earlier link are dropped.
     *
     * Care is taken to ensure that Link records are written first, bc the profile
     * can only reference records that exist. Likewise, removed records are deleted only
     * after being dereferenced from a profile.
     *
     * Method throws if a record can't be written.
     */
    suspend fun write(
        profileId: ProfileId,
        links: List<Profile.Update.Link>,
        existingRefs: List<StrongRef>,
    ): ProfileLinkWrites

    suspend fun deleteRemoved(
        profileId: ProfileId,
        writes: ProfileLinkWrites,
    )
}

@Inject
internal class AtProtoProfileLinkWriter(
    private val networkService: NetworkService,
    private val httpClient: HttpClient,
) : ProfileLinkWriter {

    override suspend fun write(
        profileId: ProfileId,
        links: List<Profile.Update.Link>,
        existingRefs: List<StrongRef>,
    ): ProfileLinkWrites = coroutineScope {
        val repo = Did(profileId.id)
        // Only records the profile already points at count as existing, so an
        // update can't rewrite a record it doesn't own.
        val existingRefsByUri = existingRefs.associateBy { it.uri.atUri }

        val seenUrls = mutableSetOf<GenericUri>()
        val acceptedLinks = links.mapNotNull { link ->
            val url = normalizeProfileLinkUrl(link.url.uri)
                ?.takeIf(GenericUri::isShowableProfileLink)
                ?.takeIf(seenUrls::add)
                ?: return@mapNotNull null
            AcceptedLink(
                existingRef = link.uri?.uri?.let(existingRefsByUri::get),
                url = url,
                title = link.title.normalizedTitle(),
            )
        }
            .take(ProfileLinks.MAX_LINKS)

        val refs = acceptedLinks
            .map { link ->
                async {
                    writeLink(
                        repo = repo,
                        link = link,
                    )
                }
            }
            .awaitAll()
            .filterNotNull()

        val keptUris = refs.mapTo(mutableSetOf()) { it.uri.atUri }
        ProfileLinkWrites(
            refs = refs,
            removed = existingRefs
                .filterNot { it.uri.atUri in keptUris }
                .map { ProfileLinkUri(it.uri.atUri) },
        )
    }

    override suspend fun deleteRemoved(
        profileId: ProfileId,
        writes: ProfileLinkWrites,
    ) = coroutineScope {
        writes.removed
            .map { uri ->
                async {
                    networkService.runCatchingWithMonitoredNetworkRetry {
                        deleteRecord(
                            DeleteRecordRequest(
                                repo = Did(profileId.id),
                                collection = Nsid(ProfileLinkUri.NAMESPACE),
                                rkey = RKey(uri.recordKey.value),
                            ),
                        )
                    }.onFailure { throwable ->
                        logcat(LogPriority.WARN) {
                            "Failed to delete profile link $uri. Cause: ${throwable.loggableText()}"
                        }
                    }
                }
            }
            .awaitAll()
        Unit
    }

    private suspend fun writeLink(
        repo: Did,
        link: AcceptedLink,
    ): StrongRef? {
        val existing = link.existingRef?.let { ref ->
            existingLink(
                repo = repo,
                ref = ref,
            )
        }
        val isSameDestination = existing != null &&
            normalizeProfileLinkUrl(existing.record.url.uri) == link.url

        if (existing != null && isSameDestination && existing.record.title.normalizedTitle() == link.title) {
            return existing.ref
        }

        // Unlike the other checks, only a new destination needs a safelink lookup.
        if (!isSameDestination && isSafelinkBlocked(link.url)) {
            logcat(LogPriority.WARN) {
                "Dropping profile link to ${link.url}; it's blocked by safelink."
            }
            // An edit to a blocked destination keeps the link as it was
            return existing?.ref
        }

        // Fetch the icon on every write, so an edit picks up a favicon the site has
        // changed. If that fails, an edit to the same destination keeps its icon.
        val icon = uploadFavicon(link.url)
            ?: existing?.record?.icon?.takeIf { isSameDestination }

        val record = BskyLink(
            url = LexiconUri(link.url.uri),
            title = link.title,
            icon = icon,
            createdAt = existing?.record?.createdAt ?: Clock.System.now(),
            labels = existing?.record?.labels?.takeIf { isSameDestination },
        ).asJsonContent(BskyLink.serializer())

        val existingRef = existing?.ref
        return if (existingRef != null) networkService.runCatchingWithMonitoredNetworkRetry {
            // An edit rewrites the same record, so a report on it still points here
            putRecord(
                PutRecordRequest(
                    repo = repo,
                    collection = Nsid(ProfileLinkUri.NAMESPACE),
                    rkey = RKey(ProfileLinkUri(existingRef.uri.atUri).recordKey.value),
                    record = record,
                ),
            )
        }
            .map { StrongRef(uri = it.uri, cid = it.cid) }
            .getOrThrow()
        else networkService.runCatchingWithMonitoredNetworkRetry {
            createRecord(
                CreateRecordRequest(
                    repo = repo,
                    collection = Nsid(ProfileLinkUri.NAMESPACE),
                    record = record,
                ),
            )
        }
            .map { StrongRef(uri = it.uri, cid = it.cid) }
            .getOrThrow()
    }

    /**
     * The link record [ref] points at, or null if it was since deleted.
     */
    private suspend fun existingLink(
        repo: Did,
        ref: StrongRef,
    ): ExistingLink? = networkService.runCatchingWithMonitoredNetworkRetry {
        getRecord(
            GetRecordQueryParams(
                repo = repo,
                collection = Nsid(ProfileLinkUri.NAMESPACE),
                rkey = RKey(ProfileLinkUri(ref.uri.atUri).recordKey.value),
            ),
        )
    }
        .map { response ->
            response.value.safeDecodeAs<BskyLink>()?.let { record ->
                ExistingLink(
                    ref = StrongRef(
                        uri = response.uri,
                        cid = response.cid ?: ref.cid,
                    ),
                    record = record,
                )
            }
        }
        .getOrElse { throwable ->
            if (throwable.isNotFound()) null
            else throw throwable
        }

    /**
     * Fetches the site's favicon through cardyb and uploads it, so it's scanned like
     * any other profile image instead of hotlinked. Null when the site has no usable
     * icon, which is a normal outcome.
     */
    private suspend fun uploadFavicon(
        url: GenericUri,
    ): Blob? {
        val iconBytes = withTimeoutOrNull(FaviconTimeout) {
            runCatchingUnlessCancelled {
                val response = httpClient.get(CardyFaviconUrl) {
                    parameter("url", url.uri)
                    // Skip cardyb's cache, so the site's current icon is used
                    parameter("fresh", true)
                    header(HttpHeaders.CacheControl, "no-store")
                }
                val isPng = response.contentType()?.withoutParameters() == ContentType.Image.PNG
                if (response.status.isSuccess() && isPng) response.readRawBytes()
                else null
            }.getOrNull()
        }
            ?.takeIf { it.size <= MaxIconBytes }
            ?: return null

        return networkService.runCatchingWithMonitoredNetworkRetry {
            uploadBlob(ByteReadChannel(iconBytes))
        }
            .onFailure { throwable ->
                logcat(LogPriority.WARN) {
                    "Failed to upload profile link icon. Cause: ${throwable.loggableText()}"
                }
            }
            .getOrNull()
            ?.blob
    }

    /**
     * Checks [url] against Bluesky's safelink rules, which trust and safety manage.
     * Fails open: link taps are checked too, so a missing answer shouldn't stop a save.
     */
    private suspend fun isSafelinkBlocked(
        url: GenericUri,
    ): Boolean = withTimeoutOrNull(SafelinkTimeout) {
        runCatchingUnlessCancelled {
            val response = httpClient.get(SafelinkCheckUrl) {
                parameter("u", url.uri)
            }
            response.status.isSuccess() &&
                response.body<SafelinkCheckResponse>().action == SafelinkBlockAction
        }.getOrNull()
    } ?: false
}

private data class AcceptedLink(
    val existingRef: StrongRef?,
    val url: GenericUri,
    val title: String?,
)

private data class ExistingLink(
    val ref: StrongRef,
    val record: BskyLink,
)

@Serializable
private data class SafelinkCheckResponse(
    val action: String? = null,
)

private fun String?.normalizedTitle(): String? =
    this?.trim()?.takeIf(String::isNotEmpty)

private const val CardyFaviconUrl = "https://cardyb.bsky.app/v1/favicon"
private const val SafelinkCheckUrl = "https://go.bsky.app/safelink/check"
private const val SafelinkBlockAction = "block"

// The lexicon's limit for a link icon
private const val MaxIconBytes = 100_000

private val FaviconTimeout = 8.seconds
private val SafelinkTimeout = 5.seconds

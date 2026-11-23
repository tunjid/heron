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

import com.atproto.repo.GetRecordQueryParams
import com.atproto.repo.PutRecordRequest
import com.tunjid.heron.data.core.models.RecordPostReference
import com.tunjid.heron.data.core.types.EventUri
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.core.types.StandardDocumentUri
import com.tunjid.heron.data.core.types.profileId
import com.tunjid.heron.data.core.types.recordKey
import com.tunjid.heron.data.core.types.requireCollection
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.utilities.edit
import com.tunjid.heron.data.utilities.mapCatchingUnlessCancelled
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import sh.christian.ozone.api.Cid
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Nsid
import sh.christian.ozone.api.RKey

internal suspend fun NetworkService.editRecord(
    uri: RecordUri,
    transform: JsonObject.() -> JsonObject,
): Result<Cid> {
    val repo = Did(uri.profileId().id)
    val collection = Nsid(uri.requireCollection())
    val rkey = RKey(uri.recordKey.value)
    return runCatchingWithMonitoredNetworkRetry {
        getRecord(
            GetRecordQueryParams(
                repo = repo,
                collection = collection,
                rkey = rkey,
            ),
        )
    }
        .mapCatchingUnlessCancelled { current ->
            val record = current.value.edit(transform)
            val response = runCatchingWithMonitoredNetworkRetry {
                putRecord(
                    PutRecordRequest(
                        repo = repo,
                        collection = collection,
                        rkey = rkey,
                        swapRecord = current.cid,
                        record = record,
                    ),
                )
            }.getOrThrow()
            response.cid
        }
}

internal fun JsonObject.withBskyPostRef(
    reference: RecordPostReference,
): JsonObject = when (reference) {
    is RecordPostReference.Unlink -> JsonObject(this - BskyPostRefKey)
    is RecordPostReference.Link -> JsonObject(
        this + Pair(
            BskyPostRefKey,
            buildJsonObject {
                put("uri", reference.postUri.uri)
                put("cid", reference.postCid.id)
                when (reference.recordUri) {
                    // atmo.rsvp only renders an event's comments when asked to; keep the
                    // author's existing choice when moving the reference to another post.
                    is EventUri -> put(
                        key = "showComments",
                        value = (this@withBskyPostRef[BskyPostRefKey] as? JsonObject)
                            ?.get("showComments")
                            ?.jsonPrimitive
                            ?.booleanOrNull
                            ?: true,
                    )
                    is StandardDocumentUri -> Unit
                }
            },
        ),
    )
}

private const val BskyPostRefKey = "bskyPostRef"

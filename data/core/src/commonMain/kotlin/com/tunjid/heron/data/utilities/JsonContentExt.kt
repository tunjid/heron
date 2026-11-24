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

package com.tunjid.heron.data.utilities

import com.tunjid.heron.data.network.BlueskyJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import sh.christian.ozone.api.model.JsonContent

internal fun <T> T.asJsonContent(
    serializer: KSerializer<T>,
): JsonContent = BlueskyJson.decodeFromString(
    BlueskyJson.encodeToString(serializer, this),
)

internal inline fun <reified T : Any> JsonContent.safeDecodeAs(): T? =
    try {
        decodeAs<T>()
    } catch (_: kotlinx.serialization.SerializationException) {
        null
    }

internal fun JsonContent.add(
    builderAction: JsonObjectBuilder.() -> Unit,
): JsonContent = edit {
    JsonObject(this + buildJsonObject(builderAction))
}

internal expect fun JsonContent.edit(
    transform: JsonObject.() -> JsonObject,
): JsonContent

internal expect fun <T> JsonContent.read(
    reader: JsonObject.() -> T,
): T

/**
 * Reads the value of [key] from the underlying JSON object as a [String], or `null`
 * if the key is absent, not a primitive, or an explicit JSON `null`.
 */
internal fun JsonContent.stringOrNull(
    key: String,
): String? = read { (this[key] as? JsonPrimitive)?.contentOrNull }

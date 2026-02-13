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

package com.tunjid.heron.data.graze.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

open class LeafFilterSerializer<T : Any>(
    tSerializer: KSerializer<T>,
    private val serialName: String = tSerializer.descriptor.serialName,
) : JsonTransformingSerializer<T>(tSerializer) {

    override fun transformSerialize(element: JsonElement): JsonElement {
        require(element is JsonObject)
        return buildJsonObject {
            put(serialName, buildJsonArray {
                element.values.forEach { add(it) }
            })
        }
    }

    override fun transformDeserialize(element: JsonElement): JsonElement {
        require(element is JsonObject)
        val array = element[serialName]?.jsonArray
            ?: throw IllegalArgumentException("Expected key '$serialName' not found")

        val descriptor = descriptor
        require(array.size == descriptor.elementsCount) {
            "Expected ${descriptor.elementsCount} elements for $serialName but found ${array.size}"
        }

        return buildJsonObject {
            for (i in 0 until descriptor.elementsCount) {
                put(descriptor.getElementName(i), array[i])
            }
        }
    }
}

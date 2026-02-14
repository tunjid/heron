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

import com.tunjid.heron.data.graze.Filter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

object LeafSerializer : KSerializer<Filter.Leaf> {
    private val delegate = PolymorphicSerializer(Filter.Leaf::class)
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: Filter.Leaf) {
        val output = encoder as? JsonEncoder ?: run {
            delegate.serialize(encoder, value)
            return
        }

        // Find the registered serializer for the concrete type (e.g. Compare)
        // Since Filter.Leaf is a sealed interface, its subclasses are registered in the module
        val strategy = output.serializersModule.getPolymorphic(Filter.Leaf::class, value)
            ?: throw IllegalStateException("No serializer found for ${value::class}")

        @Suppress("UNCHECKED_CAST")
        val castStrategy = strategy as KSerializer<Filter.Leaf>

        // Serialize to a JSON Object (default behavior of generated serializer)
        val jsonObject = output.json.encodeToJsonElement(castStrategy, value).jsonObject

        // Transform the JSON Object values into a JSON Array
        // We use the descriptor of the strategy to ensure correct order
        val jsonArray = buildJsonArray {
            val descriptor = castStrategy.descriptor
            for (i in 0 until descriptor.elementsCount) {
                val name = descriptor.getElementName(i)
                if (name in jsonObject) {
                    add(jsonObject[name]!!)
                }
            }
        }

        // Wrap in the final structure: { "serial_name": [ ... ] }
        val serialName = castStrategy.descriptor.serialName
        val finalJson = buildJsonObject {
            put(serialName, jsonArray)
        }

        output.encodeJsonElement(finalJson)
    }

    override fun deserialize(decoder: Decoder): Filter.Leaf {
        val input = decoder as? JsonDecoder ?: return delegate.deserialize(decoder)

        val tree = input.decodeJsonElement().jsonObject
        // The structure should be { "serial_name": [ ... ] }
        val serialName = tree.keys.firstOrNull()
            ?: throw IllegalStateException("Empty JSON object for Leaf filter")
        val valuesArray = tree[serialName]?.jsonArray
            ?: throw IllegalStateException("Expected JSON Array for key '$serialName'")

        // Find the serializer by serial name
        val strategy = input.serializersModule.getPolymorphic(Filter.Leaf::class, serialName)
            ?: throw IllegalStateException("No serializer found for serial name '$serialName'")

        @Suppress("UNCHECKED_CAST")
        val castStrategy = strategy as KSerializer<Filter.Leaf>

        // Transform the Array back to a keyed Object expected by the generated serializer
        val descriptor = castStrategy.descriptor
        val jsonObject = buildJsonObject {
            var arrayIndex = 0
            for (i in 0 until descriptor.elementsCount) {
                if (arrayIndex < valuesArray.size) {
                    put(descriptor.getElementName(i), valuesArray[arrayIndex])
                    arrayIndex++
                }
            }
        }

        return input.json.decodeFromJsonElement(castStrategy, jsonObject)
    }
}

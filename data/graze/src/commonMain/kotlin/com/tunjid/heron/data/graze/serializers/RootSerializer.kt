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
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject

object RootSerializer : KSerializer<Filter.Root> {
    private val delegate = PolymorphicSerializer(Filter.Root::class)
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: Filter.Root) {
        if (encoder is JsonEncoder) {
            val json = encoder.json
            val filtersElement = json.encodeToJsonElement(
                ListSerializer(Filter.serializer()),
                value.filters,
            )
            val jsonObject = buildJsonObject {
                when (value) {
                    is Filter.And -> put("and", filtersElement)
                    is Filter.Or -> put("or", filtersElement)
                }
            }
            encoder.encodeJsonElement(jsonObject)
        } else {
            delegate.serialize(encoder, value)
        }
    }

    override fun deserialize(decoder: Decoder): Filter.Root {
        if (decoder is JsonDecoder) {
            val jsonObject = decoder.decodeJsonElement().jsonObject

            return when {
                "and" in jsonObject -> {
                    val filters = decoder.json.decodeFromJsonElement(
                        ListSerializer(Filter.serializer()),
                        jsonObject["and"]!!,
                    )
                    Filter.And(filters = filters)
                }
                "or" in jsonObject -> {
                    val filters = decoder.json.decodeFromJsonElement(
                        ListSerializer(Filter.serializer()),
                        jsonObject["or"]!!,
                    )
                    Filter.Or(filters = filters)
                }
                else -> throw SerializationException("Expected 'and' or 'or' key for Filter.Root")
            }
        } else {
            return delegate.deserialize(decoder)
        }
    }
}

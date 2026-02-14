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
import kotlinx.serialization.json.jsonObject

object FilterSerializer : KSerializer<Filter> {
    private val delegate = PolymorphicSerializer(Filter::class)
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: Filter) {
        if (encoder is JsonEncoder) {
            when (value) {
                is Filter.Root -> encoder.json.encodeToJsonElement(RootSerializer, value)
                is Filter.Leaf -> encoder.json.encodeToJsonElement(LeafSerializer, value)
            }
                .let { encoder.encodeJsonElement(it) }
        } else {
            delegate.serialize(encoder, value)
        }
    }

    override fun deserialize(decoder: Decoder): Filter {
        if (decoder is JsonDecoder) {
            val tree = decoder.decodeJsonElement()
            val jsonObject = tree.jsonObject

            return if ("and" in jsonObject || "or" in jsonObject) {
                decoder.json.decodeFromJsonElement(RootSerializer, tree)
            } else {
                decoder.json.decodeFromJsonElement(LeafSerializer, tree)
            }
        } else {
            return delegate.deserialize(decoder)
        }
    }
}

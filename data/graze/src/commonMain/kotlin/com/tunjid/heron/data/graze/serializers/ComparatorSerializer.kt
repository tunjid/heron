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
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Custom serializer that uses the `value` property for both serialization and deserialization. This
 * ensures a single source of truth for the operator strings.
 */
object ComparatorSerializer : KSerializer<Filter.Comparator> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Comparator", PrimitiveKind.STRING)

    // Pre-aggregated list of all possible comparators to avoid recreating it on every deserialize
    // call
    private val allComparators: List<Filter.Comparator> =
        Filter.Comparator.Equality.entries +
            Filter.Comparator.Range.entries +
            Filter.Comparator.Set.entries

    override fun serialize(encoder: Encoder, value: Filter.Comparator) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): Filter.Comparator {
        val decoded = decoder.decodeString()
        return allComparators.find { it.value == decoded }
            ?: throw IllegalArgumentException("Unknown comparator: $decoded")
    }
}

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

package com.tunjid.heron.data.core.models

import kotlin.io.encoding.Base64
import kotlin.io.encoding.Base64.PaddingOption
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray

/**
 * A declaration for compile time known Model type that can be serialized as bytes and encoded
 * in a url as a base 64 string
 *
 * Currently does this using CBOR; Protobufs were evaluated but they're too terse.
 */
@Serializable
sealed interface UrlEncodableModel

inline fun <reified T : UrlEncodableModel> T.toCborCompatibleBytes(): ByteArray =
    ModelSerializerFormat.encodeToByteArray(value = this)

inline fun <reified T : UrlEncodableModel> String.fromBase64EncodedUrl(): T =
    ModelSerializerFormat.decodeFromByteArray(ModelUrlSafeBase64.decode(this))

inline fun <reified T : UrlEncodableModel> T.toUrlEncodedBase64(): String =
    ModelUrlSafeBase64.encode(toCborCompatibleBytes())

val ModelSerializerFormat: BinaryFormat = Cbor {
    ignoreUnknownKeys = true
}

val ModelUrlSafeBase64 = Base64.UrlSafe.withPadding(
    option = PaddingOption.ABSENT,
)

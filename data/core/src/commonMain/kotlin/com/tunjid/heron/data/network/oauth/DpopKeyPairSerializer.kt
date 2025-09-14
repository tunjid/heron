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

package com.tunjid.heron.data.network.oauth

import io.ktor.util.decodeBase64Bytes
import io.ktor.util.encodeBase64
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object JwkDpopKeyPairSerializer : DpopKeyPairSerializer(
    publicKeyFormat = DpopKeyPair.PublicKeyFormat.JWK,
    privateKeyFormat = DpopKeyPair.PrivateKeyFormat.JWK,
)

object RawDpopKeyPairSerializer : DpopKeyPairSerializer(
    publicKeyFormat = DpopKeyPair.PublicKeyFormat.RAW,
    privateKeyFormat = DpopKeyPair.PrivateKeyFormat.RAW,
)

object PemDpopKeyPairSerializer : DpopKeyPairSerializer(
    publicKeyFormat = DpopKeyPair.PublicKeyFormat.PEM,
    privateKeyFormat = DpopKeyPair.PrivateKeyFormat.PEM,
)

object DerDpopKeyPairSerializer : DpopKeyPairSerializer(
    publicKeyFormat = DpopKeyPair.PublicKeyFormat.DER,
    privateKeyFormat = DpopKeyPair.PrivateKeyFormat.DER,
)

abstract class DpopKeyPairSerializer(
    val publicKeyFormat: DpopKeyPair.PublicKeyFormat,
    val privateKeyFormat: DpopKeyPair.PrivateKeyFormat,
) : KSerializer<DpopKeyPair> {
    private val stringListSerializer = ListSerializer(String.serializer())

    override val descriptor: SerialDescriptor = stringListSerializer.descriptor

    override fun serialize(encoder: Encoder, value: DpopKeyPair) {
        val publicKeyRaw: String = value.publicKeyBlocking(publicKeyFormat).encodeBase64()
        val privateKeyRaw: String = value.privateKeyBlocking(privateKeyFormat).encodeBase64()

        // Encode the two strings as a list.
        encoder.encodeSerializableValue(stringListSerializer, listOf(publicKeyRaw, privateKeyRaw))
    }

    override fun deserialize(decoder: Decoder): DpopKeyPair {
        // Decode the list of two strings.
        val keyPair: List<String> = decoder.decodeSerializableValue(stringListSerializer)

        // Ensure we have exactly two elements (public and private keys).
        require(keyPair.size == 2) { "Expected an array of two strings for serialized DpopKeyPair." }

        val publicKeyRaw = keyPair[0]
        val privateKeyRaw = keyPair[1]

        // Convert formatted key strings back to ByteArray
        val publicKeyBytes: ByteArray = publicKeyRaw.decodeBase64Bytes()
        val privateKeyBytes: ByteArray = privateKeyRaw.decodeBase64Bytes()

        return DpopKeyPair.fromKeyPairBlocking(
            publicKey = publicKeyBytes,
            publicKeyFormat = publicKeyFormat,
            privateKey = privateKeyBytes,
            privateKeyFormat = privateKeyFormat,
        )
    }
}

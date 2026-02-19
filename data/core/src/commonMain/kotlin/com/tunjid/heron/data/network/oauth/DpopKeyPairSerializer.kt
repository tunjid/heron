/*
 *    MIT License
 *
 *    Copyright (c) 2023 Christian De Angelis
 *
 *    Permission is hereby granted, free of charge, to any person obtaining a copy
 *    of this software and associated documentation files (the "Software"), to deal
 *    in the Software without restriction, including without limitation the rights
 *    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *    copies of the Software, and to permit persons to whom the Software is
 *    furnished to do so, subject to the following conditions:
 *
 *    The above copyright notice and this permission notice shall be included in all
 *    copies or substantial portions of the Software.
 *
 *    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *    SOFTWARE.
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

object JwkDpopKeyPairSerializer :
    DpopKeyPairSerializer(
        publicKeyFormat = DpopKeyPair.PublicKeyFormat.JWK,
        privateKeyFormat = DpopKeyPair.PrivateKeyFormat.JWK,
    )

object RawDpopKeyPairSerializer :
    DpopKeyPairSerializer(
        publicKeyFormat = DpopKeyPair.PublicKeyFormat.RAW,
        privateKeyFormat = DpopKeyPair.PrivateKeyFormat.RAW,
    )

object PemDpopKeyPairSerializer :
    DpopKeyPairSerializer(
        publicKeyFormat = DpopKeyPair.PublicKeyFormat.PEM,
        privateKeyFormat = DpopKeyPair.PrivateKeyFormat.PEM,
    )

object DerDpopKeyPairSerializer :
    DpopKeyPairSerializer(
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
        require(keyPair.size == 2) {
            "Expected an array of two strings for serialized DpopKeyPair."
        }

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

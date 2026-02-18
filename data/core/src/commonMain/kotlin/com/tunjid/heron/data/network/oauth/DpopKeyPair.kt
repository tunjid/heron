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

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.CryptographyProviderApi
import dev.whyoleg.cryptography.algorithms.EC
import dev.whyoleg.cryptography.algorithms.EC.Curve.Companion.P256
import dev.whyoleg.cryptography.algorithms.ECDSA
import kotlinx.serialization.Serializable

/**
 * Represents a DPoP (Demonstrating Proof of Possession) key pair, which consists of a public and
 * private key pair.
 *
 * This key pair is used to sign DPoP headers for OAuth requests, ensuring that the request is made
 * by the holder of the private key (ie: this application instance). DPoP keys are also used to
 * authorize requests made to an atproto server like Bluesky.
 *
 * Keys can be imported or exported in various formats depending on the target platform currently
 * being used:
 * - **JS**: Does not support [publicKeyBlocking] or [privateKeyBlocking] methods but supports all
 *   key formats.
 * - **JVM**: Does not support JWK for importing or exporting keys.
 * - **Android**: Does not support JWK for importing or exporting keys.
 * - **iOS**: Does not support JWK for importing or exporting keys, and does not support RAW for
 *   importing keys.
 */
@Serializable(with = DerDpopKeyPairSerializer::class)
class DpopKeyPair internal constructor(internal val keyPair: ECDSA.KeyPair) {
    /** Export the public key of the DPoP keypair to a byte array using the specified format. */
    suspend fun publicKey(format: PublicKeyFormat): ByteArray =
        keyPair.publicKey.encodeToByteArray(format.format)

    /**
     * Export the public key of the DPoP keypair to a byte array using the specified format.
     *
     * **NOT** compatible with JS targets.
     */
    fun publicKeyBlocking(format: PublicKeyFormat): ByteArray =
        keyPair.publicKey.encodeToByteArrayBlocking(format.format)

    /** Export the private key of the DPoP keypair to a byte array using the specified format. */
    suspend fun privateKey(format: PrivateKeyFormat): ByteArray =
        keyPair.privateKey.encodeToByteArray(format.format)

    /**
     * Export the private key of the DPoP keypair to a byte array using the specified format.
     *
     * **NOT** compatible with JS targets.
     */
    fun privateKeyBlocking(format: PrivateKeyFormat): ByteArray =
        keyPair.privateKey.encodeToByteArrayBlocking(format.format)

    enum class PublicKeyFormat {
        JWK,
        RAW,
        PEM,
        DER;

        internal val format: EC.PublicKey.Format
            get() =
                when (this) {
                    JWK -> EC.PublicKey.Format.JWK
                    RAW -> EC.PublicKey.Format.RAW
                    PEM -> EC.PublicKey.Format.PEM
                    DER -> EC.PublicKey.Format.DER
                }
    }

    enum class PrivateKeyFormat {
        JWK,
        RAW,
        DER,
        DER_SEC1,
        PEM,
        PEM_SEC1;

        internal val format: EC.PrivateKey.Format
            get() =
                when (this) {
                    JWK -> EC.PrivateKey.Format.JWK
                    RAW -> EC.PrivateKey.Format.RAW
                    DER -> EC.PrivateKey.Format.DER
                    DER_SEC1 -> EC.PrivateKey.Format.DER.SEC1
                    PEM -> EC.PrivateKey.Format.PEM
                    PEM_SEC1 -> EC.PrivateKey.Format.PEM.SEC1
                }
    }

    companion object {
        /**
         * Creates a DPoP key pair from the provided public and private keys in the specified
         * formats.
         */
        suspend fun fromKeyPair(
            publicKey: ByteArray,
            publicKeyFormat: PublicKeyFormat,
            privateKey: ByteArray,
            privateKeyFormat: PrivateKeyFormat,
        ): DpopKeyPair {
            val ecdsa: ECDSA = CryptographyProvider.Default.get(ECDSA)
            val decodedPublicKey =
                ecdsa.publicKeyDecoder(P256).decodeFromByteArray(publicKeyFormat.format, publicKey)
            val decodedPrivateKey =
                ecdsa
                    .privateKeyDecoder(P256)
                    .decodeFromByteArray(privateKeyFormat.format, privateKey)

            val dpopKeyPair =
                SimpleKeyPair(publicKey = decodedPublicKey, privateKey = decodedPrivateKey)

            return DpopKeyPair(dpopKeyPair)
        }

        /**
         * Creates a DPoP key pair from the provided public and private keys in the specified
         * formats.
         *
         * **NOT** compatible with JS targets.
         */
        fun fromKeyPairBlocking(
            publicKey: ByteArray,
            publicKeyFormat: PublicKeyFormat,
            privateKey: ByteArray,
            privateKeyFormat: PrivateKeyFormat,
        ): DpopKeyPair {
            val ecdsa: ECDSA = CryptographyProvider.Default.get(ECDSA)
            val decodedPublicKey =
                ecdsa
                    .publicKeyDecoder(P256)
                    .decodeFromByteArrayBlocking(publicKeyFormat.format, publicKey)
            val decodedPrivateKey =
                ecdsa
                    .privateKeyDecoder(P256)
                    .decodeFromByteArrayBlocking(privateKeyFormat.format, privateKey)

            val dpopKeyPair =
                SimpleKeyPair(publicKey = decodedPublicKey, privateKey = decodedPrivateKey)

            return DpopKeyPair(dpopKeyPair)
        }

        /** Generates a brand new DPoP key pair using the P-256 curve. */
        suspend fun generateKeyPair(): DpopKeyPair {
            val keyPair =
                CryptographyProvider.Default.get(ECDSA).keyPairGenerator(P256).generateKey()
            return DpopKeyPair(SimpleKeyPair(keyPair.publicKey, keyPair.privateKey))
        }
    }

    @OptIn(CryptographyProviderApi::class)
    internal class SimpleKeyPair(
        override val publicKey: ECDSA.PublicKey,
        override val privateKey: ECDSA.PrivateKey,
    ) : ECDSA.KeyPair
}

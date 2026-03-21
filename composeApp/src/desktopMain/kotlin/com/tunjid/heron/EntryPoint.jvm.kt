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

package com.tunjid.heron

import com.google.crypto.tink.Aead
import com.google.crypto.tink.InsecureSecretKeyAccess
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.TinkJsonProtoKeysetFormat
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import com.tunjid.heron.data.database.getDatabaseBuilder
import com.tunjid.heron.data.di.DataBindingArgs
import com.tunjid.heron.data.logging.JvmLogger
import com.tunjid.heron.data.platform.JVMPlatform
import com.tunjid.heron.data.platform.JvmVariant
import com.tunjid.heron.data.platform.Platform
import com.tunjid.heron.data.platform.current
import com.tunjid.heron.data.repository.SavedStateEncryption
import com.tunjid.heron.images.imageLoader
import com.tunjid.heron.media.video.javafx.JavaFxPlayerController
import com.tunjid.heron.media.video.linux.GStreamerPlayerController
import com.tunjid.heron.media.video.mac.AVFoundationPlayerController
import com.tunjid.heron.scaffold.notifications.NoOpNotifier
import com.tunjid.heron.scaffold.scaffold.AppState
import dev.jordond.connectivity.Connectivity
import java.io.File
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath

fun createAppState(): AppState =
    createAppState(
        imageLoader = ::imageLoader,
        notifier = {
            NoOpNotifier
        },
        logger = {
            JvmLogger()
        },
        videoPlayerController = { appMainScope ->
            when ((Platform.current as JVMPlatform).variant) {
                JvmVariant.Mac -> AVFoundationPlayerController(
                    appMainScope = appMainScope,
                )
                JvmVariant.Linux -> GStreamerPlayerController(
                    appMainScope = appMainScope,
                )
                else -> JavaFxPlayerController(
                    appMainScope = appMainScope,
                )
            }
        },
        args = { appMainScope ->
            val appDataDir = appDataDirectory()
            DataBindingArgs(
                appMainScope = appMainScope,
                connectivity = Connectivity(),
                savedStatePath = File(
                    appDataDir,
                    SAVED_STATE_FILE_NAME,
                ).toOkioPath(),
                savedStateFileSystem = FileSystem.SYSTEM,
                savedStateEncryption = tinkEncryption(appDataDir),
                databaseBuilder = getDatabaseBuilder(),
            )
        },
    )

private fun appDataDirectory(): File {
    val platform = Platform.current as JVMPlatform
    val userHome = System.getProperty(USER_HOME_PROPERTY)
    val dir = when (platform.variant) {
        JvmVariant.Mac -> File(
            userHome,
            "$MAC_APP_SUPPORT_PATH/$APP_NAME",
        )
        JvmVariant.Windows -> File(
            System.getenv(APPDATA_ENV) ?: userHome,
            APP_NAME,
        )
        JvmVariant.Linux,
        JvmVariant.Unknown,
        ->
            File(
                System.getenv(XDG_DATA_HOME_ENV)
                    ?: "$userHome/$LINUX_DEFAULT_DATA_PATH",
                APP_NAME,
            )
    }
    dir.mkdirs()
    return dir
}

// TODO: For production, replace machine-derived KEK with OS credential store
//  (macOS Keychain, Windows Credential Manager, Linux Secret Service) via java-keyring
//  or similar. The current approach derives the key-encryption-key from machine-specific
//  data, which prevents casual file copying but is not resistant to targeted attacks
//  since the derivation logic is visible in the open-source code.
private fun tinkEncryption(appDataDir: File): SavedStateEncryption {
    AeadConfig.register()
    val kek = deriveKeyEncryptionKey()
    val keysetFile = File(appDataDir, KEYSTORE_FILE_NAME)
    val keysetHandle = loadOrCreateKeyset(kek, keysetFile)
    val aead = keysetHandle.getPrimitive(Aead::class.java)
    return object : SavedStateEncryption {
        override fun encrypt(data: ByteArray): ByteArray =
            aead.encrypt(data, ASSOCIATED_DATA)

        override fun decrypt(data: ByteArray): ByteArray =
            aead.decrypt(data, ASSOCIATED_DATA)
    }
}

/**
 * Loads an existing Tink keyset or creates a new one. If the existing keyset cannot be
 * decrypted (e.g., KEK changed due to hardware/user changes, or file corruption),
 * the old keyset is deleted and a fresh one is generated. This means the user will
 * need to re-authenticate, but the app will not crash.
 */
private fun loadOrCreateKeyset(
    kek: SecretKeySpec,
    keysetFile: File,
): KeysetHandle {
    if (keysetFile.exists()) {
        try {
            val encrypted = keysetFile.readBytes()
            val decrypted = decryptWithKek(kek, encrypted)
            return TinkJsonProtoKeysetFormat.parseKeyset(
                String(decrypted),
                InsecureSecretKeyAccess.get(),
            )
        } catch (_: Exception) {
            // KEK changed or keyset corrupted — delete and regenerate.
            // The saved state file will fail to decrypt with the new keyset,
            // which is handled gracefully in VersionedSavedStateOkioSerializer.
            keysetFile.delete()
        }
    }
    return KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM).also { handle ->
        val serialized = TinkJsonProtoKeysetFormat.serializeKeyset(
            handle,
            InsecureSecretKeyAccess.get(),
        )
        keysetFile.writeBytes(encryptWithKek(kek, serialized.toByteArray()))
    }
}

/**
 * Derives a key-encryption-key from stable machine-specific data
 * (username, home directory).
 *
 * This is a alpha-level measure that prevents the Tink keyset from being stored in plaintext.
 * It stops casual file copying and opportunistic scanning, but a targeted attacker with
 * access to this source code and the same machine can reproduce the key.
 *
 * TODO: For production, replace with OS credential store (macOS Keychain, Windows Credential
 *  Manager, Linux Secret Service) via java-keyring or similar.
 */
private fun deriveKeyEncryptionKey(): SecretKeySpec {
    val machineIdentity = buildString {
        append(System.getProperty(USER_NAME_PROPERTY))
        append(System.getProperty(USER_HOME_PROPERTY))
    }
    val factory = SecretKeyFactory.getInstance(KEK_ALGORITHM)
    val spec = PBEKeySpec(
        machineIdentity.toCharArray(),
        KEK_SALT,
        KEK_ITERATIONS,
        KEK_KEY_LENGTH_BITS,
    )
    val keyBytes = factory.generateSecret(spec).encoded
    return SecretKeySpec(keyBytes, AES_ALGORITHM)
}

private fun encryptWithKek(
    kek: SecretKeySpec,
    data: ByteArray,
): ByteArray {
    val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, kek)
    val iv = cipher.iv
    val encrypted = cipher.doFinal(data)
    // Prepend IV length (1 byte) + IV + ciphertext
    return byteArrayOf(iv.size.toByte()) + iv + encrypted
}

private fun decryptWithKek(
    kek: SecretKeySpec,
    data: ByteArray,
): ByteArray {
    val ivLength = data[0].toInt() and 0xFF
    val iv = data.sliceArray(1..ivLength)
    val ciphertext = data.sliceArray((1 + ivLength) until data.size)
    val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
    cipher.init(
        Cipher.DECRYPT_MODE,
        kek,
        GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv),
    )
    return cipher.doFinal(ciphertext)
}

private const val APP_NAME = "TunjiHeron"
private const val SAVED_STATE_FILE_NAME = "heron-saved-state.ser"
private const val KEYSTORE_FILE_NAME = ".keystore"
private const val MAC_APP_SUPPORT_PATH = "Library/Application Support"
private const val LINUX_DEFAULT_DATA_PATH = ".local/share"
private const val XDG_DATA_HOME_ENV = "XDG_DATA_HOME"
private const val APPDATA_ENV = "APPDATA"
private const val USER_HOME_PROPERTY = "user.home"
private const val USER_NAME_PROPERTY = "user.name"
private const val ASSOCIATED_DATA_STRING = "heron-saved-state"
private val ASSOCIATED_DATA = ASSOCIATED_DATA_STRING.toByteArray()

private const val KEK_ALGORITHM = "PBKDF2WithHmacSHA256"
private const val KEK_ITERATIONS = 100_000
private const val KEK_KEY_LENGTH_BITS = 256
private const val AES_ALGORITHM = "AES"
private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
private const val GCM_TAG_LENGTH_BITS = 128
private val KEK_SALT = "heron-desktop-kek".toByteArray()

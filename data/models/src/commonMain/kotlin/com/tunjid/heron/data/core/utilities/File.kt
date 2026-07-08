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

package com.tunjid.heron.data.core.utilities

import com.tunjid.heron.data.core.types.FileUri
import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

@Serializable
sealed interface File {
    val uri: FileUri

    @Serializable
    sealed class Media : File {

        // This is deliberately not a data class so the data class copy method is not leaked
        @Serializable
        class Photo(
            override val uri: FileUri,
            val width: Int,
            val height: Int,
            val altText: String? = null,
        ) : Media() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other == null || this::class != other::class) return false

                other as Photo

                if (width != other.width) return false
                if (height != other.height) return false
                if (uri != other.uri) return false
                if (altText != other.altText) return false

                return true
            }

            override fun hashCode(): Int {
                var result = width.hashCode()
                result = 31 * result + height.hashCode()
                result = 31 * result + uri.hashCode()
                result = 31 * result + altText.hashCode()
                return result
            }
        }

        // This is deliberately not a data class so the data class copy method is not leaked
        @Serializable
        class Video(
            override val uri: FileUri,
            val width: Int,
            val height: Int,
            val altText: String? = null,
        ) : Media() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other == null || this::class != other::class) return false

                other as Video

                if (width != other.width) return false
                if (height != other.height) return false
                if (uri != other.uri) return false
                if (altText != other.altText) return false

                return true
            }

            override fun hashCode(): Int {
                var result = width.hashCode()
                result = 31 * result + height.hashCode()
                result = 31 * result + uri.hashCode()
                result = 31 * result + (altText?.hashCode() ?: 0)
                return result
            }
        }
    }

    /**
     * A file on the device's own filesystem, addressed by its [relativePath]. Distinct from [Media],
     * which is user-picked content staged for upload. The okio `Path` is reconstructed from
     * [relativePath] at the I/O layer, so this type stays free of an okio dependency.
     */
    @Serializable
    @JvmInline
    value class System(
        val relativePath: String,
    ) : File {
        override val uri: FileUri
            get() = FileUri(relativePath)
    }
}

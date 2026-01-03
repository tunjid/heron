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

package com.tunjid.heron.data.files

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path

/**
 * A class representing limited access to a file on the OS.
 */
sealed class RestrictedFile {
    abstract val path: String?

    internal abstract val file: PlatformFile

    sealed class Media : RestrictedFile() {

        abstract val width: Int
        abstract val height: Int

        override val path: String?
            get() = file.path

        val hasSize
            get() = when (this) {
                is Photo -> width != 0 && height != 0
                is Video -> width != 0 && height != 0
            }

        // This is deliberately not a data class to only allow copies
        // with the the specific methods provided.
        class Photo(
            override val file: PlatformFile,
            override val width: Int = 0,
            override val height: Int = 0,
            val altText: String? = null,
        ) : Media() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other == null || this::class != other::class) return false

                other as Photo

                if (width != other.width) return false
                if (height != other.height) return false
                if (file != other.file) return false
                if (altText != other.altText) return false

                return true
            }

            override fun hashCode(): Int {
                var result = width
                result = 31 * result + height
                result = 31 * result + file.hashCode()
                result = 31 * result + (altText?.hashCode() ?: 0)
                return result
            }
        }

        // This is deliberately not a data class to only allow copies
        // with the the specific methods provided.
        class Video(
            override val file: PlatformFile,
            override val width: Int = 0,
            override val height: Int = 0,
            val altText: String? = null,
        ) : Media() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other == null || this::class != other::class) return false

                other as Video

                if (width != other.width) return false
                if (height != other.height) return false
                if (file != other.file) return false
                if (altText != other.altText) return false

                return true
            }

            override fun hashCode(): Int {
                var result = width
                result = 31 * result + height
                result = 31 * result + file.hashCode()
                result = 31 * result + (altText?.hashCode() ?: 0)
                return result
            }
        }

        fun withSize(
            width: Int,
            height: Int,
        ) = when (this) {
            is Photo -> Photo(
                file = file,
                width = width,
                height = height,
                altText = altText,
            )

            is Video -> Video(
                file = file,
                width = width,
                height = height,
                altText = altText,
            )
        }

        fun withAltText(
            altText: String?,
        ) = when (this) {
            is Photo -> Photo(
                file = file,
                width = width,
                height = height,
                altText = altText,
            )

            is Video -> Video(
                file = file,
                width = width,
                height = height,
                altText = altText,
            )
        }
    }
}

/**
 * A model used for displaying this photo in the UI.
 * The return type is deliberately [Any] not to leak the backing API
 */
val RestrictedFile.Media.Photo.uiDisplayModel: Any
    get() = file

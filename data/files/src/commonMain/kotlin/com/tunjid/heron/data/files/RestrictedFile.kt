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

        abstract val altText: String?

        override val path: String?
            get() = file.path

        sealed class Photo : Media() {

            internal data class File(
                override val file: PlatformFile,
                override val altText: String? = null,
            ) : Photo()
        }

        sealed class Video : Media() {

            internal data class File(
                override val file: PlatformFile,
                override val altText: String? = null,
            ) : Video()
        }

        fun withAltText(
            altText: String?,
        ) = when (this) {
            is Photo.File -> Photo.File(
                file = file,
                altText = altText,
            )

            is Video.File -> Video.File(
                file = file,
                altText = altText,
            )
        }
    }

    companion object {
        fun photo(
            file: PlatformFile,
        ): Media.Photo = Media.Photo.File(file)

        fun video(
            file: PlatformFile,
        ): Media.Video = Media.Video.File(file)
    }
}

/**
 * A model used for displaying this photo in the UI.
 * The return type is deliberately [Any] not to leak the backing API
 */
val RestrictedFile.Media.Photo.uiDisplayModel: Any
    get() = file

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

package com.tunjid.heron.media.picker

import androidx.compose.ui.unit.IntSize
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.readBytes

// On Android, a content resolver may be invoked for the path.
// Make sure it is only ever invoked off the main thread by enforcing with this class
sealed class MediaItem(
    val path: String?,
) {

    internal abstract val file: PlatformFile
    abstract val size: IntSize

    suspend fun readBytes() = file.readBytes()

    class Photo(
        override val file: PlatformFile,
        override val size: IntSize = IntSize.Zero,
    ) : MediaItem(file.path)

    class Video(
        override val file: PlatformFile,
        override val size: IntSize = IntSize.Zero,
    ) : MediaItem(file.path)

    fun updateSize(
        size: IntSize,
    ) = when (this) {
        is Photo -> Photo(
            file = file,
            size = size,
        )

        is Video -> Video(
            file = file,
            size = size,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as MediaItem

        if (path != other.path) return false
        if (size != other.size) return false

        return true
    }

    override fun hashCode(): Int {
        var result = path?.hashCode() ?: 0
        result = 31 * result + size.hashCode()
        return result
    }
}

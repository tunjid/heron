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

import android.graphics.BitmapFactory
import com.tunjid.heron.data.logging.logcat
import com.tunjid.heron.data.logging.loggableText
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.source
import kotlinx.io.asInputStream
import kotlinx.io.buffered

internal actual fun PlatformFile.imageDimensionsOrNull(): Dimensions? {
    // Reading the file through its source covers both the file and content Uri picks a
    // PlatformFile may wrap, so no ContentResolver is needed.
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    try {
        source().buffered().asInputStream().use { stream ->
            BitmapFactory.decodeStream(
                stream,
                null,
                bounds,
            )
        }
    } catch (e: Exception) {
        logcat { "Error reading image bounds: ${e.loggableText()}" }
        return null
    }

    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    // A bounds only decode reports raw dimensions, unlike a full one which rotates as it decodes.
    return Dimensions(
        width = bounds.outWidth,
        height = bounds.outHeight,
    )
        .orientedBy(exifOrientationOrNull())
}

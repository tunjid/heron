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

import com.tunjid.heron.data.logging.logcat
import com.tunjid.heron.data.logging.loggableText
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import java.io.File
import javax.imageio.ImageIO

internal actual fun PlatformFile.imageDimensionsOrNull(): Dimensions? {
    val dimensions = try {
        ImageIO.createImageInputStream(File(absolutePath()))
            ?.use { stream ->
                val readers = ImageIO.getImageReaders(stream)
                if (!readers.hasNext()) return@use null

                val reader = readers.next()
                try {
                    reader.input = stream
                    Dimensions(
                        width = reader.getWidth(0),
                        height = reader.getHeight(0),
                    )
                } finally {
                    reader.dispose()
                }
            }
    } catch (e: Exception) {
        logcat { "Error reading image bounds: ${e.loggableText()}" }
        null
    }
        ?: return null

    // ImageIO reports raw dimensions; it does not apply orientation.
    return dimensions.orientedBy(exifOrientationOrNull())
}

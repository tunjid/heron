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
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MetadataDesktopTest {

    @Test
    fun readsDimensionsOfAnImageWithoutOrientation() {
        val file = jpegFile(
            width = 40,
            height = 20,
        )

        assertEquals(
            expected = Dimensions(
                width = 40,
                height = 20,
            ),
            actual = PlatformFile(file.absolutePath).imageDimensionsOrNull(),
        )
    }

    @Test
    fun transposesDimensionsOfAnImageShotRotated() {
        val file = jpegFile(
            width = 40,
            height = 20,
            orientation = 6,
        )

        assertEquals(
            expected = Dimensions(
                width = 20,
                height = 40,
            ),
            actual = PlatformFile(file.absolutePath).imageDimensionsOrNull(),
        )
    }

    @Test
    fun leavesDimensionsOfAnUprightImageAlone() {
        val file = jpegFile(
            width = 40,
            height = 20,
            orientation = 1,
        )

        assertEquals(
            expected = Dimensions(
                width = 40,
                height = 20,
            ),
            actual = PlatformFile(file.absolutePath).imageDimensionsOrNull(),
        )
    }

    @Test
    fun readsNoDimensionsFromAFileThatIsNotAnImage() {
        val file = File.createTempFile("heron-not-an-image", ".jpg").apply {
            deleteOnExit()
            writeText("not an image")
        }

        assertNull(PlatformFile(file.absolutePath).imageDimensionsOrNull())
    }
}

/**
 * Writes a JPEG of the given size to a temporary file, splicing in an EXIF segment declaring
 * [orientation] when one is given.
 */
private fun jpegFile(
    width: Int,
    height: Int,
    orientation: Int? = null,
): File {
    val encoded = File.createTempFile("heron-image-dimensions", ".jpg").apply { deleteOnExit() }
    ImageIO.write(
        BufferedImage(width, height, BufferedImage.TYPE_INT_RGB),
        "jpeg",
        encoded,
    )
    if (orientation == null) return encoded

    // The EXIF segment goes directly after the two byte start of image marker.
    val bytes = encoded.readBytes()
    encoded.writeBytes(
        bytes.copyOfRange(0, 2) + exifSegment(orientation) + bytes.copyOfRange(2, bytes.size),
    )
    return encoded
}

private fun exifSegment(
    orientation: Int,
): ByteArray {
    // The EXIF identifier, then a big endian TIFF header pointing at the directory after it.
    val payload = buildList {
        addAll(listOf(0x45, 0x78, 0x69, 0x66, 0x00, 0x00))
        addAll(listOf(0x4D, 0x4D))
        addShort(42)
        addAll(listOf(0x00, 0x00, 0x00, 0x08))
        // A directory of one entry: orientation, a single inlined SHORT.
        addShort(1)
        addShort(0x0112)
        addShort(3)
        addAll(listOf(0x00, 0x00, 0x00, 0x01))
        addShort(orientation)
        addShort(0)
    }

    return buildList {
        addAll(listOf(0xFF, 0xE1))
        // A segment declares a size that covers the two bytes the size itself occupies.
        addShort(payload.size + 2)
        addAll(payload)
    }
        .let { bytes -> ByteArray(bytes.size) { bytes[it].toByte() } }
}

private fun MutableList<Int>.addShort(
    value: Int,
) {
    add((value shr 8) and 0xFF)
    add(value and 0xFF)
}

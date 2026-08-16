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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.io.Buffer
import kotlinx.io.Source

class MetadataTest {

    // region JPEG

    @Test
    fun readsBigEndianOrientation() {
        assertEquals(
            expected = 6,
            actual = jpeg(app1 = exifPayload(orientation = 6, littleEndian = false))
                .exifOrientationOrNull(),
        )
    }

    @Test
    fun readsLittleEndianOrientation() {
        assertEquals(
            expected = 8,
            actual = jpeg(app1 = exifPayload(orientation = 8, littleEndian = true))
                .exifOrientationOrNull(),
        )
    }

    @Test
    fun readsOrientationPastAnEarlierSegment() {
        assertEquals(
            expected = 3,
            actual = jpeg(
                // A JFIF APP0 segment precedes the EXIF payload in plenty of real files.
                segments = listOf(0xE0 to ByteArray(size = 14)),
                app1 = exifPayload(orientation = 3, littleEndian = false),
            )
                .exifOrientationOrNull(),
        )
    }

    @Test
    fun skipsApp1PayloadsThatAreNotExif() {
        assertEquals(
            expected = 6,
            actual = jpeg(
                // XMP claims APP1 too, so a miss must keep the walk going.
                segments = listOf(0xE1 to "http://ns.adobe.com/xap/1.0/".encodeToByteArray()),
                app1 = exifPayload(orientation = 6, littleEndian = false),
            )
                .exifOrientationOrNull(),
        )
    }

    @Test
    fun readsNoOrientationWhenThereIsNoOrientationTag() {
        assertNull(
            jpeg(app1 = exifPayload(orientation = null, littleEndian = false))
                .exifOrientationOrNull(),
        )
    }

    @Test
    fun readsNoOrientationWhenThereIsNoExifSegment() {
        assertNull(
            jpeg(segments = listOf(0xE0 to ByteArray(size = 14)))
                .exifOrientationOrNull(),
        )
    }

    @Test
    fun stopsAtTheStartOfScan() {
        // Anything past the scan is pixel data that must not be walked as though it were segments.
        assertNull(
            jpeg(
                segments = listOf(0xDA to ByteArray(size = 10)),
                app1 = exifPayload(orientation = 6, littleEndian = false),
            )
                .exifOrientationOrNull(),
        )
    }

    @Test
    fun readsNoOrientationFromInputThatIsNotAJpeg() {
        assertNull(
            byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
                .source()
                .exifOrientationOrNull(),
        )
    }

    @Test
    fun readsNoOrientationFromATruncatedSegment() {
        val complete = jpegBytes(app1 = exifPayload(orientation = 6, littleEndian = false))

        assertNull(
            complete.copyOf(newSize = complete.size - 8)
                .source()
                .exifOrientationOrNull(),
        )
    }

    @Test
    fun readsNoOrientationFromAnEmptySource() {
        assertNull(
            ByteArray(size = 0)
                .source()
                .exifOrientationOrNull(),
        )
    }

    // endregion

    // region ISO base media

    @Test
    fun readsDimensionsOfAnUprightTrack() {
        assertEquals(
            expected = Dimensions(
                width = 1920,
                height = 1080,
            ),
            actual = isoBaseMedia(
                tracks = listOf(
                    videoTrack(
                        width = 1920,
                        height = 1080,
                    ),
                ),
            )
                .videoDimensions(),
        )
    }

    @Test
    fun transposesDimensionsOfATrackRecordedSideways() {
        assertEquals(
            expected = Dimensions(
                width = 1080,
                height = 1920,
            ),
            actual = isoBaseMedia(
                tracks = listOf(
                    videoTrack(
                        width = 1920,
                        height = 1080,
                        matrix = QuarterTurnMatrix,
                    ),
                ),
            )
                .videoDimensions(),
        )
    }

    @Test
    fun readsDimensionsOfAVersionOneTrackHeader() {
        assertEquals(
            expected = Dimensions(
                width = 640,
                height = 480,
            ),
            actual = isoBaseMedia(
                tracks = listOf(
                    videoTrack(
                        width = 640,
                        height = 480,
                        version = 1,
                    ),
                ),
            )
                .videoDimensions(),
        )
    }

    @Test
    fun skipsTheAudioTrackForTheVideoOne() {
        assertEquals(
            expected = Dimensions(
                width = 720,
                height = 1280,
            ),
            actual = isoBaseMedia(
                // An audio track declares a display size of zero, and typically comes first.
                tracks = listOf(
                    videoTrack(
                        width = 0,
                        height = 0,
                    ),
                    videoTrack(
                        width = 720,
                        height = 1280,
                    ),
                ),
            )
                .videoDimensions(),
        )
    }

    @Test
    fun findsTheMovieBoxPastTheSampleData() {
        assertEquals(
            expected = Dimensions(
                width = 320,
                height = 240,
            ),
            actual = isoBaseMedia(
                // Sample data ahead of the metadata is the common layout for a recorded file.
                leadingBoxes = listOf(box(type = "mdat", payload = ByteArray(size = 4096))),
                tracks = listOf(
                    videoTrack(
                        width = 320,
                        height = 240,
                    ),
                ),
            )
                .videoDimensions(),
        )
    }

    @Test
    fun readsDimensionsPastALargeBox() {
        assertEquals(
            expected = Dimensions(
                width = 100,
                height = 200,
            ),
            actual = isoBaseMedia(
                leadingBoxes = listOf(largeBox(type = "mdat", payload = ByteArray(size = 64))),
                tracks = listOf(
                    videoTrack(
                        width = 100,
                        height = 200,
                    ),
                ),
            )
                .videoDimensions(),
        )
    }

    @Test
    fun roundsFractionalDimensions() {
        assertEquals(
            expected = Dimensions(
                width = 641,
                height = 480,
            ),
            actual = isoBaseMedia(
                tracks = listOf(
                    videoTrack(
                        width = 640,
                        widthFraction = 0x8000,
                        height = 480,
                    ),
                ),
            )
                .videoDimensions(),
        )
    }

    @Test
    fun readsNoDimensionsWithoutAMovieBox() {
        assertNull(
            bytes(listOf(box(type = "ftyp", payload = "isom".encodeToByteArray())))
                .videoDimensions(),
        )
    }

    @Test
    fun readsNoDimensionsWithoutAVideoTrack() {
        assertNull(
            isoBaseMedia(
                tracks = listOf(
                    videoTrack(
                        width = 0,
                        height = 0,
                    ),
                ),
            )
                .videoDimensions(),
        )
    }

    @Test
    fun readsNoDimensionsFromATruncatedFile() {
        val complete = isoBaseMedia(
            tracks = listOf(
                videoTrack(
                    width = 1920,
                    height = 1080,
                ),
            ),
        )

        assertNull(complete.copyOf(newSize = complete.size - 16).videoDimensions())
    }

    @Test
    fun readsNoDimensionsFromSomethingElseEntirely() {
        assertNull("not a video at all".encodeToByteArray().videoDimensions())
    }

    @Test
    fun recognisesIsoBaseMedia() {
        assertTrue(
            isoBaseMedia(
                tracks = listOf(
                    videoTrack(
                        width = 16,
                        height = 9,
                    ),
                ),
            )
                .isIsoBaseMedia(),
        )
    }

    @Test
    fun recognisesAQuickTimeFileLeadingWithItsMovie() {
        assertTrue(
            bytes(listOf(box(type = "moov", payload = ByteArray(size = 8))))
                .isIsoBaseMedia(),
        )
    }

    @Test
    fun recognisesAMovLeadingWithFillerBoxes() {
        // QuickTime .mov files often place wide/free/skip/pnot ahead of the ftyp or moov.
        assertTrue(
            bytes(
                listOf(
                    box(type = "wide", payload = ByteArray(size = 0)),
                    box(type = "free", payload = ByteArray(size = 8)),
                    box(type = "ftyp", payload = "qt  ".encodeToByteArray()),
                ),
            )
                .isIsoBaseMedia(),
        )
    }

    @Test
    fun rejectsAContainerThatIsNotIsoBaseMedia() {
        // A Matroska header, which is what a WebM pick would lead with.
        assertFalse(
            byteArrayOf(0x1A, 0x45.toByte(), 0xDF.toByte(), 0xA3.toByte(), 0x01, 0x00, 0x00, 0x00)
                .isIsoBaseMedia(),
        )
    }

    @Test
    fun rejectsMediaDataBeforeAnyMarker() {
        // The huge mdat is never stepped over, so a file hiding its moov behind it is rejected.
        assertFalse(
            bytes(listOf(box(type = "mdat", payload = ByteArray(size = 64))))
                .isIsoBaseMedia(),
        )
    }

    @Test
    fun rejectsAnEmptyFile() {
        assertFalse(ByteArray(size = 0).isIsoBaseMedia())
    }

    // endregion
}

private fun ByteArray.videoDimensions(): Dimensions? = source().use(Source::videoDimensionsOrNull)

private fun ByteArray.isIsoBaseMedia(): Boolean = source().use(Source::isIsoBaseMedia)

private fun ByteArray.source(): Source = Buffer().apply { write(this@source) }

// region JPEG builders

/**
 * Builds a JPEG stream: a start of image marker, then [segments] and an EXIF [app1] payload if
 * given, in that order.
 */
private fun jpeg(
    segments: List<Pair<Int, ByteArray>> = emptyList(),
    app1: ByteArray? = null,
): Source = jpegBytes(
    segments = segments,
    app1 = app1,
)
    .source()

private fun jpegBytes(
    segments: List<Pair<Int, ByteArray>> = emptyList(),
    app1: ByteArray? = null,
): ByteArray = buildList {
    add(0xFF)
    add(0xD8)
    segments.forEach { (marker, payload) -> addSegment(marker, payload) }
    if (app1 != null) addSegment(marker = 0xE1, payload = app1)
}
    .toByteArray()

private fun MutableList<Int>.addSegment(
    marker: Int,
    payload: ByteArray,
) {
    add(0xFF)
    add(marker)
    // A segment declares a size that covers the two bytes the size itself occupies.
    val size = payload.size + 2
    add((size shr 8) and 0xFF)
    add(size and 0xFF)
    payload.forEach { add(it.toInt() and 0xFF) }
}

/**
 * Builds an APP1 payload: the EXIF identifier, a TIFF header, and a directory holding the
 * [orientation] tag when one is given.
 */
private fun exifPayload(
    orientation: Int?,
    littleEndian: Boolean,
): ByteArray = buildList {
    fun addShort(value: Int) {
        val bytes = listOf((value shr 8) and 0xFF, value and 0xFF)
        addAll(if (littleEndian) bytes.reversed() else bytes)
    }

    fun addInt(value: Int) {
        val bytes = listOf(
            (value shr 24) and 0xFF,
            (value shr 16) and 0xFF,
            (value shr 8) and 0xFF,
            value and 0xFF,
        )
        addAll(if (littleEndian) bytes.reversed() else bytes)
    }

    addAll(listOf(0x45, 0x78, 0x69, 0x66, 0x00, 0x00))
    addAll(if (littleEndian) listOf(0x49, 0x49) else listOf(0x4D, 0x4D))
    addShort(42)
    // The directory follows the eight byte TIFF header it is offset from.
    addInt(8)

    if (orientation == null) {
        addShort(0)
        return@buildList
    }

    addShort(1)
    addShort(0x0112)
    // A single value of type SHORT, inlined in the entry and padded out to four bytes.
    addShort(3)
    addInt(1)
    addShort(orientation)
    addShort(0)
}
    .toByteArray()

// endregion

// region ISO base media builders

/**
 * Builds an ISO base media file: a file type box, then [leadingBoxes], then a movie box holding
 * [tracks].
 */
private fun isoBaseMedia(
    leadingBoxes: List<ByteArray> = emptyList(),
    tracks: List<ByteArray>,
): ByteArray = bytes(
    buildList {
        add(box(type = "ftyp", payload = "isom".encodeToByteArray()))
        addAll(leadingBoxes)
        add(box(type = "moov", payload = bytes(tracks)))
    },
)

/**
 * Builds a track box holding a single track header. A [width] and [height] of zero is how a
 * track with nothing to display, an audio one for instance, declares itself.
 */
private fun videoTrack(
    width: Int,
    height: Int,
    widthFraction: Int = 0,
    version: Int = 0,
    matrix: List<Long> = IdentityMatrix,
): ByteArray = box(
    type = "trak",
    payload = box(
        type = "tkhd",
        payload = buildList {
            add(version)
            // Flags.
            repeat(3) { add(0) }
            // Creation and modification times, the track id, a reserved word and the duration.
            // A version one header widens both timestamps and the duration.
            val timestampSize = if (version == 1) Long.SIZE_BYTES else Int.SIZE_BYTES
            repeat((timestampSize * 3) + (Int.SIZE_BYTES * 2)) { add(0) }
            // Two reserved words, then layer, alternate group, volume and one more reserved short.
            repeat(16) { add(0) }
            matrix.forEach(::addUInt)
            addUInt((width.toLong() shl 16) or widthFraction.toLong())
            addUInt(height.toLong() shl 16)
        }
            .toByteArray(),
    ),
)

private fun box(
    type: String,
    payload: ByteArray,
): ByteArray = bytes(
    listOf(
        buildList { addUInt((BoxHeaderSize + payload.size).toLong()) }.toByteArray(),
        type.encodeToByteArray(),
        payload,
    ),
)

/** A box declaring its size as a 64 bit value after its type, as an oversized one must. */
private fun largeBox(
    type: String,
    payload: ByteArray,
): ByteArray = bytes(
    listOf(
        buildList { addUInt(1) }.toByteArray(),
        type.encodeToByteArray(),
        buildList {
            addUInt(0)
            addUInt((LargeBoxHeaderSize + payload.size).toLong())
        }
            .toByteArray(),
        payload,
    ),
)

private fun MutableList<Int>.addUInt(
    value: Long,
) {
    add(((value shr 24) and 0xFF).toInt())
    add(((value shr 16) and 0xFF).toInt())
    add(((value shr 8) and 0xFF).toInt())
    add((value and 0xFF).toInt())
}

// endregion

private fun bytes(parts: List<ByteArray>): ByteArray {
    val joined = ByteArray(parts.sumOf(ByteArray::size))
    var offset = 0
    parts.forEach { part ->
        part.copyInto(joined, offset)
        offset += part.size
    }
    return joined
}

private fun List<Int>.toByteArray() = ByteArray(size) { this[it].toByte() }

private const val BoxHeaderSize = 8
private const val LargeBoxHeaderSize = 16

private const val FixedPointOne = 0x00010000L
private const val FixedPointNegativeOne = 0xFFFF0000L
private const val MatrixW = 0x40000000L

private val IdentityMatrix = listOf(
    FixedPointOne, 0L, 0L,
    0L, FixedPointOne, 0L,
    0L, 0L, MatrixW,
)

/** The matrix a phone writes when it was held sideways. */
private val QuarterTurnMatrix = listOf(
    0L, FixedPointOne, 0L,
    FixedPointNegativeOne, 0L, 0L,
    0L, 0L, MatrixW,
)

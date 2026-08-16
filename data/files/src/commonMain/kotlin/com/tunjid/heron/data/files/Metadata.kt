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
import io.github.vinceglb.filekit.source
import kotlinx.io.EOFException
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.readByteArray

internal data class Dimensions(
    val width: Int,
    val height: Int,
)

internal expect fun PlatformFile.imageDimensionsOrNull(): Dimensions?

internal fun PlatformFile.videoDimensionsOrNull(): Dimensions? =
    try {
        source().buffered()
            .use(Source::videoDimensionsOrNull)
    } catch (_: Exception) {
        null
    }

internal fun PlatformFile.isIsoBaseMedia(): Boolean =
    try {
        source().buffered()
            .use(Source::isIsoBaseMedia)
    } catch (_: Exception) {
        false
    }

internal fun PlatformFile.exifOrientationOrNull(): Int? =
    try {
        source().buffered()
            .use(Source::exifOrientationOrNull)
    } catch (_: Exception) {
        null
    }

internal fun Dimensions.orientedBy(
    orientation: Int?,
): Dimensions =
    if (orientation != null && orientation in TransposingOrientations) transposed()
    else this

private fun Dimensions.transposed() = Dimensions(
    width = height,
    height = width,
)

// region JPEG

internal fun Source.exifOrientationOrNull(): Int? {
    if (readUByteOrNull() != JpegMarkerPrefix) return null
    if (readUByteOrNull() != JpegStartOfImage) return null

    while (true) {
        if (readUByteOrNull() != JpegMarkerPrefix) return null

        // A marker may be padded with any number of leading fill bytes.
        var marker = readUByteOrNull() ?: return null
        while (marker == JpegMarkerPrefix) marker = readUByteOrNull() ?: return null

        // Metadata segments all precede the scan, so there is nothing left to find past it.
        if (marker == JpegStartOfScan || marker == JpegEndOfImage) return null

        if (marker in JpegStandaloneMarkers) continue

        // The declared size covers the two bytes the size itself occupies.
        val payloadSize = (readUShortOrNull() ?: return null) - 2
        if (payloadSize < 0) return null

        if (marker != JpegApp1) {
            skipOrNull(payloadSize.toLong()) ?: return null
            continue
        }

        // APP1 also carries non EXIF payloads, XMP among them, so keep walking on a miss.
        val payload = readByteArrayOrNull(payloadSize) ?: return null
        payload.exifOrientationOrNull()?.let { return it }
    }
}

private fun ByteArray.exifOrientationOrNull(): Int? {
    if (!startsWithExifIdentifier()) return null

    // Every offset in the structure is relative to the start of the TIFF header.
    val tiff = ExifIdentifier.size
    val littleEndian = when (uShortAt(tiff)) {
        TiffLittleEndianMark -> true
        TiffBigEndianMark -> false
        else -> return null
    }
    val magic = uShortAt(
        index = tiff + TiffMagicOffset,
        littleEndian = littleEndian,
    )
    if (magic != TiffMagic) return null

    val directoryOffset = uIntAt(
        index = tiff + TiffDirectoryOffset,
        littleEndian = littleEndian,
    )
        ?.takeIf { it <= size }
        ?.toInt()
        ?: return null
    val directory = tiff + directoryOffset
    val entryCount = uShortAt(
        index = directory,
        littleEndian = littleEndian,
    ) ?: return null
    val firstEntry = directory + Short.SIZE_BYTES

    for (entry in 0 until entryCount) {
        val offset = firstEntry + (entry * TiffEntrySize)
        val tag = uShortAt(
            index = offset,
            littleEndian = littleEndian,
        )
        if (tag != TiffOrientationTag) continue
        // Orientation is a lone SHORT, small enough to be inlined in the entry's value field.
        return uShortAt(
            index = offset + TiffEntryValueOffset,
            littleEndian = littleEndian,
        )
    }
    return null
}

private fun ByteArray.startsWithExifIdentifier(): Boolean =
    size >= ExifIdentifier.size && ExifIdentifier.indices.all { this[it] == ExifIdentifier[it] }

// endregion

// region ISO base media

internal fun Source.isIsoBaseMedia(): Boolean {
    // A conformant MP4 leads with a file type box, but QuickTime .mov files often place small
    // filler boxes (free/skip/wide/pnot) ahead of the ftyp or moov. Step over those to reach the
    // marker, stopping at the media data or any other box so the whole file is never read.
    repeat(MaxLeadingBoxesInspected) {
        val header = readBoxHeaderOrNull() ?: return false
        when (header.type) {
            FileTypeBox, MovieBox -> return true
            in LeadingFillerBoxes -> {
                val payloadSize = header.payloadSize ?: return false
                if (payloadSize > MaxLeadingFillerBoxSize) return false
                skipOrNull(payloadSize) ?: return false
            }
            else -> return false
        }
    }
    return false
}

internal fun Source.videoDimensionsOrNull(): Dimensions? {
    while (true) {
        val header = readBoxHeaderOrNull() ?: return null
        // A box running to the end of the input has nothing after it to seek to.
        val payloadSize = header.payloadSize ?: return null

        if (header.type != MovieBox) {
            skipOrNull(payloadSize) ?: return null
            continue
        }
        if (payloadSize > MaxMovieBoxSize) return null

        return readByteArrayOrNull(payloadSize.toInt())
            ?.videoTrackDimensionsOrNull()
    }
}

private fun Source.readBoxHeaderOrNull(): BoxHeader? {
    val declaredSize = readUIntOrNull() ?: return null
    val type = readByteArrayOrNull(BoxTypeSize)?.decodeToString() ?: return null

    return when (declaredSize) {
        BoxSizeToEndOfInput -> BoxHeader(
            type = type,
            payloadSize = null,
        )
        BoxSizeIsLarge -> readLongOrNull()
            ?.takeIf { it >= LargeBoxHeaderSize }
            ?.let { largeSize ->
                BoxHeader(
                    type = type,
                    payloadSize = largeSize - LargeBoxHeaderSize,
                )
            }
        else ->
            declaredSize
                .takeIf { it >= BoxHeaderSize }
                ?.let { boxSize ->
                    BoxHeader(
                        type = type,
                        payloadSize = boxSize - BoxHeaderSize,
                    )
                }
    }
}

private class BoxHeader(
    val type: String,
    val payloadSize: Long?,
)

private class BoxPayload(
    val start: Int,
    val end: Int,
)

private fun ByteArray.videoTrackDimensionsOrNull(): Dimensions? =
    boxPayloads(
        type = TrackBox,
        start = 0,
        end = size,
    )
        .firstNotNullOfOrNull { track ->
            boxPayloads(
                type = TrackHeaderBox,
                start = track.start,
                end = track.end,
            )
                .firstNotNullOfOrNull(::trackHeaderDimensionsOrNull)
        }

private fun ByteArray.trackHeaderDimensionsOrNull(
    header: BoxPayload,
): Dimensions? {
    if (header.start >= size) return null

    // A version one header widens its timestamps, pushing everything after them back.
    val timestampsSize = when (this[header.start].toInt() and 0xFF) {
        TrackHeaderVersionOne -> TrackHeaderVersionOneTimestampsSize
        else -> TrackHeaderVersionZeroTimestampsSize
    }
    val matrix = header.start +
        TrackHeaderVersionAndFlagsSize +
        timestampsSize +
        TrackHeaderLayerAndVolumeSize

    val width = fixedPointAt(matrix + TrackHeaderMatrixSize) ?: return null
    val height = fixedPointAt(matrix + TrackHeaderMatrixSize + Int.SIZE_BYTES) ?: return null
    if (width <= 0 || height <= 0) return null

    val dimensions = Dimensions(
        width = width,
        height = height,
    )
    return if (isQuarterTurn(matrix)) dimensions.transposed() else dimensions
}

private fun ByteArray.isQuarterTurn(
    matrix: Int,
): Boolean {
    val scaleX = uIntAt(matrix) ?: return false
    val shearX = uIntAt(matrix + Int.SIZE_BYTES) ?: return false
    val shearY = uIntAt(matrix + (Int.SIZE_BYTES * 3)) ?: return false
    val scaleY = uIntAt(matrix + (Int.SIZE_BYTES * 4)) ?: return false

    return scaleX == 0L && scaleY == 0L && (shearX != 0L || shearY != 0L)
}

private fun ByteArray.boxPayloads(
    type: String,
    start: Int,
    end: Int,
): List<BoxPayload> {
    val payloads = mutableListOf<BoxPayload>()
    var offset = start

    while (offset + BoxHeaderSize <= end) {
        val declaredSize = uIntAt(offset) ?: break
        val boxType = stringAt(
            index = offset + Int.SIZE_BYTES,
            length = BoxTypeSize,
        ) ?: break

        val headerSize = if (declaredSize == BoxSizeIsLarge) LargeBoxHeaderSize else BoxHeaderSize
        val boxSize = when (declaredSize) {
            BoxSizeToEndOfInput -> (end - offset).toLong()
            // Only the low word is usable; a box larger than four gigabytes is not one of ours.
            BoxSizeIsLarge -> when (uIntAt(offset + BoxHeaderSize)) {
                0L -> uIntAt(offset + BoxHeaderSize + Int.SIZE_BYTES) ?: break
                else -> break
            }
            else -> declaredSize
        }

        if (boxSize < headerSize) break
        val boxEnd = offset + boxSize
        if (boxEnd > end) break

        if (boxType == type) payloads.add(
            BoxPayload(
                start = offset + headerSize,
                end = boxEnd.toInt(),
            ),
        )
        offset = boxEnd.toInt()
    }

    return payloads
}

/**
 * Reads a 16.16 fixed point number, rounded to the nearest whole number.
 */
private fun ByteArray.fixedPointAt(
    index: Int,
): Int? = uIntAt(index)
    ?.let { fixed -> ((fixed + FixedPointHalf) shr FixedPointBits).toInt() }

// endregion

// region Byte reading

private fun Source.readUByteOrNull(): Int? =
    if (request(Byte.SIZE_BYTES.toLong())) readByte().toInt() and 0xFF
    else null

private fun Source.readUShortOrNull(): Int? =
    if (request(Short.SIZE_BYTES.toLong())) readShort().toInt() and 0xFFFF
    else null

private fun Source.readUIntOrNull(): Long? =
    if (request(Int.SIZE_BYTES.toLong())) readInt().toLong() and 0xFFFFFFFFL
    else null

private fun Source.readLongOrNull(): Long? =
    if (request(Long.SIZE_BYTES.toLong())) readLong()
    else null

private fun Source.readByteArrayOrNull(
    byteCount: Int,
): ByteArray? =
    if (byteCount >= 0 && request(byteCount.toLong())) readByteArray(byteCount)
    else null

private fun Source.skipOrNull(
    byteCount: Long,
): Unit? =
    if (byteCount < 0) null
    // skip discards in bounded segments; request(byteCount) would first buffer the whole span
    // into memory, which OOMs when skipping a large box such as an mdat ahead of the moov.
    else try {
        skip(byteCount)
    } catch (_: EOFException) {
        null
    }

private fun ByteArray.uShortAt(
    index: Int,
    littleEndian: Boolean = false,
): Int? {
    if (index < 0 || index + Short.SIZE_BYTES > size) return null
    val first = this[index].toInt() and 0xFF
    val second = this[index + 1].toInt() and 0xFF
    return if (littleEndian) (second shl 8) or first
    else (first shl 8) or second
}

private fun ByteArray.uIntAt(
    index: Int,
    littleEndian: Boolean = false,
): Long? {
    if (index < 0 || index + Int.SIZE_BYTES > size) return null
    val first = uShortAt(
        index = index,
        littleEndian = littleEndian,
    ) ?: return null
    val second = uShortAt(
        index = index + Short.SIZE_BYTES,
        littleEndian = littleEndian,
    ) ?: return null
    return if (littleEndian) (second.toLong() shl 16) or first.toLong()
    else (first.toLong() shl 16) or second.toLong()
}

private fun ByteArray.stringAt(
    index: Int,
    length: Int,
): String? =
    if (index < 0 || index + length > size) null
    else decodeToString(index, index + length)

// endregion

private const val JpegMarkerPrefix = 0xFF
private const val JpegStartOfImage = 0xD8
private const val JpegEndOfImage = 0xD9
private const val JpegStartOfScan = 0xDA
private const val JpegApp1 = 0xE1

private val JpegStandaloneMarkers = setOf(0x01, 0xD0, 0xD1, 0xD2, 0xD3, 0xD4, 0xD5, 0xD6, 0xD7)

private val ExifIdentifier = byteArrayOf(0x45, 0x78, 0x69, 0x66, 0x00, 0x00)

private const val TiffLittleEndianMark = 0x4949
private const val TiffBigEndianMark = 0x4D4D
private const val TiffMagic = 42
private const val TiffMagicOffset = 2
private const val TiffDirectoryOffset = 4
private const val TiffEntrySize = 12
private const val TiffEntryValueOffset = 8
private const val TiffOrientationTag = 0x0112

private val TransposingOrientations = 5..8

private const val FileTypeBox = "ftyp"
private const val MovieBox = "moov"
private const val TrackBox = "trak"
private const val TrackHeaderBox = "tkhd"

private val LeadingFillerBoxes = setOf("free", "skip", "wide", "pnot")

private const val MaxLeadingBoxesInspected = 8

private const val MaxLeadingFillerBoxSize = 1L * 1024 * 1024

private const val BoxTypeSize = 4
private const val BoxHeaderSize = 8
private const val LargeBoxHeaderSize = 16

/** A declared size of zero means the box runs to the end of the input. */
private const val BoxSizeToEndOfInput = 0L

/** A declared size of one means the real size follows the type, as a 64 bit value. */
private const val BoxSizeIsLarge = 1L

/**
 * A ceiling on the movie box, which holds metadata only. Real ones run to tens of kilobytes;
 * this exists so a malformed declaration cannot ask for an enormous allocation.
 */
private const val MaxMovieBoxSize = 8L * 1024 * 1024

private const val TrackHeaderVersionOne = 1
private const val TrackHeaderVersionAndFlagsSize = 4
private const val TrackHeaderVersionZeroTimestampsSize = 20
private const val TrackHeaderVersionOneTimestampsSize = 32

/** Two reserved words, then layer, alternate group, volume, and one more reserved short. */
private const val TrackHeaderLayerAndVolumeSize = 16

private const val TrackHeaderMatrixSize = 36

private const val FixedPointBits = 16
private const val FixedPointHalf = 1L shl (FixedPointBits - 1)

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

import com.tunjid.heron.data.core.types.FileUri
import com.tunjid.heron.data.core.utilities.File
import com.tunjid.heron.data.logging.logcat
import com.tunjid.heron.data.logging.loggableText
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.ImageFormat
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.compressImage
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.nameWithoutExtension
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import io.github.vinceglb.filekit.source
import io.github.vinceglb.filekit.write
import kotlin.time.Clock
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.Source
import kotlinx.io.buffered
import okio.FileSystem

internal class OkioFileKitFileManager(
    override val fileSystem: FileSystem,
) : FileManager {

    init {
        initializeFileKit()
    }

    private val mutations = MutableSharedFlow<File.System>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val fileMutations: Flow<File.System> = mutations.asSharedFlow()

    override suspend fun cacheWithoutRestrictions(
        restrictedFile: RestrictedFile,
    ): File.Media? {
        var cachedFile: PlatformFile? = null

        try {
            if (!UnrestrictedCachedFilesDir.exists()) {
                UnrestrictedCachedFilesDir.createDirectories(mustCreate = true)
            }

            // Only photos are compressed; videos are cached verbatim. Oversized photos
            // are re-encoded as JPEG so uploads stay within the server's blob size limit.
            val compressedBytes = when (restrictedFile) {
                is RestrictedFile.Media.Photo -> restrictedFile.compressedImageBytesOrNull()
                is RestrictedFile.Media.Video -> null
            }

            val epoch = Clock.System.now().toEpochMilliseconds()
            val extension =
                if (compressedBytes == null) restrictedFile.file.extension
                else COMPRESSED_IMAGE_EXTENSION
            val bookmarkFileName =
                "${restrictedFile.file.nameWithoutExtension}-$epoch.$extension"

            cachedFile = UnrestrictedCachedFilesDir / bookmarkFileName
            if (compressedBytes == null) cachedFile.write(restrictedFile.file)
            else cachedFile.write(compressedBytes)
        } catch (e: Exception) {
            logcat { "Error caching file: ${e.loggableText()}" }
            cachedFile
                ?.takeIf(PlatformFile::exists)
                ?.safeDelete()
            cachedFile = null
        }

        return cachedFile?.let {
            val uri = FileUri(it.path)
            when (restrictedFile) {
                is RestrictedFile.Media.Photo -> File.Media.Photo(
                    uri = uri,
                    width = restrictedFile.width,
                    height = restrictedFile.height,
                    altText = restrictedFile.altText,
                )
                is RestrictedFile.Media.Video -> File.Media.Video(
                    uri = uri,
                    width = restrictedFile.width,
                    height = restrictedFile.height,
                    altText = restrictedFile.altText,
                )
            }
        }
    }

    override suspend fun source(
        file: File,
    ): Source = when (file) {
        is File.System -> fileSystem.source(file.path)
            .asKotlinxIoRawSource()
            .buffered()
        is File.Media -> file.toPlatformFile()
            .source()
            .buffered()
    }

    override suspend fun size(
        file: File,
    ): Long = when (file) {
        is File.System -> fileSystem.metadataOrNull(file.path)
            ?.size
            ?: 0L
        is File.Media -> file.toPlatformFile()
            .size()
    }

    override suspend fun delete(
        file: File,
    ) {
        when (file) {
            is File.System -> {
                fileSystem.delete(
                    path = file.path,
                    mustExist = false,
                )
                mutations.emit(file)
            }
            is File.Media -> file.toPlatformFile()
                .safeDelete()
        }
    }

    override fun exists(
        file: File.System,
    ): Boolean =
        fileSystem.exists(file.path)

    override fun metadataSize(
        file: File.System,
    ): Long? =
        fileSystem.metadataOrNull(file.path)?.size

    override suspend fun createDirectories(
        file: File.System,
    ) {
        fileSystem.createDirectories(file.path)
    }

    override suspend fun atomicMove(
        source: File.System,
        target: File.System,
    ) {
        fileSystem.atomicMove(
            source = source.path,
            target = target.path,
        )
        mutations.emit(target)
    }
}

/**
 * Returns JPEG bytes for an oversized photo shrunk to fit within [MAX_IMAGE_SIZE], or null
 * when compression is unnecessary: the photo is already within the limit, or a compressed
 * pass would not actually be smaller than the original.
 *
 * Encoded size is dominated by pixel area, so the longest edge is capped first; if the
 * result is still too large, JPEG quality is stepped down until it fits or the quality
 * floor ([MIN_IMAGE_QUALITY]) is reached.
 */
private suspend fun RestrictedFile.Media.Photo.compressedImageBytesOrNull(): ByteArray? {
    val originalSize = file.size()
    if (originalSize <= MAX_IMAGE_SIZE) return null

    val originalBytes = file.readBytes()

    suspend fun compressAtQuality(
        quality: Int,
    ): ByteArray = FileKit.compressImage(
        bytes = originalBytes,
        imageFormat = ImageFormat.JPEG,
        quality = quality,
        maxWidth = MAX_IMAGE_EDGE,
        maxHeight = MAX_IMAGE_EDGE,
    )

    var quality = MAX_IMAGE_QUALITY
    var compressed = compressAtQuality(quality)
    while (compressed.size > MAX_IMAGE_SIZE && quality > MIN_IMAGE_QUALITY) {
        quality = maxOf(MIN_IMAGE_QUALITY, quality - IMAGE_QUALITY_STEP)
        compressed = compressAtQuality(quality)
    }

    return compressed.takeIf { it.size < originalSize }
}

private fun File.toPlatformFile() =
    PlatformFile(uri.uri)

private suspend fun PlatformFile.safeDelete() {
    try {
        if (exists()) delete()
    } catch (e: Exception) {
        println("Error deleting file $name with path $path: ${e.message}")
    }
}

private fun okio.Source.asKotlinxIoRawSource(): RawSource {
    val okioSource = this
    val transfer = okio.Buffer()
    return object : RawSource {
        private val tempArray = ByteArray(64 * 1024)

        override fun readAtMostTo(
            sink: Buffer,
            byteCount: Long,
        ): Long {
            if (byteCount == 0L) return 0L
            val toRead = minOf(byteCount, tempArray.size.toLong())
            val read = okioSource.read(transfer, toRead)
            if (read == -1L) return -1L
            transfer.read(tempArray, 0, read.toInt())
            sink.write(tempArray, 0, read.toInt())
            return read
        }

        override fun close() = okioSource.close()
    }
}

internal expect fun initializeFileKit()

private val UnrestrictedCachedFilesDir = FileKit.cacheDir / "unrestricted-files"

/** Photos larger than this (2 MB) are re-encoded before caching. */
private const val MAX_IMAGE_SIZE: Long = 2L * 1024 * 1024

/** Longest-edge cap applied before quality reduction; also Bluesky's display ceiling. */
private const val MAX_IMAGE_EDGE: Int = 2048

/** Starting JPEG quality; the first pass that fits keeps the highest fidelity. */
private const val MAX_IMAGE_QUALITY: Int = 85

/** Quality floor; below this the re-encode is written even if still over the limit. */
private const val MIN_IMAGE_QUALITY: Int = 40

/** Amount quality drops per pass when a result is still over [MAX_IMAGE_SIZE]. */
private const val IMAGE_QUALITY_STEP: Int = 10

private const val COMPRESSED_IMAGE_EXTENSION: String = "jpg"

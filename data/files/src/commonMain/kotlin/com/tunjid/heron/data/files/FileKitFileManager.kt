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
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.nameWithoutExtension
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.size
import io.github.vinceglb.filekit.source
import io.github.vinceglb.filekit.write
import kotlin.time.Clock
import kotlinx.io.Source
import kotlinx.io.buffered

internal class FileKitFileManager : FileManager {

    init {
        initializeFileKit()
    }

    override suspend fun cacheWithoutRestrictions(restrictedFile: RestrictedFile): File? {
        var cachedFile: PlatformFile? = null

        try {
            if (!UnrestrictedCachedFilesDir.exists()) {
                UnrestrictedCachedFilesDir.createDirectories(mustCreate = true)
            }

            val epoch = Clock.System.now().toEpochMilliseconds()
            val bookmarkFileName =
                "${restrictedFile.file.nameWithoutExtension}-$epoch.${restrictedFile.file.extension}"

            cachedFile = UnrestrictedCachedFilesDir / bookmarkFileName
            cachedFile.write(restrictedFile.file)
        } catch (e: Exception) {
            println("Error caching file: ${e.message}")
            cachedFile?.takeIf(PlatformFile::exists)?.safeDelete()
            cachedFile = null
        }

        return cachedFile?.let {
            val uri = FileUri(it.path)
            when (restrictedFile) {
                is RestrictedFile.Media.Photo ->
                    File.Media.Photo(
                        uri = uri,
                        width = restrictedFile.width,
                        height = restrictedFile.height,
                        altText = restrictedFile.altText,
                    )
                is RestrictedFile.Media.Video ->
                    File.Media.Video(
                        uri = uri,
                        width = restrictedFile.width,
                        height = restrictedFile.height,
                        altText = restrictedFile.altText,
                    )
            }
        }
    }

    override suspend fun source(file: File): Source = file.toPlatformFile().source().buffered()

    override suspend fun size(file: File): Long = file.toPlatformFile().size()

    override suspend fun delete(file: File) {
        file.toPlatformFile().safeDelete()
    }
}

private fun File.toPlatformFile() = PlatformFile(uri.uri)

suspend fun PlatformFile.safeDelete() {
    try {
        if (exists()) delete()
    } catch (e: Exception) {
        println("Error deleting file $name with path $path: ${e.message}")
    }
}

internal expect fun initializeFileKit()

private val UnrestrictedCachedFilesDir = FileKit.cacheDir / "unrestricted-files"

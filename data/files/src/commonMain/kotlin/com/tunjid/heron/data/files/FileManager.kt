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

import com.tunjid.heron.data.core.utilities.File
import kotlinx.coroutines.flow.Flow
import kotlinx.io.Source
import okio.FileSystem
import okio.Path

interface FileManager {

    val fileSystem: FileSystem

    val fileMutations: Flow<Path>

    suspend fun cacheWithoutRestrictions(
        restrictedFile: RestrictedFile,
    ): File?

    suspend fun source(
        file: File,
    ): Source

    suspend fun size(
        file: File,
    ): Long

    suspend fun delete(
        file: File,
    )

    fun exists(
        path: Path,
    ): Boolean

    fun metadataSize(
        path: Path,
    ): Long?

    suspend fun createDirectories(
        path: Path,
    )

    suspend fun delete(
        path: Path,
    )

    suspend fun atomicMove(
        source: Path,
        target: Path,
    )
}

fun createFileManager(
    fileSystem: FileSystem,
): FileManager =
    FileKitFileManager(
        fileSystem = fileSystem,
    )

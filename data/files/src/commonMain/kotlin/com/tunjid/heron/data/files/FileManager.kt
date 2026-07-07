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

interface FileManager {

    val fileSystem: FileSystem

    val fileMutations: Flow<File.System>

    suspend fun cacheWithoutRestrictions(
        restrictedFile: RestrictedFile,
    ): File.Media?

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
        file: File.System,
    ): Boolean

    fun metadataSize(
        file: File.System,
    ): Long?

    suspend fun createDirectories(
        file: File.System,
    )

    suspend fun atomicMove(
        source: File.System,
        target: File.System,
    )
}

fun createFileManager(
    fileSystem: FileSystem,
): FileManager =
    OkioFileKitFileManager(
        fileSystem = fileSystem,
    )

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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tunjid.heron.data.files.RestrictedFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher

sealed interface MediaType {
    data object Photo : MediaType

    data object Video : MediaType
}

@Composable
fun rememberMediaPicker(
    mediaType: MediaType,
    maxItems: Int?,
    onItemsPicked: (List<RestrictedFile.Media>) -> Unit,
): () -> Unit {
    val fileKitType =
        when (mediaType) {
            MediaType.Photo -> FileKitType.Image
            MediaType.Video -> FileKitType.Video
        }

    val launcher =
        when (maxItems) {
            1 ->
                rememberFilePickerLauncher(type = fileKitType, mode = FileKitMode.Single) { file ->
                    onItemsPicked(
                        when (mediaType) {
                            MediaType.Photo -> file?.let(RestrictedFile::photo)
                            MediaType.Video -> file?.let(RestrictedFile::video)
                        }?.let(::listOf) ?: emptyList()
                    )
                }
            else ->
                rememberFilePickerLauncher(
                    type = fileKitType,
                    mode = FileKitMode.Multiple(maxItems = maxItems),
                ) { files ->
                    onItemsPicked(
                        when (mediaType) {
                            MediaType.Photo -> files?.map(RestrictedFile::photo)
                            MediaType.Video -> files?.map(RestrictedFile::video)
                        } ?: emptyList()
                    )
                }
        }

    return remember(launcher) { launcher::launch }
}

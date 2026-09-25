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

package com.tunjid.heron.data.utilities.writequeue

import com.tunjid.heron.data.core.types.FileUri
import com.tunjid.heron.data.core.utilities.File
import com.tunjid.heron.data.tasks.Progress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.readByteArray

class WriteProgressTest {

    @Test
    fun weighsUploadsRemoteProcessingAndPublishing() = runTest {
        val writeProgress = WriteProgress()
        writeProgress.append(
            file = Photo,
            sizeInBytes = 100,
            processedRemotely = false,
        )
        writeProgress.append(
            file = Video,
            sizeInBytes = 200,
            processedRemotely = true,
        )
        // 100 + 200 to upload, 200 to process, and 1% of that (5) to publish.
        assertEquals(
            expected = Progress(
                completedBytes = 0,
                totalBytes = 505,
            ),
            actual = writeProgress.updates.first(),
        )

        writeProgress.onUploaded(
            file = Photo,
            bytes = 100,
        )
        writeProgress.onUploaded(
            file = Video,
            bytes = 200,
        )
        writeProgress.onProcessed(
            file = Video,
            percent = 50,
        )
        assertEquals(
            expected = Progress(
                completedBytes = 400,
                totalBytes = 505,
            ),
            actual = writeProgress.updates.first(),
        )

        writeProgress.onProcessed(
            file = Video,
            percent = 100,
        )
        writeProgress.onPublished()
        assertEquals(
            expected = 1f,
            actual = writeProgress.updates.first().fraction,
        )
    }

    @Test
    fun matchesFilesByUri() = runTest {
        val writeProgress = WriteProgress()
        writeProgress.append(
            file = Photo,
            sizeInBytes = 100,
            processedRemotely = false,
        )
        // The same file after its dimensions are read back, as a rehydrated draft is.
        writeProgress.onUploaded(
            file = File.Media.Photo(
                uri = Photo.uri,
                width = 1_080,
                height = 1_920,
            ),
            bytes = 100,
        )
        // A file that was never appended is ignored.
        writeProgress.onUploaded(
            file = Video,
            bytes = 1_000,
        )
        assertEquals(
            expected = Progress(
                completedBytes = 100,
                totalBytes = 101,
            ),
            actual = writeProgress.updates.first(),
        )
    }

    @Test
    fun countsBytesReadFromReportingSource() = runTest {
        val writeProgress = WriteProgress()
        writeProgress.append(
            file = Photo,
            sizeInBytes = 1_000,
            processedRemotely = false,
        )
        withContext(writeProgress) {
            Buffer()
                .apply { write(ByteArray(1_000)) }
                .reportingUploadProgress(file = Photo)
                .readByteArray()
        }
        assertEquals(
            expected = Progress(
                completedBytes = 1_000,
                totalBytes = 1_010,
            ),
            actual = writeProgress.updates.first(),
        )
    }

    @Test
    fun reportingSourceIsUnchangedWithoutWriteProgress() = runTest {
        val source = Buffer()
        assertSame(
            expected = source,
            actual = source.reportingUploadProgress(file = Photo),
        )
    }
}

private val Photo = File.Media.Photo(
    uri = FileUri("file://photo.jpg"),
    width = 0,
    height = 0,
)

private val Video = File.Media.Video(
    uri = FileUri("file://video.mp4"),
    width = 0,
    height = 0,
)
